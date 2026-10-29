package com.checkers.variants;

import com.checkers.rules.GameRules;
import com.checkers.variants.american.AmericanRules;
import com.checkers.variants.malaysian.MalaysianRules;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Registry keyed by stable variant IDs, independent of Spring. */
public final class VariantRegistry {
    private final Map<String, GameRules> variants = new LinkedHashMap<>();

    public VariantRegistry(Collection<? extends GameRules> implementations) {
        for (GameRules rules : implementations) {
            String id = rules.getVariantId();
            if (id == null || id.isBlank()) throw new IllegalArgumentException("Variant ID cannot be blank");
            if (variants.putIfAbsent(id, rules) != null) {
                throw new IllegalArgumentException("Duplicate variant ID: " + id);
            }
        }
    }

    public static VariantRegistry defaults() {
        return new VariantRegistry(List.of(new AmericanRules(), new MalaysianRules()));
    }

    public GameRules get(String variantId) {
        GameRules rules = variants.get(variantId);
        if (rules == null) throw new IllegalArgumentException("Unknown checkers variant: " + variantId);
        return rules;
    }

    public Collection<GameRules> getAll() { return List.copyOf(variants.values()); }
}
