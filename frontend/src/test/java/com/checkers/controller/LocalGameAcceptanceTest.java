package com.checkers.controller;

import com.checkers.logic.GameLogic;
import com.checkers.logic.Move.TurnMove;
import com.checkers.variants.GameFactory;
import com.checkers.variants.VariantRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LocalGameAcceptanceTest {

    private final VariantRegistry variants = VariantRegistry.defaults();

    @Test
    void newGameOffersHumanAndAiOpponentSelections() {
        assertEquals(List.of(LocalGameOptions.Opponent.HUMAN,
                        LocalGameOptions.Opponent.AI),
                List.of(LocalGameOptions.Opponent.values()));
        assertEquals(LocalGameOptions.Opponent.HUMAN,
                LocalGameOptions.human().opponent());
        assertNull(LocalGameOptions.human().difficulty());
    }

    @Test
    void aiSelectionRequiresAndKeepsChosenDifficulty() {
        LocalGameOptions options = LocalGameOptions.ai(
                ReinforcementLearningTrainer.Difficulty.MEDIUM);

        assertEquals(LocalGameOptions.Opponent.AI, options.opponent());
        assertEquals(ReinforcementLearningTrainer.Difficulty.MEDIUM,
                options.difficulty());
        assertThrows(NullPointerException.class,
                () -> LocalGameOptions.ai(null));
    }

    @Test
    void hardSearchesDeeperAndEasyMakesMoreWeakeningMoves() {
        var easy = ReinforcementLearningTrainer.Difficulty.EASY;
        var medium = ReinforcementLearningTrainer.Difficulty.MEDIUM;
        var hard = ReinforcementLearningTrainer.Difficulty.HARD;

        assertTrue(hard.searchDepth() > medium.searchDepth());
        assertTrue(medium.searchDepth() > easy.searchDepth());
        assertTrue(easy.randomMoveChance() > medium.randomMoveChance());
        assertTrue(medium.randomMoveChance() > hard.randomMoveChance());
    }

    @Test
    void aiRunsOnlyOnItsConfiguredTurnAndNeverInHumanMode() {
        GameLogic game = new GameFactory(variants).createGame("SENEGALESE");
        var agent = agentFor("SENEGALESE", 10,
                ReinforcementLearningTrainer.Difficulty.EASY);

        assertFalse(ReinforcementLearningTrainer.shouldPlayAsAi(
                game, agent, false, false));
        assertFalse(ReinforcementLearningTrainer.shouldPlayAsAi(
                game, null, false, false));

        TurnMove humanMove = game.getRules().getLegalTurns(
                game.getBoard(), game.getCurrentPlayer()).getFirst();
        play(game, humanMove);

        assertTrue(ReinforcementLearningTrainer.shouldPlayAsAi(
                game, agent, false, false));
        assertFalse(ReinforcementLearningTrainer.shouldPlayAsAi(
                game, agent, false, true));
        assertFalse(ReinforcementLearningTrainer.shouldPlayAsAi(
                game, agent, true, false));
    }

    @Test
    void aiChoosesACompleteLegalMoveForSenegaleseAndOtherRegisteredVariants() {
        for (String variantId : List.of("AMERICAN", "MALAYSIAN", "THAI", "SENEGALESE")) {
            GameLogic game = new GameFactory(variants).createGame(variantId);
            var agent = agentFor(variantId, game.getConfig().getBoardSize(),
                    ReinforcementLearningTrainer.Difficulty.HARD);
            play(game, game.getRules().getLegalTurns(
                    game.getBoard(), game.getCurrentPlayer()).getFirst());

            TurnMove selected = ReinforcementLearningTrainer.selectMove(agent, game);

            assertNotNull(selected, variantId);
            assertTrue(game.getRules().getLegalTurns(
                            game.getBoard(), game.getCurrentPlayer()).contains(selected),
                    variantId + " AI must choose a variant-legal turn");
            play(game, selected);
            assertEquals(GameLogic.red_player, game.getCurrentPlayer(), variantId);
        }
    }

        @Test
        void senegaleseIsRegisteredAndStartsWithItsExpectedBoard() {
                GameFactory factory = new GameFactory(variants);
                GameLogic game = factory.createGame("SENEGALESE");

                assertEquals(10, game.getBoard().getBoardSize());
                assertEquals(20, game.countPieces(GameLogic.red_player));
                assertEquals(20, game.countPieces(GameLogic.black_player));
                assertTrue(variants.getAll().stream()
                                .anyMatch(rules -> rules.getVariantId().equals("SENEGALESE")));
                assertTrue(factory.getAvailableVariants().stream()
                        .anyMatch(variant -> variant.id().equals("SENEGALESE")));
                assertTrue(VariantSelectionController.variantCardIds().contains("SENEGALESE"));
                assertEquals(6, VariantSelectionController.variantCardIds().size());

                TurnMove opening = game.getRules().getLegalTurns(
                        game.getBoard(), GameLogic.red_player).getFirst();
                for (var step : opening.steps()) {
                    assertTrue(game.move(step.startIndex(), step.endIndex()));
                }
                assertEquals(GameLogic.black_player, game.getCurrentPlayer());
        }

    private ReinforcementLearningTrainer.TrainedAgent agentFor(
            String variantId, int boardSize,
            ReinforcementLearningTrainer.Difficulty difficulty) {
        return new ReinforcementLearningTrainer.TrainedAgent(
                variantId, boardSize, difficulty,
                new double[] { 0, 1.1, 1.35, 0.16, 0.12, 0.08 });
    }

    private void play(GameLogic game, TurnMove turn) {
        for (var step : turn.steps()) {
            assertTrue(game.move(step.startIndex(), step.endIndex()));
        }
    }
}