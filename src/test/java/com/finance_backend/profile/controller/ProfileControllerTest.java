package com.finance_backend.profile.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance_backend.profile.dto.ProfileRequest;
import com.finance_backend.profile.dto.ProfileResponse;
import com.finance_backend.profile.exception.DuplicateProfileException;
import com.finance_backend.profile.exception.ProfileNotFoundException;
import com.finance_backend.profile.service.ProfileService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer tests for Module 2 (User Profile).
 *
 * @WebMvcTest loads only the MVC slice -- no database, no AI providers -- but
 * it DOES pick up GlobalExceptionHandler, so these tests also lock in the
 * HTTP status contract (404 for not-found, 409 for duplicate, 400 for
 * validation) that the frontend depends on.
 */
@WebMvcTest(ProfileController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("ProfileController")
class ProfileControllerTest {

    private static final String BASE_URL = "/api/v1/profiles";
    private static final Long PROFILE_ID = 1L;
    private static final Long USER_ID = 100L;

    @Autowired
    private MockMvc mockMvc;

    /** Used only to serialize request fixtures; MVC supplies response conversion. */
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @MockitoBean
    private ProfileService profileService;

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
                .build();
    }

    private ProfileResponse sampleResponse() {
        return ProfileResponse.builder()
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
    // POST /api/v1/profiles
    // =========================================================

    @Nested
    @DisplayName("POST /api/v1/profiles")
    class CreateProfile {

        @Test
        @DisplayName("returns 201 with the created profile")
        void createsProfile() throws Exception {
            when(profileService.createProfile(any(ProfileRequest.class))).thenReturn(sampleResponse());

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(PROFILE_ID))
                    .andExpect(jsonPath("$.userId").value(USER_ID))
                    .andExpect(jsonPath("$.fullName").value("Diksha Sharma"))
                    .andExpect(jsonPath("$.preferredCurrency").value("INR"))
                    .andExpect(jsonPath("$.monthlySalary").value(85000.00));
        }

        @Test
        @DisplayName("returns 400 with per-field errors and never reaches the service")
        void rejectsInvalidPayload() throws Exception {
            ProfileRequest invalid = ProfileRequest.builder()
                    .userId(null)              // @NotNull
                    .fullName("  ")            // @NotBlank
                    .preferredCurrency("INRX") // @Size(min=3, max=3)
                    .build();

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("Validation failed"))
                    .andExpect(jsonPath("$.path").value(BASE_URL))
                    .andExpect(jsonPath("$.fieldErrors.userId").exists())
                    .andExpect(jsonPath("$.fieldErrors.fullName").exists())
                    .andExpect(jsonPath("$.fieldErrors.preferredCurrency").exists());

            verifyNoInteractions(profileService);
        }

        @Test
        @DisplayName("returns 400 when the phone number does not match the expected pattern")
        void rejectsMalformedPhoneNumber() throws Exception {
            ProfileRequest invalid = validRequest();
            invalid.setPhoneNumber("not-a-phone");

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.phoneNumber").exists());

            verifyNoInteractions(profileService);
        }

        @Test
        @DisplayName("returns 400 when monthlySalary is negative")
        void rejectsNegativeSalary() throws Exception {
            ProfileRequest invalid = validRequest();
            invalid.setMonthlySalary(new BigDecimal("-1.00"));

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.monthlySalary").exists());
        }

        @Test
        @DisplayName("returns 409 when a profile already exists for that userId")
        void returnsConflictOnDuplicate() throws Exception {
            when(profileService.createProfile(any(ProfileRequest.class)))
                    .thenThrow(new DuplicateProfileException(USER_ID));

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.message").value("A profile already exists for userId: " + USER_ID));
        }
    }

    // =========================================================
    // GET
    // =========================================================

    @Nested
    @DisplayName("GET /api/v1/profiles/{id}")
    class GetProfileById {

        @Test
        @DisplayName("returns 200 with the profile")
        void returnsProfile() throws Exception {
            when(profileService.getProfileById(PROFILE_ID)).thenReturn(sampleResponse());

            mockMvc.perform(get(BASE_URL + "/{id}", PROFILE_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(PROFILE_ID))
                    .andExpect(jsonPath("$.fullName").value("Diksha Sharma"));
        }

        @Test
        @DisplayName("returns 404 when the profile does not exist")
        void returnsNotFound() throws Exception {
            when(profileService.getProfileById(999L)).thenThrow(ProfileNotFoundException.forId(999L));

            mockMvc.perform(get(BASE_URL + "/{id}", 999L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value("Profile not found with id: 999"));
        }

        @Test
        @DisplayName("returns 400 when the id is not a number")
        void returnsBadRequestOnNonNumericId() throws Exception {
            mockMvc.perform(get(BASE_URL + "/{id}", "abc"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(
                            org.hamcrest.Matchers.containsString("Invalid value 'abc'")));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/profiles/user/{userId}")
    class GetProfileByUserId {

        @Test
        @DisplayName("returns 200 with the profile for that user")
        void returnsProfile() throws Exception {
            when(profileService.getProfileByUserId(USER_ID)).thenReturn(sampleResponse());

            mockMvc.perform(get(BASE_URL + "/user/{userId}", USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value(USER_ID));
        }

        @Test
        @DisplayName("returns 404 when the user has no profile")
        void returnsNotFound() throws Exception {
            when(profileService.getProfileByUserId(404L))
                    .thenThrow(ProfileNotFoundException.forUserId(404L));

            mockMvc.perform(get(BASE_URL + "/user/{userId}", 404L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Profile not found for userId: 404"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/profiles")
    class GetAllProfiles {

        @Test
        @DisplayName("returns 200 with every profile")
        void returnsAll() throws Exception {
            ProfileResponse second = sampleResponse();
            second.setId(2L);
            second.setFullName("Rahul Verma");

            when(profileService.getAllProfiles()).thenReturn(List.of(sampleResponse(), second));

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[1].fullName").value("Rahul Verma"));
        }

        @Test
        @DisplayName("returns 200 with an empty array when there are no profiles")
        void returnsEmptyArray() throws Exception {
            when(profileService.getAllProfiles()).thenReturn(List.of());

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    // =========================================================
    // PUT
    // =========================================================

    @Nested
    @DisplayName("PUT /api/v1/profiles/{id}")
    class UpdateProfile {

        @Test
        @DisplayName("returns 200 with the updated profile")
        void updatesProfile() throws Exception {
            ProfileResponse updated = sampleResponse();
            updated.setFullName("Diksha S. Sharma");

            when(profileService.updateProfile(eq(PROFILE_ID), any(ProfileRequest.class))).thenReturn(updated);

            mockMvc.perform(put(BASE_URL + "/{id}", PROFILE_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.fullName").value("Diksha S. Sharma"));
        }

        @Test
        @DisplayName("returns 404 when updating a profile that does not exist")
        void returnsNotFound() throws Exception {
            when(profileService.updateProfile(eq(999L), any(ProfileRequest.class)))
                    .thenThrow(ProfileNotFoundException.forId(999L));

            mockMvc.perform(put(BASE_URL + "/{id}", 999L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("returns 409 when the new userId belongs to another profile")
        void returnsConflict() throws Exception {
            when(profileService.updateProfile(eq(PROFILE_ID), any(ProfileRequest.class)))
                    .thenThrow(new DuplicateProfileException(777L));

            mockMvc.perform(put(BASE_URL + "/{id}", PROFILE_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("returns 400 and never reaches the service when the payload is invalid")
        void rejectsInvalidPayload() throws Exception {
            ProfileRequest invalid = validRequest();
            invalid.setFullName(null);

            mockMvc.perform(put(BASE_URL + "/{id}", PROFILE_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.fullName").exists());

            verify(profileService, never()).updateProfile(any(), any());
        }
    }

    // =========================================================
    // DELETE
    // =========================================================

    @Nested
    @DisplayName("DELETE /api/v1/profiles/{id}")
    class DeleteProfile {

        @Test
        @DisplayName("returns 204 with no body")
        void deletesProfile() throws Exception {
            doNothing().when(profileService).deleteProfile(PROFILE_ID);

            mockMvc.perform(delete(BASE_URL + "/{id}", PROFILE_ID))
                    .andExpect(status().isNoContent());

            verify(profileService).deleteProfile(PROFILE_ID);
        }

        @Test
        @DisplayName("returns 404 when deleting a profile that does not exist")
        void returnsNotFound() throws Exception {
            doThrow(ProfileNotFoundException.forId(999L)).when(profileService).deleteProfile(999L);

            mockMvc.perform(delete(BASE_URL + "/{id}", 999L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Profile not found with id: 999"));
        }
    }
}
