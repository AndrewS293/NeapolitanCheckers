package com.checkers.variants;

import com.checkers.logic.GameLogic;
import com.checkers.rules.GameRules;
import com.checkers.variants.GameFactory.VariantInfo;

import java.util.List;

/** Creates one independent game engine per room/session using a registered variant. */
public final class GameFactory {
    private final VariantRegistry registry;

    public GameFactory(VariantRegistry registry) {
        if (registry == null) throw new IllegalArgumentException("Registry cannot be null");
        this.registry = registry;
    }

    public GameLogic createGame(String variantId) {
        GameRules rules = registry.get(variantId);
        return new GameLogic(rules);
    }
    
    public List<VariantInfo> getAvailableVariants() {
        return registry.getAll().stream()
                .map(rules -> new VariantInfo(rules.getVariantId(), rules.getDisplayName(),
                        rules.getConfig().getBoardSize(), rules.getConfig().getPiecesPerPlayer()))
                .toList();
    }

    public record VariantInfo(String id, String displayName, int boardSize, int piecesPerPlayer) { }
}
