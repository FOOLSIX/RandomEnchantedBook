package com.foolsix.random_enchanted_book.loot;

import com.foolsix.random_enchanted_book.config.EnchantmentPoolConfig;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.LootModifier;

final class ChestBookLootModifier extends LootModifier {
    static final MapCodec<ChestBookLootModifier> CODEC =
            RecordCodecBuilder.mapCodec(instance -> codecStart(instance).apply(instance, ChestBookLootModifier::new));

    private ChestBookLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    public MapCodec<ChestBookLootModifier> codec() {
        return CODEC;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        var settings = EnchantmentPoolConfig.CHEST_BOOKS.settings();
        if (settings.chance() <= 0.0 || !isChestLoot(context)) {
            return loot;
        }

        RandomSource random = context.getRandom();
        if (settings.chance() >= 1.0 || random.nextDouble() < settings.chance()) {
            settings.books().getRandom(random).ifPresent(book -> loot.add(new ItemStack(book.data())));
        }
        return loot;
    }

    private static boolean isChestLoot(LootContext context) {
        ResourceLocation tableId = context.getQueriedLootTableId();
        String path = tableId.getPath();
        // Vanilla uses the chest loot type for dispensers, pots, trial spawners and vault rewards too.
        if (!path.startsWith("chests/") || (tableId.getNamespace().equals(ResourceLocation.DEFAULT_NAMESPACE)
                && path.startsWith("chests/trial_chambers/reward"))) {
            return false;
        }

        var tableKey = ResourceKey.create(Registries.LOOT_TABLE, tableId);
        LootTable table = context.getLevel().getServer().reloadableRegistries().getLootTable(tableKey);
        return table.getParamSet() == LootContextParamSets.CHEST;
    }
}
