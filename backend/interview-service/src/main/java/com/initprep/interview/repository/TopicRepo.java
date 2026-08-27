package com.initprep.interview.repository;

import com.initprep.interview.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TopicRepo extends JpaRepository<Topic, UUID> {
}
