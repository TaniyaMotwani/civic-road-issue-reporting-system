package org.roadwatch.domain;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "status_history")
public class StatusHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "issue_group_id")
    private IssueGroup issueGroup;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "changed_by")
    private AppUser changedBy;
    @Column(name = "old_status", length = 20)
    private String oldStatus;
    @Column(name = "new_status", nullable = false, length = 20)
    private String newStatus;
    @Column(length = 500)
    private String note;
    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected StatusHistory() { }
    public StatusHistory(IssueGroup issueGroup, AppUser changedBy, String oldStatus, String newStatus, String note) {
        this.issueGroup = issueGroup; this.changedBy = changedBy; this.oldStatus = oldStatus;
        this.newStatus = newStatus; this.note = note;
    }
    public Long getId() { return id; }
    public IssueGroup getIssueGroup() { return issueGroup; }
    public AppUser getChangedBy() { return changedBy; }
    public String getOldStatus() { return oldStatus; }
    public String getNewStatus() { return newStatus; }
    public String getNote() { return note; }
    public Instant getCreatedAt() { return createdAt; }
}
