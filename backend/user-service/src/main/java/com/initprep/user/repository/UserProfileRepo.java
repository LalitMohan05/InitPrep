package com.initprep.user.repository;

import com.initprep.user.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserProfileRepo
    extends JpaRepository<UserProfile, UUID> {

    Optional<UserProfile> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);
}
