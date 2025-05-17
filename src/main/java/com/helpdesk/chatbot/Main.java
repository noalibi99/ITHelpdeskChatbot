package com.helpdesk.chatbot;

import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        NLPProcessor processor = new NLPProcessor();
        CorpusLoader.initializeProcessor();
        String resourcePath = "knowledge_base.json";
        List<KbEntry> kbEntries = CorpusLoader.loadKbEntriesFromResource(resourcePath);

        List<List<String>> corpus = CorpusLoader.tokenizeEntries(kbEntries);

        System.out.println("Knowledge base loaded with " + kbEntries.size() + " entries.");
        TFIDFMatcher tfidf = new TFIDFMatcher(corpus);

        OllamaClient ollamaClient = new OllamaClient();
        System.out.println("Ollama client initialized. Will be used as fallback.");

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

            List<String> processedUserInput = processor.preprocess(userPrompt);
            Map<String, Double> userVec = tfidf.computeTFIDF(processedUserInput);

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

            if (bestDocIdx != -1 && bestScore >= similarityThreshold) {
                System.out.println("Answer: " + kbEntries.get(bestDocIdx).answer);
                System.out.println("Similarity score: " + bestScore);
            } else {
                System.out.println("No good match found locally. Sending prompt to Ollama...");
                try {
                    String ollamaResponse = ollamaClient.answerHelpdeskQuestion(userPrompt);
                    System.out.println("Ollama response: " + ollamaResponse);
                } catch (Exception e) {
                    System.out.println("Error connecting to Ollama: " + e.getMessage());
                    System.out.println("Please make sure Ollama is running on your system.");
                }
            }
        }

        scanner.close();
    }
}