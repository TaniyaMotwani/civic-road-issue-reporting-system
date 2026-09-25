package org.roadwatch.repository;

import java.util.List;
import org.roadwatch.domain.StatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StatusHistoryRepository extends JpaRepository<StatusHistory, Long> {
    List<StatusHistory> findByIssueGroupIdOrderByCreatedAtDesc(Long issueGroupId);
}
