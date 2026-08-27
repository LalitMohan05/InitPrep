package com.initprep.interview.mapper;

import com.initprep.interview.dto.*;
import com.initprep.interview.entity.Company;
import com.initprep.interview.entity.Question;
import com.initprep.interview.entity.Topic;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;


@Mapper(componentModel = "spring",
nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface QuestionMapper {

    Question toEntity(CreateQuestionRequest request);

    @Mapping(target = "companies", source = "companies")
    @Mapping(target = "topics", source = "topics")
    QuestionResponse toResponse(Question question);


    CompanySummaryResponse toCompanySummary(Company company);

    TopicSummaryResponse toTopicSummary(Topic topic);

    void updateEntity(
        UpdateQuestionRequest request,
        @MappingTarget Question question
    );

    @Mapping(target = "companies", source = "companies")
    @Mapping(target = "topics", source = "topics")
    QuestionSummaryResponse toSummaryResponse(Question question);
}
