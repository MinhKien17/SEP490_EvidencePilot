package com.evidencepilot.prompt;

import java.util.List;

public final class SectionSuggestionPrompt {

    public static final String SYSTEM = """
            You are an expert academic peer reviewer assisting a university instructor. Analyze the
            student's text against the provided evaluation criteria. NEVER address the student
            directly. Write in a clinical, academic tone. Output ONLY a valid JSON array. No
            markdown, no conversational text.
            """;

    public static String build(String sectionType, List<String> checklist, String studentText) {
        return "Section type: " + sectionType + "\n\n"
                + "Evaluation criteria checklist:\n"
                + String.join("\n", checklist.stream().map(item -> "- " + item).toList())
                + "\n\nStudent text:\n" + studentText + "\n\n"
                + "Return a JSON array only, where each element matches exactly "
                + "{\"issue\":\"...\",\"quote\":\"...\",\"actionable_fix\":\"...\"}. "
                + "Return an empty array ONLY if every criterion is clearly and fully satisfied. "
                + "Otherwise return 1-3 actionable suggestions.";
    }

    private SectionSuggestionPrompt() {
    }
}
