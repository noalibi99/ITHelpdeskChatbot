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
import java.util.concurrent.*;

public class ChatBot extends Application {
    private BorderPane root;
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

        // Root Layout
        root = new BorderPane();

        // Top Bar
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
            }
        } catch (Exception e) {
            System.err.println("Error loading image: " + e.getMessage());
        }

        Label appName = new Label("TechSolver");
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

        historyListView.setOnMouseClicked(e -> {
            String selected = historyListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                chatHistory.getChildren().clear();
                String[] parts = selected.split("\\|");
                if (parts.length == 2) {
                    String userInputText = parts[0].replace("User: ", "").trim();
                    String responseText = parts[1].replace("Response: ", "").trim();
                    int conversationId = -1;

                    try {
                        List<String> fullHistory = chatbotEngine.getConversationHistory();
                        int selectedIndex = historyListView.getSelectionModel().getSelectedIndex();
                        if (selectedIndex >= 0 && selectedIndex < fullHistory.size()) {
                            String fullEntry = fullHistory.get(selectedIndex);
                            String[] fullParts = fullEntry.split("\\|");
                            if (fullParts.length == 2) {
                                String fullUserInputText = fullParts[0].replace("User: ", "").replaceAll("^[0-9]+:\\s*", "").trim();
                                if (fullUserInputText.equals(userInputText)) {
                                    String[] userParts = fullParts[0].split(":");
                                    if (userParts.length >= 2) {
                                        conversationId = Integer.parseInt(userParts[1].trim());
                                    }
                                }
                            }
                        }
                    } catch (SQLException | NumberFormatException ex) {
                        System.err.println("Error retrieving conversation ID: " + ex.getMessage());
                    }

                    addMessageToChat("Utilisateur", userInputText);
                    try {
                        ChatbotEngine.ResponseInfo responseInfo = chatbotEngine.getResponseFromHistory(conversationId);
                        if (responseInfo != null) {
                            addMessageToChat("Bot", responseInfo.response, conversationId);
                        } else {
                            addMessageToChat("Bot", responseText, conversationId);
                        }
                    } catch (Exception ex) {
                        System.err.println("Error retrieving response from history: " + ex.getMessage());
                        addMessageToChat("Bot", responseText, conversationId);
                    }
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

        // Chat Area
        chatHistory = new VBox(12);
        chatHistory.setPadding(new Insets(15));
        chatHistory.setPrefWidth(580);

        typingIndicator = new Label("Typing...");
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

        // Input Area
        userInput = new TextField();
        userInput.setPromptText("Ask your question here...");
        userInput.getStyleClass().add("chat-input");

        sendButton = new Button();
        sendButton.setGraphic(new FontIcon("fas-paper-plane"));
        sendButton.getStyleClass().add("chat-send-button");
        sendButton.setOnAction(e -> handleUserInput());

        // Responsive binding
        userInput.prefWidthProperty().bind(scrollPane.widthProperty().subtract(sendButton.widthProperty()).subtract(40));

        HBox inputArea = new HBox(10, userInput, sendButton);
        inputArea.setPadding(new Insets(15));
        inputArea.setAlignment(Pos.CENTER_RIGHT);
        inputArea.getStyleClass().add("input-area");

        root.setBottom(inputArea); // ✅ Ensure this line is present

        root.getStyleClass().add("root-pane");

        Scene scene = new Scene(root, 1000, 500);
        scene.getStylesheets().add(getClass().getResource("/chat-dark.css").toExternalForm());

        stage.setTitle("TechSolver - Dark Theme");
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
        root.requestLayout(); // Force layout refresh
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
            for (String entry : history) {
                String[] parts = entry.split("\\|");
                if (parts.length == 2) {
                    String userInputText = parts[0].replace("User: ", "").replaceAll("^[0-9]+:\\s*", "").trim();
                    String responseTextRaw = parts[1].replace("Response: ", "").trim();
                    String responseTextPreview = responseTextRaw.length() > 15 ? responseTextRaw.substring(0, 15) + "..." : responseTextRaw;
                    historyListView.getItems().add("User: " + userInputText + " | Response: " + responseTextPreview);
                }
            }
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

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<ChatbotEngine.ResponseInfo> future = executor.submit(() -> {
            Thread.sleep(800);
            return chatbotEngine.getResponse(question);
        });

        new Thread(() -> {
            try {
                ChatbotEngine.ResponseInfo responseInfo = future.get();
                final String displayMessage = responseInfo.response != null ? responseInfo.response : "No response available";
                final String source = responseInfo.source != null ? responseInfo.source : "Unknown";
                final int conversationIdFinal = responseInfo.conversationId;

                javafx.application.Platform.runLater(() -> {
                    String messageToShow = displayMessage;
                    if ("Ollama".equals(source)) {
                        messageToShow += "\n(Response provided by Ollama)";
                    } else if ("Erreur".equals(source)) {
                        messageToShow += "\n(Error connecting to Ollama)";
                    }

                    addMessageToChat("Bot", messageToShow, conversationIdFinal);
                    typingIndicator.setVisible(false);
                    updateHistoryList();
                });

            } catch (Exception e) {
                System.err.println("Error processing response: " + e.getMessage());
                javafx.application.Platform.runLater(() -> {
                    addMessageToChat("Bot", "Error: " + e.getMessage(), -1);
                    typingIndicator.setVisible(false);
                });
            } finally {
                executor.shutdownNow();
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

    private void addMessageToChat(String sender, String message, int conversationId) {
        VBox messageContainer = new VBox(5);
        messageContainer.setPadding(new Insets(8, 12, 8, 12));
        messageContainer.setAlignment(sender.equals("Utilisateur") ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        Label messageLabel = new Label(message != null ? message : "No message available");
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(360);
        messageLabel.getStyleClass().add("message-label");
        messageLabel.setEffect(new DropShadow(3, Color.color(0, 0, 0, 0.3)));

        Label statusLabel = new Label();
        statusLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: gray;");

        if (sender.equals("Utilisateur")) {
            messageLabel.getStyleClass().add("user-message");
            statusLabel.setText("Sent");
        } else {
            messageLabel.getStyleClass().add("bot-message");
            if (conversationId != -1) {
                try {
                    String feedback = chatbotEngine.getFeedback(conversationId);
                    statusLabel.setText(feedback != null ? "Feedback: " + (feedback.equals("positive") ? "Utile" : "Non Utile") : "");
                } catch (SQLException e) {
                    statusLabel.setText("Error retrieving feedback");
                }
            }
        }

        messageContainer.getChildren().addAll(messageLabel, statusLabel);

        if (!sender.equals("Utilisateur") && conversationId != -1) {
            try {
                String feedback = chatbotEngine.getFeedback(conversationId);
                if (feedback == null) {
                    HBox feedbackBox = new HBox(5);
                    Button thumbsUpButton = new Button("Utile");
                    thumbsUpButton.getStyleClass().addAll("feedback-button", "positive");
                    Button thumbsDownButton = new Button("Non Utile");
                    thumbsDownButton.getStyleClass().addAll("feedback-button", "negative");

                    thumbsUpButton.setOnAction(e -> {
                        try {
                            chatbotEngine.submitFeedback(conversationId, true);
                            statusLabel.setText("Feedback: Utile");
                            thumbsUpButton.setDisable(true);
                            thumbsDownButton.setDisable(true);
                            updateHistoryList();
                            Notifications.create().title("Feedback Submitted").text("Utile").darkStyle().showInformation();
                        } catch (SQLException ex) {
                            showErrorNotification("Failed to record feedback: " + ex.getMessage());
                        }
                    });

                    thumbsDownButton.setOnAction(e -> {
                        try {
                            chatbotEngine.submitFeedback(conversationId, false);
                            statusLabel.setText("Feedback: Non Utile");
                            thumbsUpButton.setDisable(true);
                            thumbsDownButton.setDisable(true);
                            updateHistoryList();
                            Notifications.create().title("Feedback Submitted").text("Non Utile").darkStyle().showInformation();
                        } catch (SQLException ex) {
                            showErrorNotification("Failed to record feedback: " + ex.getMessage());
                        }
                    });

                    feedbackBox.getChildren().addAll(thumbsUpButton, thumbsDownButton);
                    messageContainer.getChildren().add(feedbackBox);
                }
            } catch (SQLException e) {
                System.err.println("Error checking feedback: " + e.getMessage());
            }
        }

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

        scrollPane.layout();
        scrollPane.setVvalue(1.0);
    }

    private void addMessageToChat(String sender, String message) {
        addMessageToChat(sender, message, -1);
    }

    private void showErrorNotification(String message) {
        Notifications.create()
                .title("Error")
                .text(message)
                .darkStyle()
                .showError();
    }

    public static void main(String[] args) {
        launch(args);
    }
}