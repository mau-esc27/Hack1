package org.ide.hack1.controller;

import org.ide.hack1.dto.summary.SummaryResponseDTO;
import org.ide.hack1.entity.ReportRequest;
import org.ide.hack1.exception.ForbiddenException;
import org.ide.hack1.exception.NotFoundException;
import org.ide.hack1.repository.ReportRequestRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reports")
public class ReportsController {

    private final ReportRequestRepository reportRequestRepository;

    public ReportsController(ReportRequestRepository reportRequestRepository) {
        this.reportRequestRepository = reportRequestRepository;
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

    @GetMapping("/{id}")
    public ResponseEntity<SummaryResponseDTO> getReport(@PathVariable String id) {
        ReportRequest rr = reportRequestRepository.findById(id).orElseThrow(() -> new NotFoundException("report request not found"));
        String role = currentRole();
        String userBranch = currentBranch();
        if ("BRANCH".equals(role) && rr.getBranch() != null && !rr.getBranch().equals(userBranch)) {
            throw new ForbiddenException("branch users can only view their own reports");
        }

        SummaryResponseDTO dto = SummaryResponseDTO.builder()
                .requestId(rr.getId())
                .status(rr.getStatus() != null ? rr.getStatus().name() : null)
                .message(rr.getErrorMessage())
                .estimatedTime(null)
                .requestedAt(rr.getRequestedAt())
                .summaryText(rr.getSummaryText())
                .build();
        return ResponseEntity.ok(dto);
    }
}

