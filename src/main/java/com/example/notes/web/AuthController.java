package com.example.notes.web;

import com.example.notes.model.User;
import com.example.notes.repo.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.regex.Pattern;

@Controller
public class AuthController {

    // [HARDENING A07] polityka hasel: min. 10 znakow, wielka litera, cyfra, znak specjalny
    private static final Pattern POLICY =
            Pattern.compile("^(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9]).{10,}$");

    private final UserRepository users;
    private final PasswordEncoder encoder;

    public AuthController(UserRepository users, PasswordEncoder encoder) {
        this.users = users; this.encoder = encoder;
    }

    @GetMapping("/login")
    public String login() { return "login"; }

    @GetMapping("/register")
    public String registerForm() { return "register"; }

    @PostMapping("/register")
    public String register(@RequestParam String email, @RequestParam String password, Model model) {
        if (users.existsByEmail(email)) {
            model.addAttribute("error", "Konto o tym adresie juz istnieje.");
            return "register";
        }
        if (!POLICY.matcher(password).matches()) {
            model.addAttribute("error",
                    "Haslo musi miec min. 10 znakow oraz zawierac wielka litere, cyfre i znak specjalny.");
            return "register";
        }
        User u = new User();
        u.setEmail(email);
        u.setPassword(encoder.encode(password));
        u.setRole("USER");
        users.save(u);
        return "redirect:/login";
    }
}
