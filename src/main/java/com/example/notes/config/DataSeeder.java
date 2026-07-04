package com.example.notes.config;

import com.example.notes.model.Note;
import com.example.notes.model.User;
import com.example.notes.repo.NoteRepository;
import com.example.notes.repo.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository users;
    private final NoteRepository notes;
    private final PasswordEncoder encoder;

    public DataSeeder(UserRepository users, NoteRepository notes, PasswordEncoder encoder) {
        this.users = users; this.notes = notes; this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (users.count() > 0) return;

        User admin = create("admin@local", "admin123", "ADMIN");
        User alice = create("alice@local", "alice123", "USER");
        User bob   = create("bob@local",   "bob123",   "USER");

        notes.save(note(alice, "Lista zakupow", "mleko, chleb, kawa"));
        notes.save(note(alice, "Haslo do routera", "prywatna notatka Alicji"));
        notes.save(note(bob,   "Pomysly na projekt", "prywatna notatka Boba"));
        notes.save(note(admin, "Notatka administratora", "tylko dla admina"));
    }

    private User create(String email, String password, String role) {
        User u = new User();
        u.setEmail(email);
        u.setPassword(encoder.encode(password));
        u.setRole(role);
        return users.save(u);
    }

    private Note note(User owner, String title, String body) {
        Note n = new Note();
        n.setOwner(owner);
        n.setTitle(title);
        n.setBody(body);
        return n;
    }
}
