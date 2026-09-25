package org.roadwatch.security;

import java.util.Locale;
import org.roadwatch.domain.AppUser;
import org.roadwatch.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrap implements ApplicationRunner {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final String email;
    private final String password;
    private final String environment;
    public AdminBootstrap(UserRepository users, PasswordEncoder encoder,
                          @Value("${app.admin.email:}") String email,
                          @Value("${app.admin.password:}") String password,
                          @Value("${app.environment:local}") String environment) {
        this.users = users; this.encoder = encoder; this.email = email; this.password = password; this.environment = environment;
    }
    @Override public void run(ApplicationArguments args) {
        if (email.isBlank() && password.isBlank() && !"production".equalsIgnoreCase(environment)) return;
        if (email.isBlank() || password.length() < 12) throw new IllegalStateException("Set both ADMIN_EMAIL and an ADMIN_PASSWORD with at least 12 characters");
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (!users.existsByEmailIgnoreCase(normalized)) users.save(new AppUser(normalized, encoder.encode(password), AppUser.Role.ADMIN));
    }
}
