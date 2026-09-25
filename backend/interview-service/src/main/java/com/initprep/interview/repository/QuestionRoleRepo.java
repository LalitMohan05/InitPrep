package com.initprep.interview.repository;

import com.initprep.interview.entity.QuestionRole;
import com.initprep.interview.enums.TargetRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface QuestionRoleRepo extends JpaRepository<QuestionRole, UUID> {
    List<QuestionRole> findByCodeIn(Collection<TargetRole> codes);
}
