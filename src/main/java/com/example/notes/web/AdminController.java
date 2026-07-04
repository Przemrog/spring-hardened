package com.example.notes.web;

import com.example.notes.repo.NoteRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminController {

    private final NoteRepository notes;
    public AdminController(NoteRepository notes) { this.notes = notes; }

    // Dostep sterowany konfiguracja bezpieczenstwa. W wariancie bazowym /admin jest dostepny
    // dla KAZDEGO zalogowanego (brak wymogu roli ADMIN) => [OWASP A01 - eskalacja pionowa].
    @GetMapping("/admin")
    public String index(Model model) {
        model.addAttribute("notes", notes.findAll());
        return "admin";
    }
}
