package org.roadwatch.api;

import java.util.List;
import org.roadwatch.api.ApiModels.*;
import org.roadwatch.domain.IssueStatus;
import org.roadwatch.domain.IssueType;
import org.roadwatch.domain.VerificationState;
import org.roadwatch.service.IssueService;
import org.roadwatch.service.LocationService;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class IssueController {
    private final IssueService issues;
    private final LocationService locations;
    public IssueController(IssueService issues, LocationService locations) { this.issues = issues; this.locations = locations; }

    @PostMapping("/api/reports")
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public ReportView submit(Authentication authentication, @RequestParam IssueType issueType,
                             @RequestParam(required = false) String description,
                             @RequestParam double latitude, @RequestParam double longitude,
                             @RequestParam(required = false) String address, @RequestParam(required = false) String area,
                             @RequestPart("image") MultipartFile image) {
        return issues.submit(authentication.getName(), issueType, description, latitude, longitude, address, area, image);
    }

    @GetMapping("/api/issues")
    public List<GroupSummary> groups(@RequestParam(required = false) IssueStatus status,
                                    @RequestParam(required = false) IssueType type,
                                    @RequestParam(required = false) String area) {
        return issues.listGroups(status, type, area);
    }

    @GetMapping("/api/issues/{id}")
    public IssueService.GroupDetail detail(@PathVariable Long id) { return issues.detail(id); }

    @GetMapping("/api/dashboard")
    public DashboardView dashboard() { return issues.dashboard(); }

    @GetMapping("/api/my/reports")
    public List<ReportView> myReports(Authentication authentication) { return issues.myReports(authentication.getName()); }

    @GetMapping("/api/locations/reverse")
    public LocationView reverse(@RequestParam double latitude, @RequestParam double longitude) {
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180)
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Invalid coordinates");
        return locations.reverse(latitude, longitude);
    }

    @PutMapping("/api/admin/issues/{id}/status")
    public GroupSummary changeStatus(@PathVariable Long id, @Valid @RequestBody StatusChange change, Authentication auth) {
        if (change.status() == null) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Choose a status");
        return issues.changeStatus(id, change.status(), change.note(), auth.getName());
    }

    public record VerificationReview(@NotNull VerificationState state, String note) { }
    @PutMapping("/api/admin/reports/{id}/verification")
    public ReportView review(@PathVariable Long id, @Valid @RequestBody VerificationReview review) {
        if (review.state() == null) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Choose a review result");
        return issues.review(id, review.state(), review.note());
    }
}
