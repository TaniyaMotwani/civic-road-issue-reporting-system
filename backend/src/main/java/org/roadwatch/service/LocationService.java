package org.roadwatch.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.roadwatch.api.ApiModels.LocationView;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class LocationService {
    private final boolean enabled;
    private final String userAgent;
    private final RestClient client;
    private Instant lastRequest = Instant.EPOCH;
    private final ConcurrentMap<String, LocationView> cache = new ConcurrentHashMap<>();

    public LocationService(@Value("${app.geocoding.enabled:false}") boolean enabled,
                           @Value("${app.geocoding.user-agent:Roadwatch/1.0}") String userAgent) {
        this.enabled = enabled;
        this.userAgent = userAgent;
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.client = RestClient.builder().requestFactory(factory).build();
    }

    @SuppressWarnings("unchecked")
    public LocationView reverse(double latitude, double longitude) {
        if (!enabled) return new LocationView(latitude, longitude, null, null);
        String cacheKey = String.format(java.util.Locale.ROOT, "%.5f,%.5f", latitude, longitude);
        LocationView cached = cache.get(cacheKey);
        if (cached != null) return new LocationView(latitude, longitude, cached.address(), cached.area());
        try {
            synchronized (this) {
                long wait = 1_100 - Duration.between(lastRequest, Instant.now()).toMillis();
                if (wait > 0) Thread.sleep(wait);
                lastRequest = Instant.now();
            }
            Map<String, Object> response = client.get()
                    .uri("https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat={lat}&lon={lon}&zoom=18&addressdetails=1", latitude, longitude)
                    .header("User-Agent", userAgent).retrieve().body(Map.class);
            if (response == null) return new LocationView(latitude, longitude, null, null);
            Map<String, Object> address = (Map<String, Object>) response.getOrDefault("address", Map.of());
            String area = first(address, "neighbourhood", "suburb", "quarter", "village", "town", "city_district", "city");
            LocationView result = new LocationView(latitude, longitude, (String) response.get("display_name"), area);
            if (cache.size() > 2_000) cache.clear();
            cache.put(cacheKey, result);
            return result;
        } catch (Exception ignored) {
            // Reporting should still work when the optional reverse-geocoding service is unavailable.
            return new LocationView(latitude, longitude, null, null);
        }
    }

    private String first(Map<String, Object> address, String... keys) {
        for (String key : keys) if (address.get(key) instanceof String value) return value;
        return null;
    }
}
