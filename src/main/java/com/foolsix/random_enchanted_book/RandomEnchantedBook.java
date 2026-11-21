package com.foolsix.random_enchanted_book;

import com.foolsix.random_enchanted_book.data.BookPools;
import com.foolsix.random_enchanted_book.data.BookComponents;
import com.foolsix.random_enchanted_book.config.LootConfig;
import com.foolsix.random_enchanted_book.datagen.ModDataGenerators;
import com.foolsix.random_enchanted_book.item.ModItemReg;
import com.foolsix.random_enchanted_book.loot.ModLootModifiers;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;

@Mod(RandomEnchantedBook.MODID)
public class RandomEnchantedBook {
    public static final String MODID = "random_enchanted_book";

    public RandomEnchantedBook(IEventBus modEventBus, ModContainer modContainer) {
        ModItemReg.register(modEventBus);
        BookComponents.register(modEventBus);
        ModLootModifiers.register(modEventBus);
        modEventBus.addListener(BookPools::register);
        modEventBus.addListener(ModDataGenerators::gatherData);
        modEventBus.addListener(RandomEnchantedBook::onConfigChanged);
        modContainer.registerConfig(ModConfig.Type.COMMON, LootConfig.SPEC);
    }

    private static void onConfigChanged(ModConfigEvent event) {
        if (event.getConfig().getSpec() == LootConfig.SPEC
                && (event instanceof ModConfigEvent.Loading || event instanceof ModConfigEvent.Reloading)) {
            LootConfig.refresh();
        }
    }
}
