package com.initprep.interview.entity;

import com.initprep.interview.enums.Difficulty;
import com.initprep.interview.enums.QuestionType;
import jakarta.persistence.*;
import lombok.*;

import java.util.*;

@Entity
@Table(name = "questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuestionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Difficulty difficulty;

    @Column(columnDefinition = "TEXT")
    private String constraints;

    @Column(columnDefinition = "TEXT")
    private String examples;

    @Column(columnDefinition = "TEXT")
    private String hints;

    @Column(columnDefinition = "TEXT")
    private String starterCode;

    @Column(length = 100)
    private String expectedComplexity;

    @Column(columnDefinition = "TEXT")
    private String options;

    @Column(length = 500)
    private String correctAnswer;

    @Builder.Default
    @ManyToMany
    @JoinTable(
        name = "question_companies",
        joinColumns = @JoinColumn(name = "question_id"),
        inverseJoinColumns = @JoinColumn(name = "company_id")
    )
    Set<Company> companies= new HashSet<>();
    @Builder.Default
    @ManyToMany
    @JoinTable(
        name = "question_topics",
        joinColumns = @JoinColumn(name = "question_id"),
        inverseJoinColumns = @JoinColumn(name = "topic_id")
    )
    Set<Topic> topics = new HashSet<>();

    @OneToMany(
        mappedBy = "question",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    @Builder.Default
    private List<TestCase> testCases = new ArrayList<>();
}
