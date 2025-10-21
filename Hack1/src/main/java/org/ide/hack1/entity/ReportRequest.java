package org.ide.hack1.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "report_requests")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportRequest {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "from_date")
    private LocalDate fromDate;

    @Column(name = "to_date")
    private LocalDate toDate;

    @Column
    private String branch;

    @Column(nullable = false)
    private String emailTo;

    @Column(name = "requested_by")
    private String requestedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "summary_text", columnDefinition = "TEXT")
    private String summaryText;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @PrePersist
    public void prePersist() {
        if (this.id == null || this.id.isBlank()) {
            this.id = "req_" + UUID.randomUUID().toString().replace("-", "");
        }
        if (this.requestedAt == null) {
            this.requestedAt = Instant.now();
        }
        if (this.status == null) {
            this.status = Status.PROCESSING;
        }
    }

    public enum Status {
        PROCESSING,
        DONE,
        FAILED
    }

}
