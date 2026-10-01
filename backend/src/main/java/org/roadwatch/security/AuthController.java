package org.roadwatch.security;

import java.util.Locale;
import java.util.Map;

import org.roadwatch.api.ApiModels.UserView;
import org.roadwatch.domain.AppUser;
import org.roadwatch.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.web.csrf.CsrfToken;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository users;
    private final org.springframework.security.crypto.password.PasswordEncoder encoder;
    private final AuthenticationManager authenticationManager;
    private final HttpSessionSecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    public AuthController(UserRepository users, org.springframework.security.crypto.password.PasswordEncoder encoder,
                          AuthenticationManager authenticationManager) {
        this.users = users; this.encoder = encoder; this.authenticationManager = authenticationManager;
    }

    public record Credentials(@NotBlank @Email @Size(max=254) String email, @NotBlank @Size(min=10, max=72) String password) { }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserView register(@Valid @RequestBody Credentials credentials) {
        String email = credentials.email().trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmailIgnoreCase(email)) throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with that email already exists");
        AppUser user = users.save(new AppUser(email, encoder.encode(credentials.password()), AppUser.Role.CITIZEN));
        return new UserView(user.getId(), user.getEmail(), user.getRole().name());
    }

    @PostMapping("/login")
    public UserView login(@Valid @RequestBody Credentials credentials, HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(credentials.email().trim().toLowerCase(Locale.ROOT), credentials.password()));
        request.getSession(true);
        request.changeSessionId();
        SecurityContextHolder.getContext().setAuthentication(authentication);
        securityContextRepository.saveContext(SecurityContextHolder.getContext(), request, response);
        AppUser user = users.findByEmailIgnoreCase(authentication.getName()).orElseThrow();
        return new UserView(user.getId(), user.getEmail(), user.getRole().name());
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken csrfToken) { return Map.of("token", csrfToken.getToken()); }

    @GetMapping("/me")
    public UserView me(Authentication auth) {
        AppUser user = users.findByEmailIgnoreCase(auth.getName()).orElseThrow();
        return new UserView(user.getId(), user.getEmail(), user.getRole().name());
    }
}
