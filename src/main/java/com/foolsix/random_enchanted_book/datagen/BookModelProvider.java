package com.foolsix.random_enchanted_book.datagen;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.imageio.ImageIO;

import com.foolsix.random_enchanted_book.data.BookPools;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/** Reuses the original sprite; only faces covering its white outline receive a tint. */
public final class BookModelProvider implements DataProvider {
    private final PackOutput.PathProvider models;
    private final ExistingFileHelper files;

    public BookModelProvider(PackOutput output, ExistingFileHelper files) {
        models = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "models/item");
        this.files = files;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        try {
            BufferedImage sprite;
            try (var input = files.getResource(BookPools.id("item/random_enchanted_book"),
                    PackType.CLIENT_RESOURCES, ".png", "textures").open()) {
                sprite = ImageIO.read(input);
            }
            if (sprite == null || sprite.getWidth() != 16 || sprite.getHeight() != 16) {
                throw new IOException("Expected a 16x16 random enchanted book sprite");
            }
            var model = new JsonObject();
            // Keep vanilla item transforms without its automatic, whole-layer tinting.
            try (var reader = files.getResource(ResourceLocation.withDefaultNamespace("item/generated"),
                    PackType.CLIENT_RESOURCES, ".json", "models").openAsReader()) {
                model.add("display", JsonParser.parseReader(reader).getAsJsonObject().get("display"));
            }
            model.addProperty("gui_light", "front");
            model.addProperty("ambientocclusion", false);
            var textures = new JsonObject();
            textures.addProperty("book", BookPools.id("item/random_enchanted_book").toString());
            textures.addProperty("particle", "#book");
            model.add("textures", textures);
            var elements = new JsonArray();
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16;) {
                    if (!opaque(sprite, x, y)) { x++; continue; }
                    boolean tinted = white(sprite, x, y);
                    int end = x + 1;
                    while (end < 16 && opaque(sprite, end, y) && white(sprite, end, y) == tinted) { end++; }
                    var faces = new JsonObject();
                    faces.add("south", face(x, y, end, y + 1, tinted));
                    faces.add("north", face(end, y, x, y + 1, tinted));
                    elements.add(element(x, y, end, faces));
                    x = end;
                }
                for (int x = 0; x < 16; x++) {
                    if (!opaque(sprite, x, y)) { continue; }
                    var faces = new JsonObject();
                    if (!opaque(sprite, x - 1, y)) { faces.add("west", face(x, y, x + 1, y + 1, white(sprite, x, y))); }
                    if (!opaque(sprite, x + 1, y)) { faces.add("east", face(x, y, x + 1, y + 1, white(sprite, x, y))); }
                    if (!opaque(sprite, x, y - 1)) { faces.add("up", face(x, y, x + 1, y + 1, white(sprite, x, y))); }
                    if (!opaque(sprite, x, y + 1)) { faces.add("down", face(x, y, x + 1, y + 1, white(sprite, x, y))); }
                    if (!faces.isEmpty()) { elements.add(element(x, y, x + 1, faces)); }
                }
            }
            model.add("elements", elements);
            List<CompletableFuture<?>> writes = new ArrayList<>();
            writes.add(DataProvider.saveStable(output, model, models.json(BookPools.id("random_enchanted_book_base"))));
            for (String rarity : List.of("common", "uncommon", "rare", "epic")) {
                var child = new JsonObject();
                child.addProperty("parent", BookPools.id("item/random_enchanted_book_base").toString());
                writes.add(DataProvider.saveStable(output, child, models.json(BookPools.id("random_enchanted_book_" + rarity))));
            }
            return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
        } catch (IOException exception) {
            return CompletableFuture.failedFuture(exception);
        }
    }

    private static JsonObject element(int x, int y, int end, JsonObject faces) {
        var element = new JsonObject();
        element.add("from", array(x, 15 - y, 7.5F));
        element.add("to", array(end, 16 - y, 8.5F));
        element.add("faces", faces);
        return element;
    }

    private static JsonObject face(int u1, int v1, int u2, int v2, boolean tinted) {
        var face = new JsonObject();
        face.add("uv", array(u1, v1, u2, v2));
        face.addProperty("texture", "#book");
        if (tinted) { face.addProperty("tintindex", 0); }
        return face;
    }

    private static JsonArray array(float... values) {
        var array = new JsonArray();
        for (float value : values) { array.add(value); }
        return array;
    }

    private static boolean opaque(BufferedImage sprite, int x, int y) {
        return x >= 0 && x < 16 && y >= 0 && y < 16 && (sprite.getRGB(x, y) >>> 24) != 0;
    }

    private static boolean white(BufferedImage sprite, int x, int y) {
        return (sprite.getRGB(x, y) & 0xFFFFFF) == 0xFFFFFF;
    }

    @Override
    public String getName() {
        return "Random enchanted book models";
    }
}
