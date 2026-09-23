package com.finance_backend.profile.service;

import com.finance_backend.profile.dto.ProfileRequest;
import com.finance_backend.profile.dto.ProfileResponse;
import com.finance_backend.profile.entity.UserProfile;
import com.finance_backend.profile.exception.DuplicateProfileException;
import com.finance_backend.profile.exception.ProfileNotFoundException;
import com.finance_backend.profile.repository.UserProfileRepository;
import com.finance_backend.auth.security.ResourceOwnershipGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProfileServiceImpl implements ProfileService {

    // Dependency

    private final UserProfileRepository userProfileRepository;
    private final ResourceOwnershipGuard ownershipGuard;

    public ProfileServiceImpl(UserProfileRepository userProfileRepository,
                              ResourceOwnershipGuard ownershipGuard) {
        this.userProfileRepository = userProfileRepository;
        this.ownershipGuard = ownershipGuard;
    }


    // CREATE

    @Override
    @Transactional
    public ProfileResponse createProfile(ProfileRequest request) {

        if (userProfileRepository.existsByUserId(request.getUserId())) {
            throw new DuplicateProfileException(request.getUserId());
        }

        UserProfile profile = toEntity(request, new UserProfile());

        UserProfile savedProfile = userProfileRepository.save(profile);

        return toResponse(savedProfile);
    }

    // READ

    @Override
    @Transactional(readOnly = true)
    public ProfileResponse getProfileById(Long id) {

        UserProfile profile = userProfileRepository.findById(id)
                .orElseThrow(() -> ProfileNotFoundException.forId(id));

        ownershipGuard.check(profile.getUserId(), "profile", id);

        return toResponse(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileResponse getProfileByUserId(Long userId) {

        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> ProfileNotFoundException.forUserId(userId));

        return toResponse(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProfileResponse> getAllProfiles() {

        return userProfileRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }


    // UPDATE

    @Override
    @Transactional
    public ProfileResponse updateProfile(
            Long id,
            ProfileRequest request
    ) {

        UserProfile existingProfile = userProfileRepository.findById(id)
                .orElseThrow(() -> ProfileNotFoundException.forId(id));

        /*
         * Check ownership before toEntity(), because toEntity() can overwrite userId.
         * Otherwise, someone could use another user's profile ID to change their details.
         */
        ownershipGuard.check(existingProfile.getUserId(), "profile", id);

        // Prevent assigning an already-used userId
        if (!existingProfile.getUserId().equals(request.getUserId())
                && userProfileRepository.existsByUserId(request.getUserId())) {

            throw new DuplicateProfileException(request.getUserId());
        }

        UserProfile updatedProfile = toEntity(
                request,
                existingProfile
        );

        UserProfile savedProfile =
                userProfileRepository.save(updatedProfile);

        return toResponse(savedProfile);
    }

    @Override
    @Transactional
    public ProfileResponse updateProfilePictureByUserId(
            Long userId,
            String pictureUrl
    ) {

        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> ProfileNotFoundException.forUserId(userId));

        profile.setProfilePictureUrl(pictureUrl);

        UserProfile savedProfile =
                userProfileRepository.save(profile);

        return toResponse(savedProfile);
    }


    // DELETE

    @Override
    @Transactional
    public void deleteProfile(Long id) {

        // findById, not existsById: the row has to be loaded to know who owns it.
        UserProfile profile = userProfileRepository.findById(id)
                .orElseThrow(() -> ProfileNotFoundException.forId(id));

        ownershipGuard.check(profile.getUserId(), "profile", id);

        userProfileRepository.delete(profile);
    }


    // MAPPING: Request -> Entity

    private UserProfile toEntity(
            ProfileRequest request,
            UserProfile target
    ) {

        target.setUserId(request.getUserId());
        target.setFullName(request.getFullName());
        target.setPhoneNumber(request.getPhoneNumber());
        target.setDateOfBirth(request.getDateOfBirth());
        target.setGender(request.getGender());
        target.setMonthlySalary(request.getMonthlySalary());
        target.setPreferredCurrency(request.getPreferredCurrency());
        target.setPrimaryFinancialGoal(
                request.getPrimaryFinancialGoal()
        );
        target.setProfilePictureUrl(
                request.getProfilePictureUrl()
        );

        return target;
    }


    // MAPPING: Entity -> Response

    private ProfileResponse toResponse(UserProfile profile) {

        return ProfileResponse.builder()
                .id(profile.getId())
                .userId(profile.getUserId())
                .fullName(profile.getFullName())
                .phoneNumber(profile.getPhoneNumber())
                .dateOfBirth(profile.getDateOfBirth())
                .gender(profile.getGender())
                .monthlySalary(profile.getMonthlySalary())
                .preferredCurrency(profile.getPreferredCurrency())
                .primaryFinancialGoal(profile.getPrimaryFinancialGoal())
                .profilePictureUrl(profile.getProfilePictureUrl())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }
}
