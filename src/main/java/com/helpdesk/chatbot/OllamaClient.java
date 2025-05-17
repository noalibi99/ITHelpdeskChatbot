package com.helpdesk.chatbot;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

import org.json.JSONObject;

/**
 * Client for interacting with Ollama API
 * Sends HTTP requests to locally running Ollama instance
 */
public class OllamaClient {
    private static final String DEFAULT_URL = "http://localhost:11434/api/generate";
    private static final String DEFAULT_MODEL = "llama3.2";

    private final String apiUrl;
    private final String model;

    /**
     * Creates a new OllamaClient with default settings
     */
    public OllamaClient() {
        this(DEFAULT_URL, DEFAULT_MODEL);
    }

    /**
     * Creates a new OllamaClient with custom settings
     *
     * @param apiUrl The URL of the Ollama API
     * @param model The model name to use
     */
    public OllamaClient(String apiUrl, String model) {
        this.apiUrl = apiUrl;
        this.model = model;
    }

    /**
     * Sends a prompt to Ollama and returns the generated response
     *
     * @param prompt The prompt to send to Ollama
     * @return The generated response
     * @throws IOException If an error occurs while communicating with the API
     */
    public String generateResponse(String prompt) throws IOException {
        URL url = new URL(apiUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        // Create the request body
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", model);
        requestBody.put("prompt", prompt);
        requestBody.put("stream", false); // We want the complete response at once

        // Send the request
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = requestBody.toString().getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        // Read the response
        StringBuilder response = new StringBuilder();
        try (Scanner scanner = new Scanner(connection.getInputStream(), "utf-8")) {
            while (scanner.hasNextLine()) {
                response.append(scanner.nextLine());
            }
        }

        // Parse the JSON response
        JSONObject jsonResponse = new JSONObject(response.toString());
        return jsonResponse.getString("response");
    }

    /**
     * Sends a help desk question to Ollama with appropriate context
     *
     * @param userQuestion The user's question
     * @return The generated answer from Ollama
     * @throws IOException If an error occurs while communicating with the API
     */
    public String answerHelpdeskQuestion(String userQuestion) throws IOException {
        String prompt = String.format(
                "You are a helpful IT support assistant. Please answer the following question clearly and concisely:\n\n%s",
                userQuestion
        );
        return generateResponse(prompt);
    }
}
