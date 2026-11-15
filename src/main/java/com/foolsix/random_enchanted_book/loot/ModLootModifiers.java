package com.foolsix.random_enchanted_book.loot;

import com.foolsix.random_enchanted_book.RandomEnchantedBook;
import com.mojang.serialization.MapCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModLootModifiers {
    private ModLootModifiers() {}

    public static void register(IEventBus modEventBus) {
        DeferredRegister<MapCodec<? extends IGlobalLootModifier>> serializers =
                DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, RandomEnchantedBook.MODID);
        serializers.register("replace_enchanted_books", () -> EnchantedBookLootModifier.CODEC);
        serializers.register("add_chest_book", () -> ChestBookLootModifier.CODEC);
        serializers.register(modEventBus);
    }
}
