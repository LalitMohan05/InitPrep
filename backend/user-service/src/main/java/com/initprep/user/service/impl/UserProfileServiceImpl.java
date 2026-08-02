package com.initprep.user.service.impl;

import com.initprep.user.dto.CreateProfileRequest;
import com.initprep.user.dto.UpdateProfileRequest;
import com.initprep.user.dto.UserProfileResponse;
import com.initprep.user.entity.UserProfile;
import com.initprep.user.exception.ProfileAlreadyExistsException;
import com.initprep.user.exception.ProfileNotFoundException;
import com.initprep.user.mapper.UserProfileMapper;
import com.initprep.user.repository.UserProfileRepo;
import com.initprep.user.service.interfaces.UserProfileService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class UserProfileServiceImpl implements UserProfileService {

    private final UserProfileRepo userProfileRepo;
    private final UserProfileMapper userProfileMapper;

    @Override
    public UserProfileResponse createProfile(
        UUID userId,
        CreateProfileRequest createProfileRequest
    ){
        if(userProfileRepo.existsByUserId(userId)){
            throw new ProfileAlreadyExistsException(userId);
        }

        UserProfile profile = userProfileMapper.toEntity(createProfileRequest);

        profile.setUserId(userId);
        UserProfile saved = userProfileRepo.saveAndFlush(profile);
        return userProfileMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(UUID userId) {
        UserProfile profile = userProfileRepo.findByUserId(userId)
            .orElseThrow(()->
                new ProfileNotFoundException(userId));
        return userProfileMapper.toResponse(profile);
    }

    @Override
    public UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        UserProfile profile = userProfileRepo.findByUserId(userId)
            .orElseThrow(()->
                new ProfileNotFoundException(userId));

        userProfileMapper.updateEntity(profile,request);

        UserProfile updated = userProfileRepo.save(profile);
        return userProfileMapper.toResponse(updated);
    }


}
