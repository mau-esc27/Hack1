package org.ide.hack1.controller;

import jakarta.validation.Valid;
import org.ide.hack1.dto.sales.SaleRequest;
import org.ide.hack1.dto.sales.SaleResponse;
import org.ide.hack1.service.sales.SalesService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Optional;

@RestController
@RequestMapping("/sales")
public class SalesControlller {

    private final SalesService salesService;

    public SalesControlller(SalesService salesService) {
        this.salesService = salesService;
    }

    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? (String) auth.getPrincipal() : null;
    }

    private String currentRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        for (GrantedAuthority ga : auth.getAuthorities()) {
            String a = ga.getAuthority();
            if (a.startsWith("ROLE_")) return a.substring(5);
        }
        return null;
    }

    private String currentBranch() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        Object details = auth.getDetails();
        return details != null ? details.toString() : null;
    }

    @PostMapping
    public ResponseEntity<SaleResponse> createSale(@Valid @RequestBody SaleRequest req) {
        String username = currentUsername();
        String role = currentRole();
        String branch = currentBranch();
        SaleResponse resp = salesService.createSale(req, username, role, branch);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SaleResponse> getSale(@PathVariable String id) {
        String role = currentRole();
        String branch = currentBranch();
        SaleResponse resp = salesService.getSale(id, role, branch);
        return ResponseEntity.ok(resp);
    }

    @GetMapping
    public ResponseEntity<Page<SaleResponse>> listSales(@RequestParam Optional<String> from,
                                        @RequestParam Optional<String> to,
                                        @RequestParam Optional<String> branch,
                                        @RequestParam Optional<Integer> page,
                                        @RequestParam Optional<Integer> size) {
        Instant fromInst = from.map(Instant::parse).orElse(Instant.EPOCH);
        Instant toInst = to.map(Instant::parse).orElse(Instant.now());
        int p = page.orElse(0);
        int s = size.orElse(20);
        PageRequest pr = PageRequest.of(p, s);
        String role = currentRole();
        String userBranch = currentBranch();
        Page<SaleResponse> results = salesService.listSales(fromInst, toInst, branch.orElse(null), pr, role, userBranch);
        return ResponseEntity.ok(results);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SaleResponse> updateSale(@PathVariable String id, @Valid @RequestBody SaleRequest req) {
        String role = currentRole();
        String userBranch = currentBranch();
        SaleResponse resp = salesService.updateSale(id, req, role, userBranch);
        return ResponseEntity.ok(resp);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSale(@PathVariable String id) {
        String role = currentRole();
        salesService.deleteSale(id, role);
        return ResponseEntity.noContent().build();
    }
}
