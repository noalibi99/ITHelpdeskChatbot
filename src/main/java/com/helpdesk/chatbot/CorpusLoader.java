package com.helpdesk.chatbot;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class CorpusLoader {
    private static NLPProcessor processor;
    public static List<KbEntry> loadKbEntriesFromResource(String resourcePath) throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        InputStream is = CorpusLoader.class.getClassLoader().getResourceAsStream(resourcePath);
        if (is == null) {
            throw new IllegalArgumentException("Resource not found: " + resourcePath);
        }

        return mapper.readValue(is, new TypeReference<List<KbEntry>>() {});
    }

    public static void initializeProcessor() throws IOException {
        processor = new NLPProcessor();
    }

    // Tokenize entries (combine question + answer, lowercase, split)
    public static List<List<String>> tokenizeEntries(List<KbEntry> entries) {
        List<List<String>> tokenizedCorpus = new ArrayList<>();

        for (KbEntry entry : entries) {
            String text = entry.question.toLowerCase() + " " + entry.answer.toLowerCase() + String.join(" ", entry.tags);
            List<String> tokens = processor.preprocess(text);
            tokenizedCorpus.add(tokens);
        }

        return tokenizedCorpus;
    }
}
