package com.finance_backend.admin.controller;

import com.finance_backend.admin.dto.PlatformAnalyticsResponse;
import com.finance_backend.admin.dto.PlatformReportResponse;
import com.finance_backend.admin.dto.UserDetailResponse;
import com.finance_backend.admin.dto.UserSummaryResponse;
import com.finance_backend.admin.service.AdminService;
import com.finance_backend.common.dtos.ApiResponse;
import com.finance_backend.common.dtos.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    /** Guard-rail so a caller can't request an unbounded page of users. */
    private static final int MAX_PAGE_SIZE = 100;

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    // ==================== USER MANAGEMENT ====================

    /**
     * Paginated list of all users on the platform, newest first.
     * GET /api/v1/admin/users?page=0&size=20
     *
     * SECURED (Module 1): covered by the class-level @PreAuthorize("hasRole('ADMIN')").
     */
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<PageResponse<UserSummaryResponse>>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<UserSummaryResponse> users = adminService.getUsers(pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(users)));
    }

    /**
     * One user's full footprint: profile, money totals, AI usage, health score.
     * GET /api/v1/admin/users/{userId}
     *
     * Returns 404 if no profile exists for that userId.
     *
     * SECURED (Module 1): covered by the class-level @PreAuthorize("hasRole('ADMIN')").
     *
     * NOTE: this is the one admin route carrying a {userId} path variable, so
     * Module 1's UserOwnershipInterceptor also sees it -- reading someone else's
     * userId is the entire point of the endpoint, and the interceptor lets it
     * through because ROLE_ADMIN bypasses the ownership check.
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<UserDetailResponse>> getUserDetail(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getUserDetail(userId)));
    }

    // ==================== ANALYTICS ====================

    /**
     * Platform-wide counts and money totals across every user.
     * GET /api/v1/admin/analytics
     *
     * SECURED (Module 1): covered by the class-level @PreAuthorize("hasRole('ADMIN')").
     */
    @GetMapping("/analytics")
    public ResponseEntity<ApiResponse<PlatformAnalyticsResponse>> getPlatformAnalytics() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getPlatformAnalytics()));
    }

    // ==================== REPORTS ====================

    /**
     * Point-in-time snapshot bundling platform analytics + AI usage.
     * GET /api/v1/admin/reports
     *
     * SECURED (Module 1): covered by the class-level @PreAuthorize("hasRole('ADMIN')").
     */
    @GetMapping("/reports")
    public ResponseEntity<ApiResponse<PlatformReportResponse>> getPlatformReport() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getPlatformReport()));
    }
}
