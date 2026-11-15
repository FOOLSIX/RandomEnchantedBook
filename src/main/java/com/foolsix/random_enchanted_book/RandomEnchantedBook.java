package com.foolsix.random_enchanted_book;

import com.foolsix.random_enchanted_book.config.EnchantmentPoolConfig;
import com.foolsix.random_enchanted_book.item.ModItemReg;
import com.foolsix.random_enchanted_book.network.BookPoolSync;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(RandomEnchantedBook.MODID)
public class RandomEnchantedBook {
    public static final String MODID = "random_enchanted_book";

    public RandomEnchantedBook(IEventBus modEventBus, ModContainer modContainer) {
        ModItemReg.register(modEventBus);
        modEventBus.addListener(RandomEnchantedBook::onConfigChanged);
        modEventBus.addListener(BookPoolSync::registerPayloads);
        NeoForge.EVENT_BUS.addListener(BookPoolSync::onDatapackSync);
        modContainer.registerConfig(ModConfig.Type.COMMON, EnchantmentPoolConfig.SPEC);
    }

    private static void onConfigChanged(ModConfigEvent event) {
        if (event.getConfig().getSpec() == EnchantmentPoolConfig.SPEC
                && (event instanceof ModConfigEvent.Loading || event instanceof ModConfigEvent.Reloading)) {
            EnchantmentPoolConfig.compilePredicates();
            if (event instanceof ModConfigEvent.Reloading) {
                BookPoolSync.onConfigReload();
            }
        }
    }
}
