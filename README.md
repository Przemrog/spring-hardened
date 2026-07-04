# Aplikacja referencyjna — Spring Boot, WARIANT UTWARDZONY

Lustrzana kopia aplikacji bazowej Spring z nałożonymi zabezpieczeniami. Ta sama funkcjonalność,
te same endpointy i baza — różnicą jest warstwa ochronna. Pełne zestawienie zmian i dane do PB3
znajdują się w `HARDENING.md`.

## Uruchomienie
Zatrzymaj wcześniejsze warianty na porcie 8080, a przy zmianie schematu wyczyść wolumen:
```bash
docker compose down -v
docker compose up --build
```
Aplikacja: http://localhost:8080. Konta testowe jak w wariancie bazowym
(admin@local/admin123, alice@local/alice123, bob@local/bob123) — logowanie tymi kontami działa;
polityka haseł dotyczy nowych rejestracji przez /register.

## Szybka weryfikacja skuteczności utwardzenia
Te same próby co w wariancie bazowym powinny teraz zawieść:
- IDOR: `alice` otwiera `/notes/3` (notatka Boba) => 404.
- Eskalacja: `alice` wchodzi na `/admin` => 403 (wymagana rola ADMIN).
- XSS: notatka z `<script>` => treść wyświetla się jako tekst.
- SQLi: `/notes/search?q=%' OR '1'='1` => brak wycieku (zapytanie parametryzowane).
- SSRF: `/notes/import` z `http://169.254.169.254/...` => 400 (zablokowane).
- A10: `/debug/error?input=abc` => ogólny komunikat, bez śladu stosu.
- Nagłówki: odpowiedź zawiera Content-Security-Policy, X-Frame-Options, X-Content-Type-Options.
- Rate limit: >5 prób logowania w 30 s z jednego adresu => HTTP 429.

## Uwaga
Wariant utwardzony służy jako punkt odniesienia w porównaniu i jest przeznaczony wyłącznie
do kontrolowanych testów w izolowanym środowisku pracy magisterskiej.
