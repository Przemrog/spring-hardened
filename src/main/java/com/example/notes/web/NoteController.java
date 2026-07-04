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

@Controller
public class NoteController {

    private final NoteRepository notes;
    private final UserRepository users;

    public NoteController(NoteRepository notes, UserRepository users) {
        this.notes = notes; this.users = users;
    }

    private User current(Principal principal) {
        return users.findByEmail(principal.getName()).orElseThrow();
    }

    // [HARDENING A01] wspolny punkt weryfikacji wlasciciela (deny-by-default)
    private Note ownedNote(Long id, Principal principal) {
        Note note = notes.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!note.getOwner().getId().equals(current(principal).getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
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
    public String createForm() { return "notes/create"; }

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
        // [HARDENING A05] zapytanie parametryzowane przez Spring Data (bez surowego SQL)
        List<Note> result = notes.findByOwnerIdAndTitleContainingIgnoreCase(current(principal).getId(), q);
        model.addAttribute("notes", result);
        model.addAttribute("query", q);
        return "notes/index";
    }

    @PostMapping("/notes/import")
    public String importUrl(@RequestParam String url, Principal principal) throws Exception {
        // [HARDENING A01/SSRF] tylko http(s) + odrzucenie adresow wewnetrznych/loopback/link-local
        URI uri;
        try { uri = URI.create(url); } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Niedozwolony URL.");
        }
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equals("http") || scheme.equals("https")) || uri.getHost() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Niedozwolony URL.");
        }
        for (InetAddress addr : InetAddress.getAllByName(uri.getHost())) {
            if (addr.isLoopbackAddress() || addr.isSiteLocalAddress()
                    || addr.isLinkLocalAddress() || addr.isAnyLocalAddress()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Zablokowano adres wewnetrzny (SSRF).");
            }
        }
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder(uri).build();
        HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());
        Note n = new Note();
        n.setOwner(current(principal));
        n.setTitle("Import z " + uri.getHost());
        n.setBody(resp.body());
        notes.save(n);
        return "redirect:/notes";
    }
}
