package org.roadwatch.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.roadwatch.domain.IssueGroup;
import org.roadwatch.domain.IssueType;
import org.roadwatch.domain.Severity;

class DuplicateMatcherTest {
    private final Instant now = Instant.now();

    @Test
    void groupsSameTypeWithinFortyMetres() {
        IssueGroup existing = group(IssueType.POTHOLE, 18.5080, 73.9260);
        double nearbyLatitude = 18.5080 + 25.0 / 111_320.0;
        assertTrue(DuplicateMatcher.matches(existing, IssueType.POTHOLE, nearbyLatitude, 73.9260, now));
    }

    @Test
    void separatesDifferentTypesAndDistantReports() {
        IssueGroup existing = group(IssueType.POTHOLE, 18.5080, 73.9260);
        assertFalse(DuplicateMatcher.matches(existing, IssueType.CRACK, 18.5080, 73.9260, now));
        double farLatitude = 55.0 / 111_320.0 + 18.5080;
        assertFalse(DuplicateMatcher.matches(existing, IssueType.POTHOLE, farLatitude, 73.9260, now));
    }

    private IssueGroup group(IssueType type, double latitude, double longitude) {
        return new IssueGroup("ISS-TEST", type, Severity.UNKNOWN, latitude, longitude, null, null);
    }
}
