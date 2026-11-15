package com.foolsix.random_enchanted_book.compat.jei;

import java.util.List;

import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;

sealed interface EnchantmentPoolJeiRecipe {
    record Pool(Item input, List<Holder.Reference<Enchantment>> enchantments) implements EnchantmentPoolJeiRecipe {}

    record Sources(Holder<Enchantment> enchantment, int level, List<Item> books) implements EnchantmentPoolJeiRecipe {}
}
