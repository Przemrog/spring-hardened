package com.example.notes.web;

import com.example.notes.model.User;
import com.example.notes.repo.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.util.Set;
import java.util.UUID;

@Controller
public class ProfileController {

    private static final Set<String> ALLOWED = Set.of("jpg", "jpeg", "png", "gif");
    private static final long MAX_BYTES = 2L * 1024 * 1024;

    private final UserRepository users;
    public ProfileController(UserRepository users) { this.users = users; }

    private User current(Principal principal) {
        return users.findByEmail(principal.getName()).orElseThrow();
    }

    @GetMapping("/profile")
    public String profile(Principal principal, Model model) {
        model.addAttribute("user", current(principal));
        return "profile";
    }

    @PostMapping("/profile")
    public String upload(@RequestParam("avatar") MultipartFile avatar, Principal principal) throws Exception {
        User u = current(principal);
        if (avatar != null && !avatar.isEmpty()) {
            // [HARDENING A02/A08] walidacja rozszerzenia i rozmiaru + bezpieczna, losowa nazwa pliku
            String original = avatar.getOriginalFilename() == null ? "" : avatar.getOriginalFilename();
            int dot = original.lastIndexOf('.');
            String ext = dot >= 0 ? original.substring(dot + 1).toLowerCase() : "";
            if (!ALLOWED.contains(ext)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Niedozwolony typ pliku.");
            }
            if (avatar.getSize() > MAX_BYTES) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Plik jest zbyt duzy.");
            }
            Path dir = Paths.get("uploads");
            Files.createDirectories(dir);
            String safeName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
            avatar.transferTo(dir.resolve(safeName).toAbsolutePath());
            u.setAvatarPath("/avatars/" + safeName);
            users.save(u);
        }
        return "redirect:/profile";
    }
}
