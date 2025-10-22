package org.ide.hack1.dto.summary;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SummaryRequestDTO {
    private LocalDate fromDate;
    private LocalDate toDate;
    private String branch;
    private String emailTo;
}
