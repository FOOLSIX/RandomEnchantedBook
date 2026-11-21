package com.foolsix.random_enchanted_book.item;

import java.util.List;
import java.util.Optional;

import com.foolsix.random_enchanted_book.RandomEnchantedBook;
import com.foolsix.random_enchanted_book.data.BookComponents;
import com.foolsix.random_enchanted_book.data.BookPools;
import com.foolsix.random_enchanted_book.data.EnchantmentPool;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.ChatFormatting;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.Level;

public class RandomEnchantedBookItem extends Item {
    private final ResourceKey<EnchantmentPool> poolKey;

    RandomEnchantedBookItem(Rarity rarity, ResourceKey<EnchantmentPool> poolKey) {
        super(new Properties().rarity(rarity).stacksTo(64).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true));
        this.poolKey = poolKey;
    }

    public List<Holder.Reference<Enchantment>> getEnchantmentPool(RegistryAccess registries) {
        return getEnchantmentPool(getDefaultInstance(), registries);
    }

    public ResourceLocation getPoolId(ItemStack stack) {
        return stack.getOrDefault(BookComponents.ENCHANTMENT_POOL, poolKey.location());
    }

    public Optional<EnchantmentPool> getPool(ItemStack stack, HolderLookup.Provider registries) {
        return registries.lookup(BookPools.REGISTRY)
                .flatMap(registry -> registry.get(ResourceKey.create(BookPools.REGISTRY, getPoolId(stack))))
                .map(Holder::value);
    }

    public List<Holder.Reference<Enchantment>> getEnchantmentPool(ItemStack stack, RegistryAccess registries) {
        var pool = getPool(stack, registries);
        if (pool.isEmpty()) {
            return List.of();
        }
        return registries.registryOrThrow(Registries.ENCHANTMENT).holders()
                .filter(pool.get()::matches)
                .toList();
    }

    public boolean matchesEnchantment(RegistryAccess registries, Holder<Enchantment> enchantment) {
        return getPool(getDefaultInstance(), registries).map(pool -> pool.matches(enchantment)).orElse(false);
    }

    public int getBorderColor(ItemStack stack, RegistryAccess registries) {
        int defaultColor = getPool(stack, registries).map(EnchantmentPool::color).orElse(0xFFFFFF);
        return DyedItemColor.getOrDefault(stack, 0xFF000000 | defaultColor);
    }

    public Component missingPoolMessage(ItemStack stack) {
        return Component.translatable("message." + RandomEnchantedBook.MODID + ".missing_pool", getPoolId(stack).toString());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (stack.has(BookComponents.ENCHANTMENT_POOL) || flag.isAdvanced()) {
            tooltip.add(Component.translatable("tooltip." + RandomEnchantedBook.MODID + ".pool", getPoolId(stack).toString())
                    .withStyle(ChatFormatting.GRAY));
        }
        if (context.registries() != null && getPool(stack, context.registries()).isEmpty()) {
            tooltip.add(missingPoolMessage(stack).copy().withStyle(ChatFormatting.RED));
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack heldStack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResultHolder.success(heldStack);
        }

        if (getPool(heldStack, level.registryAccess()).isEmpty()) {
            player.displayClientMessage(missingPoolMessage(heldStack), true);
            return InteractionResultHolder.fail(heldStack);
        }

        List<Holder.Reference<Enchantment>> pool = getEnchantmentPool(heldStack, level.registryAccess());
        if (pool.isEmpty()) {
            player.displayClientMessage(Component.translatable("message." + RandomEnchantedBook.MODID + ".empty_pool",
                    getPoolId(heldStack).toString()), true);
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
