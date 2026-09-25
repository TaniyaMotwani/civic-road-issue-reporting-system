package org.roadwatch.service;

import java.time.Instant;
import org.roadwatch.domain.IssueGroup;
import org.roadwatch.domain.IssueType;

/** Small explainable duplicate heuristic; image similarity can be added behind this boundary later. */
public final class DuplicateMatcher {
    public static final double RADIUS_METERS = 40.0;
    public static final long LOOKBACK_DAYS = 180;
    private static final double EARTH_RADIUS_METERS = 6_371_000;

    private DuplicateMatcher() { }

    public static boolean matches(IssueGroup candidate, IssueType type, double latitude, double longitude, Instant now) {
        return candidate.getIssueType() == type
                && !candidate.getCreatedAt().isBefore(now.minusSeconds(LOOKBACK_DAYS * 24 * 60 * 60))
                && distanceMeters(latitude, longitude, candidate.getLatitude(), candidate.getLongitude()) <= RADIUS_METERS;
    }

    public static double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1), dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return EARTH_RADIUS_METERS * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
