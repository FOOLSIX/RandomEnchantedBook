package com.foolsix.random_enchanted_book.compat.jei;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.foolsix.random_enchanted_book.item.ModItemReg;
import com.foolsix.random_enchanted_book.item.RandomEnchantedBookItem;
import com.foolsix.random_enchanted_book.network.BookPoolSync;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.advanced.ISimpleRecipeManagerPlugin;
import mezz.jei.api.registration.IAdvancedRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.neoforge.registries.DeferredItem;

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

    private static Stream<EnchantmentPoolJeiRecipe.Pool> pools(DeferredItem<RandomEnchantedBookItem> book) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null) {
            return Stream.empty();
        }

        Registry<Enchantment> enchantments = connection.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        return BookPoolSync.getClientPool(connection.getConnection(), book.getId()).stream()
                .map(pool -> new EnchantmentPoolJeiRecipe.Pool(book.get(), pool.stream()
                        .flatMap(id -> enchantments.getHolder(id).stream())
                        .toList()));
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
                    .<EnchantmentPoolJeiRecipe>flatMap(stack -> ModItemReg.BOOKS.stream()
                            .filter(book -> stack.is(book.get()))
                            .flatMap(RandomEnchantedBookJeiPlugin::pools))
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

            List<Item> books = ModItemReg.BOOKS.stream()
                    .flatMap(RandomEnchantedBookJeiPlugin::pools)
                    .filter(pool -> pool.enchantments().contains(enchantment))
                    .map(EnchantmentPoolJeiRecipe.Pool::input)
                    .distinct()
                    .toList();
            return books.isEmpty() ? List.of() : List.of(new EnchantmentPoolJeiRecipe.Sources(enchantment, level, books));
        }

        private static Optional<ItemEnchantments> getOutputEnchantments(ITypedIngredient<?> ingredient) {
            return ingredient.getItemStack().filter(stack -> stack.is(Items.ENCHANTED_BOOK))
                    .map(stack -> stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY))
                    .filter(enchantments -> enchantments.keySet().size() == 1);
        }

        @Override
        public List<EnchantmentPoolJeiRecipe> getAllRecipes() {
            return ModItemReg.BOOKS.stream()
                    .<EnchantmentPoolJeiRecipe>flatMap(RandomEnchantedBookJeiPlugin::pools)
                    .toList();
        }
    }
}
