package org.roadwatch.repository;

import java.util.List;
import org.roadwatch.domain.IssueGroup;
import org.roadwatch.domain.IssueType;
import org.roadwatch.domain.IssueStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IssueGroupRepository extends JpaRepository<IssueGroup, Long> {
    List<IssueGroup> findByLatitudeBetweenAndLongitudeBetweenAndIssueTypeAndCreatedAtAfter(
            double south, double north, double west, double east, IssueType issueType, java.time.Instant createdAfter);
    List<IssueGroup> findByStatus(IssueStatus status);
    List<IssueGroup> findAllByOrderByUpdatedAtDesc();
}
