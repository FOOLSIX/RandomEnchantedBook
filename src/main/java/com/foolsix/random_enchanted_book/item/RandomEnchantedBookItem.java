package com.foolsix.random_enchanted_book.item;

import java.util.List;

import com.foolsix.random_enchanted_book.RandomEnchantedBook;
import com.foolsix.random_enchanted_book.config.EnchantmentPoolConfig;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.Level;

public class RandomEnchantedBookItem extends Item {
    private final EnchantmentPoolConfig poolConfig;

    RandomEnchantedBookItem(Rarity rarity, EnchantmentPoolConfig poolConfig) {
        super(new Properties().rarity(rarity).stacksTo(64).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true));
        this.poolConfig = poolConfig;
    }

    public List<Holder.Reference<Enchantment>> getEnchantmentPool(RegistryAccess registries) {
        return registries.registryOrThrow(Registries.ENCHANTMENT).holders()
                .filter(poolConfig.predicate())
                .toList();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack heldStack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResultHolder.success(heldStack);
        }

        List<Holder.Reference<Enchantment>> pool = getEnchantmentPool(level.registryAccess());
        if (pool.isEmpty()) {
            player.displayClientMessage(Component.translatable("message." + RandomEnchantedBook.MODID + ".empty_pool"), true);
            return InteractionResultHolder.fail(heldStack);
        }

        int booksToOpen = player.isShiftKeyDown() ? heldStack.getCount() : 1;
        RandomSource random = level.getRandom();
        ItemStack resultInHand = heldStack;
        if (booksToOpen == heldStack.getCount()) {
            // Keep the current slot occupied until the use result replaces it.
            resultInHand = rollBook(pool, random);
            booksToOpen--;
        } else {
            heldStack.shrink(booksToOpen);
        }

        for (int i = 0; i < booksToOpen; i++) {
            player.getInventory().placeItemBackInInventory(rollBook(pool, random), false);
        }
        return InteractionResultHolder.consume(resultInHand);
    }

    private static ItemStack rollBook(List<Holder.Reference<Enchantment>> pool, RandomSource random) {
        Holder<Enchantment> enchantment = pool.get(random.nextInt(pool.size()));
        int enchantmentLevel = Mth.nextInt(random, enchantment.value().getMinLevel(), enchantment.value().getMaxLevel());
        return EnchantedBookItem.createForEnchantment(new EnchantmentInstance(enchantment, enchantmentLevel));
    }
}
