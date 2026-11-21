package com.foolsix.random_enchanted_book.compat.jei;

import java.util.List;
import java.util.Optional;

import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.enchantment.Enchantment;

sealed interface EnchantmentPoolJeiRecipe {
    record Pool(ItemStack input, List<Holder.Reference<Enchantment>> enchantments,
                Optional<Component> error) implements EnchantmentPoolJeiRecipe {}

    record Sources(Holder<Enchantment> enchantment, int level, List<ItemStack> books) implements EnchantmentPoolJeiRecipe {}
}
