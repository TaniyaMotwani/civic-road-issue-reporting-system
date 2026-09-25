package org.roadwatch.repository;

import java.util.List;
import org.roadwatch.domain.RoadReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<RoadReport, Long> {
    List<RoadReport> findByCitizenIdOrderByCreatedAtDesc(Long citizenId);
    List<RoadReport> findByIssueGroupIdOrderByCreatedAtAsc(Long issueGroupId);
    long countByIssueGroupId(Long issueGroupId);
    long count();
}
