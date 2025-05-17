package com.helpdesk.chatbot;

import java.util.*;

public class TFIDFMatcher {

    private final List<List<String>> corpus;  // tokenized documents
    private Map<String, Integer> df;    // document frequency for each term
    private int totalDocs;

    public TFIDFMatcher(List<List<String>> corpus) {
        this.corpus = corpus;
        this.totalDocs = corpus.size();
        computeDocumentFrequency();
    }

    private void computeDocumentFrequency() {
        df = new HashMap<>();
        for (List<String> doc : corpus) {
            Set<String> uniqueTerms = new HashSet<>(doc);
            for (String term : uniqueTerms) {
                df.put(term, df.getOrDefault(term, 0) + 1);
            }
        }
    }

    // Compute TF for a single document
    private Map<String, Double> computeTF(List<String> doc) {
        Map<String, Double> tf = new HashMap<>();
        int docLength = doc.size();
        for (String term : doc) {
            tf.put(term, tf.getOrDefault(term, 0.0) + 1.0);
        }
        // Normalize TF by dividing by doc length
        for (String term : tf.keySet()) {
            tf.put(term, tf.get(term) / docLength);
        }
        return tf;
    }

    // Compute IDF for a term
    private double computeIDF(String term) {
        int docFreq = df.getOrDefault(term, 0);
        if (docFreq == 0) {
            return 0; // or small smoothing value like 1e-6
        }
        return Math.log((double) totalDocs / docFreq);
    }

    // Compute TF-IDF vector for a document
    public Map<String, Double> computeTFIDF(List<String> doc) {
        Map<String, Double> tfidf = new HashMap<>();
        Map<String, Double> tf = computeTF(doc);

        for (String term : tf.keySet()) {
            double idf = computeIDF(term);
            tfidf.put(term, tf.get(term) * idf);
        }
        return tfidf;
    }

    // Cosine similarity between two TF-IDF vectors
    public static double cosineSimilarity(Map<String, Double> vec1, Map<String, Double> vec2) {
        Set<String> allTerms = new HashSet<>();
        allTerms.addAll(vec1.keySet());
        allTerms.addAll(vec2.keySet());

        double dotProduct = 0.0;
        double normVec1 = 0.0;
        double normVec2 = 0.0;

        for (String term : allTerms) {
            double v1 = vec1.getOrDefault(term, 0.0);
            double v2 = vec2.getOrDefault(term, 0.0);
            dotProduct += v1 * v2;
            normVec1 += v1 * v1;
            normVec2 += v2 * v2;
        }

        if (normVec1 == 0 || normVec2 == 0) return 0.0;
        return dotProduct / (Math.sqrt(normVec1) * Math.sqrt(normVec2));
    }
}
