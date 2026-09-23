package com.finance_backend.profile.service;

import com.finance_backend.profile.dto.ProfileRequest;
import com.finance_backend.profile.dto.ProfileResponse;

import java.util.List;

public interface ProfileService {

    /**
     * Create a new user profile.
     */
    ProfileResponse createProfile(ProfileRequest request);

    /**
     * Fetch a profile by its own profile ID.
     */
    ProfileResponse getProfileById(Long id);

    /**
     * Fetch a profile by the linked authentication user ID.
     */
    ProfileResponse getProfileByUserId(Long userId);

    /**
     * Fetch all profiles.
     */
    List<ProfileResponse> getAllProfiles();

    /**
     * Update an existing profile.
     */
    ProfileResponse updateProfile(
            Long id,
            ProfileRequest request
    );

    /**
     * Updates only the profile picture URL after a successful upload.
     * This is separate from updateProfile() because changing the picture
     * does not require all profile details.
     */
    ProfileResponse updateProfilePictureByUserId(
            Long userId,
            String pictureUrl
    );

    /**
     * Delete a profile.
     */
    void deleteProfile(Long id);
}