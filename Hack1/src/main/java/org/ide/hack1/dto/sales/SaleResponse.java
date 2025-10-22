package org.ide.hack1.dto.sales;

import lombok.*;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaleResponse {
    private String id;
    private String sku;
    private int units;
    private double price;
    private String branch;
    private Instant soldAt;
    private String createdBy;
    private Instant createdAt;
}
