package com.foolsix.random_enchanted_book.compat.jei;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.foolsix.random_enchanted_book.data.BookComponents;
import com.foolsix.random_enchanted_book.data.BookPools;
import com.foolsix.random_enchanted_book.item.ModItemReg;
import com.foolsix.random_enchanted_book.item.RandomEnchantedBookItem;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.recipe.advanced.ISimpleRecipeManagerPlugin;
import mezz.jei.api.registration.IAdvancedRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IExtraIngredientRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

@JeiPlugin
public final class RandomEnchantedBookJeiPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return EnchantmentPoolJeiCategory.RECIPE_TYPE.getUid();
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new EnchantmentPoolJeiCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerAdvanced(IAdvancedRegistration registration) {
        registration.addTypedRecipeManagerPlugin(EnchantmentPoolJeiCategory.RECIPE_TYPE, new PoolLookup());
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        ISubtypeInterpreter<ItemStack> interpreter = new ISubtypeInterpreter<>() {
            @Override
            public Object getSubtypeData(ItemStack stack, UidContext context) {
                return ((RandomEnchantedBookItem) stack.getItem()).getPoolId(stack);
            }

            @Override
            public String getLegacyStringSubtypeInfo(ItemStack stack, UidContext context) {
                return ((RandomEnchantedBookItem) stack.getItem()).getPoolId(stack).toString();
            }
        };
        ModItemReg.BOOKS.forEach(book -> registration.registerSubtypeInterpreter(book.get(), interpreter));
    }

    @Override
    public void registerExtraIngredients(IExtraIngredientRegistration registration) {
        registration.addExtraItemStacks(knownBooks().filter(stack -> stack.has(BookComponents.ENCHANTMENT_POOL)).toList());
    }

    private static Stream<ItemStack> knownBooks() {
        ClientPacketListener listener = Minecraft.getInstance().getConnection();
        if (listener == null) {
            return Stream.empty();
        }
        List<ItemStack> defaults = ModItemReg.BOOKS.stream().map(book -> book.get().getDefaultInstance()).toList();
        Set<ResourceLocation> defaultPools = defaults.stream()
                .map(stack -> ((RandomEnchantedBookItem) stack.getItem()).getPoolId(stack)).collect(Collectors.toSet());
        Stream<ItemStack> custom = listener.registryAccess().registryOrThrow(BookPools.REGISTRY).keySet().stream()
                .filter(id -> !defaultPools.contains(id)).sorted().map(id -> {
                    ItemStack stack = ModItemReg.COMMON_BOOK.get().getDefaultInstance();
                    stack.set(BookComponents.ENCHANTMENT_POOL, id);
                    return stack;
                });
        return Stream.concat(defaults.stream(), custom);
    }

    private static Stream<EnchantmentPoolJeiRecipe.Pool> loadPools(Stream<ItemStack> books) {
        ClientPacketListener listener = Minecraft.getInstance().getConnection();
        if (listener == null) {
            return Stream.empty();
        }

        return books.map(stack -> {
            RandomEnchantedBookItem book = (RandomEnchantedBookItem) stack.getItem();
            Optional<Component> error = book.getPool(stack, listener.registryAccess()).isEmpty()
                    ? Optional.of(book.missingPoolMessage(stack)) : Optional.empty();
            return new EnchantmentPoolJeiRecipe.Pool(stack,
                    book.getEnchantmentPool(stack, listener.registryAccess()), error);
        });
    }

    private static final class PoolLookup implements ISimpleRecipeManagerPlugin<EnchantmentPoolJeiRecipe> {
        @Override
        public boolean isHandledInput(ITypedIngredient<?> ingredient) {
            return ingredient.getItemStack().filter(stack -> stack.getItem() instanceof RandomEnchantedBookItem)
                    .isPresent();
        }

        @Override
        public boolean isHandledOutput(ITypedIngredient<?> ingredient) {
            return getOutputEnchantments(ingredient).isPresent();
        }

        @Override
        public List<EnchantmentPoolJeiRecipe> getRecipesForInput(ITypedIngredient<?> ingredient) {
            return ingredient.getItemStack().stream()
                    .filter(stack -> stack.getItem() instanceof RandomEnchantedBookItem)
                    .<EnchantmentPoolJeiRecipe>flatMap(stack -> loadPools(Stream.of(stack.copyWithCount(1))))
                    .toList();
        }

        @Override
        public List<EnchantmentPoolJeiRecipe> getRecipesForOutput(ITypedIngredient<?> ingredient) {
            Optional<ItemEnchantments> output = getOutputEnchantments(ingredient);
            if (output.isEmpty()) {
                return List.of();
            }

            ItemEnchantments enchantments = output.get();
            Holder<Enchantment> enchantment = enchantments.keySet().iterator().next();
            int level = enchantments.getLevel(enchantment);
            if (level < enchantment.value().getMinLevel() || level > enchantment.value().getMaxLevel()) {
                return List.of();
            }

            List<ItemStack> books = findSourceBooks(enchantment);
            return books.isEmpty() ? List.of() : List.of(new EnchantmentPoolJeiRecipe.Sources(enchantment, level, books));
        }

        private static List<ItemStack> findSourceBooks(Holder<Enchantment> enchantment) {
            return loadPools(knownBooks())
                    .filter(pool -> pool.enchantments().contains(enchantment))
                    .map(EnchantmentPoolJeiRecipe.Pool::input)
                    .toList();
        }

        private static Optional<ItemEnchantments> getOutputEnchantments(ITypedIngredient<?> ingredient) {
            return ingredient.getItemStack().filter(stack -> stack.is(Items.ENCHANTED_BOOK))
                    .map(stack -> stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY))
                    .filter(enchantments -> enchantments.size() == 1);
        }

        @Override
        public List<EnchantmentPoolJeiRecipe> getAllRecipes() {
            return loadPools(knownBooks())
                    .<EnchantmentPoolJeiRecipe>map(pool -> pool)
                    .toList();
        }
    }
}
