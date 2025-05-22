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

        public ResponseInfo(String response, String source) {
            this.response = response;
            this.source = source;
        }
    }

    public ResponseInfo getResponse(String userInput) throws Exception {
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
            logger.logConversation(userInput, localResponse, null);
            return new ResponseInfo(localResponse, "Base locale");
        } else {
            try {
                ollamaResponse = ollamaClient.answerHelpdeskQuestion(userInput);
                logger.logConversation(userInput, null, ollamaResponse);
                return new ResponseInfo(ollamaResponse, "Ollama");
            } catch (Exception e) {
                ollamaResponse = "Error: Ollama unavailable.";
                logger.logConversation(userInput, null, ollamaResponse);
                return new ResponseInfo(ollamaResponse, "Erreur");
            }
        }
    }

    public List<String> getConversationHistory() throws SQLException {
        List<String> history = new ArrayList<>();
        try (Connection conn = logger.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT user_input, local_response, ollama_response FROM conversations ORDER BY timestamp DESC")) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String userInput = rs.getString("user_input");
                String localResponse = rs.getString("local_response");
                String ollamaResponse = rs.getString("ollama_response");
                String response = localResponse != null ? localResponse : ollamaResponse;
                if (userInput != null && response != null) {
                    history.add("User: " + userInput + " | Response: " + response);
                }
            }
        }
        return history;
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