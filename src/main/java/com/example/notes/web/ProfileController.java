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

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Controller
public class ProfileController {

    private static final long MAX_BYTES = 2L * 1024 * 1024;

    private static final int MAX_WIDTH = 4096;
    private static final int MAX_HEIGHT = 4096;
    private static final long MAX_PIXELS = 16_000_000L;

    private static final Set<String> ALLOWED_FORMATS =
            Set.of("png", "jpeg", "jpg");

    private final UserRepository users;

    public ProfileController(UserRepository users) {
        this.users = users;
    }

    private User current(Principal principal) {
        return users.findByEmail(principal.getName())
                .orElseThrow();
    }

    @GetMapping("/profile")
    public String profile(
            Principal principal,
            Model model) {

        model.addAttribute(
                "user",
                current(principal));

        return "profile";
    }

    @PostMapping("/profile")
    public String upload(
            @RequestParam("avatar") MultipartFile avatar,
            Principal principal) {

        User user = current(principal);

        if (avatar == null || avatar.isEmpty()) {
            return "redirect:/profile";
        }

        // [HARDENING A08]
        // Limit rozmiaru pliku przed rozpoczęciem dekodowania.
        if (avatar.getSize() > MAX_BYTES) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Plik jest zbyt duży.");
        }

        byte[] uploadedBytes;

        try {
            uploadedBytes = avatar.getBytes();
        } catch (IOException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Nie udało się odczytać pliku.");
        }

        BufferedImage decodedImage;

        try (
                ByteArrayInputStream byteStream =
                        new ByteArrayInputStream(uploadedBytes);

                ImageInputStream imageStream =
                        ImageIO.createImageInputStream(byteStream)
        ) {
            if (imageStream == null) {
                throw invalidImage();
            }

            Iterator<ImageReader> readers =
                    ImageIO.getImageReaders(imageStream);

            if (!readers.hasNext()) {
                throw invalidImage();
            }

            ImageReader reader = readers.next();

            try {
                reader.setInput(
                        imageStream,
                        true,
                        true);

                String detectedFormat =
                        reader.getFormatName()
                                .toLowerCase(Locale.ROOT);

                // [HARDENING A08]
                // Format jest ustalany z zawartości, nie z nazwy
                // ani Content-Type przesłanego przez klienta.
                if (!ALLOWED_FORMATS.contains(detectedFormat)) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Dozwolone są wyłącznie obrazy PNG i JPEG.");
                }

                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                long pixelCount = (long) width * height;

                // [HARDENING A08]
                // Ograniczenie wymiarów i liczby pikseli.
                if (width <= 0
                        || height <= 0
                        || width > MAX_WIDTH
                        || height > MAX_HEIGHT
                        || pixelCount > MAX_PIXELS) {

                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Wymiary obrazu są nieprawidłowe lub zbyt duże.");
                }

                // Pełne dekodowanie sprawdza strukturę pliku.
                decodedImage = reader.read(0);

                if (decodedImage == null) {
                    throw invalidImage();
                }
            } finally {
                reader.dispose();
            }
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (IOException | RuntimeException ex) {
            throw invalidImage();
        }

        /*
         * [HARDENING A08]
         * Normalizacja obrazu do zwykłego rastra usuwa zależność
         * od oryginalnej struktury pliku i jego metadanych.
         */
        int outputType = decodedImage.getColorModel().hasAlpha()
                ? BufferedImage.TYPE_INT_ARGB
                : BufferedImage.TYPE_INT_RGB;

        BufferedImage normalizedImage =
                new BufferedImage(
                        decodedImage.getWidth(),
                        decodedImage.getHeight(),
                        outputType);

        Graphics2D graphics =
                normalizedImage.createGraphics();

        try {
            graphics.drawImage(
                    decodedImage,
                    0,
                    0,
                    null);
        } finally {
            graphics.dispose();
        }

        Path directory = Paths.get("uploads")
                .toAbsolutePath()
                .normalize();

        String safeName =
                UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        + ".png";

        Path target = directory.resolve(safeName)
                .normalize();

        // Ochrona dodatkowa: ścieżka wynikowa musi pozostać
        // wewnątrz katalogu uploads.
        if (!target.startsWith(directory)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Nieprawidłowa ścieżka pliku.");
        }

        try {
            Files.createDirectories(directory);

            try (OutputStream output =
                         Files.newOutputStream(target)) {

                boolean encoded = ImageIO.write(
                        normalizedImage,
                        "png",
                        output);

                if (!encoded) {
                    throw new IOException(
                            "Brak kodera PNG.");
                }
            }

            user.setAvatarPath(
                    "/avatars/" + safeName);

            users.save(user);
        } catch (IOException ex) {
            try {
                Files.deleteIfExists(target);
            } catch (IOException ignored) {
                // Usuwanie awaryjne nie powinno przesłonić
                // pierwotnego błędu zapisu.
            }

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Nie udało się zapisać obrazu.");
        }

        return "redirect:/profile";
    }

    private ResponseStatusException invalidImage() {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Plik nie jest prawidłowym obrazem PNG lub JPEG.");
    }
}