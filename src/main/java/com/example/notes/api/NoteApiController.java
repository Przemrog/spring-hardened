package com.example.notes.api;

import com.example.notes.repo.NoteRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notes")
public class NoteApiController {

    private final NoteRepository notes;

    public NoteApiController(NoteRepository notes) {
        this.notes = notes;
    }

    private Long currentUserId() {
        var authentication =
                SecurityContextHolder.getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()) {
            throw new IllegalStateException(
                    "Brak uwierzytelnionego użytkownika.");
        }

        return Long.valueOf(authentication.getName());
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<NoteDto> mine() {
        Long userId = currentUserId();

        return notes.findByOwnerId(userId)
                .stream()
                .map(NoteDto::from)
                .toList();
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<NoteDto> one(
            @PathVariable Long id) {

        Long userId = currentUserId();

        // [HARDENING A01]
        // Właściciel jest weryfikowany przed utworzeniem DTO.
        return notes.findById(id)
                .filter(note ->
                        note.getOwner()
                                .getId()
                                .equals(userId))
                .map(NoteDto::from)
                .map(ResponseEntity::ok)
                .orElseGet(() ->
                        ResponseEntity.notFound().build());
    }
}