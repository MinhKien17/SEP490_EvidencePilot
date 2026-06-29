package com.evidencepilot.service.impl;

import com.evidencepilot.dto.SparseVector;
import com.google.common.hash.Hashing;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Slf4j
@Service
public class SparseVectorGenerator {

    private static final Pattern PUNCTUATION = Pattern.compile("[^a-z0-9\\s]");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private static final Set<String> STOP_WORDS = Set.of(
        "a", "an", "the", "and", "or", "but", "if", "because", "as",
        "this", "that", "these", "those", "then", "just", "so", "than",
        "such", "both", "through", "about", "for", "is", "of", "while", "during",
        "to", "from", "in", "on", "at", "by", "with", "without", "into", "onto",
        "per", "among", "between", "above", "below", "under", "over", "after",
        "before", "against", "within", "along", "across", "behind", "beneath",
        "beside", "beyond", "up", "down", "out", "off", "around",
        "it", "its", "you", "your", "he", "him", "his", "she", "her", "we",
        "us", "our", "they", "them", "their",
        "i", "me", "my", "am", "are", "was", "were", "be", "been", "being",
        "have", "has", "had", "having", "do", "does", "did", "doing",
        "will", "would", "shall", "should", "may", "might", "must", "can", "could",
        "not", "no", "nor", "never", "now", "here", "there",
        "all", "each", "every", "few", "more", "most", "other", "some", "any",
        "many", "much", "several", "own", "same", "only", "very", "too", "also",
        "still", "even", "already", "yet", "however", "though", "although",
        "else", "instead", "rather", "whether", "why", "how", "when", "where",
        "who", "whom", "whose", "which", "what"
    );

    public SparseVector generate(String text) {
        if (text == null || text.isBlank()) {
            return new SparseVector(List.of(), List.of());
        }

        String normalized = PUNCTUATION.matcher(text.toLowerCase(Locale.ROOT)).replaceAll("");
        String[] tokens = WHITESPACE.split(normalized.trim());

        Map<Integer, Integer> freq = new HashMap<>();
        for (String token : tokens) {
            if (token.isEmpty() || STOP_WORDS.contains(token)) {
                continue;
            }
            int hash = Hashing.murmur3_32().hashString(token, StandardCharsets.UTF_8).asInt();
            int index = hash & Integer.MAX_VALUE;
            freq.merge(index, 1, Integer::sum);
        }

        List<Integer> sortedIndices = new ArrayList<>(freq.keySet());
        Collections.sort(sortedIndices);

        List<Long> indices = new ArrayList<>(sortedIndices.size());
        List<Float> values = new ArrayList<>(sortedIndices.size());
        for (int idx : sortedIndices) {
            indices.add((long) idx);
            values.add((float) freq.get(idx));
        }

        return new SparseVector(indices, values);
    }
}
