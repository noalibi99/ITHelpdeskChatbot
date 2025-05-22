package com.helpdesk.chatbot;

import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.controlsfx.control.Notifications;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;

public class ChatBot extends Application {

    private VBox chatHistory;
    private ScrollPane scrollPane;
    private TextField userInput;
    private Button sendButton;
    private ChatbotEngine chatbotEngine;
    private Label typingIndicator;
    private ListView<String> historyListView;
    private VBox sidebar;
    private boolean sidebarVisible = false;

    @Override
    public void start(Stage stage) {
        try {
            chatbotEngine = new ChatbotEngine();
        } catch (Exception e) {
            System.err.println("Error initializing engine: " + e.getMessage());
            return;
        }

        // Layout principal
        BorderPane root = new BorderPane();

        // Navbar améliorée
        HBox topBar = new HBox(10);
        topBar.setPadding(new Insets(10, 15, 10, 15));
        topBar.getStyleClass().add("top-bar");
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setEffect(new DropShadow(5, Color.color(0, 0, 0, 0.2)));

        Button toggleSidebarButton = new Button();
        toggleSidebarButton.setGraphic(new FontIcon("fas-bars"));
        toggleSidebarButton.getStyleClass().add("menu-button");
        toggleSidebarButton.setOnAction(e -> toggleSidebar());

        ImageView logo = null;
        try (InputStream inputStream = getClass().getResourceAsStream("/chatbot.png")) {
            if (inputStream != null) {
                logo = new ImageView(new Image(inputStream));
                logo.setFitHeight(30);
                logo.setPreserveRatio(true);
                logo.setSmooth(true);
            } else {
                System.err.println("Error: Could not load robot.png");
            }
        } catch (Exception e) {
            System.err.println("Error loading image: " + e.getMessage());
        }

        Label appName = new Label("Support Chatbot");
        appName.getStyleClass().add("top-bar-label");

        topBar.getChildren().addAll(toggleSidebarButton, logo != null ? logo : new Label("Logo Missing"), appName);
        root.setTop(topBar);

        // Sidebar
        sidebar = new VBox(10);
        sidebar.setPrefWidth(250);
        sidebar.setStyle("-fx-background-color: #2a2a2a; -fx-padding: 10;");
        sidebar.setVisible(sidebarVisible);

        Button newChatButton = new Button("New Chat");
        newChatButton.setGraphic(new FontIcon("fas-plus"));
        newChatButton.getStyleClass().add("sidebar-button");
        newChatButton.setMaxWidth(Double.MAX_VALUE);
        newChatButton.setOnAction(e -> startNewChat());

        historyListView = new ListView<>();
        historyListView.getStyleClass().add("history-list");
        updateHistoryList();
        // Ajout de l'événement de clic pour recharger la conversation
        historyListView.setOnMouseClicked(e -> {
            String selected = historyListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                chatHistory.getChildren().clear();
                String[] parts = selected.split("\\|");
                if (parts.length == 2) {
                    String userInputText = parts[0].replace("User: ", "").trim();
                    String responseText = parts[1].replace("Response: ", "").trim();
                    addMessageToChat("Utilisateur", userInputText);
                    addMessageToChat("Bot", responseText);
                }
            }
        });

        Button clearHistoryButton = new Button("Clear History");
        clearHistoryButton.setGraphic(new FontIcon("fas-trash"));
        clearHistoryButton.getStyleClass().add("sidebar-button");
        clearHistoryButton.setMaxWidth(Double.MAX_VALUE);
        clearHistoryButton.setOnAction(e -> {
            try {
                chatbotEngine.clearHistory();
                updateHistoryList();
                chatHistory.getChildren().clear();
            } catch (SQLException ex) {
                System.err.println("Error clearing history: " + ex.getMessage());
            }
        });

        sidebar.getChildren().addAll(newChatButton, historyListView, clearHistoryButton);
        root.setLeft(sidebar);

        // Zone de chat
        chatHistory = new VBox(12);
        chatHistory.setPadding(new Insets(15));
        chatHistory.setPrefWidth(580);

        typingIndicator = new Label();
        typingIndicator.setTextFill(Color.LIGHTGRAY);
        typingIndicator.setPadding(new Insets(5, 10, 5, 10));
        typingIndicator.setVisible(false);

        VBox chatContainer = new VBox(chatHistory, typingIndicator);
        chatContainer.setSpacing(5);
        chatContainer.setAlignment(Pos.TOP_CENTER);

        scrollPane = new ScrollPane(chatContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.getStyleClass().add("chat-scroll-pane");

        chatHistory.prefWidthProperty().bind(scrollPane.widthProperty().subtract(20));

        root.setCenter(scrollPane);

        // Zone de saisie en bas
        userInput = new TextField();
        userInput.setPromptText("Ask your question here...");
        userInput.setPrefWidth(480);
        userInput.getStyleClass().add("chat-input");

        sendButton = new Button();
        sendButton.setGraphic(new FontIcon("fas-paper-plane"));
        sendButton.getStyleClass().add("chat-send-button");
        sendButton.setOnAction(e -> handleUserInput());

        userInput.prefWidthProperty().bind(scrollPane.widthProperty().subtract(sendButton.widthProperty()).subtract(40));

        HBox inputArea = new HBox(10, userInput, sendButton);
        inputArea.setPadding(new Insets(15));
        inputArea.setAlignment(Pos.CENTER);
        inputArea.getStyleClass().add("input-area");

        root.setBottom(inputArea);
        root.getStyleClass().add("root-pane");

        Scene scene = new Scene(root, 1000, 500);
        scene.getStylesheets().add(getClass().getResource("/chat-dark.css").toExternalForm());

        stage.setTitle("Technical Support Chatbot - Dark Theme");
        stage.setScene(scene);
        stage.show();

        userInput.requestFocus();

        stage.setOnCloseRequest(e -> {
            try {
                chatbotEngine.close();
            } catch (Exception ex) {
                System.err.println("Error during shutdown: " + ex.getMessage());
            }
        });
    }

    private void toggleSidebar() {
        sidebarVisible = !sidebarVisible;
        sidebar.setVisible(sidebarVisible);
        sidebar.setManaged(sidebarVisible);
    }

    private void startNewChat() {
        chatHistory.getChildren().clear();
        userInput.clear();
        Notifications.create()
                .title("New Chat Started")
                .text("A new chat session has been started.")
                .darkStyle()
                .showInformation();
    }

    private void updateHistoryList() {
        try {
            List<String> history = chatbotEngine.getConversationHistory();
            historyListView.getItems().clear();
            historyListView.getItems().addAll(history);
        } catch (SQLException e) {
            System.err.println("Error retrieving history: " + e.getMessage());
        }
    }

    private void handleUserInput() {
        String question = userInput.getText().trim();
        if (question.isEmpty()) return;

        addMessageToChat("Utilisateur", question);
        userInput.clear();

        typingIndicator.setVisible(true);
        typingIndicator.setText("Searching...");

        new Thread(() -> {
            try {
                Thread.sleep(800);
                ChatbotEngine.ResponseInfo responseInfo = chatbotEngine.getResponse(question);
                javafx.application.Platform.runLater(() -> {
                    String displayMessage = responseInfo.response;
                    if ("Ollama".equals(responseInfo.source)) {
                        displayMessage += "\n(Response provided by Ollama)";
                    } else if ("Erreur".equals(responseInfo.source)) {
                        displayMessage += "\n(Error connecting to Ollama)";
                    }
                    addMessageToChat("Bot", displayMessage);
                    typingIndicator.setVisible(false);
                    updateHistoryList(); // Mise à jour de l'historique après chaque réponse
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    addMessageToChat("Bot", "Error: " + e.getMessage());
                    typingIndicator.setVisible(false);
                });
            }
        }).start();

        scrollPane.layout();
        scrollPane.setVvalue(1.0);

        Notifications.create()
                .title("Response Received")
                .text("The chatbot has responded to your question.")
                .darkStyle()
                .showInformation();
    }

    private void addMessageToChat(String sender, String message) {
        VBox messageContainer = new VBox();
        messageContainer.setPadding(new Insets(8, 12, 8, 12));
        messageContainer.setMaxWidth(520);

        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(360);
        messageLabel.getStyleClass().add("message-label");
        messageLabel.setEffect(new DropShadow(3, Color.color(0, 0, 0, 0.3)));

        Label statusLabel = new Label();
        statusLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: gray;");

        if (sender.equals("Utilisateur")) {
            messageContainer.setAlignment(Pos.CENTER_RIGHT);
            messageLabel.getStyleClass().add("user-message");
            statusLabel.setText("Sent");
        } else {
            messageContainer.setAlignment(Pos.CENTER_LEFT);
            messageLabel.getStyleClass().add("bot-message");
            statusLabel.setText("");
        }

        messageContainer.getChildren().addAll(messageLabel, statusLabel);
        messageContainer.setOpacity(0);
        chatHistory.getChildren().add(messageContainer);

        FadeTransition fade = new FadeTransition(Duration.millis(400), messageContainer);
        fade.setFromValue(0);
        fade.setToValue(1);

        TranslateTransition slide = new TranslateTransition(Duration.millis(400), messageContainer);
        slide.setFromY(20);
        slide.setToY(0);

        fade.play();
        slide.play();
    }

    public static void main(String[] args) {
        launch();
    }
}