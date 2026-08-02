package com.initprep.user.service.interfaces;

import com.initprep.user.dto.CreateProfileRequest;
import com.initprep.user.dto.UpdateProfileRequest;
import com.initprep.user.dto.UserProfileResponse;

import java.util.UUID;

public interface UserProfileService {

    UserProfileResponse createProfile(UUID userId , CreateProfileRequest request);
    UserProfileResponse getProfile(UUID userId);
    UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request);
}
