package com.example.notes.api;

import com.example.notes.model.Note;
import com.example.notes.repo.NoteRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notes")
public class NoteApiController {

    private final NoteRepository notes;
    public NoteApiController(NoteRepository notes) { this.notes = notes; }

    private Long currentUserId() {
        return Long.valueOf(SecurityContextHolder.getContext().getAuthentication().getName());
    }

    @GetMapping
    public List<Note> mine() {
        return notes.findByOwnerId(currentUserId());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Note> one(@PathVariable Long id) {
        // [HARDENING A01] weryfikacja wlasciciela rowniez w warstwie API
        return notes.findById(id)
                .filter(n -> n.getOwner().getId().equals(currentUserId()))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
