package com.foolsix.random_enchanted_book.datagen;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.foolsix.random_enchanted_book.RandomEnchantedBook;
import com.foolsix.random_enchanted_book.data.BookPools;
import com.foolsix.random_enchanted_book.data.EnchantmentPool;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;

public final class ModDataGenerators {
    private ModDataGenerators() {}

    public static void gatherData(GatherDataEvent event) {
        var generator = event.getGenerator();
        var output = generator.getPackOutput();
        var builder = new RegistrySetBuilder().add(BookPools.REGISTRY, context -> {
            context.register(BookPools.COMMON, defaultPool(10, 0xFFFFFF));
            context.register(BookPools.UNCOMMON, defaultPool(5, 0xF7DD30));
            context.register(BookPools.RARE, defaultPool(2, 0x3CEEF7));
            context.register(BookPools.EPIC, defaultPool(1, 0xA44CE0));
        });
        generator.addProvider(event.includeServer(), new DatapackBuiltinEntriesProvider(
                output, event.getLookupProvider(), builder, Set.of(RandomEnchantedBook.MODID)));
        generator.addProvider(event.includeClient(), new BookModelProvider(output, event.getExistingFileHelper()));
    }

    private static EnchantmentPool defaultPool(int weight, int color) {
        return new EnchantmentPool(List.of(weight), List.of(EnchantmentTags.ON_RANDOM_LOOT),
                List.of(EnchantmentTags.CURSE), 1, Enchantment.MAX_LEVEL, Optional.empty(), Optional.empty(), color);
    }
}
