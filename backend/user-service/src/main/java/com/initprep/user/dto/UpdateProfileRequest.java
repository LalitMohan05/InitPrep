package com.initprep.user.dto;

import com.initprep.user.enums.PreferredLanguage;
import com.initprep.user.enums.TargetRole;
import com.initprep.user.validation.annotation.ProfileUrl;
import com.initprep.user.validation.enums.Platform;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.validator.constraints.URL;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateProfileRequest {

    @Size(min = 3, max = 100)
    private String fullName;

    @Size(max = 500)
    private String bio;

    @Size(max = 100)
    private String college;

    @Size(max = 100)
    private String branch;

    @Min(2000)
    @Max(2100)
    private Integer graduationYear;

    @ProfileUrl(platform = Platform.GITHUB)
    private String githubUrl;

    @ProfileUrl(platform = Platform.LINKEDIN)
    private String linkedinUrl;

    @ProfileUrl(platform = Platform.LEETCODE)
    private String leetcodeUrl;

    @ProfileUrl(platform = Platform.CODEFORCES)
    private String codeforcesUrl;

    @ProfileUrl(platform = Platform.CODECHEF)
    private String codechefUrl;

    @URL(message = "Invalid avatar URL")
    private String avatarUrl;

    private PreferredLanguage preferredLanguage;

    private TargetRole targetRole;

    @Size(max = 100)
    private String targetCompany;

}
