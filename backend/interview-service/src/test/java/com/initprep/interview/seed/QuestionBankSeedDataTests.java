package com.initprep.interview.seed;

import com.initprep.interview.enums.Difficulty;
import com.initprep.interview.enums.QuestionType;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class QuestionBankSeedDataTests {
    @Test
    void mcqSeedHasEnoughUniqueQuestionsAndEveryAnswerMatchesOneOfFourOptions() {
        List<QuestionBankSeedData.BankQuestion> questions = QuestionBankSeedData.mcq();
        assertTrue(questions.size() >= 100);
        assertEquals(questions.size(), questions.stream().map(q -> q.title().toLowerCase(Locale.ROOT)).collect(Collectors.toSet()).size());
        assertDifficultyCapacity(questions);
        for (QuestionBankSeedData.BankQuestion question : questions) {
            assertEquals(QuestionType.MCQ, question.type());
            assertFalse(question.roles().isEmpty(), question.title());
            assertFalse(question.topics().isEmpty(), question.title());
            assertFalse(question.description().contains("\\n"), question.title());
            String[] options = question.options().split("\\R");
            assertEquals(4, options.length, question.title());
            assertTrue(options["ABCD".indexOf(question.correctAnswer())].startsWith(question.correctAnswer() + ". "), question.title());
        }
    }

    @Test
    void theorySeedHasExpectedAnswerOutlinesAndUniqueTitles() {
        List<QuestionBankSeedData.BankQuestion> questions = QuestionBankSeedData.theory();
        assertTrue(questions.size() >= 100);
        assertEquals(questions.size(), questions.stream().map(q -> q.title().toLowerCase(Locale.ROOT)).collect(Collectors.toSet()).size());
        assertDifficultyCapacity(questions);
        for (QuestionBankSeedData.BankQuestion question : questions) {
            assertEquals(QuestionType.THEORY, question.type());
            assertFalse(question.roles().isEmpty(), question.title());
            assertFalse(question.topics().isEmpty(), question.title());
            assertTrue(question.hints().contains("\n"), question.title());
            assertFalse(question.hints().contains("\\n"), question.title());
        }
    }

    private static void assertDifficultyCapacity(List<QuestionBankSeedData.BankQuestion> questions) {
        Set<Difficulty> represented = questions.stream().map(QuestionBankSeedData.BankQuestion::difficulty).collect(Collectors.toCollection(HashSet::new));
        for (Difficulty difficulty : List.of(Difficulty.EASY, Difficulty.MEDIUM, Difficulty.HARD)) {
            long count = questions.stream().filter(question -> question.difficulty() == difficulty).count();
            assertTrue(represented.contains(difficulty));
            assertTrue(count >= switch (difficulty) { case EASY -> 30; case MEDIUM -> 50; case HARD -> 20; });
        }
    }
}
