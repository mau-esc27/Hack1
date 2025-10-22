package org.ide.hack1.dto.sales;

import jakarta.validation.constraints.*;
import lombok.*;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaleRequest {

    @NotBlank
    private String sku;

    @Min(1)
    private int units;

    @DecimalMin(value = "0.0", inclusive = false)
    private double price;

    @NotBlank
    private String branch;

    @NotNull
    private Instant soldAt;
}
