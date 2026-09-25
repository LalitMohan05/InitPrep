package com.initprep.attempt.entity;

import com.initprep.attempt.enums.AttemptType;
import com.initprep.attempt.enums.MockInterviewDifficulty;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "mock_interview_session_questions", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"session_id", "position"}),
    @UniqueConstraint(columnNames = {"session_id", "question_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MockInterviewQuestion {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private MockInterview mockInterview;

    @Column(nullable = false)
    private UUID questionId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AttemptType questionType;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private MockInterviewDifficulty difficulty;

    @Column(name = "position", nullable = false)
    private Integer sequenceNumber;

    @Column(length = 200)
    private String questionTitleSnapshot;

    @Column(columnDefinition = "TEXT")
    private String topicSnapshot;

    @Column(length = 80)
    private String roleSnapshot;

    private UUID attemptId;
}
