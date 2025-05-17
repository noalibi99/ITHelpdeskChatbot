package com.helpdesk.chatbot;

import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.lemmatizer.DictionaryLemmatizer;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class NLPProcessor {
    private final TokenizerME tokenizer;
    private final DictionaryLemmatizer lemmatizer;
    private final POSTaggerME posTagger;
    private final Set<String> stopWords;
    public NLPProcessor() throws IOException {
        try(InputStream tokenizerModelIn = getClass().getResourceAsStream("/models/en-token.bin");
            InputStream stopWordsIn = getClass().getResourceAsStream("/models/en-stopwords.txt");
            InputStream POSmodelIn = getClass().getResourceAsStream("/models/en-pos-maxent.bin");
            InputStream lemmatizerIn = getClass().getResourceAsStream("/models/en-lemmatizer.dict")) {
            if (tokenizerModelIn == null) {
                throw new IOException("Tokenizer model file not found");
            }
            if (stopWordsIn == null) {
                throw new IOException("Stop-words file not found");
            }
            if (lemmatizerIn == null) {
                throw new IOException("Lemmatizer file not found");
            }
            if (POSmodelIn == null) {
                throw new IOException("POS model file not found");
            }
            TokenizerModel model = new TokenizerModel(tokenizerModelIn);
            POSModel posModel = new POSModel(POSmodelIn);
            posTagger = new POSTaggerME(posModel);
            lemmatizer = new DictionaryLemmatizer(lemmatizerIn);
            tokenizer = new TokenizerME(model);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stopWordsIn))) {
                stopWords = reader.lines()
                        .map(String::trim)
                        .filter(line -> !line.isEmpty() && !line.startsWith("#")) // skip empty and comment lines
                        .map(String::toLowerCase)
                        .collect(Collectors.toSet());
            }
        }
    }

//    public String[] tokenize(String text) {
//        return tokenizer.tokenize(text.toLowerCase());
//    }
//
//    public String[] removeStopWords(String[] tokens) {
//        return java.util.Arrays.stream(tokens)
//                .filter(token -> !stopWords.contains(token))
//                .toArray(String[]::new);
//    }
//
//    public String[] getPOSTags(String[] tokens) {
//        return posTagger.tag(tokens);
//    }
//
//    public String[] getLemmas(String[] tokens, String[] posTags) {
//        return lemmatizer.lemmatize(tokens, posTags);
//    }

    public List<String> preprocess(String text) {
        // Step 1: Tokenize
        String[] tokens = tokenizer.tokenize(text);

        // Step 2: Remove stopwords
        List<String> filteredTokens = Arrays.stream(tokens)
                .map(String::toLowerCase)
                .filter(token -> !stopWords.contains(token))
                .toList();

        // Step 3: POS tagging
        String[] filteredArray = filteredTokens.toArray(new String[0]);
        String[] posTags = posTagger.tag(filteredArray);

        // Step 4: Lemmatize
        String[] lemmas = lemmatizer.lemmatize(filteredArray, posTags);

        // Step 5: Return lemmatized tokens (fallback to original if lemma is "O")
        List<String> finalTokens = new ArrayList<>();
        for (String lemma : lemmas) {
            if (!lemma.equals("O")) {
                finalTokens.add(lemma);
            }
        }

        return finalTokens;
    }

}
