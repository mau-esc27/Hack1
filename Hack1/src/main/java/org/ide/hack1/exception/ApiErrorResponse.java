package org.ide.hack1.exception;

import lombok.*;

import java.time.Instant;
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ApiErrorResponse {
    private String error;
    private String message;
    private Instant timestamp;
    private String path;

}

