package com.helpdesk.chatbot;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        ChatbotEngine engine = new ChatbotEngine();

        Scanner scanner = new Scanner(System.in);
        System.out.println("Welcome to the Helpdesk chatbot! Type your question below (type 'exit' to quit):");

        while (true) {
            System.out.print("> ");
            String userPrompt = scanner.nextLine().trim();
            if (userPrompt.equalsIgnoreCase("exit")) {
                System.out.println("Goodbye!");
                break;
            }

            if (userPrompt.isEmpty()) {
                System.out.println("Please enter a valid question.");
                continue;
            }

            try {
                ChatbotEngine.ResponseInfo responseInfo = engine.getResponse(userPrompt);
                System.out.println("Answer (" + responseInfo.source + "): " + responseInfo.response);
            } catch (Exception e) {
                System.err.println("An error occurred while processing your request: " + e.getMessage());
            }
        }

        scanner.close();
        engine.close();
    }
}
