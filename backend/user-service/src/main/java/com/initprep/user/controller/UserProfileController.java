package com.initprep.user.controller;

import com.initprep.user.dto.CreateProfileRequest;
import com.initprep.user.dto.UpdateProfileRequest;
import com.initprep.user.dto.UserProfileResponse;
import com.initprep.user.security.UserPrincipal;
import com.initprep.user.service.interfaces.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Profile", description = "User profile management APIs")
public class UserProfileController {

    private final UserProfileService userProfileService;

    @PostMapping("/profile")
    @Operation(summary = "Create user profile")
    public ResponseEntity<UserProfileResponse> createProfile(
        @AuthenticationPrincipal UserPrincipal principal,
        @Valid @RequestBody CreateProfileRequest request) {

        UserProfileResponse response =
            userProfileService.createProfile(
                principal.getUserId(),
                request
            );

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(response);
    }

    @GetMapping("/profile")
    @Operation(summary = "Get current user's profile")
    public ResponseEntity<UserProfileResponse> getProfile(
        @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity.ok(
            userProfileService.getProfile(
                principal.getUserId()
            )
        );
    }

    @PatchMapping("/profile")
    @Operation(summary = "Update current user's profile")
    public ResponseEntity<UserProfileResponse> updateProfile(
        @AuthenticationPrincipal UserPrincipal principal,
        @Valid @RequestBody UpdateProfileRequest request) {

        return ResponseEntity.ok(
            userProfileService.updateProfile(
                principal.getUserId(),
                request
            )
        );
    }
}
