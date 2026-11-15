package com.foolsix.random_enchanted_book.loot;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.foolsix.random_enchanted_book.RandomEnchantedBook;
import com.foolsix.random_enchanted_book.config.EnchantmentPoolConfig;
import com.foolsix.random_enchanted_book.item.ModItemReg;
import com.foolsix.random_enchanted_book.item.RandomEnchantedBookItem;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class EnchantedBookLootModifier extends LootModifier {
    private static final MapCodec<EnchantedBookLootModifier> CODEC =
            RecordCodecBuilder.mapCodec(instance -> codecStart(instance).apply(instance, EnchantedBookLootModifier::new));
    private static volatile ConcurrentMap<Holder.Reference<Enchantment>, Optional<RandomEnchantedBookItem>> matches =
            new ConcurrentHashMap<>();

    private EnchantedBookLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    public static void register(IEventBus modEventBus) {
        DeferredRegister<MapCodec<? extends IGlobalLootModifier>> serializers =
                DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, RandomEnchantedBook.MODID);
        serializers.register("replace_enchanted_books", () -> CODEC);
        serializers.register(modEventBus);
    }

    public static void invalidateMatches() {
        matches = new ConcurrentHashMap<>();
    }

    public static void onTagsUpdated(TagsUpdatedEvent event) {
        if (event.getUpdateCause() == TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD) {
            invalidateMatches();
        }
    }

    @Override
    public MapCodec<EnchantedBookLootModifier> codec() {
        return CODEC;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        double chance = EnchantmentPoolConfig.lootReplacementChance();
        if (chance <= 0.0) {
            return loot;
        }

        // Compact the original loot in place, leaving the newly appended stacks after it.
        int originalSize = loot.size();
        int kept = 0;
        for (int index = 0; index < originalSize; index++) {
            ItemStack original = loot.get(index);
            ItemStack remaining = replaceBook(original, chance, context, loot);
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

    private static ItemStack replaceBook(ItemStack original, double chance, LootContext context,
                                         ObjectArrayList<ItemStack> loot) {
        if (!original.is(Items.ENCHANTED_BOOK) || original.isEmpty()) {
            return original;
        }
        ItemEnchantments stored = original.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (stored.isEmpty()) {
            return original;
        }

        int selected = countSelectedBooks(original.getCount(), chance, context.getRandom());
        if (selected == 0) {
            return original;
        }

        var matchingBooks = matches;
        ItemEnchantments.Mutable unmatched = null;
        for (Holder<Enchantment> enchantment : stored.keySet()) {
            if (!(enchantment instanceof Holder.Reference<Enchantment> reference)) {
                continue;
            }
            var book = matchingBooks.computeIfAbsent(reference, EnchantedBookLootModifier::findMatchingBook).orElse(null);
            if (book == null) {
                continue;
            }

            if (unmatched == null) {
                unmatched = new ItemEnchantments.Mutable(stored);
            }
            unmatched.set(enchantment, 0);
            int maxStackSize = book.getDefaultMaxStackSize();
            for (int remaining = selected; remaining > 0; remaining -= maxStackSize) {
                loot.add(new ItemStack(book, Math.min(remaining, maxStackSize)));
            }
        }
        if (unmatched == null) {
            return original;
        }

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

    private static Optional<RandomEnchantedBookItem> findMatchingBook(Holder.Reference<Enchantment> enchantment) {
        for (var book : ModItemReg.BOOKS) {
            RandomEnchantedBookItem item = book.get();
            if (item.matchesEnchantment(enchantment)) {
                return Optional.of(item);
            }
        }
        return Optional.empty();
    }
}
