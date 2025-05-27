package com.helpdesk.chatbot;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ChatbotEngine {
    private static NLPProcessor processor;
    private static List<KbEntry> kbEntries;
    private static List<List<String>> corpus;
    private static TFIDFMatcher tfidf;
    private static OllamaClient ollamaClient;
    private static ConversationLogger logger;

    public ChatbotEngine() throws Exception {
        CorpusLoader.initializeProcessor();
        processor = new NLPProcessor();
        kbEntries = CorpusLoader.loadKbEntriesFromResource("knowledge_base.json");
        corpus = CorpusLoader.tokenizeEntries(kbEntries);
        tfidf = new TFIDFMatcher(corpus);
        ollamaClient = new OllamaClient();
        logger = new ConversationLogger("chatbot_conversations.db");
    }

    public class ResponseInfo {
        public String response;
        public String source;
        public int conversationId;

        public ResponseInfo(String response, String source, int conversationId) {
            this.response = response != null ? response : "No response available";
            this.source = source != null ? source : "Unknown";
            this.conversationId = conversationId;
        }
    }

    public ResponseInfo getResponse(String userInput) throws Exception {
        if (userInput == null || userInput.trim().isEmpty()) {
            int conversationId = (int) logger.logConversation(userInput, "Error: Empty input provided.", null);
            return new ResponseInfo("Error: Empty input provided.", "Local", conversationId);
        }

        List<String> processedInput = processor.preprocess(userInput);
        Map<String, Double> userVec = tfidf.computeTFIDF(processedInput);

        double bestScore = -1;
        int bestDocIdx = -1;
        for (int i = 0; i < corpus.size(); i++) {
            Map<String, Double> docVec = tfidf.computeTFIDF(corpus.get(i));
            double score = TFIDFMatcher.cosineSimilarity(userVec, docVec);
            if (score > bestScore) {
                bestScore = score;
                bestDocIdx = i;
            }
        }

        double similarityThreshold = 0.34;
        String localResponse = null;
        String ollamaResponse = null;

        if (bestDocIdx != -1 && bestScore >= similarityThreshold) {
            localResponse = kbEntries.get(bestDocIdx).answer;
            if (localResponse == null) {
                localResponse = "Error: No answer available in local database.";
            }
            int conversationId = (int) logger.logConversation(userInput, localResponse, null);
            return new ResponseInfo(localResponse, "Base locale", conversationId);
        } else {
            try {
                ollamaResponse = ollamaClient.answerHelpdeskQuestion(userInput);
                if (ollamaResponse == null) {
                    ollamaResponse = "Error: Ollama returned no response.";
                }
                int conversationId = (int) logger.logConversation(userInput, null, ollamaResponse);
                return new ResponseInfo(ollamaResponse, "Ollama", conversationId);
            } catch (Exception e) {
                ollamaResponse = "Error: Ollama unavailable. " + e.getMessage();
                int conversationId = (int) logger.logConversation(userInput, null, ollamaResponse);
                return new ResponseInfo(ollamaResponse, "Erreur", conversationId);
            }
        }
    }

    public List<String> getConversationHistory() throws SQLException {
        List<String> history = new ArrayList<>();
        try (Connection conn = logger.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT user_input, local_response, ollama_response, id FROM conversations ORDER BY timestamp DESC")) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String userInput = rs.getString("user_input");
                String localResponse = rs.getString("local_response");
                String ollamaResponse = rs.getString("ollama_response");
                int conversationId = rs.getInt("id");
                String response = localResponse != null ? localResponse : ollamaResponse;
                if (userInput != null && response != null) {
                    history.add("User: " + conversationId + ": " + userInput + " | Response: " + response);
                } else {
                    System.err.println("Skipping history entry due to null userInput or response: ID=" + conversationId);
                }
            }
        }
        return history;
    }

    public ResponseInfo getResponseFromHistory(int conversationId) throws SQLException {
        try (Connection conn = logger.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT local_response, ollama_response FROM conversations WHERE id = ?")) {
            stmt.setInt(1, conversationId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String localResponse = rs.getString("local_response");
                String ollamaResponse = rs.getString("ollama_response");
                String response = localResponse != null ? localResponse : ollamaResponse;
                if (response == null) {
                    response = "Error: No response found in history.";
                }
                return new ResponseInfo(response, localResponse != null ? "Base locale" : "Ollama", conversationId);
            }
        }
        return new ResponseInfo("Error: Conversation not found in history.", "Local", conversationId);
    }

    public String getFeedback(int conversationId) throws SQLException {
        try (Connection conn = logger.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT feedback FROM conversations WHERE id = ?")) {
            stmt.setInt(1, conversationId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("feedback");
            }
        }
        return null;
    }

    public void submitFeedback(int conversationId, boolean isPositive) throws SQLException {
        try (Connection conn = logger.getConnection();
             PreparedStatement stmt = conn.prepareStatement("UPDATE conversations SET feedback = ? WHERE id = ?")) {
            stmt.setString(1, isPositive ? "positive" : "negative");
            stmt.setInt(2, conversationId);
            stmt.executeUpdate();
        }
    }

    public void clearHistory() throws SQLException {
        try (Connection conn = logger.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM conversations")) {
            stmt.executeUpdate();
        }
    }

    public void close() throws Exception {
        if (logger != null) logger.close();
    }
}