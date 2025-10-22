package org.ide.hack1.event.Listeners;

import org.ide.hack1.event.ReportRequestedEvent;
import org.ide.hack1.entity.ReportRequest;
import org.ide.hack1.exception.NotFoundException;
import org.ide.hack1.repository.ReportRequestRepository;
import org.ide.hack1.service.mail.EmailService;
import org.ide.hack1.service.sales.SalesAggregationService;
import org.ide.hack1.dto.summary.SalesAggregatesDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.context.event.EventListener;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

@Component
public class ReportEventlisteners {

    private static final Logger log = LoggerFactory.getLogger(ReportEventlisteners.class);

    private final ReportRequestRepository reportRequestRepository;
    private final SalesAggregationService salesAggregationService;
    private final EmailService emailService;

    public ReportEventlisteners(ReportRequestRepository reportRequestRepository,
                                SalesAggregationService salesAggregationService,
                                EmailService emailService) {
        this.reportRequestRepository = reportRequestRepository;
        this.salesAggregationService = salesAggregationService;
        this.emailService = emailService;
    }

    @Async("taskExecutor")
    @EventListener
    public void handleReportRequested(ReportRequestedEvent event) {
        String requestId = event.getRequestId();
        log.info("Processing report request {} for {} - {} to {}", requestId, event.getRequestedBy(), event.getFrom(), event.getTo());
        ReportRequest rr = reportRequestRepository.findById(requestId).orElseThrow(() -> new NotFoundException("report request not found"));
        try {
            // convert LocalDate to Instant range (start of day to end of day UTC)
            LocalDate fromDate = event.getFrom();
            LocalDate toDate = event.getTo();
            if (fromDate == null || toDate == null) {
                throw new IllegalArgumentException("from/to dates are required");
            }
            Instant from = fromDate.atStartOfDay().toInstant(ZoneOffset.UTC);
            Instant to = toDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).minusSeconds(1);

            SalesAggregatesDTO agg = salesAggregationService.calculateAggregates(from, to, event.getBranch());

            if (agg == null) {
                throw new IllegalStateException("failed to calculate aggregates");
            }

            // generate simple summary (fallback until LLM integration)
            String summary = buildSimpleSummary(agg);

            rr.setSummaryText(summary);
            rr.setStatus(ReportRequest.Status.DONE);
            rr.setCompletedAt(Instant.now());
            reportRequestRepository.save(rr);

            // send email if provided
            String emailTo = event.getEmailTo();
            if (emailTo == null || emailTo.isBlank() || !emailTo.contains("@")) {
                log.warn("Invalid or missing email for report {}: {}", requestId, emailTo);
                return; // we already set DONE with summary, just skip email
            }

            String subject = String.format("Reporte Semanal Oreo - %s a %s", fromDate, toDate);
            String body = "Resumen:\n\n" + summary + "\n\nGracias.";
            try {
                emailService.sendSimpleEmail(emailTo, subject, body);
                log.info("Email sent for report {} to {}", requestId, emailTo);
            } catch (Exception mailEx) {
                // log but don't fail the whole process since aggregates were computed
                log.error("Failed to send email for report {}: {}", requestId, mailEx.getMessage(), mailEx);
                // optionally, update the report with email error
                rr.setErrorMessage("email error: " + mailEx.getMessage());
                reportRequestRepository.save(rr);
            }
        } catch (Exception ex) {
            log.error("Error processing report {}: {}", requestId, ex.getMessage(), ex);
            rr.setStatus(ReportRequest.Status.FAILED);
            rr.setErrorMessage(ex.getMessage());
            rr.setCompletedAt(Instant.now());
            reportRequestRepository.save(rr);
        }
    }

    private String buildSimpleSummary(SalesAggregatesDTO agg) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Se vendieron %d unidades en el periodo.", agg.getTotalUnits()));
        sb.append(" ");
        sb.append(String.format("Ingresos totales: %.2f.", agg.getTotalRevenue()));
        if (agg.getTopSku() != null) {
            sb.append(" ");
            sb.append(String.format("SKU más vendido: %s.", agg.getTopSku()));
        }
        if (agg.getTopBranch() != null) {
            sb.append(" ");
            sb.append(String.format("Sucursal destacada: %s.", agg.getTopBranch()));
        }
        // ensure <= 120 words roughly by truncating if needed
        String full = sb.toString();
        String[] words = full.split("\\s+");
        if (words.length > 120) {
            StringBuilder t = new StringBuilder();
            for (int i = 0; i < 120; i++) {
                t.append(words[i]).append(" ");
            }
            t.append("...");
            return t.toString().trim();
        }
        return full;
    }
}
