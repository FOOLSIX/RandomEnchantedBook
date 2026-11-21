package com.foolsix.random_enchanted_book.loot;

import javax.annotation.Nullable;

import com.foolsix.random_enchanted_book.config.LootConfig;
import com.foolsix.random_enchanted_book.item.ModItemReg;
import com.foolsix.random_enchanted_book.item.RandomEnchantedBookItem;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.LootModifier;

public final class EnchantedBookLootModifier extends LootModifier {
    static final MapCodec<EnchantedBookLootModifier> CODEC =
            RecordCodecBuilder.mapCodec(instance -> codecStart(instance).apply(instance, EnchantedBookLootModifier::new));

    private EnchantedBookLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    public MapCodec<EnchantedBookLootModifier> codec() {
        return CODEC;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        double chance = LootConfig.replacementChance();
        if (chance <= 0.0) {
            return loot;
        }

        // Compact the original loot in place, leaving the newly appended stacks after it.
        RandomSource random = context.getRandom();
        int originalSize = loot.size();
        int kept = 0;
        for (int index = 0; index < originalSize; index++) {
            ItemStack original = loot.get(index);
            ItemStack remaining = replaceBook(original, chance, random, loot, context.getLevel().registryAccess());
            if (remaining == ItemStack.EMPTY && remaining != original) {
                continue;
            }
            if (index != kept || remaining != original) {
                loot.set(kept, remaining);
            }
            kept++;
        }
        if (kept < originalSize) {
            loot.removeElements(kept, originalSize);
        }
        return loot;
    }

    private static ItemStack replaceBook(ItemStack original, double chance, RandomSource random,
                                         ObjectArrayList<ItemStack> loot, RegistryAccess registries) {
        if (!original.is(Items.ENCHANTED_BOOK) || original.isEmpty()) {
            return original;
        }
        ItemEnchantments stored = original.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (stored.isEmpty()) {
            return original;
        }

        int selected = countSelectedBooks(original.getCount(), chance, random);
        if (selected == 0) {
            return original;
        }

        ItemEnchantments.Mutable unmatched = replaceMatchingEnchantments(stored, selected, loot, registries);
        return unmatched == null ? original : keepRemainingBooks(original, selected, unmatched, loot);
    }

    @Nullable
    private static ItemEnchantments.Mutable replaceMatchingEnchantments(ItemEnchantments stored, int selected,
                                                                         ObjectArrayList<ItemStack> loot,
                                                                         RegistryAccess registries) {
        ItemEnchantments.Mutable unmatched = null;
        for (Holder<Enchantment> enchantment : stored.keySet()) {
            var book = findMatchingBook(registries, enchantment);
            if (book == null) {
                continue;
            }

            if (unmatched == null) {
                unmatched = new ItemEnchantments.Mutable(stored);
            }
            unmatched.set(enchantment, 0);
            appendReplacementBooks(loot, book, selected);
        }
        return unmatched;
    }

    private static void appendReplacementBooks(ObjectArrayList<ItemStack> loot, RandomEnchantedBookItem book,
                                               int count) {
        int maxStackSize = book.getDefaultMaxStackSize();
        for (int remaining = count; remaining > 0; remaining -= maxStackSize) {
            loot.add(new ItemStack(book, Math.min(remaining, maxStackSize)));
        }
    }

    private static ItemStack keepRemainingBooks(ItemStack original, int selected, ItemEnchantments.Mutable unmatched,
                                                ObjectArrayList<ItemStack> loot) {
        int untouched = original.getCount() - selected;
        if (!unmatched.keySet().isEmpty()) {
            ItemStack partial = original.copyWithCount(selected);
            partial.set(DataComponents.STORED_ENCHANTMENTS, unmatched.toImmutable());
            if (untouched == 0) {
                return partial;
            }
            loot.add(partial);
        }
        return untouched == 0 ? ItemStack.EMPTY : original.copyWithCount(untouched);
    }

    private static int countSelectedBooks(int count, double chance, RandomSource random) {
        if (chance >= 1.0) {
            return count;
        }
        int selected = 0;
        for (int index = 0; index < count; index++) {
            if (random.nextDouble() < chance) {
                selected++;
            }
        }
        return selected;
    }

    @Nullable
    private static RandomEnchantedBookItem findMatchingBook(RegistryAccess registries, Holder<Enchantment> enchantment) {
        for (var book : ModItemReg.BOOKS) {
            RandomEnchantedBookItem item = book.get();
            if (item.matchesEnchantment(registries, enchantment)) {
                return item;
            }
        }
        return null;
    }
}
