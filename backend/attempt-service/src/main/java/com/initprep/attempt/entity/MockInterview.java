package com.initprep.attempt.entity;

import com.initprep.attempt.enums.MockInterviewStatus;
import com.initprep.attempt.enums.MockInterviewTargetRole;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "mock_interview_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MockInterview {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private MockInterviewTargetRole targetRole;

    @Column(nullable = false, length = 20)
    private String difficulty;

    @Column(name = "number_of_questions", nullable = false)
    private Integer totalQuestions;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MockInterviewStatus status;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    @Column(name = "score")
    private Double overallScore;

    @Builder.Default
    @OneToMany(mappedBy = "mockInterview", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequenceNumber ASC")
    private List<MockInterviewQuestion> questions = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (startedAt == null) startedAt = LocalDateTime.now();
    }
}
