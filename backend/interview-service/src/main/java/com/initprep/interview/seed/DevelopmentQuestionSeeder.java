package com.initprep.interview.seed;

import com.initprep.interview.entity.Company;
import com.initprep.interview.entity.Question;
import com.initprep.interview.entity.TestCase;
import com.initprep.interview.entity.Topic;
import com.initprep.interview.enums.Difficulty;
import com.initprep.interview.enums.QuestionType;
import com.initprep.interview.repository.CompanyRepo;
import com.initprep.interview.repository.QuestionRepo;
import com.initprep.interview.repository.TestCaseRepo;
import com.initprep.interview.repository.TopicRepo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/** Replaces question content only when explicitly enabled for a development run. */
@Slf4j
@Component
@Profile("dev")
@ConditionalOnProperty(name = "initprep.seed.enabled", havingValue = "true")
@RequiredArgsConstructor
public class DevelopmentQuestionSeeder implements ApplicationRunner {
    private static final int CASE_COUNT = 30;
    private static final String JAVA_STARTER = """
        import java.util.*;

        public class Main {
            public static void main(String[] args) throws Exception {
                Scanner in = new Scanner(System.in);
                // Read the input format described in the problem.
                // Compute the result and print it to standard output.
            }
        }
        """;

    private final QuestionRepo questionRepo;
    private final TestCaseRepo testCaseRepo;
    private final TopicRepo topicRepo;
    private final CompanyRepo companyRepo;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<ProblemSeed> problems = problemSeeds();
        validateSeedData(problems);

        List<Question> oldQuestions = questionRepo.findAll();
        oldQuestions.forEach(question -> {
            question.getTopics().clear();
            question.getCompanies().clear();
        });
        questionRepo.saveAll(oldQuestions);
        questionRepo.flush();
        testCaseRepo.deleteAllInBatch();
        questionRepo.deleteAllInBatch();

        Map<String, Topic> topics = loadTopics(problems);
        Map<String, Company> companies = loadCompanies(problems);
        List<Question> questions = problems.stream()
            .map(problem -> toQuestion(problem, topics, companies))
            .toList();
        questionRepo.saveAll(questions);
        questionRepo.flush();

        verifyPersistedDataset();
        log.info("Development seed replaced question data with {} coding questions and {} test cases.",
            questions.size(), questions.size() * CASE_COUNT);
    }

    private Map<String, Topic> loadTopics(List<ProblemSeed> problems) {
        Set<String> names = problems.stream().flatMap(p -> p.topics().stream()).collect(Collectors.toSet());
        Map<String, Topic> result = topicRepo.findAll().stream()
            .filter(topic -> names.contains(topic.getName()))
            .collect(Collectors.toMap(Topic::getName, Function.identity()));
        List<Topic> missing = names.stream().filter(name -> !result.containsKey(name))
            .map(name -> Topic.builder().name(name).build()).toList();
        topicRepo.saveAll(missing).forEach(topic -> result.put(topic.getName(), topic));
        return result;
    }

    private Map<String, Company> loadCompanies(List<ProblemSeed> problems) {
        Set<String> names = problems.stream().flatMap(p -> p.companies().stream()).collect(Collectors.toSet());
        Map<String, Company> result = companyRepo.findAll().stream()
            .filter(company -> names.contains(company.getName()))
            .collect(Collectors.toMap(Company::getName, Function.identity()));
        List<Company> missing = names.stream().filter(name -> !result.containsKey(name))
            .map(name -> Company.builder().name(name).build()).toList();
        companyRepo.saveAll(missing).forEach(company -> result.put(company.getName(), company));
        return result;
    }

    private Question toQuestion(ProblemSeed problem, Map<String, Topic> topics, Map<String, Company> companies) {
        Question question = Question.builder()
            .title(problem.title())
            .description(problem.description())
            .type(QuestionType.CODING)
            .difficulty(problem.difficulty())
            .constraints(problem.constraints())
            .examples(problem.examples())
            .hints(problem.hints())
            .starterCode(JAVA_STARTER)
            .expectedComplexity(problem.expectedComplexity())
            .topics(problem.topics().stream().map(topics::get).collect(Collectors.toSet()))
            .companies(problem.companies().stream().map(companies::get).collect(Collectors.toSet()))
            .build();

        List<TestCase> cases = IntStream.range(0, CASE_COUNT)
            .mapToObj(index -> {
                CaseData data = problem.cases().apply(index);
                return TestCase.builder()
                    .question(question)
                    .input(data.input())
                    .expectedOutput(data.expectedOutput())
                    .hidden(index >= 3)
                    .build();
            })
            .toList();
        question.setTestCases(cases);
        return question;
    }

    private void validateSeedData(List<ProblemSeed> problems) {
        if (problems.size() != 30) throw new IllegalStateException("Seed must contain exactly 30 questions.");
        Map<Difficulty, Long> counts = problems.stream().collect(Collectors.groupingBy(ProblemSeed::difficulty, Collectors.counting()));
        if (counts.getOrDefault(Difficulty.EASY, 0L) != 10 || counts.getOrDefault(Difficulty.MEDIUM, 0L) != 10 || counts.getOrDefault(Difficulty.HARD, 0L) != 10) {
            throw new IllegalStateException("Seed must contain 10 questions at each difficulty.");
        }
        Set<String> titles = new HashSet<>();
        for (ProblemSeed problem : problems) {
            if (!titles.add(problem.title().toLowerCase(Locale.ROOT))) throw new IllegalStateException("Duplicate seed title: " + problem.title());
            if (blank(problem.title()) || blank(problem.description()) || blank(problem.constraints()) || blank(problem.examples()) || blank(problem.hints()) || blank(problem.expectedComplexity()) || problem.starterCode().contains("class Solution")) {
                throw new IllegalStateException("Missing or invalid fields for " + problem.title());
            }
            if (!problem.description().contains("\n") || !problem.examples().contains("\n") || !problem.starterCode().contains("\n")) {
                throw new IllegalStateException("Expected multiline content for " + problem.title());
            }
            if (problem.topics().isEmpty() || problem.companies().isEmpty()) throw new IllegalStateException("Missing relations for " + problem.title());
            for (int i = 0; i < CASE_COUNT; i++) {
                CaseData data = problem.cases().apply(i);
                if (blank(data.input()) || blank(data.expectedOutput())) throw new IllegalStateException("Missing test data for " + problem.title() + " case " + i);
            }
        }
    }

    private void verifyPersistedDataset() {
        List<Question> saved = questionRepo.findAll();
        if (saved.size() != 30) throw new IllegalStateException("Persisted question count is not 30.");
        Map<Difficulty, Long> counts = saved.stream().collect(Collectors.groupingBy(Question::getDifficulty, Collectors.counting()));
        if (counts.getOrDefault(Difficulty.EASY, 0L) != 10 || counts.getOrDefault(Difficulty.MEDIUM, 0L) != 10 || counts.getOrDefault(Difficulty.HARD, 0L) != 10) {
            throw new IllegalStateException("Persisted difficulty distribution is incorrect.");
        }
        Set<String> titles = new HashSet<>();
        for (Question question : saved) {
            if (question.getType() != QuestionType.CODING || blank(question.getTitle()) || blank(question.getDescription()) || blank(question.getConstraints()) || blank(question.getExamples()) || blank(question.getHints()) || blank(question.getStarterCode()) || blank(question.getExpectedComplexity())) {
                throw new IllegalStateException("Persisted question is missing a required field: " + question.getId());
            }
            if (!titles.add(question.getTitle().toLowerCase(Locale.ROOT))) throw new IllegalStateException("Duplicate persisted question title.");
            if (!question.getDescription().contains("\n") || !question.getExamples().contains("\n") || !question.getStarterCode().contains("\n")) {
                throw new IllegalStateException("Persisted multiline content was not preserved: " + question.getTitle());
            }
            if (question.getTopics().isEmpty() || question.getCompanies().isEmpty()) throw new IllegalStateException("Invalid relation on " + question.getTitle());
            List<TestCase> cases = testCaseRepo.findAllById(question.getTestCases().stream().map(TestCase::getId).toList());
            long visible = cases.stream().filter(testCase -> !testCase.isHidden()).count();
            long hidden = cases.stream().filter(TestCase::isHidden).count();
            if (cases.size() != CASE_COUNT || visible != 3 || hidden != 27 || cases.stream().anyMatch(testCase -> blank(testCase.getInput()) || blank(testCase.getExpectedOutput()) || testCase.getQuestion() == null || !testCase.getQuestion().getId().equals(question.getId()))) {
                throw new IllegalStateException("Invalid test-case data for " + question.getTitle());
            }
        }
    }

    private static boolean blank(String value) { return value == null || value.isBlank(); }

    private record CaseData(String input, String expectedOutput) { }

    private record ProblemSeed(String title, Difficulty difficulty, String description, String constraints,
                               String examples, String hints, String starterCode, String expectedComplexity,
                               Set<String> topics, Set<String> companies, IntFunction<CaseData> cases) { }

    private static ProblemSeed p(String title, Difficulty difficulty, String description, String constraints,
                                 String examples, String hints, String complexity, String topic, String company,
                                 IntFunction<CaseData> cases) {
        return new ProblemSeed(title, difficulty, description, constraints, examples, hints, JAVA_STARTER,
            complexity, Set.of(topic), Set.of(company), cases);
    }

    private static String arrayInput(int[] values) {
        return values.length + "\n" + Arrays.stream(values).mapToObj(String::valueOf).collect(Collectors.joining(" ")) + "\n";
    }

    private static int[] arrayValues(int caseIndex, int maxSize) {
        if (caseIndex == 0) return new int[]{0};
        if (caseIndex == 1) return new int[]{-5, 0, 5};
        if (caseIndex == 2) return new int[]{8, -3, 8, 2, 0};
        int n = 1 + (caseIndex * 17 % maxSize);
        return IntStream.range(0, n).map(i -> ((i * 31 + caseIndex * 13) % 101) - 50).toArray();
    }

    private static int[] readArray(Scanner scanner) {
        int n = scanner.nextInt();
        int[] values = new int[n];
        for (int i = 0; i < n; i++) values[i] = scanner.nextInt();
        return values;
    }

    private static List<ProblemSeed> problemSeeds() {
        List<ProblemSeed> seeds = new ArrayList<>();
        Difficulty easy = Difficulty.EASY, medium = Difficulty.MEDIUM, hard = Difficulty.HARD;

        seeds.add(p("Sum of an Array", easy, """
            Given an integer array, print the sum of all its values.
            The first input line is n, followed by n integers. Print one integer.
            """, "1 ≤ n ≤ 100000; -10^9 ≤ value ≤ 10^9", """
            Input:
            4
            3 -2 7 1
            Output:
            9
            """, "Use a 64-bit accumulator to avoid overflow.", "O(n) time, O(1) extra space", "Arrays", "Amazon", i -> {
                int[] a = arrayValues(i, 120);
                String input = arrayInput(a);
                long sum = Arrays.stream(a).asLongStream().sum();
                return new CaseData(input, Long.toString(sum));
            }));

        seeds.add(p("Maximum Element", easy, """
            Find the largest value in a non-empty integer array.
            The input contains n followed by n integers. Print the maximum.
            """, "1 ≤ n ≤ 100000; values fit in a signed 32-bit integer", """
            Input:
            5
            -7 4 4 0 -2
            Output:
            4
            """, "Initialize from the first element; values may all be negative.", "O(n) time, O(1) extra space", "Arrays", "Google", i -> {
                int[] a = arrayValues(i, 100);
                return new CaseData(arrayInput(a), Integer.toString(Arrays.stream(a).max().orElseThrow()));
            }));

        seeds.add(p("Reverse a Word", easy, """
            Reverse the characters in one whitespace-free word.
            Read one token and print its characters in reverse order.
            """, "1 ≤ word length ≤ 100000; the word contains visible ASCII characters", """
            Input:
            interview
            Output:
            weivretni
            """, "A two-pointer swap works in place on a character array.", "O(n) time, O(n) output space", "Strings", "Meta", i -> {
                String s = i == 0 ? "a" : i == 1 ? "level" : i == 2 ? "InitPrep" : "word" + "x".repeat(i * 3);
                return new CaseData(s + "\n", new StringBuilder(s).reverse().toString());
            }));

        seeds.add(p("Palindrome Number", easy, """
            Determine whether a signed 32-bit integer reads the same forwards and backwards in decimal.
            Print YES for a palindrome and NO otherwise. Negative values are not palindromes.
            """, "-2^31 ≤ x < 2^31", """
            Input:
            12321
            Output:
            YES
            """, "Avoid converting to a string if you want constant extra space.", "O(log |x|) time, O(1) extra space", "Math", "Microsoft", i -> {
                int x = i == 0 ? 0 : i == 1 ? 121 : i == 2 ? -121 : i % 3 == 0 ? i * 101 : i * 17;
                String s = Integer.toString(x);
                boolean yes = x >= 0 && s.contentEquals(new StringBuilder(s).reverse());
                return new CaseData(x + "\n", yes ? "YES" : "NO");
            }));

        seeds.add(p("Count Vowels", easy, """
            Count the English vowels (a, e, i, o, u) in a single word, ignoring case.
            Read one token and print the count.
            """, "1 ≤ word length ≤ 100000; input contains English letters only", """
            Input:
            Education
            Output:
            5
            """, "Convert each character to lowercase before checking membership.", "O(n) time, O(1) extra space", "Strings", "Adobe", i -> {
                String s = i == 0 ? "b" : i == 1 ? "AEIOU" : i == 2 ? "rhythm" : "Interview" + "aeiou".repeat(i);
                long count = s.toLowerCase(Locale.ROOT).chars().filter(c -> "aeiou".indexOf(c) >= 0).count();
                return new CaseData(s + "\n", Long.toString(count));
            }));

        seeds.add(p("First Non-Repeating Character", easy, """
            Print the first character in a lowercase word that occurs exactly once.
            Print NONE if every character repeats.
            """, "1 ≤ word length ≤ 100000; word contains lowercase English letters", """
            Input:
            swiss
            Output:
            w
            """, "Count frequencies first, then scan the word in its original order.", "O(n) time, O(1) space for the fixed alphabet", "Hash Tables", "Amazon", i -> {
                String s = i == 0 ? "a" : i == 1 ? "aabb" : i == 2 ? "swiss" : "abcabcx" + "q".repeat(i);
                int[] f = new int[26]; s.chars().forEach(c -> f[c - 'a']++);
                String answer = s.chars().filter(c -> f[c - 'a'] == 1).mapToObj(c -> Character.toString((char)c)).findFirst().orElse("NONE");
                return new CaseData(s + "\n", answer);
            }));

        seeds.add(p("Running Sum", easy, """
            For each array position, print the sum of all values up to and including that position.
            Input is n followed by n integers. Print the n prefix sums on one line.
            """, "1 ≤ n ≤ 100000; values fit in a signed 32-bit integer", """
            Input:
            4
            1 2 3 4
            Output:
            1 3 6 10
            """, "Update a running total as you scan from left to right.", "O(n) time, O(n) output space", "Arrays", "Meta", i -> {
                int[] a = arrayValues(i, 80); long sum = 0; List<String> out = new ArrayList<>();
                for (int value : a) { sum += value; out.add(Long.toString(sum)); }
                return new CaseData(arrayInput(a), String.join(" ", out));
            }));

        seeds.add(p("Move Zeroes to the End", easy, """
            Move every zero in an integer array to the end while preserving the relative order of non-zero values.
            Read n followed by n integers. Print the resulting array on one line.
            """, "1 ≤ n ≤ 100000; values fit in a signed 32-bit integer", """
            Input:
            6
            0 1 0 3 12 0
            Output:
            1 3 12 0 0 0
            """, "Compact non-zero values first, then fill the remaining slots with zero.", "O(n) time, O(1) extra space", "Arrays", "Facebook", i -> {
                int[] a = i == 0 ? new int[]{0} : i == 1 ? new int[]{0, 1, 0} : arrayValues(i, 90);
                int[] out = Arrays.stream(a).filter(v -> v != 0).toArray();
                String answer = IntStream.concat(Arrays.stream(out), IntStream.generate(() -> 0).limit(a.length - out.length)).mapToObj(String::valueOf).collect(Collectors.joining(" "));
                return new CaseData(arrayInput(a), answer);
            }));

        seeds.add(p("Valid Brackets", easy, """
            Check whether a string containing only (), [], and {} is correctly nested and matched.
            Print YES if valid and NO otherwise. The empty string is represented by the token EMPTY.
            """, "0 ≤ string length ≤ 100000", """
            Input:
            {[()]}
            Output:
            YES
            """, "Use a stack: each closing bracket must match the most recent opening bracket.", "O(n) time, O(n) space", "Stacks", "Amazon", i -> {
                String s = i == 0 ? "EMPTY" : i == 1 ? "()[]{}" : i == 2 ? "([)]" : i % 2 == 0 ? "{".repeat(i) + "}".repeat(i) : "(".repeat(i) + "]";
                Deque<Character> stack = new ArrayDeque<>(); boolean valid = true;
                if (!s.equals("EMPTY")) for (char c : s.toCharArray()) {
                    if (c == '(' || c == '[' || c == '{') stack.push(c);
                    else if (stack.isEmpty() || (c == ')' && stack.pop() != '(') || (c == ']' && stack.pop() != '[') || (c == '}' && stack.pop() != '{')) { valid = false; break; }
                }
                valid &= stack.isEmpty();
                return new CaseData(s + "\n", valid ? "YES" : "NO");
            }));

        seeds.add(p("Greatest Common Divisor", easy, """
            Print the non-negative greatest common divisor of two integers.
            """, "0 ≤ a, b ≤ 10^18", """
            Input:
            84 30
            Output:
            6
            """, "Repeatedly replace (a, b) with (b, a mod b).", "O(log min(a,b)) time, O(1) space", "Math", "Google", i -> {
                long a = i == 0 ? 0 : i == 1 ? 17 : i == 2 ? 84 : i * 101L;
                long b = i == 0 ? 0 : i == 1 ? 0 : i == 2 ? 30 : i * 37L;
                long x = a, y = b; while (y != 0) { long t = x % y; x = y; y = t; }
                return new CaseData(a + " " + b + "\n", Long.toString(x));
            }));

        seeds.add(p("Two Sum Indices", medium, """
            Find two distinct indices whose values add to the target. Print the lexicographically smallest matching pair (smaller index first).
            If no pair exists, print -1. Input is n, the n values, then target.
            """, "2 ≤ n ≤ 100000; exactly one pair exists or no pair exists", """
            Input:
            4
            2 7 11 15
            9
            Output:
            0 1
            """, "Store previously seen values in a hash map as you scan once.", "O(n) expected time, O(n) space", "Hash Tables", "Google", i -> {
                int[] a = i == 0 ? new int[]{1, 2} : i == 1 ? new int[]{3, 3, 8} : arrayValues(i, 45);
                if (a.length < 2) a = new int[]{0, 1};
                int target = i % 2 == 0 ? a[0] + a[a.length - 1] : 10000 + i;
                if (i % 2 == 0 && a.length == 2) target = a[0] + a[1];
                String input = a.length + "\n" + Arrays.stream(a).mapToObj(String::valueOf).collect(Collectors.joining(" ")) + "\n" + target + "\n";
                String out = "-1";
                outer: for (int x = 0; x < a.length; x++) for (int y = x + 1; y < a.length; y++) if (a[x] + a[y] == target) { out = x + " " + y; break outer; }
                return new CaseData(input, out);
            }));

        seeds.add(p("Product Except Self", medium, """
            For each position, print the product of every array value except the value at that position.
            Do not use division. Input is n followed by n integers.
            """, "2 ≤ n ≤ 100000; each result fits in a signed 64-bit integer", """
            Input:
            4
            1 2 3 4
            Output:
            24 12 8 6
            """, "Use prefix and suffix products so zero values are handled naturally.", "O(n) time, O(1) auxiliary space excluding output", "Arrays", "Microsoft", i -> {
                int[] a = i == 0 ? new int[]{0, 5} : i == 1 ? new int[]{1, 2, 3} : arrayValues(i, 30).length < 2 ? new int[]{i, i + 1} : arrayValues(i, 30);
                long[] out = new long[a.length]; Arrays.fill(out, 1); long prefix = 1;
                for (int j = 0; j < a.length; j++) { out[j] = prefix; prefix *= a[j]; }
                long suffix = 1; for (int j = a.length - 1; j >= 0; j--) { out[j] *= suffix; suffix *= a[j]; }
                return new CaseData(arrayInput(a), Arrays.stream(out).mapToObj(String::valueOf).collect(Collectors.joining(" ")));
            }));

        seeds.add(p("Merge Overlapping Intervals", medium, """
            Merge all overlapping closed intervals and print them in ascending start order.
            Input begins with n, followed by n start/end pairs. Print one merged interval per line.
            """, "1 ≤ n ≤ 100000; start ≤ end", """
            Input:
            4
            1 3
            2 6
            8 10
            15 18
            Output:
            1 6
            8 10
            15 18
            """, "Sort by start, then extend the last output interval when ranges overlap.", "O(n log n) time, O(n) space", "Sorting", "Google", i -> {
                int n = 1 + i % 12; int[][] a = new int[n][2];
                for (int j = 0; j < n; j++) { a[j][0] = j * 3 + i % 2; a[j][1] = a[j][0] + (j % 3 == 0 ? 4 : 1); }
                StringBuilder input = new StringBuilder(n + "\n"); for (int[] v : a) input.append(v[0]).append(' ').append(v[1]).append('\n');
                Arrays.sort(a, Comparator.comparingInt(v -> v[0])); List<int[]> merged = new ArrayList<>();
                for (int[] v : a) { if (merged.isEmpty() || merged.get(merged.size()-1)[1] < v[0]) merged.add(v.clone()); else merged.get(merged.size()-1)[1] = Math.max(merged.get(merged.size()-1)[1], v[1]); }
                return new CaseData(input.toString(), merged.stream().map(v -> v[0] + " " + v[1]).collect(Collectors.joining("\n")));
            }));

        seeds.add(p("Rotate a Matrix Clockwise", medium, """
            Rotate an n by n integer matrix 90 degrees clockwise.
            Input is n followed by n rows. Print the rotated matrix, one row per line.
            """, "1 ≤ n ≤ 300; matrix values fit in signed 32-bit integers", """
            Input:
            2
            1 2
            3 4
            Output:
            3 1
            4 2
            """, "A value at row r, column c moves to row c, column n-1-r.", "O(n^2) time, O(n^2) output space", "Matrices", "Amazon", i -> {
                int n = i == 0 ? 1 : i == 1 ? 2 : 1 + i % 12; int[][] a = new int[n][n];
                StringBuilder input = new StringBuilder(n + "\n");
                for (int r = 0; r < n; r++) { for (int c = 0; c < n; c++) { a[r][c] = r * n + c - i; if (c > 0) input.append(' '); input.append(a[r][c]); } input.append('\n'); }
                List<String> lines = new ArrayList<>(); for (int r = 0; r < n; r++) { List<String> row = new ArrayList<>(); for (int c = 0; c < n; c++) row.add(Integer.toString(a[n - 1 - c][r])); lines.add(String.join(" ", row)); }
                return new CaseData(input.toString(), String.join("\n", lines));
            }));

        seeds.add(p("Longest Unique Substring", medium, """
            Print the length of the longest substring containing no repeated characters.
            Input is one token made from ASCII letters and digits.
            """, "1 ≤ string length ≤ 200000", """
            Input:
            abcabcbb
            Output:
            3
            """, "Use a sliding window and remember the last position of each character.", "O(n) time, O(min(n, alphabet)) space", "Sliding Window", "Meta", i -> {
                String s = i == 0 ? "a" : i == 1 ? "aaaa" : i == 2 ? "abcabcbb" : ("abca" + "xyz".repeat(i));
                Map<Character,Integer> seen = new HashMap<>(); int start = 0, best = 0;
                for (int j = 0; j < s.length(); j++) { if (seen.containsKey(s.charAt(j))) start = Math.max(start, seen.get(s.charAt(j)) + 1); seen.put(s.charAt(j), j); best = Math.max(best, j - start + 1); }
                return new CaseData(s + "\n", Integer.toString(best));
            }));

        seeds.add(p("Kth Largest Value", medium, """
            Print the kth largest array value, counting duplicate values separately.
            Input is n, the n values, and k.
            """, "1 ≤ k ≤ n ≤ 100000", """
            Input:
            5
            3 2 1 5 6
            2
            Output:
            5
            """, "A min-heap of size k or selection gives an efficient solution.", "O(n log k) time, O(k) space with a heap", "Heaps", "Amazon", i -> {
                int[] a = arrayValues(i, 70); int k = 1 + i % a.length; String input = arrayInput(a) + k + "\n";
                int[] sorted = a.clone(); Arrays.sort(sorted); return new CaseData(input, Integer.toString(sorted[sorted.length - k]));
            }));

        seeds.add(p("Minimum Coin Change", medium, """
            Given coin denominations and an amount, print the minimum number of coins needed to make the amount.
            Coins may be reused. Print -1 if the amount cannot be formed. Input is amount, coin count, then denominations.
            """, "0 ≤ amount ≤ 100000; 1 ≤ coin count ≤ 100", """
            Input:
            11
            3
            1 5 7
            Output:
            3
            """, "Dynamic programming over amounts avoids assumptions about greedy choices.", "O(amount × coin count) time, O(amount) space", "Dynamic Programming", "Apple", i -> {
                int amount = i == 0 ? 0 : i == 1 ? 3 : i == 2 ? 11 : i * 19;
                int[] coins = i % 3 == 0 ? new int[]{2, 5, 9} : i % 3 == 1 ? new int[]{1, 3, 4} : new int[]{1, 5, 7};
                String input = amount + "\n" + coins.length + "\n" + Arrays.stream(coins).mapToObj(String::valueOf).collect(Collectors.joining(" ")) + "\n";
                int[] dp = new int[amount + 1]; Arrays.fill(dp, amount + 1); dp[0] = 0;
                for (int x = 1; x <= amount; x++) for (int coin : coins) if (coin <= x) dp[x] = Math.min(dp[x], dp[x - coin] + 1);
                return new CaseData(input, Integer.toString(dp[amount] > amount ? -1 : dp[amount]));
            }));

        seeds.add(p("Count Islands", medium, """
            Count groups of horizontally or vertically connected land cells in a binary grid.
            Input is rows, columns, then one string of 0/1 cells per row. Print the island count.
            """, "1 ≤ rows, columns ≤ 500", """
            Input:
            3 4
            1100
            0100
            0011
            Output:
            2
            """, "Flood-fill each unvisited land cell using four directions.", "O(rows × columns) time and space", "Graphs", "Amazon", i -> {
                int rows = i < 3 ? 3 : 2 + i % 9, cols = i < 3 ? 4 : 2 + (i * 3) % 9; char[][] grid = new char[rows][cols];
                StringBuilder input = new StringBuilder(rows + " " + cols + "\n");
                for (int r = 0; r < rows; r++) { for (int c = 0; c < cols; c++) grid[r][c] = (r + c + i) % 4 == 0 ? '1' : '0'; input.append(grid[r]).append('\n'); }
                boolean[][] seen = new boolean[rows][cols]; int count = 0; int[] dr = {-1,1,0,0}, dc = {0,0,-1,1};
                for (int r = 0; r < rows; r++) for (int c = 0; c < cols; c++) if (grid[r][c] == '1' && !seen[r][c]) {
                    count++; ArrayDeque<int[]> q = new ArrayDeque<>(); q.add(new int[]{r,c}); seen[r][c] = true;
                    while (!q.isEmpty()) { int[] v = q.remove(); for (int d=0; d<4; d++) { int nr=v[0]+dr[d], nc=v[1]+dc[d]; if(nr>=0&&nr<rows&&nc>=0&&nc<cols&&grid[nr][nc]=='1'&&!seen[nr][nc]) { seen[nr][nc]=true; q.add(new int[]{nr,nc}); } } }
                }
                return new CaseData(input.toString(), Integer.toString(count));
            }));

        seeds.add(p("Minimum in a Rotated Sorted Array", medium, """
            An ascending array of distinct integers was rotated an unknown number of places. Print its minimum.
            Input is n followed by the array values.
            """, "1 ≤ n ≤ 100000; values are distinct and the input is a rotation of an ascending array", """
            Input:
            5
            3 4 5 1 2
            Output:
            1
            """, "Binary search the part where the rotation boundary lies.", "O(log n) time, O(1) space", "Binary Search", "Microsoft", i -> {
                int n = i == 0 ? 1 : i == 1 ? 2 : 2 + i % 35, pivot = i % n; int[] a = new int[n];
                for (int j=0;j<n;j++) a[j] = (j + pivot) % n - 20;
                return new CaseData(arrayInput(a), Integer.toString(Arrays.stream(a).min().orElseThrow()));
            }));

        seeds.add(p("Top K Frequent Values", medium, """
            Print the k most frequent values in an integer array. Break frequency ties by smaller numeric value first.
            Input is n, the values, and k. Print the selected values on one line.
            """, "1 ≤ k ≤ number of distinct values ≤ n ≤ 100000", """
            Input:
            6
            1 1 1 2 2 3
            2
            Output:
            1 2
            """, "Count values, then order by descending frequency and ascending value.", "O(n log n) time, O(n) space", "Heaps", "Facebook", i -> {
                int n = 5 + i % 50; int[] a = new int[n]; for (int j=0;j<n;j++) a[j] = (j * 7 + i) % (3 + i % 9) - 5;
                Map<Integer,Long> freq = Arrays.stream(a).boxed().collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
                List<Integer> sorted = freq.keySet().stream().sorted(Comparator.<Integer>comparingLong(freq::get).reversed().thenComparingInt(v -> v)).toList();
                int k = 1 + i % sorted.size(); String in = arrayInput(a) + k + "\n";
                return new CaseData(in, sorted.subList(0,k).stream().map(String::valueOf).collect(Collectors.joining(" ")));
            }));

        seeds.add(p("Longest Increasing Subsequence", hard, """
            Print the length of the longest strictly increasing subsequence of an integer array.
            Input is n followed by n integers.
            """, "1 ≤ n ≤ 200000; values fit in signed 32-bit integers", """
            Input:
            8
            10 9 2 5 3 7 101 18
            Output:
            4
            """, "Maintain the smallest tail for each subsequence length using binary search.", "O(n log n) time, O(n) space", "Dynamic Programming", "Google", i -> {
                int[] a = arrayValues(i, 100); int[] tails = new int[a.length]; int len=0;
                for (int x:a) { int pos=Arrays.binarySearch(tails,0,len,x); if(pos<0) pos=-pos-1; tails[pos]=x; if(pos==len) len++; }
                return new CaseData(arrayInput(a), Integer.toString(len));
            }));

        seeds.add(p("Trapping Rain Water", hard, """
            Given non-negative bar heights of unit width, print how much rain water is trapped after raining.
            Input is n followed by n heights.
            """, "1 ≤ n ≤ 200000; 0 ≤ height ≤ 10^9", """
            Input:
            12
            0 1 0 2 1 0 1 3 2 1 2 1
            Output:
            6
            """, "Track the highest wall on each side with two pointers.", "O(n) time, O(1) extra space", "Two Pointers", "Amazon", i -> {
                int n=1+i%45; int[] h=new int[n]; for(int j=0;j<n;j++) h[j]=(j*11+i*3)%13;
                int left=0,right=n-1,lmax=0,rmax=0; long water=0;
                while(left<=right) if(h[left]<=h[right]) { lmax=Math.max(lmax,h[left]); water+=lmax-h[left++]; } else { rmax=Math.max(rmax,h[right]); water+=rmax-h[right--]; }
                return new CaseData(arrayInput(h),Long.toString(water));
            }));

        seeds.add(p("Edit Distance", hard, """
            Print the minimum number of insertions, deletions, and substitutions needed to transform one word into another.
            The input contains two lowercase words on separate lines.
            """, "0 ≤ each word length ≤ 2000", """
            Input:
            kitten
            sitting
            Output:
            3
            """, "Use dynamic programming over prefixes of both words.", "O(mn) time, O(min(m,n)) space is possible", "Dynamic Programming", "Microsoft", i -> {
                String a=i==0?"a":i==1?"horse":i==2?"kitten":"abc".repeat(i%8+1);
                String b=i==0?"":i==1?"ros":i==2?"sitting":"acb".repeat(i%7+1);
                int[][] dp=new int[a.length()+1][b.length()+1]; for(int x=0;x<=a.length();x++)dp[x][0]=x; for(int y=0;y<=b.length();y++)dp[0][y]=y;
                for(int x=1;x<=a.length();x++)for(int y=1;y<=b.length();y++)dp[x][y]=Math.min(Math.min(dp[x-1][y]+1,dp[x][y-1]+1),dp[x-1][y-1]+(a.charAt(x-1)==b.charAt(y-1)?0:1));
                return new CaseData(a+"\n"+b+"\n",Integer.toString(dp[a.length()][b.length()]));
            }));

        seeds.add(p("N-Queens Count", hard, """
            Count the distinct ways to place n queens on an n by n board so that no two queens attack one another.
            Print the count as a decimal integer.
            """, "1 ≤ n ≤ 14", """
            Input:
            4
            Output:
            2
            """, "Backtrack by row while tracking occupied columns and diagonals.", "O(n!) time in the worst case, O(n) auxiliary space", "Backtracking", "Meta", i -> {
                int n=1+i%14; long count=countQueens(n,0,0,0,0); return new CaseData(n+"\n",Long.toString(count));
            }));

        seeds.add(p("Median of Two Sorted Arrays", hard, """
            Given two non-empty sorted integer arrays, print the median of the combined values.
            Print an integer when the median is integral, otherwise print one decimal digit.
            Input contains n, m, the first array, and the second array.
            """, "1 ≤ n,m ≤ 100000; combined length ≤ 200000", """
            Input:
            2 2
            1 3
            2 4
            Output:
            2.5
            """, "A binary search partition can achieve logarithmic time in the shorter array.", "O(log min(n,m)) time, O(1) space", "Binary Search", "Google", i -> {
                int n=1+i%20,m=1+(i*3)%20; int[] a=IntStream.range(0,n).map(x->x*3-20).toArray(),b=IntStream.range(0,m).map(x->x*4-15).toArray();
                int[] all=IntStream.concat(Arrays.stream(a),Arrays.stream(b)).sorted().toArray(); double med=(all[(all.length-1)/2]+(long)all[all.length/2])/2.0;
                String out=med==(long)med?Long.toString((long)med):String.format(Locale.ROOT,"%.1f",med);
                return new CaseData(n+" "+m+"\n"+Arrays.stream(a).mapToObj(String::valueOf).collect(Collectors.joining(" "))+"\n"+Arrays.stream(b).mapToObj(String::valueOf).collect(Collectors.joining(" "))+"\n",out);
            }));

        seeds.add(p("Word Break", hard, """
            Decide whether a string can be split into one or more words from the supplied dictionary.
            Input is the string, the dictionary size, then one dictionary word per line. Print YES or NO.
            """, "1 ≤ string length ≤ 300; 1 ≤ dictionary size ≤ 1000", """
            Input:
            leetcode
            2
            leet
            code
            Output:
            YES
            """, "Let dp[i] indicate whether the prefix ending at i can be segmented.", "O(n^2) time, O(n) space (plus dictionary)", "Dynamic Programming", "Apple", i -> {
                String s=i%2==0?"leet".repeat(1+i/2)+"code":"catsanddog"+"x".repeat(1+i/2); List<String> words=i%2==0?List.of("leet","code"):List.of("cat","cats","and","sand","dog");
                boolean[] dp=new boolean[s.length()+1];dp[0]=true;for(int x=1;x<=s.length();x++)for(int y=0;y<x;y++)if(dp[y]&&words.contains(s.substring(y,x))){dp[x]=true;break;}
                String input=s+"\n"+words.size()+"\n"+String.join("\n",words)+"\n"; return new CaseData(input,dp[s.length()]?"YES":"NO");
            }));

        seeds.add(p("Shortest Path in a Binary Matrix", hard, """
            Find the shortest path from the top-left to bottom-right cell of a binary square grid.
            Movement is allowed in 8 directions through zero cells. The path length counts visited cells; print -1 if unreachable.
            Input is n followed by n strings of 0/1 cells.
            """, "1 ≤ n ≤ 1000", """
            Input:
            3
            000
            010
            000
            Output:
            3
            """, "Run breadth-first search from the top-left cell.", "O(n^2) time and space", "Graphs", "Amazon", i -> {
                int n=2+i%14; char[][] g=new char[n][n];StringBuilder in=new StringBuilder(n+"\n");
                for(int r=0;r<n;r++){for(int c=0;c<n;c++)g[r][c]=(r==c||r==0||c==n-1)?'0':((r*7+c*5+i)%5==0?'1':'0');in.append(g[r]).append('\n');}
                int answer=-1;if(g[0][0]=='0'&&g[n-1][n-1]=='0'){int[][] d=new int[n][n];ArrayDeque<int[]>q=new ArrayDeque<>();q.add(new int[]{0,0});d[0][0]=1;int[] dr={-1,-1,-1,0,0,1,1,1},dc={-1,0,1,-1,1,-1,0,1};while(!q.isEmpty()){int[]v=q.remove();for(int k=0;k<8;k++){int r=v[0]+dr[k],c=v[1]+dc[k];if(r>=0&&r<n&&c>=0&&c<n&&g[r][c]=='0'&&d[r][c]==0){d[r][c]=d[v[0]][v[1]]+1;q.add(new int[]{r,c});}}}if(d[n-1][n-1]>0)answer=d[n-1][n-1];}
                return new CaseData(in.toString(),Integer.toString(answer));
            }));

        seeds.add(p("Maximum Subarray Sum", hard, """
            Find the largest sum of a non-empty contiguous subarray.
            Input is n followed by n integers. Print the maximum sum.
            """, "1 ≤ n ≤ 200000; values fit in signed 32-bit integers", """
            Input:
            9
            -2 1 -3 4 -1 2 1 -5 4
            Output:
            6
            """, "Kadane's algorithm decides whether to extend or restart at each value.", "O(n) time, O(1) extra space", "Dynamic Programming", "LinkedIn", i -> {
                int[] a=arrayValues(i,120);long best=a[0],cur=a[0];for(int j=1;j<a.length;j++){cur=Math.max(a[j],cur+a[j]);best=Math.max(best,cur);}return new CaseData(arrayInput(a),Long.toString(best));
            }));

        seeds.add(p("Shortest Paths in a Directed Graph", hard, """
            Find shortest distances from a source vertex in a directed graph with non-negative edge weights.
            Input is vertex count, edge count, source, then edges (from, to, weight). Print distances in vertex order; use INF when unreachable.
            """, "1 ≤ vertices ≤ 10000; 0 ≤ edges ≤ 100000; weights ≤ 10^9", """
            Input:
            4 4 0
            0 1 4
            0 2 1
            2 1 2
            1 3 1
            Output:
            0 3 1 4
            """, "Use Dijkstra's algorithm with a min-priority queue.", "O((V+E) log V) time, O(V+E) space", "Graphs", "Uber", i -> {
                int n=2+i%14,source=i%n;List<int[]>edges=new ArrayList<>();for(int v=0;v<n-1;v++)edges.add(new int[]{v,v+1,1+(v+i)%9});if(n>2)for(int v=0;v<n-2;v++)edges.add(new int[]{v,v+2,3+(v+i)%7});
                StringBuilder in=new StringBuilder(n+" "+edges.size()+" "+source+"\n");for(int[]e:edges)in.append(e[0]).append(' ').append(e[1]).append(' ').append(e[2]).append('\n');
                long[]d=new long[n];Arrays.fill(d,Long.MAX_VALUE);d[source]=0;boolean[]done=new boolean[n];for(int k=0;k<n;k++){int u=-1;for(int v=0;v<n;v++)if(!done[v]&&(u<0||d[v]<d[u]))u=v;if(u<0||d[u]==Long.MAX_VALUE)break;done[u]=true;for(int[]e:edges)if(e[0]==u)d[e[1]]=Math.min(d[e[1]],d[u]+e[2]);}
                String out=Arrays.stream(d).mapToObj(x->x==Long.MAX_VALUE?"INF":Long.toString(x)).collect(Collectors.joining(" "));return new CaseData(in.toString(),out);
            }));

        seeds.add(p("Longest Palindromic Substring Length", hard, """
            Print the length of the longest contiguous substring that is a palindrome.
            Input is one lowercase token.
            """, "1 ≤ string length ≤ 5000", """
            Input:
            babad
            Output:
            3
            """, "Expand around every possible center, including gaps between characters.", "O(n^2) time, O(1) extra space", "Strings", "Bloomberg", i -> {
                String s=i==0?"a":i==1?"abba":i==2?"babad":"ab".repeat(1+i%60);int best=0;for(int c=0;c<s.length();c++){for(int d=0;d<2;d++){int l=c,r=c+d;while(l>=0&&r<s.length()&&s.charAt(l)==s.charAt(r)){best=Math.max(best,r-l+1);l--;r++;}}}return new CaseData(s+"\n",Integer.toString(best));
            }));

        return List.copyOf(seeds);
    }

    private static long countQueens(int n, int row, int cols, int diagA, int diagB) {
        if (row == n) return 1;
        long count=0; for(int col=0;col<n;col++){int bit=1<<col, a=1<<(row-col+n-1), b=1<<(row+col);if((cols&bit)==0&&(diagA&a)==0&&(diagB&b)==0)count+=countQueens(n,row+1,cols|bit,diagA|a,diagB|b);} return count;
    }

}
