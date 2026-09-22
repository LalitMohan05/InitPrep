package com.initprep.attempt.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "judge_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JudgeResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id", nullable = false, unique = true)
    private Attempt attempt;

    @Column(nullable = false)
    private Integer passedTestCases;

    @Column(nullable = false)
    private Integer totalTestCases;

    private Long executionTime;

    private Long memoryUsed;

    @Column(columnDefinition = "TEXT")
    private String compilerOutput;

    @Column(columnDefinition = "TEXT")
    private String runtimeOutput;

    @Column(columnDefinition = "TEXT")
    private String failedInput;

    @Column(columnDefinition = "TEXT")
    private String expectedOutput;

    @Column(columnDefinition = "TEXT")
    private String actualOutput;
}
