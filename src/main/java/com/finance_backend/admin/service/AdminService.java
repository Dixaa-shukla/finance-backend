package com.finance_backend.admin.service;

import com.finance_backend.admin.dto.PlatformAnalyticsResponse;
import com.finance_backend.admin.dto.PlatformReportResponse;
import com.finance_backend.admin.dto.UserDetailResponse;
import com.finance_backend.admin.dto.UserSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminService {

    /**
     * Paginated list of users for the admin user table.
     */
    Page<UserSummaryResponse> getUsers(Pageable pageable);

    /**
     * One user's complete footprint: profile, money totals, AI usage, and
     * current health score.
     *
     * @throws com.finance_backend.profile.exception.ProfileNotFoundException
     *         if no profile exists for that userId
     */
    UserDetailResponse getUserDetail(Long userId);

    /** Platform-wide counts and money totals across every user. */
    PlatformAnalyticsResponse getPlatformAnalytics();

    PlatformReportResponse getPlatformReport();
}
