# Architecture Decisions

## 1. Spring Boot 4.1.2 with Java 21

- **Date:** 2026-10-30
- **Decision:** Spring Boot 4.1.2 with Java 21
- **Rationale:** latest stable release on start.spring.io at project start; Java 21 is the LTS both team members use (Temurin 21)

## 2. Everything in English; project name VetClinic, package com.vetclinic

- **Date:** 2026-10-30
- **Decision:** code, class/method/variable names, all user-facing text (pages, buttons, messages, e-mails, SMS), docs, diagrams and commit messages are in English. The project is named VetClinic and the root package is `com.vetclinic`.
- **Rationale:** one language avoids mixed Turkish/English identifiers, keeps the code readable for any reviewer, and matches the Spring and Java ecosystem conventions. Turkish example messages in `docs/TASKS.md` only show the meaning and are implemented in English.

## 3. English enum values

- **Date:** 2026-10-30
- **Decision:** all enum values are English and stored as `VARCHAR` (`@Enumerated(EnumType.STRING)`): role `VET`, `OWNER`; species `CAT`, `DOG`, `BIRD`, `RABBIT`, `OTHER`; gender `MALE`, `FEMALE`, `UNKNOWN`; listing status `DRAFT`, `ACTIVE`, `RESERVED`, `ADOPTED`, `CLOSED`; application status `PENDING`, `APPROVED`, `REJECTED`; channel `EMAIL`, `SMS`.
- **Rationale:** follows decision 2; string storage keeps the database readable and safe against reordering enum constants.

## 4. Documentation file names

- **Date:** 2026-10-30
- **Decision:** the task list is `docs/TASKS.md`, decision records are `docs/decisions.md`, requirements are `docs/requirements.md`; diagrams live in `docs/uml/` with PNGs in `docs/uml/png/`.
- **Rationale:** fixed, English file names so both team members and `CLAUDE.md` refer to the same paths.

## 5. Locale rule: Locale.ROOT and .mvn/jvm.config

- **Date:** 2026-10-30
- **Decision:** always pass `Locale.ROOT` to `toLowerCase`/`toUpperCase` and to `String.format` when the result is compared, stored or parsed. `.mvn/jvm.config` sets `-Duser.language=en -Duser.country=US`.
- **Rationale:** developer machines run with a Turkish locale (`tr_TR`), where `"I".toLowerCase()` gives `"ı"`; this breaks e-mail normalization and enum parsing. The JVM config makes builds and tests behave the same on every machine and in Docker.

## 6. Local note: Windows PostgreSQL conflicts with the Docker db

- **Date:** 2026-10-30
- **Decision:** a PostgreSQL installed on Windows listens on port 5432 and conflicts with the Docker `db` service; stop that Windows service before running `docker compose up -d db mailpit`.
- **Rationale:** otherwise the app connects to the wrong database (or the container cannot bind the port) and Flyway/login fail with confusing errors.

## 7. Approval allowed while a listing is ACTIVE; RESERVED is optional

- **Date:** 2026-10-30
- **Decision:** a vet may approve an application while the listing is ACTIVE; RESERVED is an optional interview step. `complete()` is valid from both ACTIVE and RESERVED and moves the listing to ADOPTED. `acceptsApplications()` is true only in ACTIVE.
- **Rationale:** Y6 `approve()` accepts listings that are ACTIVE or RESERVED; forcing an interview step would add a click without business value.

## 8. adoption_listings.updated_at

- **Date:** 2026-10-30
- **Decision:** `updated_at TIMESTAMPTZ NOT NULL DEFAULT now()`; the default fills it on insert, and the service sets it via the injected `Clock` on every update.
- **Rationale:** the Y2 table did not specify a type; this follows the time rule in `CLAUDE.md` (no direct `now()` calls in Java, business timestamps set by services using the Clock).

## 9. CHECK constraints on enum columns

- **Date:** 2026-10-30
- **Decision:** all enum columns have CHECK constraints in `V1__init.sql`: `users.role`, `pets.species`, `pets.gender`, `vaccines.target_species` (`IS NULL OR IN (...)`, NULL = all species), `adoption_listings.status`, `adoption_applications.status` and `otp_codes.channel`. Each constraint lists exactly the values in decision 3.
- **Rationale:** the database rejects invalid values even outside the application (manual SQL, seed scripts, future migrations), so a typo cannot store a value that `@Enumerated(EnumType.STRING)` fails to read back.

## 10. Test database: one shared Testcontainers container as a Spring bean

- **Date:** 2026-10-31
- **Decision:** `AbstractIntegrationTest` (`@SpringBootTest`, profile `test`) imports `TestcontainersConfiguration`, which declares the `postgres:16` `PostgreSQLContainer` as a `@Bean` with `@ServiceConnection`. We do not use `@Testcontainers` + a static `@Container` field. The `test` profile runs Flyway with `classpath:db/migration` only, so no seed data is loaded.
- **Rationale:** with `@Testcontainers`, the JUnit extension stops the container after each test class, while Spring caches and reuses the application context; the next test class would then connect to a stopped database. A container bean lives exactly as long as the context, so all integration tests share one container, and `TestVetclinicApplication` reuses the same configuration.

## 11. One Channel enum in notification.domain

- **Date:** 2026-10-31
- **Decision:** the delivery channel enum is `com.vetclinic.notification.domain.Channel` (`EMAIL`, `SMS`). `auth.domain.OtpCode.channel` uses it, and the notification code in C3 (`NotificationSender`, `NotificationSenderFactory`, `NotificationService`) reuses it instead of declaring its own.
- **Rationale:** `otp_codes.channel` and the notification strategies describe the same concept; one enum keeps the values in one place (matching the CHECK constraint in `V1__init.sql`) and avoids converting between two identical enums. It lives in `notification` because that package owns delivery; `auth` depends on `notification` anyway.

## 12. Demo seed: repeated doses and the "latest dose per vaccine" rule

- **Date:** 2026-10-31
- **Decision:** `V1001__demo_pets_vaccines_adoptions.sql` gives the bird (Kiwi) and the rabbit (Snowball) two Rabies doses, because Rabies is the only catalog vaccine for those species and every owned pet has 2–4 records. The older dose's `next_due_date` is the date the newer dose was given (booster on time). Only the **latest dose of each vaccine** counts for a pet's vaccination status (`docs/TASKS.md` C4 step 3); the same applies to the vet's upcoming/overdue list (Y4 step 4). The reminder query (`findAllByNextDueDateBetween` from today onward) is not affected.
- **Rationale:** a vaccination card is a history; an older dose that was followed by a booster is completed, not overdue. Evaluating every record on its own would show the old Rabies dose as OVERDUE.

## 13. Privacy notice route is /privacy

- **Date:** 2026-10-31
- **Decision:** Privacy notice route is `/privacy` instead of `/kvkk` (English-only rule). The template is `templates/privacy.html`.
- **Rationale:** `CLAUDE.md` requires everything, including URLs, to be in English; `/kvkk` is a Turkish acronym.
