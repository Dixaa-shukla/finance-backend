package com.finance_backend.profile.repository;

import com.finance_backend.profile.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    /**
     * Finds a user's profile using their authentication user ID.
     * Returns the profile if it exists.
     */
    Optional<UserProfile> findByUserId(Long userId);

    /**
     * Checks if a profile already exists for the given user ID.
     * Returns true if it exists, otherwise false.
     */
    boolean existsByUserId(Long userId);
}

