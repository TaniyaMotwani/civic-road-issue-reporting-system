package org.roadwatch.api;

import java.time.Instant;
import java.util.List;
import org.roadwatch.domain.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class ApiModels {
    private ApiModels() { }
    public record UserView(Long id, String email, String role) { }
    public record GroupSummary(Long id, String code, IssueType issueType, IssueStatus status, Severity severity,
                               String address, String area, double latitude, double longitude,
                               long reportCount, Instant createdAt, Instant updatedAt) { }
    public record ReportView(Long id, String code, Long issueGroupId, String issueCode, IssueType issueType,
                             String description, String imageUrl, double latitude, double longitude,
                             String address, String area, Severity severity, VerificationState verificationState,
                             String verificationNote, Instant createdAt, IssueStatus status) { }
    public record StatusChange(@NotNull IssueStatus status, @Size(max = 500) String note) { }
    public record HistoryView(String oldStatus, String newStatus, String note, String changedBy, Instant createdAt) { }
    public record DashboardView(long reports, long groups, long open, long verified, long inProgress, long resolved,
                                List<GroupSummary> recentGroups) { }
    public record LocationView(double latitude, double longitude, String address, String area) { }
}
