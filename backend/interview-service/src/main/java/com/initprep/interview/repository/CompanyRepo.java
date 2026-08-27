package com.initprep.interview.repository;

import com.initprep.interview.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CompanyRepo extends JpaRepository<Company, UUID> {
}
