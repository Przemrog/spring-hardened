package com.example.notes.api;

import com.example.notes.model.User;
import com.example.notes.repo.UserRepository;
import com.example.notes.security.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class AuthApiController {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtUtil jwt;

    public AuthApiController(UserRepository users, PasswordEncoder encoder, JwtUtil jwt) {
        this.users = users; this.encoder = encoder; this.jwt = jwt;
    }

    public record LoginDto(String email, String password) {}

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginDto dto) {
        Optional<User> found = users.findByEmail(dto.email());
        if (found.isEmpty() || !encoder.matches(dto.password(), found.get().getPassword())) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(Map.of("token", jwt.generate(found.get())));
    }
}
