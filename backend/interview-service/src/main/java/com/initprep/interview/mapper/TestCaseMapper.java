package com.initprep.interview.mapper;

import com.initprep.interview.dto.TestCaseRequest;
import com.initprep.interview.dto.TestCaseResponse;
import com.initprep.interview.entity.TestCase;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface TestCaseMapper {

    TestCaseResponse toResponse(TestCase testCase);

    TestCase toEntity(TestCaseRequest request);

    void updateEntity(
        TestCaseRequest request,
        @MappingTarget TestCase testCase
    );
}
