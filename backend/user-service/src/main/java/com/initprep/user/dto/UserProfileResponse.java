package com.initprep.user.dto;

import com.initprep.user.enums.PreferredLanguage;
import com.initprep.user.enums.TargetRole;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponse {

    private UUID id;

    private UUID userId;

    private String fullName;

    private String bio;

    private String college;

    private String branch;

    private Integer graduationYear;

    private String githubUrl;

    private String linkedinUrl;

    private String leetcodeUrl;

    private String codeforcesUrl;

    private String codechefUrl;

    private PreferredLanguage preferredLanguage;

    private TargetRole targetRole;

    private String targetCompany;

    private String avatarUrl;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
