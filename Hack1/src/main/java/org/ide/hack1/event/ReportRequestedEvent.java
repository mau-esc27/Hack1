package org.ide.hack1.event;

import java.time.LocalDate;

public class ReportRequestedEvent {

    private final String requestId;
    private final LocalDate from;
    private final LocalDate to;
    private final String branch;
    private final String emailTo;
    private final String requestedBy;

    public ReportRequestedEvent(String requestId, LocalDate from, LocalDate to, String branch, String emailTo, String requestedBy) {
        this.requestId = requestId;
        this.from = from;
        this.to = to;
        this.branch = branch;
        this.emailTo = emailTo;
        this.requestedBy = requestedBy;
    }

    public String getRequestId() {
        return requestId;
    }

    public LocalDate getFrom() {
        return from;
    }

    public LocalDate getTo() {
        return to;
    }

    public String getBranch() {
        return branch;
    }

    public String getEmailTo() {
        return emailTo;
    }

    public String getRequestedBy() {
        return requestedBy;
    }
}
