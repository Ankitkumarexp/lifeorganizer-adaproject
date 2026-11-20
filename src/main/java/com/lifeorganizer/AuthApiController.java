package com.lifeorganizer;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthApiController {
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;

    public AuthApiController(JdbcTemplate jdbc, PasswordEncoder passwordEncoder) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("token", token.getToken());
    }

    @GetMapping("/me")
    public Map<String, Object> me(Authentication authentication) {
        return jdbc.queryForMap(
            "SELECT user_id AS id,name,email FROM users WHERE email=?",
            authentication.getName());
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@RequestBody Registration registration) {
        String name = registration.name() == null ? "" : registration.name().trim();
        String email = registration.email() == null ? "" : registration.email().trim().toLowerCase(Locale.ROOT);
        String password = registration.password() == null ? "" : registration.password();
        if (name.isBlank() || name.length() > 100 || !EMAIL.matcher(email).matches() || email.length() > 190) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Enter a name and a valid email address.");
        }
        if (password.length() < 12 || password.length() > 128) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Choose a password between 12 and 128 characters.");
        }

        try {
            jdbc.update("INSERT INTO users(name,email,password_hash) VALUES(?,?,?)",
                name, email, passwordEncoder.encode(password));
        } catch (DuplicateKeyException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "An account with that email already exists.");
        }
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(Map.of("message", "Account created. You can now sign in."));
    }

    public record Registration(String name, String email, String password) {}
}
