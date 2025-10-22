package org.ide.hack1.dto.summary;

import lombok.*;
import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SummaryResponseDTO {
    private String requestId;
    private String status;
    private String message;
    private String estimatedTime;
    private Instant requestedAt;
    private List<String> features;
    private String summaryText;
}

