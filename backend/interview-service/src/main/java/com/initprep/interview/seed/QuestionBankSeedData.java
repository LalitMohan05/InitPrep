package com.initprep.interview.seed;

import com.initprep.interview.enums.Difficulty;
import com.initprep.interview.enums.TargetRole;
import com.initprep.interview.enums.QuestionType;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

final class QuestionBankSeedData {
    private QuestionBankSeedData() { }

    record BankQuestion(String title, QuestionType type, Difficulty difficulty, String description,
                        String hints, String options, String correctAnswer, Set<String> topics,
                        Set<String> companies, Set<TargetRole> roles) { }

    static List<BankQuestion> mcq() {
        return read("/question-bank-mcq.tsv").stream().map(line -> {
            String[] fields = line.split("\\|", -1);
            if (fields.length != 8) throw new IllegalStateException("Malformed MCQ seed line: " + line);
            String[] choices = fields[5].split("~", -1);
            if (choices.length != 4) throw new IllegalStateException("MCQ must have four choices: " + fields[0]);
            return new BankQuestion(fields[0], QuestionType.MCQ, Difficulty.valueOf(fields[1]),
                "Interview prompt:\n" + fields[4].trim(), null,
                String.join("\n", java.util.stream.IntStream.range(0, choices.length)
                    .mapToObj(index -> (char) ('A' + index) + ". " + choices[index].trim()).toList()),
                fields[6].trim(), Set.of(fields[2].trim()), csv(fields[7]), roles(fields[3]));
        }).toList();
    }

    static List<BankQuestion> theory() {
        return read("/question-bank-theory.tsv").stream().map(line -> {
            String[] fields = line.split("\\|", -1);
            if (fields.length != 7) throw new IllegalStateException("Malformed theory seed line: " + line);
            return new BankQuestion(fields[3].trim(), QuestionType.THEORY, Difficulty.valueOf(fields[0]),
                "Interview prompt:\n" + fields[4].trim(),
                "A strong answer should cover:\n" + fields[5].trim(), null, null,
                Set.of(fields[1].trim()), csv(fields[6]), roles(fields[2]));
        }).toList();
    }

    private static List<String> read(String resourceName) {
        try (InputStream stream = QuestionBankSeedData.class.getResourceAsStream(resourceName)) {
            if (stream == null) throw new IllegalStateException("Missing question bank resource " + resourceName);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                return reader.lines().filter(line -> !line.isBlank() && !line.startsWith("#")).toList();
            }
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Could not read question bank " + resourceName, exception);
        }
    }

    private static Set<TargetRole> roles(String value) {
        return Arrays.stream(value.split(",")).map(String::trim).map(TargetRole::valueOf).collect(Collectors.toUnmodifiableSet());
    }

    private static Set<String> csv(String value) {
        return Arrays.stream(value.split(",")).map(String::trim).filter(item -> !item.isEmpty()).collect(Collectors.toUnmodifiableSet());
    }
}
