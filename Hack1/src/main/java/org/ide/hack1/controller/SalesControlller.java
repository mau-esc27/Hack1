package org.ide.hack1.controller;

import jakarta.validation.Valid;
import org.ide.hack1.dto.sales.SaleRequest;
import org.ide.hack1.dto.sales.SaleResponse;
import org.ide.hack1.dto.summary.SummaryRequestDTO;
import org.ide.hack1.dto.summary.SummaryResponseDTO;
import org.ide.hack1.entity.ReportRequest;
import org.ide.hack1.event.ReportRequestedEvent;
import org.ide.hack1.exception.ForbiddenException;
import org.ide.hack1.repository.ReportRequestRepository;
import org.ide.hack1.service.sales.SalesService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

@RestController
@RequestMapping("/sales")
public class SalesControlller {

    private final SalesService salesService;
    private final ReportRequestRepository reportRequestRepository;
    private final ApplicationEventPublisher eventPublisher;

    public SalesControlller(SalesService salesService, ReportRequestRepository reportRequestRepository, ApplicationEventPublisher eventPublisher) {
        this.salesService = salesService;
        this.reportRequestRepository = reportRequestRepository;
        this.eventPublisher = eventPublisher;
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

    @PostMapping("/summary/weekly")
    public ResponseEntity<SummaryResponseDTO> requestWeeklySummary(@Valid @RequestBody SummaryRequestDTO req) {
        // determine user and permissions
        String username = currentUsername();
        String role = currentRole();
        String userBranch = currentBranch();

        LocalDate from = req.getFrom();
        LocalDate to = req.getTo();
        String branch = req.getBranch();
        String emailTo = req.getEmailTo();

        // default to last 7 days if not provided
        if (from == null || to == null) {
            // last full 7 days ending yesterday
            LocalDate end = LocalDate.now(ZoneOffset.UTC).minusDays(1);
            LocalDate start = end.minusDays(6);
            if (from == null) from = start;
            if (to == null) to = end;
        }

        // BRANCH users can only request for their own branch
        if ("BRANCH".equals(role)) {
            if (branch != null && !branch.isBlank() && !branch.equals(userBranch)) {
                throw new ForbiddenException("Branch users can only request summaries for their assigned branch");
            }
            // enforce user's branch
            branch = userBranch;
        }

        // create ReportRequest and persist
        ReportRequest rr = ReportRequest.builder()
                .fromDate(from)
                .toDate(to)
                .branch(branch)
                .emailTo(emailTo)
                .requestedBy(username)
                .build();

        reportRequestRepository.save(rr);

        // publish event for async processing
        eventPublisher.publishEvent(new ReportRequestedEvent(rr.getId(), from, to, branch, emailTo, username));

        SummaryResponseDTO resp = SummaryResponseDTO.builder()
                .requestId(rr.getId())
                .status(rr.getStatus() != null ? rr.getStatus().name() : ReportRequest.Status.PROCESSING.name())
                .message(String.format("Su solicitud de reporte está siendo procesada. Recibirá el resumen en %s en unos momentos.", emailTo))
                .estimatedTime("30-60 segundos")
                .requestedAt(rr.getRequestedAt())
                .build();

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(resp);
    }
}
