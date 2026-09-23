package com.finance_backend.profile.controller;

import com.finance_backend.profile.dto.ProfileRequest;
import com.finance_backend.profile.dto.ProfileResponse;
import com.finance_backend.profile.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/profiles")
public class ProfileController {

    private final ProfileService profileService;

    // Constructor Injection
    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    /**
     * Create a new user profile.
     *
     * POST /api/v1/profiles
     */
    @PostMapping
    public ResponseEntity<ProfileResponse> createProfile(
            @Valid @RequestBody ProfileRequest request) {

        ProfileResponse response = profileService.createProfile(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * Fetch a profile by its own record ID.
     *
     * GET /api/v1/profiles/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProfileResponse> getProfileById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                profileService.getProfileById(id)
        );
    }

    /**
     * Fetch a profile by the linked authentication user ID.
     *
     * GET /api/v1/profiles/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<ProfileResponse> getProfileByUserId(
            @PathVariable Long userId) {

        return ResponseEntity.ok(
                profileService.getProfileByUserId(userId)
        );
    }

    /**
     * Admin-facing: Fetch all profiles.
     *
     * GET /api/v1/profiles
     *
     * ⚠️ @PreAuthorize IS LOAD-BEARING, NOT DECORATIVE. The javadoc always said
     * "admin-facing", but nothing enforced it: every signed-in user could read
     * every full name, phone number, city and monthly salary on the platform in
     * one request. Neither ownership guard can help here -- there is no {userId}
     * in the path and no request body -- so the role check is the only defence.
     *
     * Safe to add: no frontend screen calls this route. The UI reads a single
     * profile through GET /api/v1/profiles/user/{userId}, and the admin user list
     * comes from /api/v1/admin/users.
     *
     * NOTE:
     * Currently unpaginated.
     * Later, this can be changed to Pageable<ProfileResponse>
     * when the number of users grows.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ProfileResponse>> getAllProfiles() {

        return ResponseEntity.ok(
                profileService.getAllProfiles()
        );
    }

    /**
     * Update an existing profile.
     *
     * PUT /api/v1/profiles/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProfileResponse> updateProfile(
            @PathVariable Long id,
            @Valid @RequestBody ProfileRequest request) {

        return ResponseEntity.ok(
                profileService.updateProfile(id, request)
        );
    }

    /**
     * Delete a profile.
     *
     * DELETE /api/v1/profiles/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProfile(
            @PathVariable Long id) {

        profileService.deleteProfile(id);

        return ResponseEntity.noContent().build();
    }
}
