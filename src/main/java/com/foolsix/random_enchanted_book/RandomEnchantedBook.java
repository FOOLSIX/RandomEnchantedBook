package com.foolsix.random_enchanted_book;

import com.foolsix.random_enchanted_book.config.EnchantmentPoolConfig;
import com.foolsix.random_enchanted_book.item.ModItemReg;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(RandomEnchantedBook.MODID)
public class RandomEnchantedBook {
    public static final String MODID = "random_enchanted_book";

    public RandomEnchantedBook(IEventBus modEventBus, ModContainer modContainer) {
        ModItemReg.register(modEventBus);
        modEventBus.addListener(EnchantmentPoolConfig::onConfigChanged);
        modContainer.registerConfig(ModConfig.Type.COMMON, EnchantmentPoolConfig.SPEC);
    }
}
