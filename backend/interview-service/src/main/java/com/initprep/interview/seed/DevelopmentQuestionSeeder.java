package com.initprep.interview.seed;

import com.initprep.interview.entity.Company;
import com.initprep.interview.entity.Question;
import com.initprep.interview.entity.QuestionRole;
import com.initprep.interview.entity.TestCase;
import com.initprep.interview.entity.Topic;
import com.initprep.interview.enums.Difficulty;
import com.initprep.interview.enums.QuestionType;
import com.initprep.interview.enums.TargetRole;
import com.initprep.interview.repository.CompanyRepo;
import com.initprep.interview.repository.QuestionRepo;
import com.initprep.interview.repository.QuestionRoleRepo;
import com.initprep.interview.repository.TopicRepo;
import com.initprep.interview.repository.specification.QuestionSpecification;
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
    private final TopicRepo topicRepo;
    private final CompanyRepo companyRepo;
    private final QuestionRoleRepo questionRoleRepo;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<ProblemSeed> problems = problemSeeds();
        validateSeedData(problems);
        List<QuestionBankSeedData.BankQuestion> bank = new ArrayList<>();
        bank.addAll(QuestionBankSeedData.mcq());
        bank.addAll(QuestionBankSeedData.theory());
        validateBankSeedData(bank);

        List<Question> questions = questionRepo.findAll();
        List<Question> additions = new ArrayList<>();
        Map<TargetRole, QuestionRole> roles = loadRoles();
        Map<String, Topic> topics = loadTopics(problems, bank);
        Map<String, Company> companies = loadCompanies(problems, bank);

        ensureCodingQuestions(problems, questions, additions, topics, companies, roles);
        questions.addAll(additions);
        synchronizeBankQuestions(QuestionType.MCQ, QuestionBankSeedData.mcq(), questions, additions, topics, companies, roles);
        synchronizeBankQuestions(QuestionType.THEORY, QuestionBankSeedData.theory(), questions, additions, topics, companies, roles);

        assignRolesAndTopics(questions, bank, roles, topics);
        rebalanceDifficulty(questions.stream().filter(question -> question.getType() == QuestionType.MCQ).toList());
        rebalanceDifficulty(questions.stream().filter(question -> question.getType() == QuestionType.THEORY).toList());

        questionRepo.saveAll(questions);
        questionRepo.flush();
        verifyPersistedDataset();
        log.info("Development seed verified {} questions (30 coding, 100 MCQ, 100 theory).", questions.size());
    }

    private Map<TargetRole, QuestionRole> loadRoles() {
        Map<TargetRole, QuestionRole> result = questionRoleRepo.findAll().stream()
            .collect(Collectors.toMap(QuestionRole::getCode, Function.identity()));
        List<QuestionRole> missing = Arrays.stream(TargetRole.values()).filter(code -> !result.containsKey(code))
            .map(code -> QuestionRole.builder().code(code).build()).toList();
        questionRoleRepo.saveAll(missing).forEach(role -> result.put(role.getCode(), role));
        return result;
    }

    private Map<String, Topic> loadTopics(List<ProblemSeed> problems, List<QuestionBankSeedData.BankQuestion> bank) {
        Set<String> names = new HashSet<>(problems.stream().flatMap(p -> p.topics().stream()).toList());
        bank.stream().flatMap(question -> question.topics().stream()).forEach(names::add);
        Map<String, Topic> result = topicRepo.findAll().stream()
            .filter(topic -> names.contains(topic.getName()))
            .collect(Collectors.toMap(Topic::getName, Function.identity()));
        List<Topic> missing = names.stream().filter(name -> !result.containsKey(name))
            .map(name -> Topic.builder().name(name).build()).toList();
        topicRepo.saveAll(missing).forEach(topic -> result.put(topic.getName(), topic));
        return result;
    }

    private Map<String, Company> loadCompanies(List<ProblemSeed> problems, List<QuestionBankSeedData.BankQuestion> bank) {
        Set<String> names = new HashSet<>(problems.stream().flatMap(p -> p.companies().stream()).toList());
        bank.stream().flatMap(question -> question.companies().stream()).forEach(names::add);
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

    private void ensureCodingQuestions(List<ProblemSeed> seeds, List<Question> existing, List<Question> additions,
                                       Map<String, Topic> topics, Map<String, Company> companies,
                                       Map<TargetRole, QuestionRole> roles) {
        List<Question> coding = existing.stream().filter(q -> q.getType() == QuestionType.CODING).toList();
        if (coding.size() > 30) throw new IllegalStateException("Expected the existing 30 coding questions; found " + coding.size());
        Set<String> titles = existing.stream().map(Question::getTitle).map(DevelopmentQuestionSeeder::titleKey).collect(Collectors.toSet());
        for (ProblemSeed seed : seeds) {
            if (coding.size() + additions.size() >= 30) break;
            if (titles.add(titleKey(seed.title()))) {
                Question question = toQuestion(seed, topics, companies);
                question.setRoles(EnumSet.allOf(TargetRole.class).stream().map(roles::get).collect(Collectors.toSet()));
                additions.add(question);
            }
        }
        if (coding.size() + additions.size() != 30) throw new IllegalStateException("Could not complete the 30-question coding bank without replacing existing questions");
    }

    private void synchronizeBankQuestions(QuestionType type, List<QuestionBankSeedData.BankQuestion> bank,
                                         List<Question> questions, List<Question> additions,
                                         Map<String, Topic> topics, Map<String, Company> companies,
                                         Map<TargetRole, QuestionRole> roles) {
        Set<String> seedTitles = bank.stream().map(seed -> titleKey(seed.title())).collect(Collectors.toSet());
        List<Question> typed = questions.stream().filter(question -> question.getType() == type).toList();
        if (typed.size() > 100) throw new IllegalStateException("Question bank already has more than 100 " + type + " questions");
        List<Question> unmanaged = typed.stream().filter(question -> !seedTitles.contains(titleKey(question.getTitle()))).toList();
        if (unmanaged.size() > 100) throw new IllegalStateException("More than 100 unseeded " + type + " questions are present");
        rebalanceDifficulty(unmanaged);

        Set<String> titles = questions.stream().filter(question -> question.getType() != type)
            .map(Question::getTitle).map(DevelopmentQuestionSeeder::titleKey).collect(Collectors.toSet());
        unmanaged.stream().map(Question::getTitle).map(DevelopmentQuestionSeeder::titleKey).forEach(titles::add);
        List<QuestionBankSeedData.BankQuestion> desiredSeeds = new ArrayList<>();
        for (Difficulty target : List.of(Difficulty.EASY, Difficulty.MEDIUM, Difficulty.HARD)) {
            long existingCount = unmanaged.stream().filter(question -> question.getDifficulty() == target).count();
            int missing = difficultyTarget(target) - Math.toIntExact(existingCount);
            if (missing <= 0) continue;
            Map<TargetRole, Integer> roleCounts = new EnumMap<>(TargetRole.class);
            for (TargetRole role : TargetRole.values()) {
                roleCounts.put(role, Math.toIntExact(unmanaged.stream().filter(question -> question.getDifficulty() == target &&
                    question.getRoles().stream().anyMatch(existingRole -> existingRole.getCode() == role)).count()));
            }
            List<QuestionBankSeedData.BankQuestion> available = bank.stream()
                .filter(seed -> seed.type() == type && seed.difficulty() == target && !titles.contains(titleKey(seed.title())))
                .collect(Collectors.toCollection(ArrayList::new));
            if (available.size() < missing) {
                bank.stream().filter(seed -> seed.type() == type && seed.difficulty() != target && !titles.contains(titleKey(seed.title())))
                    .forEach(available::add);
            }
            for (int index = 0; index < missing; index++) {
                if (available.isEmpty()) throw new IllegalStateException("Not enough unique seed questions for " + type + " " + target);
                QuestionBankSeedData.BankQuestion selected = selectForRoleCoverage(available, roleCounts, type);
                desiredSeeds.add(withDifficulty(selected, target));
                titles.add(titleKey(selected.title()));
                selected.roles().forEach(role -> roleCounts.merge(role, 1, Integer::sum));
                available.remove(selected);
            }
        }

        List<Question> managed = typed.stream().filter(question -> seedTitles.contains(titleKey(question.getTitle())))
            .collect(Collectors.toCollection(ArrayList::new));
        Map<String, Question> existingByTitle = managed.stream()
            .collect(Collectors.toMap(question -> titleKey(question.getTitle()), Function.identity()));
        List<Question> reusable = managed.stream().filter(question -> desiredSeeds.stream()
            .noneMatch(seed -> titleKey(seed.title()).equals(titleKey(question.getTitle())))).collect(Collectors.toCollection(ArrayList::new));

        for (QuestionBankSeedData.BankQuestion seed : desiredSeeds) {
            Question question = existingByTitle.get(titleKey(seed.title()));
            if (question == null && !reusable.isEmpty()) question = reusable.remove(0);
            if (question == null) {
                question = new Question();
                questions.add(question);
                additions.add(question);
            }
            applyBankSeed(question, seed, topics, companies, roles);
        }
        if (!reusable.isEmpty()) throw new IllegalStateException("Seed reconciliation would require deleting " + type + " questions");
    }

    private static QuestionBankSeedData.BankQuestion selectForRoleCoverage(
        List<QuestionBankSeedData.BankQuestion> candidates, Map<TargetRole, Integer> roleCounts, QuestionType type) {
        int minimum = type == QuestionType.MCQ ? 4 : 5;
        return candidates.stream().max(Comparator
            .comparingInt((QuestionBankSeedData.BankQuestion seed) -> (int) seed.roles().stream()
                .filter(role -> roleCounts.getOrDefault(role, 0) < minimum).count())
            .thenComparingDouble(seed -> seed.roles().stream()
                .mapToDouble(role -> 1.0 / (1 + roleCounts.getOrDefault(role, 0))).sum()))
            .orElseThrow();
    }

    private static QuestionBankSeedData.BankQuestion withDifficulty(QuestionBankSeedData.BankQuestion seed, Difficulty difficulty) {
        return new QuestionBankSeedData.BankQuestion(seed.title(), seed.type(), difficulty, seed.description(), seed.hints(),
            seed.options(), seed.correctAnswer(), seed.topics(), seed.companies(), seed.roles());
    }

    private static void applyBankSeed(Question question, QuestionBankSeedData.BankQuestion seed,
                                      Map<String, Topic> topics, Map<String, Company> companies,
                                      Map<TargetRole, QuestionRole> roles) {
        question.setTitle(seed.title());
        question.setDescription(seed.description());
        question.setType(seed.type());
        question.setDifficulty(seed.difficulty());
        question.setHints(seed.hints());
        question.setOptions(seed.options());
        question.setCorrectAnswer(seed.correctAnswer());
        question.setTopics(seed.topics().stream().map(topics::get).collect(Collectors.toSet()));
        question.setCompanies(seed.companies().stream().map(companies::get).collect(Collectors.toSet()));
        question.setRoles(seed.roles().stream().map(roles::get).collect(Collectors.toSet()));
    }

    private void rebalanceDifficulty(List<Question> questions) {
        for (Difficulty source : List.of(Difficulty.EASY, Difficulty.MEDIUM, Difficulty.HARD)) {
            long excess = questions.stream().filter(question -> question.getDifficulty() == source).count() - difficultyTarget(source);
            if (excess <= 0) continue;
            List<Question> movable = questions.stream().filter(question -> question.getDifficulty() == source)
                .sorted(Comparator.comparing(Question::getTitle)).toList();
            for (Question question : movable) {
                if (excess == 0) break;
                for (Difficulty target : List.of(Difficulty.EASY, Difficulty.MEDIUM, Difficulty.HARD)) {
                    long count = questions.stream().filter(item -> item.getDifficulty() == target).count();
                    if (count < difficultyTarget(target)) {
                        question.setDifficulty(target);
                        excess--;
                        break;
                    }
                }
            }
        }
    }

    private static int difficultyTarget(Difficulty difficulty) {
        return switch (difficulty) {
            case EASY -> 30;
            case MEDIUM -> 50;
            case HARD -> 20;
        };
    }

    private void assignRolesAndTopics(List<Question> questions, List<QuestionBankSeedData.BankQuestion> bank,
                                      Map<TargetRole, QuestionRole> roles, Map<String, Topic> topics) {
        Map<String, QuestionBankSeedData.BankQuestion> byTitle = bank.stream()
            .collect(Collectors.toMap(seed -> titleKey(seed.title()), Function.identity(), (left, right) -> left));
        for (Question question : questions) {
            QuestionBankSeedData.BankQuestion seed = byTitle.get(titleKey(question.getTitle()));
            if (seed != null) {
                question.setRoles(seed.roles().stream().map(roles::get).collect(Collectors.toSet()));
            } else if (question.getRoles().isEmpty()) {
                question.setRoles(inferRoles(question, roles));
            }
            if (question.getRoles().isEmpty()) throw new IllegalStateException("Question has no target role: " + question.getTitle());
            if (question.getTopics().isEmpty()) {
                String topicName = inferTopic(question);
                Topic topic = topics.computeIfAbsent(topicName, name -> topicRepo.findAll().stream()
                    .filter(existing -> existing.getName().equalsIgnoreCase(name)).findFirst()
                    .orElseGet(() -> topicRepo.save(Topic.builder().name(name).build())));
                question.getTopics().add(topic);
            }
        }
    }

    private static Set<QuestionRole> inferRoles(Question question, Map<TargetRole, QuestionRole> roles) {
        if (question.getType() == QuestionType.CODING) {
            return Arrays.stream(TargetRole.values()).map(roles::get).collect(Collectors.toSet());
        }
        String text = (question.getTitle() + " " + question.getDescription() + " " +
            question.getTopics().stream().map(Topic::getName).collect(Collectors.joining(" "))).toLowerCase(Locale.ROOT);
        EnumSet<TargetRole> matches = EnumSet.noneOf(TargetRole.class);
        if (containsAny(text, "python", "numpy", "pandas", "machine learning", "model", "regression", "classification", "statistics", "probability", "feature")) matches.add(TargetRole.MACHINE_LEARNING_ENGINEER);
        if (containsAny(text, "react", "javascript", "typescript", "html", "css", "dom", "browser", "accessibility", "frontend")) {
            matches.add(TargetRole.FRONTEND_DEVELOPER);
            matches.add(TargetRole.FULL_STACK_DEVELOPER);
        }
        if (containsAny(text, "docker", "kubernetes", "linux", "network", "ci/cd", "deployment", "monitoring", "cloud", "container", "devops", "git")) matches.add(TargetRole.DEVOPS_ENGINEER);
        if (containsAny(text, "java", "spring", "sql", "database", "dbms", "jpa", "hibernate", "rest", "api", "backend", "transaction", "microservice", "cache", "security", "concurrency")) {
            matches.add(TargetRole.BACKEND_DEVELOPER);
            matches.add(TargetRole.FULL_STACK_DEVELOPER);
        }
        if (matches.isEmpty()) {
            matches.add(TargetRole.BACKEND_DEVELOPER);
            matches.add(TargetRole.FULL_STACK_DEVELOPER);
        }
        return matches.stream().map(roles::get).collect(Collectors.toSet());
    }

    private static boolean containsAny(String text, String... values) {
        return Arrays.stream(values).anyMatch(text::contains);
    }

    private static String inferTopic(Question question) {
        String text = (question.getTitle() + " " + question.getDescription()).toLowerCase(Locale.ROOT);
        if (containsAny(text, "python", "numpy", "pandas", "machine learning", "model", "regression", "classification", "statistics", "probability")) return "Machine Learning";
        if (containsAny(text, "react", "javascript", "typescript", "html", "css", "browser", "frontend")) return "Frontend Engineering";
        if (containsAny(text, "docker", "kubernetes", "linux", "ci/cd", "deployment", "monitoring", "cloud", "devops")) return "DevOps";
        if (containsAny(text, "sql", "database", "dbms", "index", "transaction")) return "Databases";
        return "Software Engineering";
    }

    private void validateBankSeedData(List<QuestionBankSeedData.BankQuestion> bank) {
        Set<String> titles = new HashSet<>();
        for (QuestionBankSeedData.BankQuestion seed : bank) {
            if (!titles.add(titleKey(seed.title()))) throw new IllegalStateException("Duplicate question bank title: " + seed.title());
            if (seed.roles().isEmpty() || seed.topics().isEmpty() || blank(seed.description()) || !seed.description().contains("\n")) {
                throw new IllegalStateException("Incomplete role-aware question seed: " + seed.title());
            }
            if (seed.type() == QuestionType.MCQ && (!hasFourOptions(seed.options()) || !validMcqAnswer(seed.options(), seed.correctAnswer()))) {
                throw new IllegalStateException("MCQ must contain four choices and a matching correct answer: " + seed.title());
            }
            if (seed.type() == QuestionType.THEORY && (blank(seed.hints()) || !seed.hints().contains("\n"))) {
                throw new IllegalStateException("Theory seed must include a multiline expected-answer outline: " + seed.title());
            }
        }
        for (QuestionType type : List.of(QuestionType.MCQ, QuestionType.THEORY)) {
            List<QuestionBankSeedData.BankQuestion> typed = bank.stream().filter(seed -> seed.type() == type).toList();
            if (typed.stream().filter(seed -> seed.difficulty() == Difficulty.EASY).count() < 30 ||
                typed.stream().filter(seed -> seed.difficulty() == Difficulty.MEDIUM).count() < 50 ||
                typed.stream().filter(seed -> seed.difficulty() == Difficulty.HARD).count() < 20) {
                throw new IllegalStateException("Insufficient seed questions for required difficulty distribution: " + type);
            }
        }
    }

    private void validateRolePools() {
        for (TargetRole role : TargetRole.values()) {
            List<Question> roleQuestions = questionRepo.findAll(QuestionSpecification.hasRole(role));
            if (roleQuestions.isEmpty() || roleQuestions.stream().anyMatch(question ->
                question.getRoles().stream().noneMatch(questionRole -> questionRole.getCode() == role))) {
                throw new IllegalStateException("Role filtering returned no matching questions for " + role);
            }
            for (Difficulty difficulty : Difficulty.values()) {
                long coding = roleQuestions.stream().filter(question -> question.getType() == QuestionType.CODING && question.getDifficulty() == difficulty).count();
                long mcq = roleQuestions.stream().filter(question -> question.getType() == QuestionType.MCQ && question.getDifficulty() == difficulty).count();
                long theory = roleQuestions.stream().filter(question -> question.getType() == QuestionType.THEORY && question.getDifficulty() == difficulty).count();
                // The minimum pool supports the fixed 40/30/30 mix for a ten-question interview.
                if (coding < 4 || mcq < 3 || theory < 3) {
                    throw new IllegalStateException("Role/difficulty pool is too small for Mock Interview selection: " + role + " " + difficulty + " (" + coding + "/" + theory + "/" + mcq + ")");
                }
            }
        }
    }

    private static boolean hasFourOptions(String options) {
        return options != null && options.split("\\R").length == 4;
    }

    private static boolean validMcqAnswer(String options, String answer) {
        if (answer == null || options == null) return false;
        return Arrays.stream(options.split("\\R")).anyMatch(option -> {
            String normalized = option.trim();
            return normalized.equalsIgnoreCase(answer.trim()) || normalized.matches("(?i)^" + java.util.regex.Pattern.quote(answer.trim()) + "[.)].*");
        });
    }

    private static String titleKey(String title) { return title.trim().toLowerCase(Locale.ROOT); }

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
        validateRolePools();
        if (saved.size() != 230) throw new IllegalStateException("Persisted question count must be 230, found " + saved.size());
        Map<QuestionType, Long> types = saved.stream().collect(Collectors.groupingBy(Question::getType, Collectors.counting()));
        if (types.getOrDefault(QuestionType.CODING, 0L) != 30 || types.getOrDefault(QuestionType.MCQ, 0L) != 100 || types.getOrDefault(QuestionType.THEORY, 0L) != 100) {
            throw new IllegalStateException("Persisted question type distribution must be 30 CODING, 100 MCQ, 100 THEORY");
        }
        Set<String> titles = new HashSet<>();
        for (Question question : saved) {
            if (question.getType() == null || question.getDifficulty() == null || blank(question.getTitle()) || blank(question.getDescription())) {
                throw new IllegalStateException("Persisted question is missing a required field: " + question.getId());
            }
            if (!titles.add(titleKey(question.getTitle()))) throw new IllegalStateException("Duplicate persisted question title: " + question.getTitle());
            if (question.getRoles().isEmpty()) throw new IllegalStateException("Question has no target role: " + question.getTitle());
            if (question.getType() != QuestionType.CODING && question.getTopics().isEmpty()) throw new IllegalStateException("Non-coding question has no topic: " + question.getTitle());
            if (question.getType() != QuestionType.CODING && (!question.getDescription().contains("\n") || question.getDescription().contains("\\n"))) {
                throw new IllegalStateException("Persisted multiline content was not preserved: " + question.getTitle());
            }
            if (question.getType() == QuestionType.MCQ && (!hasFourOptions(question.getOptions()) || !validMcqAnswer(question.getOptions(), question.getCorrectAnswer()))) {
                throw new IllegalStateException("Invalid MCQ options or correct answer: " + question.getTitle());
            }
            if (question.getType() == QuestionType.THEORY && (blank(question.getHints()) || !question.getHints().contains("\n"))) {
                throw new IllegalStateException("Theory question is missing expected-answer content: " + question.getTitle());
            }
            if (question.getType() == QuestionType.CODING) {
                List<TestCase> cases = question.getTestCases();
                long visible = cases.stream().filter(testCase -> !testCase.isHidden()).count();
                long hidden = cases.stream().filter(TestCase::isHidden).count();
                if (cases.size() != CASE_COUNT || visible != 3 || hidden != 27 || cases.stream().anyMatch(testCase -> blank(testCase.getInput()) || blank(testCase.getExpectedOutput()))) {
                    throw new IllegalStateException("Existing coding test cases changed or are invalid: " + question.getTitle());
                }
            }
        }
        for (QuestionType type : List.of(QuestionType.MCQ, QuestionType.THEORY)) {
            Map<Difficulty, Long> counts = saved.stream().filter(question -> question.getType() == type)
                .collect(Collectors.groupingBy(Question::getDifficulty, Collectors.counting()));
            if (counts.getOrDefault(Difficulty.EASY, 0L) != 30 || counts.getOrDefault(Difficulty.MEDIUM, 0L) != 50 || counts.getOrDefault(Difficulty.HARD, 0L) != 20) {
                throw new IllegalStateException("Incorrect difficulty distribution for " + type);
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
