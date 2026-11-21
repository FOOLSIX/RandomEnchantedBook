package com.foolsix.random_enchanted_book.data;

import java.util.List;
import java.util.Optional;
import java.util.Locale;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.enchantment.Enchantment;

/** All filters must match; an absent allow filter imposes no restriction. */
public record EnchantmentPool(List<Integer> weights, List<TagKey<Enchantment>> requiredTags,
                              List<TagKey<Enchantment>> excludedTags, int minMaxLevel, int maxMaxLevel,
                              Optional<List<String>> allow, Optional<List<String>> deny, int color) {
    private static final Codec<String> NAMESPACE_CODEC = Codec.STRING.validate(namespace ->
            namespace.matches("[a-z0-9_.-]+")
                    ? DataResult.success(namespace)
                    : DataResult.error(() -> "Expected an enchantment namespace such as minecraft or a mod ID, without ':' or '#': "
                            + namespace));
    public static final Codec<Integer> COLOR_CODEC = Codec.STRING.comapFlatMap(
            text -> text.matches("#[0-9a-fA-F]{6}")
                    ? DataResult.success(Integer.parseInt(text.substring(1), 16))
                    : DataResult.error(() -> "Book color must use #RRGGBB format"),
            color -> String.format(Locale.ROOT, "#%06X", color));
    public static final Codec<EnchantmentPool> CODEC = RecordCodecBuilder.<EnchantmentPool>create(instance -> instance.group(
            Codec.intRange(1, 1024).listOf().optionalFieldOf("weights", List.of()).forGetter(EnchantmentPool::weights),
            TagKey.hashedCodec(Registries.ENCHANTMENT).listOf().optionalFieldOf("required_tags", List.of())
                    .forGetter(EnchantmentPool::requiredTags),
            TagKey.hashedCodec(Registries.ENCHANTMENT).listOf().optionalFieldOf("excluded_tags", List.of())
                    .forGetter(EnchantmentPool::excludedTags),
            Codec.intRange(1, Enchantment.MAX_LEVEL).optionalFieldOf("min_max_level", 1).forGetter(EnchantmentPool::minMaxLevel),
            Codec.intRange(1, Enchantment.MAX_LEVEL).optionalFieldOf("max_max_level", Enchantment.MAX_LEVEL)
                    .forGetter(EnchantmentPool::maxMaxLevel),
            NAMESPACE_CODEC.listOf().optionalFieldOf("allow").forGetter(EnchantmentPool::allow),
            NAMESPACE_CODEC.listOf().optionalFieldOf("deny").forGetter(EnchantmentPool::deny),
            COLOR_CODEC.optionalFieldOf("color", 0xFFFFFF).forGetter(EnchantmentPool::color)
    ).apply(instance, EnchantmentPool::new)).validate(pool -> pool.minMaxLevel <= pool.maxMaxLevel
            ? DataResult.success(pool) : DataResult.error(() -> "min_max_level must not exceed max_max_level"));

    public boolean matches(Holder<Enchantment> enchantment) {
        Enchantment value = enchantment.value();
        String namespace = enchantment.unwrapKey().map(key -> key.location().getNamespace()).orElse("");
        return (weights.isEmpty() || weights.contains(value.getWeight()))
                && value.getMaxLevel() >= minMaxLevel && value.getMaxLevel() <= maxMaxLevel
                && requiredTags.stream().allMatch(enchantment::is)
                && excludedTags.stream().noneMatch(enchantment::is)
                && allow.map(namespaces -> namespaces.contains(namespace)).orElse(true)
                && !deny.map(namespaces -> namespaces.contains(namespace)).orElse(false);
    }
}
