package com.example.notes.api;

import com.example.notes.repo.NoteRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import com.example.notes.model.Note;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@RestController
@RequestMapping("/api/notes")
public class NoteApiController {

        private final NoteRepository notes;

        private static final Logger log = LoggerFactory.getLogger(
                        NoteApiController.class);

        public NoteApiController(NoteRepository notes) {
                this.notes = notes;
        }

        private Long currentUserId() {
                var authentication = SecurityContextHolder.getContext()
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

                var noteOptional = notes.findById(id);

                if (noteOptional.isEmpty()) {
                        return ResponseEntity
                                        .notFound()
                                        .build();
                }

                Note note = noteOptional.get();
                Long ownerId = note.getOwner().getId();

                if (!ownerId.equals(userId)) {
                        log.warn(
                                        "SECURITY event=idor_denied "
                                                        + "channel=api user_id={} "
                                                        + "note_id={} owner_id={}",
                                        userId,
                                        id,
                                        ownerId);

                        return ResponseEntity
                                        .notFound()
                                        .build();
                }

                return ResponseEntity.ok(
                                NoteDto.from(note));
        }
}