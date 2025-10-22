package org.ide.hack1.dto.summary;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SummaryRequestDTO {

    private LocalDate from;

    private LocalDate to;

    private String branch;

    @NotBlank
    @Email
    private String emailTo;

    private String format;
}
