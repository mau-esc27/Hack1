package org.ide.hack1.service.sales;

import org.ide.hack1.dto.summary.SalesAggregatesDTO;
import org.ide.hack1.entity.Sale;
import org.ide.hack1.repository.SaleRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SalesAggregationService {

    private final SaleRepository saleRepository;

    public SalesAggregationService(SaleRepository saleRepository) {
        this.saleRepository = saleRepository;
    }

    public SalesAggregatesDTO calculateAggregates(Instant from, Instant to, String branch) {
        List<Sale> sales;
        if (branch == null || branch.isBlank()) {
            sales = saleRepository.findBySoldAtBetween(from, to);
        } else {
            sales = saleRepository.findBySoldAtBetweenAndBranch(from, to, branch, PageRequest.of(0, Integer.MAX_VALUE)).getContent();
        }

        long totalUnits = sales.stream().mapToLong(Sale::getUnits).sum();
        double totalRevenue = sales.stream().mapToDouble(s -> s.getUnits() * s.getPrice()).sum();

        Map<String, Long> unitsBySku = sales.stream().collect(Collectors.groupingBy(Sale::getSku, Collectors.summingLong(Sale::getUnits)));
        String topSku = unitsBySku.entrySet().stream()
                .max(Comparator.comparingLong(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse(null);

        Map<String, Double> revenueByBranch = sales.stream().collect(Collectors.groupingBy(Sale::getBranch, Collectors.summingDouble(s -> s.getUnits() * s.getPrice())));
        String topBranch = revenueByBranch.entrySet().stream()
                .max(Comparator.comparingDouble(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse(null);

        return SalesAggregatesDTO.builder()
                .totalUnits(totalUnits)
                .totalRevenue(totalRevenue)
                .topSku(topSku)
                .topBranch(topBranch)
                .unitsBySku(unitsBySku)
                .revenueByBranch(revenueByBranch)
                .build();
    }
}
