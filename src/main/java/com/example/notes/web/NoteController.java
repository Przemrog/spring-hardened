package com.example.notes.web;

import com.example.notes.model.Note;
import com.example.notes.model.User;
import com.example.notes.repo.NoteRepository;
import com.example.notes.repo.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.Principal;
import java.time.Instant;
import java.util.List;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class NoteController {

    private final NoteRepository notes;
    private final UserRepository users;

    private static final Logger log = LoggerFactory.getLogger(
            NoteController.class);

    public NoteController(NoteRepository notes, UserRepository users) {
        this.notes = notes;
        this.users = users;
    }

    private User current(Principal principal) {
        return users.findByEmail(principal.getName()).orElseThrow();
    }

    // [HARDENING A01] wspolny punkt weryfikacji wlasciciela (deny-by-default)
    private Note ownedNote(
            Long id,
            Principal principal) {

        Note note = notes.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND));

        User user = current(principal);

        if (!note.getOwner()
                .getId()
                .equals(user.getId())) {

            log.warn(
                    "SECURITY event=idor_denied "
                            + "channel=web user={} user_id={} "
                            + "note_id={} owner_id={}",
                    user.getEmail(),
                    user.getId(),
                    id,
                    note.getOwner().getId());

            // Na zewnątrz pozostaje 404.
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND);
        }

        return note;
    }

    @GetMapping("/notes")
    public String index(Principal principal, Model model) {
        model.addAttribute("notes", notes.findByOwnerId(current(principal).getId()));
        return "notes/index";
    }

    @GetMapping("/notes/{id}")
    public String view(@PathVariable Long id, Principal principal, Model model) {
        model.addAttribute("note", ownedNote(id, principal));
        return "notes/view";
    }

    @GetMapping("/notes/new")
    public String createForm() {
        return "notes/create";
    }

    @PostMapping("/notes")
    public String create(@RequestParam String title, @RequestParam String body, Principal principal) {
        Note n = new Note();
        n.setOwner(current(principal));
        n.setTitle(title);
        n.setBody(body);
        notes.save(n);
        return "redirect:/notes";
    }

    @GetMapping("/notes/{id}/edit")
    public String editForm(@PathVariable Long id, Principal principal, Model model) {
        model.addAttribute("note", ownedNote(id, principal));
        return "notes/edit";
    }

    @PostMapping("/notes/{id}/edit")
    public String edit(@PathVariable Long id, @RequestParam String title,
            @RequestParam String body, Principal principal) {
        Note n = ownedNote(id, principal);
        n.setTitle(title);
        n.setBody(body);
        n.setUpdatedAt(Instant.now());
        notes.save(n);
        return "redirect:/notes/" + id;
    }

    @PostMapping("/notes/{id}/delete")
    public String delete(@PathVariable Long id, Principal principal) {
        Note n = ownedNote(id, principal);
        notes.delete(n);
        return "redirect:/notes";
    }

    @GetMapping("/notes/search")
    public String search(@RequestParam String q, Principal principal, Model model) {
        // [HARDENING A05] zapytanie parametryzowane przez Spring Data (bez surowego
        // SQL)
        List<Note> result = notes.findByOwnerIdAndTitleContainingIgnoreCase(current(principal).getId(), q);
        model.addAttribute("notes", result);
        model.addAttribute("query", q);
        return "notes/index";
    }

    @PostMapping("/notes/import")
    public String importUrl(
            @RequestParam String url,
            Principal principal) {

        URI uri;

        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Niedozwolony URL.");
        }

        String scheme = uri.getScheme();

        if (scheme == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Adres musi zawierać schemat HTTP lub HTTPS.");
        }

        scheme = scheme.toLowerCase(Locale.ROOT);

        if (!scheme.equals("http")
                && !scheme.equals("https")) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Dozwolone są wyłącznie adresy HTTP i HTTPS.");
        }

        String host = uri.getHost();

        if (host == null || host.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Adres nie zawiera prawidłowej nazwy hosta.");
        }

        // Dane użytkownika w URI, np. user@example.com,
        // nie są potrzebne w funkcji importu.
        if (uri.getUserInfo() != null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Adresy zawierające dane użytkownika są niedozwolone.");
        }

        InetAddress[] resolvedAddresses;

        try {
            resolvedAddresses = InetAddress.getAllByName(host);
        } catch (UnknownHostException ex) {
            /*
             * [HARDENING A01/SSRF]
             * Błąd DNS jest błędem wejścia użytkownika, a nie
             * nieobsłużonym błędem aplikacji.
             */
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Nie udało się rozwiązać nazwy hosta.");
        }

        for (InetAddress address : resolvedAddresses) {
            if (isForbiddenAddress(address)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Zablokowano adres wewnętrzny.");
            }
        }

        HttpClient client = HttpClient.newBuilder()
                // Jawny zakaz przekierowań blokuje scenariusz:
                // publiczny URL -> przekierowanie do sieci lokalnej.
                .followRedirects(
                        HttpClient.Redirect.NEVER)
                .connectTimeout(
                        Duration.ofSeconds(5))
                .build();

        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        HttpResponse<String> response;

        try {
            response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Przerwano pobieranie zasobu.");
        } catch (IOException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Nie udało się pobrać wskazanego zasobu.");
        }

        /*
         * Opcjonalne, ale rozsądne ograniczenie:
         * nie zapisujemy stron błędu jako poprawnego importu.
         */
        if (response.statusCode() < 200
                || response.statusCode() >= 300) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Zdalny serwer zwrócił błąd.");
        }

        Note note = new Note();
        note.setOwner(current(principal));
        note.setTitle("Import z " + host);
        note.setBody(response.body());

        notes.save(note);

        return "redirect:/notes";
    }

    private boolean isForbiddenAddress(
            InetAddress address) {

        return address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress();
    }
}
