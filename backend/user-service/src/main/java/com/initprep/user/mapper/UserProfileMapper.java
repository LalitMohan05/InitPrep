package com.initprep.user.mapper;

import com.initprep.user.dto.CreateProfileRequest;
import com.initprep.user.dto.UpdateProfileRequest;
import com.initprep.user.dto.UserProfileResponse;
import com.initprep.user.entity.UserProfile;
import org.springframework.stereotype.Component;

@Component
public class UserProfileMapper {

    public UserProfile toEntity(CreateProfileRequest request) {

        return UserProfile.builder()
            .fullName(request.getFullName())
            .bio(request.getBio())
            .college(request.getCollege())
            .branch(request.getBranch())
            .graduationYear(request.getGraduationYear())
            .githubUrl(request.getGithubUrl())
            .linkedinUrl(request.getLinkedinUrl())
            .leetcodeUrl(request.getLeetcodeUrl())
            .codeforcesUrl(request.getCodeforcesUrl())
            .codechefUrl(request.getCodechefUrl())
            .preferredLanguage(request.getPreferredLanguage())
            .targetRole(request.getTargetRole())
            .targetCompany(request.getTargetCompany())
            .avatarUrl(request.getAvatarUrl())
            .build();
    }

    public UserProfileResponse toResponse(UserProfile profile) {

        return UserProfileResponse.builder()
            .id(profile.getId())
            .userId(profile.getUserId())
            .fullName(profile.getFullName())
            .bio(profile.getBio())
            .college(profile.getCollege())
            .branch(profile.getBranch())
            .graduationYear(profile.getGraduationYear())
            .githubUrl(profile.getGithubUrl())
            .linkedinUrl(profile.getLinkedinUrl())
            .leetcodeUrl(profile.getLeetcodeUrl())
            .codeforcesUrl(profile.getCodeforcesUrl())
            .codechefUrl(profile.getCodechefUrl())
            .preferredLanguage(profile.getPreferredLanguage())
            .targetRole(profile.getTargetRole())
            .targetCompany(profile.getTargetCompany())
            .avatarUrl(profile.getAvatarUrl())
            .createdAt(profile.getCreatedAt())
            .updatedAt(profile.getUpdatedAt())
            .build();
    }

    public void updateEntity(UserProfile profile,
                             UpdateProfileRequest request) {

        if (request.getFullName() != null)
            profile.setFullName(request.getFullName());

        if (request.getBio() != null)
            profile.setBio(request.getBio());

        if (request.getCollege() != null)
            profile.setCollege(request.getCollege());

        if (request.getBranch() != null)
            profile.setBranch(request.getBranch());

        if (request.getGraduationYear() != null)
            profile.setGraduationYear(request.getGraduationYear());

        if (request.getGithubUrl() != null)
            profile.setGithubUrl(request.getGithubUrl());

        if (request.getLinkedinUrl() != null)
            profile.setLinkedinUrl(request.getLinkedinUrl());

        if (request.getLeetcodeUrl() != null)
            profile.setLeetcodeUrl(request.getLeetcodeUrl());

        if (request.getCodeforcesUrl() != null)
            profile.setCodeforcesUrl(request.getCodeforcesUrl());

        if (request.getCodechefUrl() != null)
            profile.setCodechefUrl(request.getCodechefUrl());

        if (request.getPreferredLanguage() != null)
            profile.setPreferredLanguage(request.getPreferredLanguage());

        if (request.getTargetRole() != null)
            profile.setTargetRole(request.getTargetRole());

        if (request.getTargetCompany() != null)
            profile.setTargetCompany(request.getTargetCompany());

        if (request.getAvatarUrl() != null)
            profile.setAvatarUrl(request.getAvatarUrl());
    }
}
