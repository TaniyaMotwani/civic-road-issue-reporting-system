package org.roadwatch.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "reports")
public class RoadReport {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 24)
    private String code;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "citizen_id", nullable = false)
    private AppUser citizen;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_group_id", nullable = false)
    private IssueGroup issueGroup;
    @Enumerated(EnumType.STRING) @Column(name = "issue_type", nullable = false, length = 32)
    private IssueType issueType;
    @Column(length = 1000)
    private String description;
    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;
    @Column(nullable = false)
    private double latitude;
    @Column(nullable = false)
    private double longitude;
    @Column(length = 500)
    private String address;
    @Column(length = 160)
    private String area;
    @Enumerated(EnumType.STRING) @Column(length = 16)
    private Severity severity;
    @Enumerated(EnumType.STRING) @Column(name = "verification_state", nullable = false, length = 24)
    private VerificationState verificationState;
    @Column(name = "verification_note", length = 1000)
    private String verificationNote;
    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected RoadReport() { }

    public RoadReport(String code, AppUser citizen, IssueGroup issueGroup, IssueType issueType, String description,
                      String imageUrl, double latitude, double longitude, String address, String area,
                      Severity severity, VerificationState verificationState, String verificationNote) {
        this.code = code;
        this.citizen = citizen;
        this.issueGroup = issueGroup;
        this.issueType = issueType;
        this.description = description;
        this.imageUrl = imageUrl;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.area = area;
        this.severity = severity;
        this.verificationState = verificationState;
        this.verificationNote = verificationNote;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public AppUser getCitizen() { return citizen; }
    public IssueGroup getIssueGroup() { return issueGroup; }
    public IssueType getIssueType() { return issueType; }
    public String getDescription() { return description; }
    public String getImageUrl() { return imageUrl; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getAddress() { return address; }
    public String getArea() { return area; }
    public Severity getSeverity() { return severity; }
    public VerificationState getVerificationState() { return verificationState; }
    public String getVerificationNote() { return verificationNote; }
    public Instant getCreatedAt() { return createdAt; }
    public void review(VerificationState state, String note) { this.verificationState = state; this.verificationNote = note; }
}
