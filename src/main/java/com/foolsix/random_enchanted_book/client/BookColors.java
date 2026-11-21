package com.foolsix.random_enchanted_book.client;

import com.foolsix.random_enchanted_book.RandomEnchantedBook;
import com.foolsix.random_enchanted_book.item.ModItemReg;
import com.foolsix.random_enchanted_book.item.RandomEnchantedBookItem;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

@EventBusSubscriber(modid = RandomEnchantedBook.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class BookColors {
    private BookColors() {}

    @SubscribeEvent
    public static void register(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> {
            var connection = Minecraft.getInstance().getConnection();
            if (tintIndex != 0 || connection == null || !(stack.getItem() instanceof RandomEnchantedBookItem book)) {
                return 0xFFFFFFFF;
            }
            return book.getBorderColor(stack, connection.registryAccess());
        }, ModItemReg.BOOKS.stream().map(holder -> holder.get()).toArray(Item[]::new));
    }
}
