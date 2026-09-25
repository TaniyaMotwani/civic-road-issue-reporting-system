package org.roadwatch.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "issue_groups")
public class IssueGroup {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 24)
    private String code;
    @Enumerated(EnumType.STRING) @Column(name = "issue_type", nullable = false, length = 32)
    private IssueType issueType;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private IssueStatus status = IssueStatus.OPEN;
    @Enumerated(EnumType.STRING) @Column(length = 16)
    private Severity severity;
    @Column(nullable = false)
    private double latitude;
    @Column(nullable = false)
    private double longitude;
    @Column(length = 500)
    private String address;
    @Column(length = 160)
    private String area;
    @Column(nullable = false)
    private Instant createdAt = Instant.now();
    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    protected IssueGroup() { }

    public IssueGroup(String code, IssueType issueType, Severity severity, double latitude, double longitude,
                      String address, String area) {
        this.code = code;
        this.issueType = issueType;
        this.severity = severity;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.area = area;
    }

    public void updateStatus(IssueStatus status) { this.status = status; this.updatedAt = Instant.now(); }
    public void updateSummary(Severity severity, String address, String area) {
        if (severity != null && severityRank(severity) > severityRank(this.severity)) this.severity = severity;
        if (this.address == null && address != null) this.address = address;
        if (this.area == null && area != null) this.area = area;
        this.updatedAt = Instant.now();
    }
    private int severityRank(Severity value) {
        if (value == null) return 0;
        return switch (value) { case LOW -> 1; case MEDIUM -> 2; case HIGH -> 3; case UNKNOWN -> 0; };
    }
    public Long getId() { return id; }
    public String getCode() { return code; }
    public IssueType getIssueType() { return issueType; }
    public IssueStatus getStatus() { return status; }
    public Severity getSeverity() { return severity; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getAddress() { return address; }
    public String getArea() { return area; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
