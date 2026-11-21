package com.foolsix.random_enchanted_book.data;

import com.foolsix.random_enchanted_book.RandomEnchantedBook;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

public final class BookPools {
    public static final ResourceKey<Registry<EnchantmentPool>> REGISTRY = ResourceKey.createRegistryKey(id("enchantment_pool"));
    public static final ResourceKey<EnchantmentPool> COMMON = key("common");
    public static final ResourceKey<EnchantmentPool> UNCOMMON = key("uncommon");
    public static final ResourceKey<EnchantmentPool> RARE = key("rare");
    public static final ResourceKey<EnchantmentPool> EPIC = key("epic");

    private BookPools() {}

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(RandomEnchantedBook.MODID, path);
    }

    private static ResourceKey<EnchantmentPool> key(String path) {
        return ResourceKey.create(REGISTRY, id(path));
    }

    public static void register(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(REGISTRY, EnchantmentPool.CODEC, EnchantmentPool.CODEC);
    }
}
