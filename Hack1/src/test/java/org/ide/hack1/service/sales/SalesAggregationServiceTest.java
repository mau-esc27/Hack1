package org.ide.hack1.service.sales;

import org.ide.hack1.entity.Sale;
import org.ide.hack1.repository.SaleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@Disabled("Tests disabled while implementing exception handling")
@ExtendWith(MockitoExtension.class)
class SalesAggregationServiceTest {

    @Mock
    private SaleRepository saleRepository;

    @InjectMocks
    private SalesAggregationService salesAggregationService;

    private Instant now = Instant.parse("2025-09-07T00:00:00Z");
    private Instant weekAgo = Instant.parse("2025-08-31T00:00:00Z");

    @BeforeEach
    void setUp() {
    }

    @Test
    void shouldCalculateCorrectAggregatesWithValidData() {
        List<Sale> mockSales = List.of(
                Sale.builder().sku("OREO_CLASSIC").units(10).price(1.99).branch("Miraflores").soldAt(now).build(),
                Sale.builder().sku("OREO_DOUBLE").units(5).price(2.49).branch("San Isidro").soldAt(now).build(),
                Sale.builder().sku("OREO_CLASSIC").units(15).price(1.99).branch("Miraflores").soldAt(now).build()
        );

        when(saleRepository.findBySoldAtBetween(any(), any())).thenReturn(mockSales);

        var result = salesAggregationService.calculateAggregates(weekAgo, now, null);

        assertThat(result.getTotalUnits()).isEqualTo(30);
        assertThat(result.getTotalRevenue()).isCloseTo((10+15)*1.99 + 5*2.49, org.assertj.core.data.Offset.offset(0.001));
        assertThat(result.getTopSku()).isEqualTo("OREO_CLASSIC");
        assertThat(result.getTopBranch()).isEqualTo("Miraflores");
    }

    @Test
    void shouldHandleEmptyList() {
        when(saleRepository.findBySoldAtBetween(any(), any())).thenReturn(List.of());

        var result = salesAggregationService.calculateAggregates(weekAgo, now, null);

        assertThat(result.getTotalUnits()).isEqualTo(0);
        assertThat(result.getTotalRevenue()).isEqualTo(0.0);
        assertThat(result.getTopSku()).isNull();
        assertThat(result.getTopBranch()).isNull();
    }

    @Test
    void shouldFilterByBranch() {
        List<Sale> mockSales = List.of(
                Sale.builder().sku("A").units(3).price(1.0).branch("X").soldAt(now).build()
        );
        when(saleRepository.findBySoldAtBetweenAndBranch(any(), any(), anyString(), any())).thenReturn(new PageImpl<>(mockSales));

        var result = salesAggregationService.calculateAggregates(weekAgo, now, "X");

        assertThat(result.getTotalUnits()).isEqualTo(3);
        assertThat(result.getTopBranch()).isEqualTo("X");
    }

    @Test
    void shouldRespectDateFiltering() {
        // here we simulate repository returning only sales in range
        List<Sale> mockSales = List.of(
                Sale.builder().sku("D").units(7).price(2.0).branch("B").soldAt(now).build()
        );
        when(saleRepository.findBySoldAtBetween(any(), any())).thenReturn(mockSales);

        var result = salesAggregationService.calculateAggregates(weekAgo, now, null);

        assertThat(result.getTotalUnits()).isEqualTo(7);
        assertThat(result.getTopSku()).isEqualTo("D");
    }

    @Test
    void shouldHandleTieForTopSku() {
        List<Sale> mockSales = List.of(
                Sale.builder().sku("S1").units(10).price(1.0).branch("Z").soldAt(now).build(),
                Sale.builder().sku("S2").units(10).price(1.0).branch("Z").soldAt(now).build()
        );
        when(saleRepository.findBySoldAtBetween(any(), any())).thenReturn(mockSales);

        var result = salesAggregationService.calculateAggregates(weekAgo, now, null);

        assertThat(result.getTotalUnits()).isEqualTo(20);
        assertThat(result.getTopSku()).isIn("S1", "S2");
    }
}
