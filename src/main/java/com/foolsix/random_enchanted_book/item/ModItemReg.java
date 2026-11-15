package com.foolsix.random_enchanted_book.item;

import java.util.List;

import com.foolsix.random_enchanted_book.RandomEnchantedBook;
import com.foolsix.random_enchanted_book.config.EnchantmentPoolConfig;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItemReg {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(RandomEnchantedBook.MODID);
    private static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, RandomEnchantedBook.MODID);

    public static final DeferredItem<RandomEnchantedBookItem> COMMON_BOOK =
            ITEMS.register("random_enchanted_book_common",
                    () -> new RandomEnchantedBookItem(Rarity.COMMON, EnchantmentPoolConfig.COMMON));
    public static final DeferredItem<RandomEnchantedBookItem> UNCOMMON_BOOK =
            ITEMS.register("random_enchanted_book_uncommon",
                    () -> new RandomEnchantedBookItem(Rarity.UNCOMMON, EnchantmentPoolConfig.UNCOMMON));
    public static final DeferredItem<RandomEnchantedBookItem> RARE_BOOK =
            ITEMS.register("random_enchanted_book_rare",
                    () -> new RandomEnchantedBookItem(Rarity.RARE, EnchantmentPoolConfig.RARE));
    public static final DeferredItem<RandomEnchantedBookItem> EPIC_BOOK =
            ITEMS.register("random_enchanted_book_epic",
                    () -> new RandomEnchantedBookItem(Rarity.EPIC, EnchantmentPoolConfig.EPIC));

    public static final List<DeferredItem<RandomEnchantedBookItem>> BOOKS =
            List.of(COMMON_BOOK, UNCOMMON_BOOK, RARE_BOOK, EPIC_BOOK);

    static {
        CREATIVE_MODE_TABS.register("random_enchanted_books", () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup." + RandomEnchantedBook.MODID + ".random_enchanted_books"))
                .icon(() -> COMMON_BOOK.get().getDefaultInstance())
                .displayItems((parameters, output) -> BOOKS.forEach(book -> output.accept(book.get())))
                .build());
    }

    private ModItemReg() {}

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
    }
}
