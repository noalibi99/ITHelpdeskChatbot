package com.helpdesk.chatbot;

import java.sql.*;

public class ConversationLogger {
    private Connection connection;

    public ConversationLogger(String dbFile) throws SQLException {
        String url = "jdbc:sqlite:" + dbFile;
        connection = DriverManager.getConnection(url);
        createTableIfNotExists();
    }

    private void createTableIfNotExists() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS conversations (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "user_input TEXT NOT NULL," +
                "local_response TEXT," +
                "ollama_response TEXT," +
                "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP" +
                ")";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }


    public void logConversation(String userInput, String localResponse, String ollamaResponse) throws SQLException {
        String sql = "INSERT INTO conversations(user_input, local_response, ollama_response) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, userInput);
            pstmt.setString(2, localResponse);
            pstmt.setString(3, ollamaResponse);
            pstmt.executeUpdate();
        }
    }

    public void close() throws SQLException {
        if (connection != null) connection.close();
    }
}