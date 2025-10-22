package org.ide.hack1.service.sales;

import org.ide.hack1.dto.summary.SalesAggregatesDTO;
import org.ide.hack1.entity.Sale;
import org.ide.hack1.repository.SaleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalesAggregationServiceTest {

    @Mock
    private SaleRepository saleRepository;

    @InjectMocks
    private SalesAggregationService salesAggregationService;

    private Sale createSale(String id, String sku, int units, double price, String branch, Instant soldAt) {
        Sale s = new Sale();
        s.setId(id);
        s.setSku(sku);
        s.setUnits(units);
        s.setPrice(price);
        s.setBranch(branch);
        s.setSoldAt(soldAt);
        s.setCreatedBy("test");
        s.setCreatedAt(Instant.now());
        return s;
    }

    @Test
    void shouldCalculateCorrectAggregatesWithValidData() {
        // Given
        Instant now = Instant.now();
        List<Sale> mockSales = List.of(
                createSale("s1", "OREO_CLASSIC", 10, 1.99, "Miraflores", now),
                createSale("s2", "OREO_DOUBLE", 5, 2.49, "San Isidro", now),
                createSale("s3", "OREO_CLASSIC", 15, 1.99, "Miraflores", now)
        );

        when(saleRepository.findBySoldAtBetween(any(), any())).thenReturn(mockSales);

        // When
        SalesAggregatesDTO result = salesAggregationService.calculateAggregates(Instant.EPOCH, Instant.now(), null);

        // Then
        assertThat(result.getTotalUnits()).isEqualTo(30);
        double expectedRevenue = 10 * 1.99 + 5 * 2.49 + 15 * 1.99; // 10+15 =25 *1.99 + 12.45
        assertThat(result.getTotalRevenue()).isEqualTo(expectedRevenue);
        assertThat(result.getTopSku()).isEqualTo("OREO_CLASSIC");
        assertThat(result.getTopBranch()).isEqualTo("Miraflores");
    }

    @Test
    void shouldHandleEmptySalesList() {
        // Given
        when(saleRepository.findBySoldAtBetween(any(), any())).thenReturn(List.of());

        // When
        SalesAggregatesDTO result = salesAggregationService.calculateAggregates(Instant.EPOCH, Instant.now(), null);

        // Then
        assertThat(result.getTotalUnits()).isEqualTo(0);
        assertThat(result.getTotalRevenue()).isEqualTo(0.0);
        assertThat(result.getTopSku()).isNull();
        assertThat(result.getTopBranch()).isNull();
    }

    @Test
    void shouldFilterByBranch() {
        // Given
        Instant now = Instant.now();
        List<Sale> branchSales = List.of(
                createSale("s1", "A", 3, 1.0, "Miraflores", now),
                createSale("s2", "B", 2, 2.0, "Miraflores", now)
        );
        when(saleRepository.findBySoldAtBetweenAndBranch(any(), any(), any(), any(PageRequest.class))).thenReturn(new PageImpl<>(branchSales));

        // When
        SalesAggregatesDTO result = salesAggregationService.calculateAggregates(Instant.EPOCH, Instant.now(), "Miraflores");

        // Then
        assertThat(result.getTotalUnits()).isEqualTo(5);
        assertThat(result.getTopBranch()).isEqualTo("Miraflores");
    }

    @Test
    void shouldRespectDateFiltering() {
        // Given: simulate repository returning only sales in the date range
        Instant inRange = Instant.parse("2025-09-02T00:00:00Z");
        List<Sale> onlyRange = List.of(
                createSale("s1", "X", 4, 1.0, "M", inRange)
        );
        when(saleRepository.findBySoldAtBetween(any(), any())).thenReturn(onlyRange);

        // When
        SalesAggregatesDTO result = salesAggregationService.calculateAggregates(inRange, inRange, null);

        // Then
        assertThat(result.getTotalUnits()).isEqualTo(4);
        assertThat(result.getTotalRevenue()).isEqualTo(4.0);
    }

    @Test
    void shouldChooseOneTopSkuWhenTie() {
        // Given
        Instant now = Instant.now();
        List<Sale> sales = List.of(
                createSale("s1", "SKU_A", 10, 1.0, "B", now),
                createSale("s2", "SKU_B", 10, 1.0, "B", now)
        );
        when(saleRepository.findBySoldAtBetween(any(), any())).thenReturn(sales);

        // When
        SalesAggregatesDTO result = salesAggregationService.calculateAggregates(Instant.EPOCH, Instant.now(), null);

        // Then: topSku should be one of the tied ones
        assertThat(List.of("SKU_A", "SKU_B")).contains(result.getTopSku());
    }
}

