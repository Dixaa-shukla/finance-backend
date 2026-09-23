package com.finance_backend.profile.service;

import com.finance_backend.profile.dto.ProfileRequest;
import com.finance_backend.profile.dto.ProfileResponse;
import com.finance_backend.profile.entity.UserProfile;
import com.finance_backend.profile.exception.DuplicateProfileException;
import com.finance_backend.profile.exception.ProfileNotFoundException;
import com.finance_backend.profile.repository.UserProfileRepository;
import com.finance_backend.auth.security.ResourceOwnershipGuard;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for Module 2 (User Profile) business logic.
 *
 * Pure Mockito -- no Spring context and no database, so these run in
 * milliseconds and assert the service's own rules (duplicate detection,
 * not-found signalling, field mapping) rather than framework behaviour.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProfileServiceImpl")
class ProfileServiceImplTest {

    private static final Long PROFILE_ID = 1L;
    private static final Long USER_ID = 100L;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private ResourceOwnershipGuard ownershipGuard;

    @InjectMocks
    private ProfileServiceImpl profileService;

    // =========================================================
    // Fixtures
    // =========================================================

    private ProfileRequest validRequest() {
        return ProfileRequest.builder()
                .userId(USER_ID)
                .fullName("Diksha Sharma")
                .phoneNumber("+919876543210")
                .dateOfBirth(LocalDate.of(1998, 5, 20))
                .gender("FEMALE")
                .monthlySalary(new BigDecimal("85000.00"))
                .preferredCurrency("INR")
                .primaryFinancialGoal("Buy a house")
                .profilePictureUrl("https://cdn.example.com/pic.png")
                .build();
    }

    private UserProfile existingProfile() {
        return UserProfile.builder()
                .id(PROFILE_ID)
                .userId(USER_ID)
                .fullName("Diksha Sharma")
                .phoneNumber("+919876543210")
                .dateOfBirth(LocalDate.of(1998, 5, 20))
                .gender("FEMALE")
                .monthlySalary(new BigDecimal("85000.00"))
                .preferredCurrency("INR")
                .primaryFinancialGoal("Buy a house")
                .profilePictureUrl("https://cdn.example.com/pic.png")
                .createdAt(LocalDateTime.of(2026, 1, 1, 10, 0))
                .updatedAt(LocalDateTime.of(2026, 1, 2, 10, 0))
                .build();
    }

    // =========================================================
    // CREATE
    // =========================================================

    @Nested
    @DisplayName("createProfile")
    class CreateProfile {

        @Test
        @DisplayName("persists the profile and maps every field onto the response")
        void createsProfile() {
            ProfileRequest request = validRequest();
            when(userProfileRepository.existsByUserId(USER_ID)).thenReturn(false);
            when(userProfileRepository.save(any(UserProfile.class)))
                    .thenAnswer(invocation -> {
                        UserProfile toSave = invocation.getArgument(0);
                        toSave.setId(PROFILE_ID);
                        return toSave;
                    });

            ProfileResponse response = profileService.createProfile(request);

            assertThat(response.getId()).isEqualTo(PROFILE_ID);
            assertThat(response.getUserId()).isEqualTo(USER_ID);
            assertThat(response.getFullName()).isEqualTo("Diksha Sharma");
            assertThat(response.getPhoneNumber()).isEqualTo("+919876543210");
            assertThat(response.getDateOfBirth()).isEqualTo(LocalDate.of(1998, 5, 20));
            assertThat(response.getGender()).isEqualTo("FEMALE");
            assertThat(response.getMonthlySalary()).isEqualByComparingTo("85000.00");
            assertThat(response.getPreferredCurrency()).isEqualTo("INR");
            assertThat(response.getPrimaryFinancialGoal()).isEqualTo("Buy a house");
            assertThat(response.getProfilePictureUrl()).isEqualTo("https://cdn.example.com/pic.png");
        }

        @Test
        @DisplayName("passes the request values through to the entity that gets saved")
        void mapsRequestOntoSavedEntity() {
            when(userProfileRepository.existsByUserId(USER_ID)).thenReturn(false);
            when(userProfileRepository.save(any(UserProfile.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            profileService.createProfile(validRequest());

            ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
            verify(userProfileRepository).save(captor.capture());

            UserProfile saved = captor.getValue();
            assertThat(saved.getUserId()).isEqualTo(USER_ID);
            assertThat(saved.getFullName()).isEqualTo("Diksha Sharma");
            assertThat(saved.getPreferredCurrency()).isEqualTo("INR");
            assertThat(saved.getMonthlySalary()).isEqualByComparingTo("85000.00");
        }

        @Test
        @DisplayName("rejects a second profile for the same userId (one profile per user)")
        void rejectsDuplicateUserId() {
            when(userProfileRepository.existsByUserId(USER_ID)).thenReturn(true);

            assertThatThrownBy(() -> profileService.createProfile(validRequest()))
                    .isInstanceOf(DuplicateProfileException.class)
                    .hasMessageContaining(String.valueOf(USER_ID));

            verify(userProfileRepository, never()).save(any());
        }
    }

    // =========================================================
    // READ
    // =========================================================

    @Nested
    @DisplayName("getProfileById")
    class GetProfileById {

        @Test
        @DisplayName("returns the profile when it exists")
        void returnsProfile() {
            when(userProfileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(existingProfile()));

            ProfileResponse response = profileService.getProfileById(PROFILE_ID);

            assertThat(response.getId()).isEqualTo(PROFILE_ID);
            assertThat(response.getFullName()).isEqualTo("Diksha Sharma");
            assertThat(response.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 1, 1, 10, 0));
        }

        @Test
        @DisplayName("throws ProfileNotFoundException naming the id when absent")
        void throwsWhenMissing() {
            when(userProfileRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> profileService.getProfileById(999L))
                    .isInstanceOf(ProfileNotFoundException.class)
                    .hasMessageContaining("id: 999");
        }
    }

    @Nested
    @DisplayName("getProfileByUserId")
    class GetProfileByUserId {

        @Test
        @DisplayName("returns the profile linked to the authenticated user")
        void returnsProfile() {
            when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existingProfile()));

            ProfileResponse response = profileService.getProfileByUserId(USER_ID);

            assertThat(response.getUserId()).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("throws ProfileNotFoundException naming the userId when absent")
        void throwsWhenMissing() {
            when(userProfileRepository.findByUserId(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> profileService.getProfileByUserId(404L))
                    .isInstanceOf(ProfileNotFoundException.class)
                    .hasMessageContaining("userId: 404");
        }
    }

    @Nested
    @DisplayName("getAllProfiles")
    class GetAllProfiles {

        @Test
        @DisplayName("maps every stored profile to a response")
        void returnsAll() {
            UserProfile second = existingProfile();
            second.setId(2L);
            second.setUserId(200L);
            second.setFullName("Rahul Verma");

            when(userProfileRepository.findAll()).thenReturn(List.of(existingProfile(), second));

            List<ProfileResponse> responses = profileService.getAllProfiles();

            assertThat(responses).hasSize(2)
                    .extracting(ProfileResponse::getFullName)
                    .containsExactly("Diksha Sharma", "Rahul Verma");
        }

        @Test
        @DisplayName("returns an empty list rather than null when there are no profiles")
        void returnsEmptyList() {
            when(userProfileRepository.findAll()).thenReturn(List.of());

            assertThat(profileService.getAllProfiles()).isEmpty();
        }
    }

    // =========================================================
    // UPDATE
    // =========================================================

    @Nested
    @DisplayName("updateProfile")
    class UpdateProfile {

        @Test
        @DisplayName("overwrites the existing record's fields")
        void updatesFields() {
            UserProfile existing = existingProfile();
            ProfileRequest request = validRequest();
            request.setFullName("Diksha S. Sharma");
            request.setMonthlySalary(new BigDecimal("95000.00"));
            request.setPrimaryFinancialGoal("Retire early");

            when(userProfileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(existing));
            when(userProfileRepository.save(any(UserProfile.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            ProfileResponse response = profileService.updateProfile(PROFILE_ID, request);

            assertThat(response.getFullName()).isEqualTo("Diksha S. Sharma");
            assertThat(response.getMonthlySalary()).isEqualByComparingTo("95000.00");
            assertThat(response.getPrimaryFinancialGoal()).isEqualTo("Retire early");
            // The same managed instance is updated in place, not replaced.
            assertThat(response.getId()).isEqualTo(PROFILE_ID);
        }

        @Test
        @DisplayName("skips the duplicate check when the userId is unchanged")
        void doesNotCheckDuplicateWhenUserIdUnchanged() {
            when(userProfileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(existingProfile()));
            when(userProfileRepository.save(any(UserProfile.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            profileService.updateProfile(PROFILE_ID, validRequest());

            verify(userProfileRepository, never()).existsByUserId(anyLong());
        }

        @Test
        @DisplayName("rejects reassigning the profile to a userId that is already taken")
        void rejectsReassignmentToTakenUserId() {
            ProfileRequest request = validRequest();
            request.setUserId(777L);

            when(userProfileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(existingProfile()));
            when(userProfileRepository.existsByUserId(777L)).thenReturn(true);

            assertThatThrownBy(() -> profileService.updateProfile(PROFILE_ID, request))
                    .isInstanceOf(DuplicateProfileException.class)
                    .hasMessageContaining("777");

            verify(userProfileRepository, never()).save(any());
        }

        @Test
        @DisplayName("allows moving the profile to a free userId")
        void allowsReassignmentToFreeUserId() {
            ProfileRequest request = validRequest();
            request.setUserId(777L);

            when(userProfileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(existingProfile()));
            when(userProfileRepository.existsByUserId(777L)).thenReturn(false);
            when(userProfileRepository.save(any(UserProfile.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            ProfileResponse response = profileService.updateProfile(PROFILE_ID, request);

            assertThat(response.getUserId()).isEqualTo(777L);
        }

        @Test
        @DisplayName("throws ProfileNotFoundException when the profile does not exist")
        void throwsWhenMissing() {
            when(userProfileRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> profileService.updateProfile(999L, validRequest()))
                    .isInstanceOf(ProfileNotFoundException.class)
                    .hasMessageContaining("id: 999");

            verify(userProfileRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateProfilePictureByUserId")
    class UpdateProfilePicture {

        @Test
        @DisplayName("replaces only the picture URL, leaving other fields untouched")
        void updatesOnlyPictureUrl() {
            UserProfile existing = existingProfile();
            when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existing));
            when(userProfileRepository.save(any(UserProfile.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            ProfileResponse response = profileService
                    .updateProfilePictureByUserId(USER_ID, "https://cdn.example.com/new.png");

            assertThat(response.getProfilePictureUrl()).isEqualTo("https://cdn.example.com/new.png");
            assertThat(response.getFullName()).isEqualTo("Diksha Sharma");
            assertThat(response.getMonthlySalary()).isEqualByComparingTo("85000.00");
        }

        @Test
        @DisplayName("throws ProfileNotFoundException when the user has no profile yet")
        void throwsWhenMissing() {
            when(userProfileRepository.findByUserId(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    profileService.updateProfilePictureByUserId(404L, "https://cdn.example.com/x.png"))
                    .isInstanceOf(ProfileNotFoundException.class)
                    .hasMessageContaining("userId: 404");

            verify(userProfileRepository, never()).save(any());
        }
    }

    // =========================================================
    // DELETE
    // =========================================================

    @Nested
    @DisplayName("deleteProfile")
    class DeleteProfile {

        @Test
        @DisplayName("deletes by id when the profile exists")
        void deletesProfile() {
            UserProfile profile = existingProfile();
            when(userProfileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(profile));

            profileService.deleteProfile(PROFILE_ID);

            verify(userProfileRepository).delete(profile);
        }

        @Test
        @DisplayName("throws ProfileNotFoundException and deletes nothing when absent")
        void throwsWhenMissing() {
            when(userProfileRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> profileService.deleteProfile(999L))
                    .isInstanceOf(ProfileNotFoundException.class)
                    .hasMessageContaining("id: 999");

            verify(userProfileRepository, never()).delete(any());
        }
    }
}
