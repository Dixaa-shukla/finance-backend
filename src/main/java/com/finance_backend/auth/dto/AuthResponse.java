package com.finance_backend.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    /** Short-lived signed JWT. Send as: Authorization: Bearer &lt;accessToken&gt; */
    private String accessToken;

    /**
     * Long-lived opaque UUID. Store it, send it to /refresh when the access
     * token expires. Rotated on every refresh -- the old value stops working.
     */
    private String refreshToken;

    /** Always "Bearer". Present so the client never has to hardcode the scheme. */
    @Builder.Default
    private String tokenType = "Bearer";

    private Long expiresIn;

    private Long userId;

    private String email;

    /** e.g. ["USER"] or ["USER","ADMIN"] -- without the ROLE_ prefix. */
    private Set<String> roles;
}
