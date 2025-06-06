package com.helpdesk.chatbot;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;

public class ConversationLogger {
    private final String dbUrl;
    private Connection connection;

    public ConversationLogger(String dbPath) throws SQLException {
        this.dbUrl = "jdbc:sqlite:" + dbPath;
        initializeDatabase();
    }

    private void initializeDatabase() throws SQLException {
        connection = DriverManager.getConnection(dbUrl);
        try (Statement stmt = connection.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS conversations (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP," +
                    "user_input TEXT," +
                    "local_response TEXT," +
                    "ollama_response TEXT," +
                    "feedback TEXT)";
            stmt.execute(sql);
        }
    }

    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(dbUrl);
        }
        return connection;
    }

    public long logConversation(String userInput, String localResponse, String ollamaResponse) throws SQLException {
        String sql = "INSERT INTO conversations (user_input, local_response, ollama_response) VALUES (?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, userInput);
            stmt.setString(2, localResponse);
            stmt.setString(3, ollamaResponse);
            stmt.executeUpdate();

            // Récupérer l'ID généré
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1); // Retourner l'ID auto-incrémenté
                } else {
                    throw new SQLException("Failed to retrieve generated ID.");
                }
            }
        }
    }

    public void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}