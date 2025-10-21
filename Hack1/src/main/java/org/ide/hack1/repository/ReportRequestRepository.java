package org.ide.hack1.repository;
import org.ide.hack1.entity.ReportRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReportRequestRepository extends JpaRepository<ReportRequest, String> {
    List<ReportRequest> findByRequestedBy(String requestedBy);
    List<ReportRequest> findByStatus(org.ide.hack1.entity.ReportRequest.Status status);
}
