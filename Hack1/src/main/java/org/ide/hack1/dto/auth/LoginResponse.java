package org.ide.hack1.dto.auth;

import lombok.*;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {
    private String token;
    private long expiresIn;
    private String role;
    private String branch;
}
