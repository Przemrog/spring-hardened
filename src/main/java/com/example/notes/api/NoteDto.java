package com.example.notes.api;

import com.example.notes.model.Note;

import java.time.Instant;

/*
 * [HARDENING A08]
 * DTO ogranicza dane zwracane przez API do jawnie wybranych pól.
 * Encja User, skrót hasła i wewnętrzne szczegóły modelu nie są
 * serializowane do odpowiedzi.
 */
public record NoteDto(
        Long id,
        Long ownerId,
        String title,
        String body,
        boolean isPublic,
        Instant createdAt,
        Instant updatedAt
) {

    public static NoteDto from(Note note) {
        return new NoteDto(
                note.getId(),
                note.getOwner().getId(),
                note.getTitle(),
                note.getBody(),
                note.isPublic(),
                note.getCreatedAt(),
                note.getUpdatedAt()
        );
    }
}