package org.ide.hack1.dto.summary;

import lombok.*;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesAggregatesDTO {
    private long totalUnits;
    private double totalRevenue;
    private String topSku;
    private String topBranch;
    private Map<String, Long> unitsBySku;
    private Map<String, Double> revenueByBranch;
}

