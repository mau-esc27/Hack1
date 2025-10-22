package org.ide.hack1.dto.auth;

import lombok.*;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    private String id;
    private String username;
    private String email;
    private String role;
    private String branch;
    private Instant createdAt;
}

