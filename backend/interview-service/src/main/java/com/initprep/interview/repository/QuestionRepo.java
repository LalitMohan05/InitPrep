package com.initprep.interview.repository;

import com.initprep.interview.entity.Question;
import com.initprep.interview.enums.Difficulty;
import com.initprep.interview.enums.QuestionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface QuestionRepo extends JpaRepository<Question, UUID>, JpaSpecificationExecutor<Question> {
    Page<Question> findByDifficulty(Difficulty difficulty, Pageable pageable);

    Page<Question> findByType(QuestionType type,Pageable pageable);

    Page<Question> findByDifficultyAndType(Difficulty difficulty, QuestionType type,Pageable pageable);
}
