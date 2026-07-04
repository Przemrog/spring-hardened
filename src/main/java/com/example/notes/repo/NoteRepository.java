package com.example.notes.repo;

import com.example.notes.model.Note;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NoteRepository extends JpaRepository<Note, Long> {
    List<Note> findByOwnerId(Long ownerId);

    // [HARDENING A05] zapytanie pochodne = parametryzowane przez Spring Data (bez surowego SQL)
    List<Note> findByOwnerIdAndTitleContainingIgnoreCase(Long ownerId, String title);
}
