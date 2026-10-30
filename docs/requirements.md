# VetClinic – Requirements

> Task: C1 · Author: Ceylin · Diagrams: `docs/uml/` · Related decisions: `docs/decisions.md`

## 1. Purpose and scope
VetClinic supports two core activities of a veterinary clinic on the web:
1. **Pet owners** log in without a password (with a one-time code sent to their e-mail or phone) and view their own pets' details and vaccination cards.
2. **Adoption** of homeless animals in the clinic's care: listings are published publicly, applications are collected, and when an application is approved the animal is transferred to its new owner.

Out of scope: online appointments, payments, examination/treatment records, real SMS delivery (supported by the design; a mock sender is used for the demo).

## 2. Actors
| Actor | Description |
|---|---|
| **Visitor** | Anyone who is not logged in. Browses adoption listings and applies. |
| **Pet Owner (OWNER)** | A user registered by a veterinarian or created when an adoption is approved. Can only see their own pets. |
| **Veterinarian (VET)** | Clinic staff. Manages owners, pets, vaccinations and adoption records. |
| **Scheduler (System)** | Automated job that runs every morning and triggers vaccination reminders. |

## 3. Functional requirements

### 3.1 Authentication
| ID | Requirement | Use case |
|---|---|---|
| FR-01 | A registered user must be able to request a login code by entering **either** an e-mail address **or** a mobile number into a single field. | UC-04 |
| FR-02 | The system must normalize the input (e-mail in lower case; phone numbers in the formats `0532…`, `532…`, `90532…`, `+90532…` converted to `+90532…`). An invalid format must show a form error. | UC-04 |
| FR-03 | The system must generate a 6-digit code and send it by e-mail if the user entered an e-mail address, or by SMS if the user entered a phone number. | UC-04 |
| FR-04 | The user must be able to log in by entering the code. On success, a Veterinarian is redirected to the dashboard (`/vet`) and a Pet Owner to "My Pets" (`/my/pets`). | UC-05 |
| FR-05 | The user must be able to request a new code after the cooldown period. | UC-06 |
| FR-06 | The user must be able to log out. | UC-07 |

### 3.2 Pet owner portal
| ID | Requirement | Use case |
|---|---|---|
| FR-07 | A pet owner must be able to see their pets as cards: name, species/breed, age calculated from the birth date, and the nearest vaccination status. | UC-08 |
| FR-08 | A pet owner must be able to see a pet's details and **vaccination card**: all vaccination records from newest to oldest, with overdue and upcoming doses highlighted. | UC-09 |
| FR-09 | The vaccination card must be printable. | UC-09 |
| FR-10 | A pet owner must be able to see their own contact details (read-only). | UC-10 |

### 3.3 Veterinarian panel
| ID | Requirement | Use case |
|---|---|---|
| FR-11 | A veterinarian must be able to see a dashboard summary: total owners, total pets, pets with vaccinations due within 7 days, number of pending applications. | UC-11 |
| FR-12 | A veterinarian must be able to add, search, edit and deactivate pet owners. At least one contact detail and privacy consent are required; the same e-mail/phone cannot belong to two users. | UC-12 |
| FR-13 | A veterinarian must be able to add, edit, list and filter pets. A pet can belong to an owner or be ownerless. | UC-13 |
| FR-14 | A veterinarian must be able to manage the vaccine catalog (name, target species, repeat interval). | UC-14 |
| FR-15 | A veterinarian must be able to add, edit and delete vaccination records for a pet. The vaccine list must be filtered by the pet's species, and the next due date must be calculated automatically from the repeat interval. | UC-15 |
| FR-16 | A veterinarian must be able to list overdue vaccinations and vaccinations due within 30 days. | UC-16 |
| FR-17 | A veterinarian must be able to see the latest messages in the mock SMS outbox (for the demo). | UC-21 |

### 3.4 Adoption
| ID | Requirement | Use case |
|---|---|---|
| FR-18 | A visitor must be able to browse published listings and filter them by species, gender and age range. | UC-01 |
| FR-19 | A visitor must be able to view listing details. Draft or closed listings must not be visible. | UC-02 |
| FR-20 | A visitor must be able to apply for a published (ACTIVE) listing: full name, e-mail and/or phone, message, privacy consent. A second pending application for the same listing with the same contact details is not allowed. | UC-03 |
| FR-21 | A veterinarian must be able to create listings only for ownerless pets and change a listing's status according to its lifecycle (Draft → Active → Reserved → Adopted; Closed). Invalid transitions must be prevented. | UC-17 |
| FR-22 | A veterinarian must be able to see applications and approve or reject an application. | UC-18 |
| FR-23 | When an application is approved, the system must find the applicant among existing users or create a new pet owner, transfer the pet to them, mark the listing as "Adopted" and reject the other pending applications for the same listing. The pet's vaccination history must be kept. | UC-19 |
| FR-24 | The approved applicant must receive a welcome notification, and rejected applicants an information notification. | UC-20 |

### 3.5 Reminders
| ID | Requirement | Use case |
|---|---|---|
| FR-25 | Every morning at 09:00 (Istanbul time), the system must notify pet owners about vaccinations whose next dose is due within 7 days. A reminder must be sent only once per dose. | UC-22 |
| FR-26 | A veterinarian must be able to run the reminder job manually (for the demo). | UC-23 |

## 4. Non-functional requirements
| ID | Category | Requirement |
|---|---|---|
| NFR-01 | Security | Login codes must be generated with `SecureRandom`, stored hashed with BCrypt, valid for 5 minutes and single-use. |
| NFR-02 | Security | At most 5 wrong attempts are allowed per code; then the code must be invalidated. |
| NFR-03 | Security | At most one code per 60 seconds and at most 5 codes per hour may be sent to the same destination. |
| NFR-04 | Security | The code request screen must not reveal whether a user is registered (the same message in every case). |
| NFR-05 | Security | A pet owner must not be able to access another owner's records; such a request must return 404 (IDOR protection). |
| NFR-06 | Security | Role-based authorization: `/vet/**` for Veterinarians only, `/my/**` for Pet Owners only. CSRF protection enabled; the session ID must be renewed on login; the session must expire after 30 minutes of inactivity. |
| NFR-07 | Privacy / KVKK | A privacy notice must be presented and consent obtained before personal data is processed. Logs must not contain codes, full phone numbers or full e-mail addresses. |
| NFR-08 | Usability | The interface must be mobile-friendly (usable at 375 px width). |
| NFR-09 | Portability | The system must run with its demo data using a single command (`docker compose up --build`). |
| NFR-10 | Maintainability | Layered architecture (Controller → Service → Repository), package-by-feature; database changes only through Flyway migrations. |
| NFR-11 | Extensibility | A new notification channel (e.g. a real SMS provider) must be supported by adding a new class, without changing existing code. |
| NFR-12 | Testability | The service layer must be verified by unit tests (70%+ coverage) and the security rules by integration tests; CI must run the tests on every PR. |

## 5. Design decisions and rationale
| Decision | Rationale |
|---|---|
| **Passwordless login (OTP)** | Pet owners use the site rarely; a forgotten-password and reset flow would be unnecessary overhead. Sending the code only to the registered address, a short validity period, single use and attempt limits provide sufficient security. A second factor can be added for veterinarian accounts later. |
| **No self-registration** | The link between a pet and its owner is established by the clinic. With open registration, anyone could appear to own someone else's pet. |
| **Birth date instead of age** | Age is a derived value that changes over time; if stored, it quickly becomes wrong. Age is calculated at display time. |
| **Session, not JWT** | Pages are rendered on the server (Thymeleaf); there is no separate client. Sessions are simpler, revocable and come with built-in CSRF protection. |
| **404, not 403** | Returning 403 for another user's record reveals that the ID exists. 404 reveals nothing. |
| **Mock SMS** | Sending SMS in Turkey is paid and requires sender ID approval. Since the channel is abstracted with the Strategy pattern, a real provider can be added with a single class. |
| **Single `users` table with a role column** | Veterinarians and owners use the same login flow; separate tables would duplicate code. Authorization differences are handled through the role. |
| **Separate vaccine catalog table** | Prevents the same vaccine from being entered with different spellings and enables automatic calculation of the next due date. |

## 6. Use case list
| UC | Name | Actor | Requirement |
|---|---|---|---|
| UC-01 | Browse and filter adoption listings | Visitor | FR-18 |
| UC-02 | View listing details | Visitor | FR-19 |
| UC-03 | Apply for adoption | Visitor | FR-20 |
| UC-04 | Log in with code | Pet Owner, Veterinarian | FR-01, FR-02, FR-03 |
| UC-05 | Verify code (`<<include>>` from UC-04) | Pet Owner, Veterinarian | FR-04 |
| UC-06 | Resend code (`<<extend>>` UC-05) | Pet Owner, Veterinarian | FR-05 |
| UC-07 | Log out | Pet Owner, Veterinarian | FR-06 |
| UC-08 | View my pets | Pet Owner | FR-07 |
| UC-09 | View / print vaccination card | Pet Owner | FR-08, FR-09 |
| UC-10 | View my profile | Pet Owner | FR-10 |
| UC-11 | View dashboard | Veterinarian | FR-11 |
| UC-12 | Manage pet owners | Veterinarian | FR-12 |
| UC-13 | Manage pets | Veterinarian | FR-13 |
| UC-14 | Manage vaccine catalog | Veterinarian | FR-14 |
| UC-15 | Add / edit vaccination record | Veterinarian | FR-15 |
| UC-16 | View upcoming vaccinations | Veterinarian | FR-16 |
| UC-17 | Manage adoption listings | Veterinarian | FR-21 |
| UC-18 | Review applications | Veterinarian | FR-22 |
| UC-19 | Transfer ownership (`<<extend>>` UC-18, on approval only) | Veterinarian | FR-23 |
| UC-20 | Notify adoption result (`<<include>>` from UC-18) | Veterinarian | FR-24 |
| UC-21 | View SMS outbox | Veterinarian | FR-17 |
| UC-22 | Send vaccination reminder | Scheduler | FR-25 |
| UC-23 | Run reminders manually (`<<include>>` UC-22) | Veterinarian | FR-26 |
