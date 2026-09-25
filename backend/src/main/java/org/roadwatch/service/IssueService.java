package org.roadwatch.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.roadwatch.api.ApiModels.*;
import org.roadwatch.domain.*;
import org.roadwatch.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class IssueService {
    private final IssueGroupRepository groups;
    private final ReportRepository reports;
    private final StatusHistoryRepository history;
    private final UserRepository users;
    private final ImageStorageService storage;
    private final VerificationService verification;

    public IssueService(IssueGroupRepository groups, ReportRepository reports, StatusHistoryRepository history,
                        UserRepository users, ImageStorageService storage, VerificationService verification) {
        this.groups = groups; this.reports = reports; this.history = history; this.users = users;
        this.storage = storage; this.verification = verification;
    }

    @Transactional
    public ReportView submit(String email, IssueType type, String description, double latitude, double longitude,
                             String address, String area, MultipartFile image) {
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Location coordinates are outside the valid range");
        AppUser citizen = users.findByEmailIgnoreCase(email).orElseThrow();
        String imageUrl = storage.store(image);
        VerificationService.Assessment assessment = verification.assess(readImage(image), image.getContentType(), type);
        String normalizedArea = clean(area, 160);
        String normalizedAddress = clean(address, 500);
        IssueGroup group = findDuplicate(type, latitude, longitude);
        if (group == null) {
            group = groups.save(new IssueGroup(code("ISS"), type, assessment.severity(), latitude, longitude, normalizedAddress, normalizedArea));
        } else {
            group.updateSummary(assessment.severity(), normalizedAddress, normalizedArea);
        }
        RoadReport report = reports.save(new RoadReport(code("RPT"), citizen, group, type, clean(description, 1000),
                imageUrl, latitude, longitude, normalizedAddress, normalizedArea, assessment.severity(), assessment.state(), assessment.note()));
        return reportView(report);
    }

    @Transactional(readOnly = true)
    public List<GroupSummary> listGroups(IssueStatus status, IssueType type, String area) {
        List<IssueGroup> values = status == null ? groups.findAllByOrderByUpdatedAtDesc() : groups.findByStatus(status);
        return values.stream().filter(g -> type == null || g.getIssueType() == type)
                .filter(g -> area == null || area.isBlank() || (g.getArea() != null && g.getArea().toLowerCase(Locale.ROOT).contains(area.toLowerCase(Locale.ROOT))))
                .map(this::summary).toList();
    }

    @Transactional(readOnly = true)
    public GroupDetail detail(Long id) {
        IssueGroup group = groups.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Issue was not found"));
        List<ReportView> items = reports.findByIssueGroupIdOrderByCreatedAtAsc(id).stream().map(this::reportView).toList();
        List<HistoryView> changes = history.findByIssueGroupIdOrderByCreatedAtDesc(id).stream()
                .map(h -> new HistoryView(h.getOldStatus(), h.getNewStatus(), h.getNote(), h.getChangedBy().getEmail(), h.getCreatedAt())).toList();
        return new GroupDetail(summary(group), items, changes);
    }

    public record GroupDetail(GroupSummary issue, List<ReportView> reports, List<HistoryView> statusHistory) { }

    @Transactional(readOnly = true)
    public List<ReportView> myReports(String email) {
        AppUser user = users.findByEmailIgnoreCase(email).orElseThrow();
        return reports.findByCitizenIdOrderByCreatedAtDesc(user.getId()).stream().map(this::reportView).toList();
    }

    @Transactional(readOnly = true)
    public DashboardView dashboard() {
        List<GroupSummary> recent = groups.findAllByOrderByUpdatedAtDesc().stream().limit(8).map(this::summary).toList();
        List<IssueGroup> all = groups.findAll();
        return new DashboardView(reports.count(), all.size(), count(all, IssueStatus.OPEN), count(all, IssueStatus.VERIFIED),
                count(all, IssueStatus.IN_PROGRESS), count(all, IssueStatus.RESOLVED), recent);
    }

    @Transactional
    public GroupSummary changeStatus(Long id, IssueStatus next, String note, String email) {
        IssueGroup group = groups.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Issue was not found"));
        IssueStatus old = group.getStatus();
        group.updateStatus(next);
        AppUser admin = users.findByEmailIgnoreCase(email).orElseThrow();
        history.save(new StatusHistory(group, admin, old.name(), next.name(), clean(note, 500)));
        return summary(group);
    }

    @Transactional
    public ReportView review(Long id, VerificationState state, String note) {
        RoadReport report = reports.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report was not found"));
        report.review(state, clean(note, 1000));
        return reportView(report);
    }

    private IssueGroup findDuplicate(IssueType type, double lat, double lon) {
        double latDelta = DuplicateMatcher.RADIUS_METERS / 111_320.0;
        double lonDelta = DuplicateMatcher.RADIUS_METERS / (111_320.0 * Math.max(0.1, Math.cos(Math.toRadians(lat))));
        return groups.findByLatitudeBetweenAndLongitudeBetweenAndIssueTypeAndCreatedAtAfter(
                        lat - latDelta, lat + latDelta, lon - lonDelta, lon + lonDelta, type, Instant.now().minus(DuplicateMatcher.LOOKBACK_DAYS, ChronoUnit.DAYS))
                .stream().filter(g -> DuplicateMatcher.matches(g, type, lat, lon, Instant.now()))
                .findFirst().orElse(null);
    }

    private byte[] readImage(MultipartFile image) {
        try { return image.getBytes(); } catch (java.io.IOException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read the selected image"); }
    }
    private long count(List<IssueGroup> list, IssueStatus status) { return list.stream().filter(g -> g.getStatus() == status).count(); }
    private String code(String prefix) { return prefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT); }
    private String clean(String value, int max) { if (value == null || value.isBlank()) return null; return value.trim().substring(0, Math.min(value.trim().length(), max)); }
    private GroupSummary summary(IssueGroup g) {
        return new GroupSummary(g.getId(), g.getCode(), g.getIssueType(), g.getStatus(), g.getSeverity(), g.getAddress(), g.getArea(),
                g.getLatitude(), g.getLongitude(), reports.countByIssueGroupId(g.getId()), g.getCreatedAt(), g.getUpdatedAt());
    }
    private ReportView reportView(RoadReport r) {
        IssueGroup g = r.getIssueGroup();
        return new ReportView(r.getId(), r.getCode(), g.getId(), g.getCode(), r.getIssueType(), r.getDescription(), r.getImageUrl(),
                r.getLatitude(), r.getLongitude(), r.getAddress(), r.getArea(), r.getSeverity(), r.getVerificationState(),
                r.getVerificationNote(), r.getCreatedAt(), g.getStatus());
    }
}
