package org.ide.hack1.service.sales;

import org.ide.hack1.dto.sales.SaleRequest;
import org.ide.hack1.dto.sales.SaleResponse;
import org.ide.hack1.entity.Sale;
import org.ide.hack1.exception.ForbiddenException;
import org.ide.hack1.exception.NotFoundException;
import org.ide.hack1.repository.SaleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SalesService {

    private final SaleRepository saleRepository;

    public SalesService(SaleRepository saleRepository) {
        this.saleRepository = saleRepository;
    }

    public SaleResponse createSale(SaleRequest req, String username, String role, String userBranch) {
        // If role is BRANCH, ensure branch matches user's branch
        if ("BRANCH".equals(role) && (req.getBranch() == null || !req.getBranch().equals(userBranch))) {
            throw new ForbiddenException("branch mismatch");
        }

        Sale s = new Sale();
        s.setSku(req.getSku());
        s.setUnits(req.getUnits());
        s.setPrice(req.getPrice());
        s.setBranch(req.getBranch());
        s.setSoldAt(req.getSoldAt());
        s.setCreatedBy(username);

        Sale saved = saleRepository.save(s);
        return toDto(saved);
    }

    public SaleResponse getSale(String id, String role, String userBranch) {
        Optional<Sale> o = saleRepository.findById(id);
        if (o.isEmpty()) throw new NotFoundException("sale not found");
        Sale s = o.get();
        if ("BRANCH".equals(role) && !s.getBranch().equals(userBranch)) {
            throw new ForbiddenException("forbidden");
        }
        return toDto(s);
    }

    public Page<SaleResponse> listSales(Instant from, Instant to, String branch, Pageable pageable, String role, String userBranch) {
        // enforce branch for BRANCH users
        if ("BRANCH".equals(role)) {
            branch = userBranch;
        }

        List<Sale> sales;
        if (branch == null || branch.isBlank()) {
            sales = saleRepository.findBySoldAtBetween(from, to);
            // simple manual paging
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), sales.size());
            List<Sale> pageList = start <= end ? sales.subList(start, end) : List.of();
            List<SaleResponse> dtoList = pageList.stream().map(this::toDto).collect(Collectors.toList());
            return new PageImpl<>(dtoList, pageable, sales.size());
        } else {
            Page<Sale> page = saleRepository.findBySoldAtBetweenAndBranch(from, to, branch, pageable);
            return page.map(this::toDto);
        }
    }

    public SaleResponse updateSale(String id, SaleRequest req, String role, String userBranch) {
        Sale s = saleRepository.findById(id).orElseThrow(() -> new NotFoundException("sale not found"));
        if ("BRANCH".equals(role) && !s.getBranch().equals(userBranch)) {
            throw new ForbiddenException("forbidden");
        }
        // Update allowed fields
        s.setSku(req.getSku());
        s.setUnits(req.getUnits());
        s.setPrice(req.getPrice());
        s.setBranch(req.getBranch());
        s.setSoldAt(req.getSoldAt());

        Sale saved = saleRepository.save(s);
        return toDto(saved);
    }

    public void deleteSale(String id, String role) {
        if (!"CENTRAL".equals(role)) {
            throw new ForbiddenException("only central can delete");
        }
        saleRepository.deleteById(id);
    }

    private SaleResponse toDto(Sale s) {
        return SaleResponse.builder()
                .id(s.getId())
                .sku(s.getSku())
                .units(s.getUnits())
                .price(s.getPrice())
                .branch(s.getBranch())
                .soldAt(s.getSoldAt())
                .createdBy(s.getCreatedBy())
                .createdAt(s.getCreatedAt())
                .build();
    }
}
