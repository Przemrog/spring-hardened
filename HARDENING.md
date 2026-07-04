# Wariant utwardzony Spring — wykaz zmian (do rozdziału 7 i PB3)

Lustrzana kopia aplikacji bazowej Spring z nałożonymi zabezpieczeniami. Każda zmiana zamyka
konkretną słabość oznaczoną w wariancie bazowym jako `[OWASP Axx]` i jest w kodzie utwardzonym
oznaczona jako `[HARDENING Axx]`.

## Mapa: słabość bazowa → utwardzenie

| OWASP 2025 | Wariant bazowy | Wariant utwardzony | Plik |
|---|---|---|---|
| A01 IDOR (poziomo) | pobranie po `id` bez sprawdzenia właściciela | wspólna metoda `ownedNote` (deny-by-default) | `web/NoteController.java` |
| A01 IDOR (API)     | j.w. w warstwie API | filtr właściciela w `one()` | `api/NoteApiController.java` |
| A01 eskalacja      | `/admin` dla każdego zalogowanego | `requestMatchers("/admin").hasRole("ADMIN")` | `config/SecurityConfig.java` |
| A01 SSRF           | pobranie dowolnego URL | tylko http(s) + blokada adresów prywatnych/loopback/link-local | `web/NoteController.java` |
| A02 nagłówki       | brak CSP (były domyślne X-Frame-Options, nosniff) | + CSP, HSTS, Referrer-Policy | `config/SecurityConfig.java` |
| A02/A10 błędy      | `include-stacktrace=always` | `never` + globalny `@ControllerAdvice` | `application.properties`, `web/GlobalExceptionHandler.java` |
| A04 ciasteczka     | domyślne | HttpOnly + SameSite=Strict (Secure pod HTTPS) | `application.properties` |
| A05 SQLi           | surowy SQL z konkatenacją | zapytanie pochodne Spring Data (parametryzowane) | `repo/NoteRepository.java`, `web/NoteController.java` |
| A05 XSS            | `th:utext` | `th:text` (kodowanie wyjścia) | `templates/notes/view.html` |
| A06/A07 rate limit | brak | własny `LoginRateLimitFilter` na `/login` i `/api/login` | `security/LoginRateLimitFilter.java` |
| A07 polityka haseł | brak | min. 10 znaków, wielka litera, cyfra, znak specjalny (rejestracja) | `web/AuthController.java` |
| A08 JWT            | brak walidacji issuer/audience, 24 h | walidacja issuer + audience, token 1 h | `security/JwtUtil.java` |
| A08 upload         | oryginalna nazwa, brak walidacji | allowlist rozszerzeń, limit rozmiaru, nazwa UUID | `web/ProfileController.java` |
| A09 logowanie      | brak audytu | nasłuch zdarzeń Spring Security (`AuthEventLogger`) | `security/AuthEventLogger.java` |
| CSRF               | **już włączony** (Spring domyślnie) | bez zmian — pozostaje włączony | `config/SecurityConfig.java` |
| A03 zależności     | miejsce na pakiet z CVE | wersje wolne od podatności | `pom.xml` |

## Nakład utwardzenia (dane do PB3 / H2) — kontrast ze .NET

- **Nowe zależności zewnętrzne: 0** — ale w odróżnieniu od .NET, gdzie rate limiting był gotowy
  w rdzeniu (`AddRateLimiter`), w Spring **rate limiting wymagał napisania własnego filtra**
  (`LoginRateLimitFilter`, ~40 linii). To namacalny przykład wyższego kosztu utwardzenia po
  stronie Springa dla tej konkretnej funkcji (istotne dla H2). Alternatywą byłaby zewnętrzna
  biblioteka (np. Bucket4j), czyli dodatkowa zależność.
- **Element, którego .NET dostarczał gotowego, a Spring nie:** pełna blokada konta (account
  lockout) — w .NET wbudowana w Identity, w Spring wymaga własnej logiki (licznik nieudanych
  prób + `AuthenticationFailureHandler`). Tu jako mitygację brute-force zastosowano rate limiting;
  pełny lockout wskaż w pracy jako dodatkowy koszt po stronie Springa.
- **Element, który Spring miał od razu, a .NET-MVC nie:** walidacja anty-CSRF — w Springu
  włączona domyślnie, więc w utwardzeniu nie trzeba było nic dodawać; w .NET-MVC trzeba było
  włączyć globalny `AutoValidateAntiforgeryToken`.
- **Pliki zmienione/dodane:** `SecurityConfig.java`, `NoteController.java`, `NoteApiController.java`,
  `NoteRepository.java`, `JwtUtil.java`, `AuthController.java`, `ProfileController.java`,
  `templates/notes/view.html`, `application.properties`, `docker-compose.yml`, `pom.xml` oraz
  nowe: `LoginRateLimitFilter.java`, `AuthEventLogger.java`, `GlobalExceptionHandler.java`.

> Dokładne liczby linii uzyskaj przez `git diff --stat` (lub `diff -r`) między katalogami
> `spring-base` i `spring-hardened` — to najczystsze, powtarzalne źródło danych do tabeli nakładu.

## Uwaga o hasłach seed
Polityka haseł działa na poziomie kontrolera rejestracji, więc dotyczy NOWYCH rejestracji.
Konta seedowane (`DataSeeder`) tworzone są bezpośrednio przez repozytorium i zachowują hasła
testowe (admin123/alice123/bob123) — logowanie tymi kontami działa. To odmiennie niż w .NET,
gdzie polityka egzekwowana jest w warstwie Identity i obejmuje także seed — różnica
architektoniczna warta odnotowania w pracy.

## Uwaga o TLS
Aplikacja nasłuchuje po HTTP na :8080 (jak wariant bazowy), aby zachować identyczny sposób
testowania. Flaga `Secure` ciasteczka i HSTS działają docelowo pod TLS (terminacja na
reverse-proxy). Pozostałe zabezpieczenia (nagłówki, CSRF, walidacja JWT, kontrola dostępu,
ochrona przed SQLi/XSS/SSRF, rate limiting) są w pełni weryfikowalne po HTTP.
