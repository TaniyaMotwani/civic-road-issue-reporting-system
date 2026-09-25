package org.roadwatch.api;

import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MediaConfiguration implements WebMvcConfigurer {
    private final String directory;
    public MediaConfiguration(@Value("${app.storage.directory:./uploads}") String directory) {
        String uri = Path.of(directory).toAbsolutePath().normalize().toUri().toString();
        this.directory = uri.endsWith("/") ? uri : uri + "/";
    }
    @Override public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**").addResourceLocations(directory).setCachePeriod(3600);
    }
}
