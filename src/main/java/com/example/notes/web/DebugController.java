package com.example.notes.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DebugController {

    @GetMapping("/debug/error")
    public String error(@RequestParam(required = false) String input) {
        // [OWASP A10] celowe wejscie w stan wyjatkowy; przy include-stacktrace=always slad stosu wycieka
        int value = Integer.parseInt(input);
        return "Parsed: " + value;
    }
}
