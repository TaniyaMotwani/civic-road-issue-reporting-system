package org.roadwatch.service;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.roadwatch.domain.IssueType;
import org.roadwatch.domain.Severity;
import org.roadwatch.domain.VerificationState;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import java.time.Duration;

@Service
public class VerificationService {
    public record Assessment(VerificationState state, IssueType suggestedType, Severity severity, String note) { }
    private final String ollamaUrl;
    private final String model;
    private final RestClient client;

    public VerificationService(@Value("${app.ai.ollama-url:}") String ollamaUrl,
                               @Value("${app.ai.model:llama3.2-vision}") String model) {
        this.ollamaUrl = ollamaUrl == null ? "" : ollamaUrl.trim(); this.model = model;
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(120));
        this.client = RestClient.builder().requestFactory(factory).build();
    }

    @SuppressWarnings("unchecked")
    public Assessment assess(byte[] image, String mimeType, IssueType citizenType) {
        if (ollamaUrl.isBlank()) return new Assessment(VerificationState.NEEDS_REVIEW, citizenType, Severity.UNKNOWN,
                "Automated image review is not configured. An administrator should confirm the report.");
        try {
            Map<String, Object> payload = Map.of(
                    "model", model,
                    "stream", false,
                    "format", "json",
                    "messages", List.of(Map.of("role", "user", "images", List.of(Base64.getEncoder().encodeToString(image)),
                            "content", "Assess this photo for a visible road-surface issue. Return JSON only with roadIssue (boolean), type (POTHOLE, UNEVEN_ROAD, OBSTRUCTION, CRACK, WATERLOGGING, DAMAGED_SURFACE, OTHER), severity (LOW, MEDIUM, HIGH), confidence (0 to 1). This is decision support, not a definitive judgment.")));
            Map<String, Object> response = client.post().uri(ollamaUrl.replaceAll("/$", "") + "/api/chat")
                    .body(payload).retrieve().body(Map.class);
            Map<String, Object> message = (Map<String, Object>) response.get("message");
            String content = (String) message.get("content");
            Map<String, Object> result = new com.fasterxml.jackson.databind.ObjectMapper().readValue(content, Map.class);
            boolean roadIssue = Boolean.TRUE.equals(result.get("roadIssue"));
            IssueType suggested = enumValue(IssueType.class, result.get("type"), citizenType);
            Severity severity = enumValue(Severity.class, result.get("severity"), Severity.UNKNOWN);
            double confidence = result.get("confidence") instanceof Number n ? n.doubleValue() : 0;
            if (!Double.isFinite(confidence)) confidence = 0;
            confidence = Math.max(0, Math.min(1, confidence));
            VerificationState state = roadIssue && confidence >= 0.65 ? VerificationState.VERIFIED
                    : roadIssue ? VerificationState.NEEDS_REVIEW : VerificationState.NOT_A_ROAD_ISSUE;
            return new Assessment(state, suggested, severity,
                    "Local AI suggestion: " + suggested + ", " + severity + " severity, " + Math.round(confidence * 100) + "% confidence. Please review; this is not conclusive.");
        } catch (Exception ignored) {
            return new Assessment(VerificationState.NEEDS_REVIEW, citizenType, Severity.UNKNOWN,
                    "Automated image review was unavailable. An administrator should confirm the report.");
        }
    }

    private <E extends Enum<E>> E enumValue(Class<E> type, Object value, E fallback) {
        try { return Enum.valueOf(type, String.valueOf(value).toUpperCase()); }
        catch (Exception ignored) { return fallback; }
    }
}
