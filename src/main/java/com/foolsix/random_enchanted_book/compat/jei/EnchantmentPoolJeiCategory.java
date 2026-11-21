package com.foolsix.random_enchanted_book.compat.jei;

import java.util.List;

import com.foolsix.random_enchanted_book.item.ModItemReg;
import com.foolsix.random_enchanted_book.item.RandomEnchantedBookItem;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IScalableDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.gui.widgets.ScrollbarVisibility;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

import static com.foolsix.random_enchanted_book.RandomEnchantedBook.MODID;

final class EnchantmentPoolJeiCategory implements IRecipeCategory<EnchantmentPoolJeiRecipe> {
    static final RecipeType<EnchantmentPoolJeiRecipe> RECIPE_TYPE =
            RecipeType.create(MODID, "possible_enchantments", EnchantmentPoolJeiRecipe.class);

    private static final int WIDTH = 180;
    private static final int HEIGHT = 134;
    private static final int ITEM_SIZE = 16;
    private static final int ITEM_GAP = 6;
    private static final int ITEM_Y = 0;
    private static final int LIST_MARGIN = 3;
    private static final int LIST_TOP = ITEM_Y + ITEM_SIZE + 5;
    private static final int LIST_WIDTH = WIDTH - 2 * LIST_MARGIN;
    private static final int LIST_HEIGHT = HEIGHT - LIST_TOP - LIST_MARGIN;
    private static final int LIST_INSET = 4;
    private final IDrawable icon;
    private final IScalableDrawable listBackground;

    EnchantmentPoolJeiCategory(IGuiHelper gui) {
        icon = gui.createDrawableItemLike(ModItemReg.COMMON_BOOK.get());
        listBackground = gui.createScalableDrawableSprite(
                ResourceLocation.fromNamespaceAndPath("jei", "single_recipe_background_v2"));
    }

    @Override
    public void draw(EnchantmentPoolJeiRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics,
                     double mouseX, double mouseY) {
        listBackground.draw(graphics, LIST_MARGIN, LIST_TOP, LIST_WIDTH, LIST_HEIGHT);
    }

    @Override
    public RecipeType<EnchantmentPoolJeiRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei." + MODID + ".possible_enchantments");
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public boolean needsRecipeBorder() {
        return false;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, EnchantmentPoolJeiRecipe recipe, IFocusGroup focuses) {
        List<ItemStack> books = switch (recipe) {
            case EnchantmentPoolJeiRecipe.Pool pool -> List.of(pool.input());
            case EnchantmentPoolJeiRecipe.Sources sources -> sources.books();
        };
        // Keep the header within bounds even when a pack defines many custom pools.
        int slots = Math.min(books.size(), 4);
        int rowWidth = slots * ITEM_SIZE + (slots - 1) * ITEM_GAP;
        int x = (WIDTH - rowWidth) / 2;
        for (int i = 0; i < slots; i++) {
            var slot = builder.addInputSlot(x, ITEM_Y);
            for (int j = i; j < books.size(); j += slots) {
                slot.addItemStack(books.get(j));
            }
            x += ITEM_SIZE + ITEM_GAP;
        }
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, EnchantmentPoolJeiRecipe recipe, IFocusGroup focuses) {
        List<FormattedText> rows = switch (recipe) {
            case EnchantmentPoolJeiRecipe.Pool pool -> pool.error().isPresent()
                    ? List.of(pool.error().get())
                    : pool.enchantments().isEmpty()
                            ? List.of(Component.translatable("message." + MODID + ".empty_pool",
                                    ((RandomEnchantedBookItem) pool.input().getItem()).getPoolId(pool.input()).toString()))
                            : pool.enchantments().stream().<FormattedText>map(enchantment -> formatRow(enchantment.value())).toList();
            case EnchantmentPoolJeiRecipe.Sources sources -> List.of(
                    sources.enchantment().value().description().copy().append(" ").append(formatLevel(sources.level())));
        };
        builder.addScrollBoxWidget(LIST_WIDTH - 2 * LIST_INSET, LIST_HEIGHT - 2 * LIST_INSET,
                        LIST_MARGIN + LIST_INSET, LIST_TOP + LIST_INSET)
                .setScrollbarVisibility(ScrollbarVisibility.AUTO_HIDE)
                .setContents(rows);
    }

    private static Component formatRow(Enchantment enchantment) {
        int minLevel = enchantment.getMinLevel();
        int maxLevel = enchantment.getMaxLevel();
        MutableComponent row = enchantment.description().copy().append(" ").append(formatLevel(minLevel));
        return minLevel == maxLevel ? row : row.append("-").append(formatLevel(maxLevel));
    }

    private static Component formatLevel(int level) {
        return Component.translatableWithFallback("enchantment.level." + level, Integer.toString(level));
    }
}
