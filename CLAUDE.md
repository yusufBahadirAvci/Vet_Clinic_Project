# VetClinic – Project Rules (Claude Code reads this file every session)

Software Design course project. Team: Ceylin and Yusuf. All tasks are in `docs/TASKS.md`.
Ask the other team member before changing any rule in this file.

## Project
A website for a veterinary clinic:
- **Visitor:** browses adoption listings and applies.
- **Pet owner (OWNER):** logs in with a 6-digit one-time code (OTP) sent to their e-mail or phone; sees their own pets and vaccination cards.
- **Veterinarian (VET):** logs in with the same OTP flow; manages owners, pets, vaccinations and adoption listings.
- Owners cannot self-register. A vet creates them, or the system creates them when an adoption is approved.

## Tech stack (do not change; ask before adding a dependency)
Java 21, Spring Boot (Maven, `./mvnw`), Thymeleaf + Bootstrap 5 + Thymeleaf Spring Security extras,
Spring Security (session based; NO JWT), Spring Data JPA, Bean Validation, PostgreSQL 16, Flyway,
Spring Mail + Mailpit, Lombok, JUnit 5 + Mockito + Testcontainers, Docker Compose.
The Spring Boot version is pinned in `pom.xml`; `docs/decisions.md` records why it was chosen. Dependency names follow that version;
do not copy dependency names from documentation for a different version.

## Commands
- Start database and Mailpit: `docker compose up -d db mailpit`
- Run the app (dev profile): `./mvnw spring-boot:run`
- Tests: `./mvnw verify` (Docker must be running, Testcontainers is used)
- Run everything together: `docker compose up --build`
- App: http://localhost:8080 · Mailpit: http://localhost:8025

## Package structure (by feature)
```
com.vetclinic
├── auth          OTP, login/logout, SecurityConfig, CurrentUserService, SessionLoginService
├── notification  NotificationSender (Strategy), EmailSender, MockSmsSender, Factory, NotificationService, event listeners
├── user          User entity, UserRepository, owner management in the vet panel
├── pet           Pet entity, pet management, owner portal, PetProfileFacade
├── vaccination   Vaccine, Vaccination, vaccination management, reminder job
├── adoption      Listings, applications, State pattern, PetAdoptedEvent
└── common        ContactNormalizer, AgeFormatter, error handling, Clock, shared web (home page)
```
Inside each package, as needed: `web` (controllers + forms/DTOs), `service`, `domain` (entities, enums), `repository`.

## Code rules
1. **Layers:** Controller → Service → Repository. No business logic in controllers. Entities never go to views; use DTOs/form objects.
2. **The database changes only through Flyway.** `src/main/resources/db/migration/V{n}__description.sql`. **Never edit an existing migration**; add a new version instead. `spring.jpa.hibernate.ddl-auto=validate`.
   Demo data lives in `db/seed/` (`V1000__demo_data.sql` …) and is loaded only in the `dev` and `docker` profiles.
3. **Enums** are stored as `VARCHAR` (`@Enumerated(EnumType.STRING)`).
4. **Contact normalization:** e-mails and phone numbers are always normalized with `common.ContactNormalizer`. Phones are stored as `+905321234567`, e-mails in lower case.
5. **Time:** never call `LocalDate.now()` / `LocalDateTime.now()` / `Instant.now()` directly. Inject the `java.time.Clock` bean (zone `Europe/Istanbul`).
   `created_at` columns are filled by the database `DEFAULT now()`; entities use `@Column(insertable = false, updatable = false)` + `@Generated`, not `@CreationTimestamp`.
   Business timestamps (`expires_at`, `decided_at`, `used_at`, `kvkk_consent_at`, `reminder_sent_at`) are set in services using the Clock.
6. **Ownership check (IDOR):** every record an OWNER accesses is queried with an ownership condition (`findByIdAndOwnerId`). If it does not exist or belongs to someone else, return **404** (not 403).
7. **Current user:** obtained via `auth.CurrentUserService` (`requireUserId()`, `requireUser()`).
8. **Sending notifications:** only through `notification.NotificationService`. `JavaMailSender` is not called anywhere else.
9. **Errors:** throw `common.exception.NotFoundException` for missing records; `GlobalExceptionHandler` maps it to the 404 page.
10. **Security:** never log OTP codes, full phone numbers or full e-mail addresses; use `common.LogMasker` when needed. CSRF stays enabled.
11. **Language:** everything is in English — code, class/method/variable names, all user-facing text (pages, buttons, messages, e-mails, SMS), docs, diagrams and commit messages. `docs/TASKS.md` contains Turkish example messages; implement them in English.
12. **Transactions:** an exception thrown in a `@Transactional` method rolls the transaction back. If a change must survive a failure (e.g. `attempt_count++` on a wrong OTP), do not throw; return a result object instead.
    A `@TransactionalEventListener(phase = AFTER_COMMIT)` listener that writes to the database (e.g. `sms_outbox`) must be annotated `@Transactional(propagation = Propagation.REQUIRES_NEW)`.
13. **Tests:** a unit test for every service; MockMvc tests for controllers; database tests extend `AbstractIntegrationTest` (Testcontainers). `./mvnw verify` must pass before opening a PR.
14. **UI:** every page uses the `head`, `navbar`, `footer` and `flash` fragments from `templates/fragments/layout.html`. Bootstrap 5, mobile friendly.
15. **Locale:** developer machines run with a Turkish locale (`tr_TR`), where `"I".toLowerCase()` gives `"ı"`. Always pass `Locale.ROOT` to `toLowerCase`/`toUpperCase` and to `String.format` when the result is compared, stored or parsed. `.mvn/jvm.config` sets `-Duser.language=en -Duser.country=US` so builds and tests behave the same on every machine and in Docker.

## URL and access map
| Path | Access |
|---|---|
| `/`, `/adoptions`, `/adoptions/{id}`, `POST /adoptions/{id}/apply` | Everyone |
| `/login`, `/login/verify`, `POST /login/resend` | Everyone |
| `/css/**`, `/js/**`, `/images/**`, `/error` | Everyone |
| `/my/**` | OWNER |
| `/vet/**` | VET |
| `/dev/login` | `dev` profile only |
| `POST /logout` | Any logged-in user |

## Cross-team contracts (do not change signatures without discussing first)
```java
// common
ContactNormalizer: ContactType detectType(String raw); String normalize(String raw);
                   String normalizeEmail(String raw); String normalizePhone(String raw);  // throws InvalidContactException if invalid
AgeFormatter:      String format(LocalDate birthDate);   // "3 years", "4 months", "2 weeks"
// vaccination
VaccinationStatusCalculator: VaccinationStatus statusOf(LocalDate nextDueDate); // OVERDUE, DUE_SOON (<= 30 days), OK, NONE
// auth
CurrentUserService: Long requireUserId(); User requireUser(); Optional<Long> currentUserId();
SessionLoginService: void loginAs(User user, HttpServletRequest req, HttpServletResponse res);
// notification
NotificationService: void notifyUser(User user, String subject, String body);       // e-mail if the user has one, otherwise SMS
                     void notifyContact(String normalizedContact, String subject, String body);
// adoption (events; listeners use @TransactionalEventListener(AFTER_COMMIT))
// (the records are added to the adoption.event package in the first commit of Y5 so Ceylin can write C6 without waiting)
record PetAdoptedEvent(Long listingId, Long petId, Long newOwnerId, boolean newOwnerCreated) {}
record ApplicationRejectedEvent(Long applicationId) {}
// vaccination
record VaccineDueEvent(Long vaccinationId) {}
```

## Git
- `main` is protected; no direct pushes.
- Branch name: `feature/<task-code>-<short-name>` (e.g. `feature/c3-otp-login`).
- Every change is merged through a Pull Request; the other team member reviews and approves; CI must be green.
- Do not start the next phase until both tasks of the current phase are merged. Pull `main` at the start of each phase.
