package com.foolsix.random_enchanted_book.data;

import com.foolsix.random_enchanted_book.RandomEnchantedBook;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BookComponents {
    private static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, RandomEnchantedBook.MODID);

    // Only the ID is saved on a stack. The pool definition lives in the datapack registry.
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ResourceLocation>> ENCHANTMENT_POOL =
            COMPONENTS.registerComponentType("enchantment_pool", builder -> builder
                    .persistent(ResourceLocation.CODEC)
                    .networkSynchronized(ResourceLocation.STREAM_CODEC));

    private BookComponents() {}

    public static void register(IEventBus bus) {
        COMPONENTS.register(bus);
    }
}
