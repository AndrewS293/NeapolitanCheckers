package com.checkers.controller;

import com.checkers.Main;
import com.checkers.logic.Board;
import com.checkers.logic.GameLogic;
import com.checkers.logic.Move.MoveStep;
import com.checkers.logic.Move.TurnMove;
import com.checkers.logic.Piece;
import com.checkers.rules.GameRules;
import com.checkers.variants.VariantRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Spinner;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/** Trains and previews variant-specific self-play checkers agents. */
public final class ReinforcementLearningTrainer {

    private static final int FEATURE_COUNT = 6;
    private static final int MAX_TURNS_PER_GAME = 250;
    private static final double LEARNING_RATE = 0.025;
    private static final double DISCOUNT = 0.96;
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Path MODEL_DIRECTORY = resolveModelDirectory();

    public enum Difficulty {
        EASY("Easy", 1, 0.18),
        MEDIUM("Medium", 2, 0.08),
        HARD("Hard", 3, 0.02);

        private final String label;
        private final int searchDepth;
        private final double exploration;

        Difficulty(String label, int searchDepth, double exploration) {
            this.label = label;
            this.searchDepth = searchDepth;
            this.exploration = exploration;
        }

        public int searchDepth() {
            return searchDepth;
        }

        public double randomMoveChance() {
            return exploration;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private final Stage stage;
    private final List<GameRules> variants = List.copyOf(
            VariantRegistry.defaults().getAll());
    private final ComboBox<GameRules> variantPicker = new ComboBox<>();
    private final Spinner<Integer> episodeCount = new Spinner<>(1, 1_000_000, 300, 50);
    private final Spinner<Integer> previewInterval = new Spinner<>(1, 100_000, 25, 1);
    private final Label status = new Label("Choose a variant and start self-play training.");
    private final ProgressBar progress = new ProgressBar(0);
    private final GridPane boardView = new GridPane();
    private Button startButton;
    private Button cancelButton;
    private Task<Void> trainingTask;

    public ReinforcementLearningTrainer(Stage stage) {
        if (stage == null) {
            throw new IllegalArgumentException("Stage cannot be null.");
        }
        this.stage = stage;
    }

    public void show() {
        Label title = new Label("Reinforcement Learning Lab");
        title.setStyle("-fx-font-family: Georgia; -fx-font-size: 25px;"
                + " -fx-text-fill: #173c36;");

        variantPicker.getItems().setAll(variants);
        variantPicker.getSelectionModel().selectFirst();
        variantPicker.setMaxWidth(Double.MAX_VALUE);
        variantPicker.setCellFactory(list -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(GameRules rules, boolean empty) {
                super.updateItem(rules, empty);
                setText(empty || rules == null ? null : rules.getDisplayName());
            }
        });
        variantPicker.setButtonCell(new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(GameRules rules, boolean empty) {
                super.updateItem(rules, empty);
                setText(empty || rules == null ? null : rules.getDisplayName());
            }
        });
        variantPicker.setOnAction(event -> drawBoard(new GameLogic(variantPicker.getValue())));

        boardView.setAlignment(Pos.CENTER);
        boardView.setHgap(0);
        boardView.setVgap(0);
        GameRules firstVariant = variants.getFirst();
        drawBoard(new GameLogic(firstVariant));

        startButton = new Button("Train all 3 levels");
        AppTheme.stylePrimaryButton(startButton);
        startButton.setOnAction(event -> startTraining());
        cancelButton = new Button("Cancel");
        AppTheme.styleSecondaryButton(cancelButton);
        cancelButton.setDisable(true);
        cancelButton.setOnAction(event -> {
            if (trainingTask != null) {
                trainingTask.cancel(true);
            }
        });
        Button backButton = new Button("Back");
        AppTheme.styleSecondaryButton(backButton);
        backButton.setOnAction(event -> Main.showVariantSelection());

        HBox settings = new HBox(10,
                labeledControl("Variant", variantPicker),
                labeledControl("Episodes per level", episodeCount),
                labeledControl("Show a game every", previewInterval));
        settings.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(settings.getChildren().getFirst(), Priority.ALWAYS);
        HBox actions = new HBox(10, startButton, cancelButton, backButton);
        actions.setAlignment(Pos.CENTER_LEFT);
        progress.setMaxWidth(Double.MAX_VALUE);

        VBox controls = new VBox(12,
                title,
                new Label("The preview updates during each selected checkpoint episode."),
                settings,
                actions,
                progress,
                status);
        controls.setPadding(new Insets(22));

        BorderPane root = new BorderPane();
        root.setTop(controls);
        root.setCenter(boardView);
        AppTheme.applyBackground(root);
        BorderPane.setMargin(boardView, new Insets(0, 22, 22, 22));

        stage.setTitle("Neapolitan Checkers - RL Training");
        stage.setScene(new Scene(root, 860, 760));
        stage.setMinWidth(680);
        stage.setMinHeight(620);
        stage.show();
    }

    private VBox labeledControl(String label, javafx.scene.Node control) {
        VBox group = new VBox(5, new Label(label), control);
        group.setAlignment(Pos.CENTER_LEFT);
        return group;
    }

    private void startTraining() {
        GameRules rules = variantPicker.getValue();
        int episodes = episodeCount.getValue();
        int every = previewInterval.getValue();
        trainingTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                long totalEpisodes = (long) episodes * Difficulty.values().length;
                GameLogic initial = new GameLogic(rules);
                publishBoard(initial);

                for (int level = 0; level < Difficulty.values().length; level++) {
                    Difficulty difficulty = Difficulty.values()[level];
                        TrainedAgent agent = loadOrInitialize(rules, difficulty);

                    for (int episode = 1; episode <= episodes; episode++) {
                        if (isCancelled()) {
                            return null;
                        }
                        boolean showGame = episode % every == 0;
                        GameLogic result = trainEpisode(agent, rules, showGame);
                        if (showGame) {
                            publishBoard(result);
                            Thread.sleep(120);
                        }
                        long complete = (long) level * episodes + episode;
                        updateProgress(complete, totalEpisodes);
                        updateMessage(difficulty + " | episode " + episode + "/" + episodes
                                + " | latest result: " + result.getGameResultMessage());
                    }
                    saveAgent(agent);
                    updateMessage(difficulty + " model saved for " + rules.getDisplayName());
                }
                return null;
            }
        };

        startButton.setDisable(true);
        cancelButton.setDisable(false);
        progress.progressProperty().bind(trainingTask.progressProperty());
        status.textProperty().bind(trainingTask.messageProperty());
        trainingTask.setOnSucceeded(event -> finishTraining("Training complete."));
        trainingTask.setOnCancelled(event -> finishTraining("Training cancelled. Completed models are saved."));
        trainingTask.setOnFailed(event -> finishTraining(
                "Training failed: " + trainingTask.getException().getMessage()));
        Thread worker = new Thread(trainingTask, "checkers-rl-training");
        worker.setDaemon(true);
        worker.start();
    }

    private void finishTraining(String message) {
        progress.progressProperty().unbind();
        status.textProperty().unbind();
        progress.setProgress(trainingTask.getState()
            == javafx.concurrent.Worker.State.SUCCEEDED ? 1 : progress.getProgress());
        status.setText(message);
        startButton.setDisable(false);
        cancelButton.setDisable(true);
    }

    private GameLogic trainEpisode(
            TrainedAgent agent, GameRules rules, boolean showGame) throws InterruptedException {
        GameLogic game = new GameLogic(rules);

        for (int turnNumber = 0; turnNumber < MAX_TURNS_PER_GAME && !game.isGameOver(); turnNumber++) {
            int player = game.getCurrentPlayer();
            double[] before = features(game, player);
            List<TurnMove> turns = rules.getLegalTurns(game.getBoard(), player);
            if (turns.isEmpty()) {
                break;
            }

                TurnMove selected = chooseTurn(agent, game, turns);
            playTurn(game, selected);

            double target;
            if (game.isGameOver()) {
                target = terminalValue(game, player);
            } else {
                target = -DISCOUNT * value(agent.weights,
                        features(game, game.getCurrentPlayer()));
            }
            updateWeights(agent.weights, before, target - value(agent.weights, before));

            if (showGame) {
                publishBoard(game);
                Thread.sleep(100);
            }
        }
        return game;
    }

    private static TurnMove chooseTurn(
            TrainedAgent agent, GameLogic game, List<TurnMove> turns) {
        Random random = new Random();
        if (random.nextDouble() < agent.difficulty.exploration) {
            return turns.get(random.nextInt(turns.size()));
        }
        List<TurnMove> ordered = new ArrayList<>(turns);
        ordered.sort(Comparator.comparingLong(TurnMove::getCapturedCount).reversed());
        TurnMove best = ordered.getFirst();
        double bestScore = Double.NEGATIVE_INFINITY;
        for (TurnMove turn : ordered) {
            GameLogic next = applyCopy(game, turn);
            double score = -search(agent.weights, next,
                    agent.difficulty.searchDepth - 1,
                    Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
            if (score > bestScore) {
                best = turn;
                bestScore = score;
            }
        }
        return best;
    }

    private static double search(
            double[] weights, GameLogic game, int depth, double alpha, double beta) {
        if (game.isGameOver()) {
            return terminalValue(game, game.getCurrentPlayer());
        }
        if (depth <= 0) {
            return value(weights, features(game, game.getCurrentPlayer()));
        }

        List<TurnMove> turns = game.getRules().getLegalTurns(
                game.getBoard(), game.getCurrentPlayer());
        if (turns.isEmpty()) {
            return -1;
        }
        turns = new ArrayList<>(turns);
        turns.sort(Comparator.comparingLong(TurnMove::getCapturedCount).reversed());

        double best = Double.NEGATIVE_INFINITY;
        for (TurnMove turn : turns) {
            GameLogic next = applyCopy(game, turn);
            double score = -search(weights, next, depth - 1, -beta, -alpha);
            best = Math.max(best, score);
            alpha = Math.max(alpha, score);
            if (alpha >= beta) {
                break;
            }
        }
        return best;
    }

    private static GameLogic applyCopy(GameLogic game, TurnMove turn) {
        GameLogic copy = game.copy();
        playTurn(copy, turn);
        return copy;
    }

    private static void playTurn(GameLogic game, TurnMove turn) {
        for (MoveStep step : turn.steps()) {
            if (!game.move(step.startIndex(), step.endIndex())) {
                throw new IllegalStateException("Rules returned an unplayable legal turn: " + turn);
            }
        }
    }

    private static double[] features(GameLogic game, int player) {
        Board board = game.getBoard();
        int opponent = player == GameLogic.red_player
                ? GameLogic.black_player : GameLogic.red_player;
        double ownMen = 0;
        double otherMen = 0;
        double ownKings = 0;
        double otherKings = 0;
        double ownAdvance = 0;
        double otherAdvance = 0;
        double ownCenter = 0;
        double otherCenter = 0;
        int size = board.getBoardSize();
        double normalizer = Math.max(1, board.getConfig().getPiecesPerPlayer());

        for (int index = 0; index < board.getCellCount(); index++) {
            Piece piece = board.getPiece(index);
            if (piece == null || piece.isEmpty()) {
                continue;
            }
            boolean ours = player == GameLogic.red_player ? piece.isRed() : piece.isBlack();
            boolean theirs = player == GameLogic.red_player ? piece.isBlack() : piece.isRed();
            if (!ours && !theirs) {
                continue;
            }
            if (ours) {
                if (piece.isKing()) {
                    ownKings++;
                } else {
                    ownMen++;
                    ownAdvance += player == GameLogic.red_player
                            ? size - 1 - board.rowOf(index) : board.rowOf(index);
                }
                ownCenter += centerValue(board, index);
            } else {
                if (piece.isKing()) {
                    otherKings++;
                } else {
                    otherMen++;
                    otherAdvance += player == GameLogic.red_player
                            ? board.rowOf(index) : size - 1 - board.rowOf(index);
                }
                otherCenter += centerValue(board, index);
            }
        }

        int ownMoves = game.getRules().getLegalTurns(board, player).size();
        int otherMoves = game.getRules().getLegalTurns(board, opponent).size();
        return new double[] {
                1,
                (ownMen - otherMen) / normalizer,
                (ownKings - otherKings) / normalizer,
                (ownAdvance - otherAdvance) / (normalizer * Math.max(1, size - 1)),
                (ownCenter - otherCenter) / normalizer,
                Math.tanh((ownMoves - otherMoves) / normalizer)
        };
    }

    private static double centerValue(Board board, int index) {
        double center = (board.getBoardSize() - 1) / 2.0;
        double rowDistance = Math.abs(board.rowOf(index) - center) / Math.max(1, center);
        double colDistance = Math.abs(board.colOf(index) - center) / Math.max(1, center);
        return 1 - (rowDistance + colDistance) / 2;
    }

    private static double terminalValue(GameLogic game, int player) {
        return switch (game.getGameState()) {
            case DRAW, IN_PROGRESS -> 0;
            case RED_WON -> player == GameLogic.red_player ? 1 : -1;
            case BLACK_WON -> player == GameLogic.black_player ? 1 : -1;
        };
    }

    private static double value(double[] weights, double[] features) {
        double result = 0;
        for (int index = 0; index < FEATURE_COUNT; index++) {
            result += weights[index] * features[index];
        }
        return Math.tanh(result);
    }

    private static void updateWeights(double[] weights, double[] features, double error) {
        double derivative = 1 - Math.pow(value(weights, features), 2);
        for (int index = 0; index < FEATURE_COUNT; index++) {
            weights[index] += LEARNING_RATE * error * derivative * features[index];
            weights[index] = Math.max(-4, Math.min(4, weights[index]));
        }
    }

    private static double[] initialWeights() {
        return new double[] { 0, 1.1, 1.35, 0.16, 0.12, 0.08 };
    }

    private void publishBoard(GameLogic game) {
        GameLogic snapshot = game.copy();
        Platform.runLater(() -> drawBoard(snapshot));
    }

    private void drawBoard(GameLogic game) {
        Board board = game.getBoard();
        int size = board.getBoardSize();
        double squareSize = Math.min(48, 540.0 / size);
        boardView.getChildren().clear();
        boardView.setPrefSize(size * squareSize, size * squareSize);

        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                int index = board.indexAt(row, col);
                boolean playable = board.getConfig().isPlayableSquare(row, col);
                Color darkSquare = "SENEGALESE".equals(board.getConfig().getVariantId())
                        ? Color.web("#00853f") : Color.web("#52635d");
                Color lightSquare = "SENEGALESE".equals(board.getConfig().getVariantId())
                        ? Color.web("#fff4c2") : Color.web("#e8dfcd");
                Rectangle square = new Rectangle(squareSize, squareSize,
                        playable ? darkSquare : lightSquare);
                VBox cell = new VBox(square);
                cell.setAlignment(Pos.CENTER);
                Piece piece = index < 0 ? null : board.getPiece(index);
                if (piece != null && !piece.isEmpty()) {
                    Circle token = new Circle(squareSize * 0.34,
                            piece.isRed() ? Color.web("#b94b3a") : Color.web("#222a28"));
                    token.setStroke(piece.isKing() ? Color.GOLD : Color.web("#f4f1e8"));
                    token.setStrokeWidth(piece.isKing() ? 3 : 1.5);
                    javafx.scene.layout.StackPane stack = new javafx.scene.layout.StackPane(square, token);
                    boardView.add(stack, col, row);
                } else {
                    boardView.add(square, col, row);
                }
            }
        }
    }

    private static Path modelPath(String variantId, Difficulty difficulty) {
        return MODEL_DIRECTORY.resolve(variantId + "-" + difficulty.name().toLowerCase() + ".json");
    }

    private static Path resolveModelDirectory() {
        String configuredPath = System.getProperty("checkers.model.directory");
        if (configuredPath != null && !configuredPath.isBlank()) {
            return Path.of(configuredPath);
        }
        Path workingDirectory = Path.of("").toAbsolutePath();
        Path repositoryModelDirectory = workingDirectory.resolve(
                "gamelogic/src/main/resources/ai-models");
        if (java.nio.file.Files.isDirectory(workingDirectory.resolve("gamelogic"))) {
            return repositoryModelDirectory;
        }
        Path parentModelDirectory = workingDirectory.resolve(
                "../gamelogic/src/main/resources/ai-models").normalize();
        if (java.nio.file.Files.isDirectory(workingDirectory.resolve("../gamelogic"))) {
            return parentModelDirectory;
        }
        return Path.of("gamelogic/src/main/resources/ai-models");
    }

    private static String classpathModelPath(String variantId, Difficulty difficulty) {
        return "/ai-models/" + variantId + "-"
                + difficulty.name().toLowerCase() + ".json";
    }

    private static TrainedAgent loadOrInitialize(GameRules rules, Difficulty difficulty)
            throws IOException {
        if (hasTrainedAgent(rules.getVariantId(), difficulty)) {
            return loadAgent(rules.getVariantId(), difficulty);
        }
        return new TrainedAgent(rules.getVariantId(),
                rules.getConfig().getBoardSize(), difficulty, initialWeights());
    }

    private static void saveAgent(TrainedAgent agent) throws IOException {
        java.nio.file.Files.createDirectories(MODEL_DIRECTORY);
        ObjectNode document = MAPPER.createObjectNode();
        document.put("formatVersion", 1);
        document.put("variantId", agent.variantId);
        document.put("boardSize", agent.boardSize);
        document.put("difficulty", agent.difficulty.name());
        ArrayNode weights = document.putArray("weights");
        for (double weight : agent.weights) {
            weights.add(weight);
        }
        MAPPER.writerWithDefaultPrettyPrinter().writeValue(
                modelPath(agent.variantId, agent.difficulty).toFile(), document);
    }

    public static boolean hasTrainedAgent(String variantId, Difficulty difficulty) {
        if (java.nio.file.Files.isRegularFile(modelPath(variantId, difficulty))) {
            return true;
        }
        return ReinforcementLearningTrainer.class
            .getResource(classpathModelPath(variantId, difficulty)) != null;
    }

    public static TrainedAgent loadAgent(String variantId, Difficulty difficulty)
            throws IOException {
        JsonNode document;
        Path localModel = modelPath(variantId, difficulty);
        if (java.nio.file.Files.isRegularFile(localModel)) {
            document = MAPPER.readTree(localModel.toFile());
        } else {
            try (InputStream input = ReinforcementLearningTrainer.class
                    .getResourceAsStream(classpathModelPath(variantId, difficulty))) {
                if (input == null) {
                    throw new IOException("No packaged model exists for "
                            + variantId + " " + difficulty + ".");
                }
                document = MAPPER.readTree(input);
            }
        }
        if (document.path("formatVersion").asInt() != 1
                || !variantId.equals(document.path("variantId").asText())
                || !difficulty.name().equals(document.path("difficulty").asText())) {
            throw new IOException("Trained model does not match the selected variant and difficulty.");
        }
        JsonNode values = document.path("weights");
        if (!values.isArray() || values.size() != FEATURE_COUNT) {
            throw new IOException("Trained model has an unsupported feature format.");
        }
        double[] weights = new double[FEATURE_COUNT];
        for (int index = 0; index < FEATURE_COUNT; index++) {
            weights[index] = values.get(index).asDouble();
        }
        return new TrainedAgent(variantId, document.path("boardSize").asInt(),
                difficulty, weights);
    }

    public static TurnMove selectMove(TrainedAgent agent, GameLogic game) {
        if (!agent.variantId.equals(game.getConfig().getVariantId())
                || agent.boardSize != game.getConfig().getBoardSize()) {
            throw new IllegalArgumentException("The trained model is for a different game variant.");
        }
        List<TurnMove> turns = game.getRules().getLegalTurns(
                game.getBoard(), game.getCurrentPlayer());
        if (turns.isEmpty()) {
            return null;
        }
        return chooseTurn(agent, game, turns);
    }

    public static final class TrainedAgent {
        private final String variantId;
        private final int boardSize;
        private final Difficulty difficulty;
        private final double[] weights;

        TrainedAgent(
                String variantId, int boardSize, Difficulty difficulty, double[] weights) {
            this.variantId = variantId;
            this.boardSize = boardSize;
            this.difficulty = difficulty;
            this.weights = weights.clone();
        }
    }

    public static boolean shouldPlayAsAi(
            GameLogic game, TrainedAgent agent, boolean onlineMode, boolean thinking) {
        return game != null && agent != null && !onlineMode && !thinking
                && !game.isGameOver()
                && game.getCurrentPlayer() == GameLogic.black_player
                && agent.variantId.equals(game.getConfig().getVariantId())
                && agent.boardSize == game.getConfig().getBoardSize();
    }
}