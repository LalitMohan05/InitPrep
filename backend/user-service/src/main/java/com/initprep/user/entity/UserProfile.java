package com.initprep.user.entity;

import com.initprep.user.enums.PreferredLanguage;
import com.initprep.user.enums.TargetRole;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "user_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID userId;

    @NotBlank(message = "Full name is required")
    @Size(min = 3, max = 100)
    private String fullName;

    @Size(max = 500)
    private String bio;

    @Size(max = 100)
    private String college;

    @Size(max = 100)
    private String branch;

    @Min(value = 2000)
    @Max(value = 2100)
    private Integer graduationYear;

    private String githubUrl;
    private String linkedinUrl;
    private String leetcodeUrl;
    private String codeforcesUrl;
    private String codechefUrl;

    @Enumerated(EnumType.STRING)
    private PreferredLanguage preferredLanguage;

    @Enumerated(EnumType.STRING)
    private TargetRole targetRole;

    @Size(max = 100)
    private String targetCompany;
    private String avatarUrl;
}
