# VetClinic – Görev Dağılımı (Ceylin & Yusuf)

> Bu dosya repoda `docs/TASKS.md` olarak durur. Proje kuralları `CLAUDE.md` dosyasında.
> Her görev kartını kendi yapay zekânıza **olduğu gibi** verin (aşağıda "Yapay zekâya nasıl görev verilir" bölümü var).
> **Dil kuralı (`CLAUDE.md` kural 11):** Kod, kullanıcıya görünen tüm metinler (sayfalar, butonlar, mesajlar, e-posta, SMS), dokümanlar, diyagramlar ve commit mesajları **İngilizce**. Bu dosyadaki Türkçe örnek mesajlar sadece anlamı göstermek içindir; uygulamada İngilizce yazılır.

## Genel bakış

| Faz | Ceylin | Yusuf | Faz sonunda çalışan şey |
|---|---|---|---|
| 0 | Kurulum, repoyu klonla | Kurulum, repo + iskelet + `CLAUDE.md` + docker-compose | Boş uygulama ikinizde de açılıyor |
| 1 | **C1** Gereksinimler + davranış diyagramları | **Y1** Yapı diyagramları + ortak toplantı notları | `docs/` dolu, sözleşmeler net |
| 2 | **C2** Layout, SecurityConfig, ortak yardımcılar, CI | **Y2** Şema, entity'ler, repository'ler, seed, test altyapısı, Dockerfile | Dev girişiyle sayfalar açılıyor, CI yeşil |
| 3 | **C3** Bildirim katmanı + OTP girişi | **Y3** Veteriner paneli: sahip + hayvan yönetimi | Veteriner kodla girip sahip/hayvan ekliyor |
| 4 | **C4** Sahip portalı + aşı kartı | **Y4** Aşı kataloğu + aşı kaydı | **Ana senaryo uçtan uca çalışıyor** |
| 5 | **C5** Güvenlik sertleştirme | **Y5** Sahiplendirme: ilanlar, State, herkese açık sayfa, başvuru | Ziyaretçi başvuru yapabiliyor |
| 6 | **C6** Aşı hatırlatma + event dinleyicileri | **Y6** Başvuru onayı + sahiplik aktarımı + event'ler | Sahiplendirme ve hatırlatma tamam |
| 7 | **C7** Testler, README, demo senaryosu, rapor bölümleri | **Y7** Docker son hali, arayüz cilası, rapor bölümleri, slaytlar | Sunuma hazır, `v1.0` etiketi |

**Bağımlılık okları (kim kime ne teslim ediyor):**
- C2 → Yusuf: `ContactNormalizer`, `AgeFormatter`, `VaccinationStatusCalculator`, `CurrentUserService`, layout, dev girişi
- Y2 → Ceylin: entity'ler, repository'ler, seed verisi, `AbstractIntegrationTest`
- C3 → Yusuf: `NotificationService` (Y6'da sahiplendirme sonucu bildirimleri için)
- Y5/Y6 → Ceylin: `PetAdoptedEvent`, `ApplicationRejectedEvent` (C6'da dinlenecek)

**Aynı faz içindeki bağımlılıklar (beklemeyi önlemek için erken PR):**
- Faz 2: Yusuf önce **Y2a** küçük PR'ını açar (`V1__init.sql` + `User` + `Role` + `UserRepository` + kullanıcı seed'i), Ceylin onaylar ve hemen merge edilir. Ceylin C2'deki `CurrentUserService`, `DevLoginController` ve `SecurityConfigTest`'i bunun üstüne yazar. Y2'nin geri kalanı (Y2b) aynı branch'ten devam eder.
- Faz 5: Yusuf Y5'in ilk commit'inde `PetAdoptedEvent` ve `ApplicationRejectedEvent` record'larını ekler ve küçük bir PR ile merge eder; Ceylin C6'ya Faz 6'da beklemeden başlar.

---

## Yapay zekâya nasıl görev verilir

1. Fazın başında: `git checkout main && git pull`, sonra `git checkout -b feature/<kod>-<ad>`.
2. Yapay zekâya şunu yazın:
   > `CLAUDE.md` ve `docs/TASKS.md` dosyalarını oku. Benim görevim **C3** (veya Y3…). Önce bana adım adım bir plan çıkar, hangi dosyaları oluşturacağını/değiştireceğini listele, kod yazma. Onaylayınca uygula.
3. Planı okuyun. Kartta olmayan bir şey ekliyorsa ya da sözleşmelerdeki bir imzayı değiştiriyorsa durdurun.
4. Uygulattıktan sonra: `./mvnw verify` çalıştırın, uygulamayı açıp kabul kriterlerini **elle** tek tek deneyin.
5. Kodu satır satır okuyun. Sunumda hoca sizin kodunuzu soracak, "yapay zekâ yazdı" cevap değil.
6. PR açın, açıklamaya kabul kriterlerini checklist olarak yapıştırın. Diğer kişi inceleyip onaylasın.

**PR şablonu (`.github/pull_request_template.md`, Faz 0'da Yusuf ekler):**
```
## Görev
C? / Y? – kısa açıklama
## Yapılanlar
-
## Kabul kriterleri
- [ ] ...
## Kontrol
- [ ] ./mvnw verify geçti
- [ ] Yeni migration varsa eski migration'lara dokunulmadı
- [ ] All user-facing text, docs and commit messages are in English
- [ ] Loglarda kod / tam telefon / tam e-posta yok
```

---

# FAZ 0 – Kurulum

### İkiniz de
1. Kurun: **Git**, **JDK 21** (Temurin), **IntelliJ IDEA**, **Docker Desktop**, **Claude Code** + IntelliJ eklentisi.
2. Kontrol: `java -version` (21), `docker run hello-world` çalışıyor.
3. Git kimliğini ayarlayın: `git config --global user.name "..."`, `user.email "..."` (GitHub hesabınızdaki e-posta).

### Yusuf
1. GitHub'da `Vet_Clinic_Project` adında **private** repo aç (yerel klasör adı da aynı: `C:\dev\Vet_Clinic_Project`).
2. https://start.spring.io ile proje oluştur:
   - Maven, Java 21, Group `com`, Artifact `vetclinic`, paket `com.vetclinic`
   - **Spring Boot sürümü:** listede (SNAPSHOT/M/RC olmayan) en güncel kararlı sürümü seç. Seçtiğin sürümü ve nedenini `docs/decisions.md`'ye yaz.
   - Bağımlılıklar (hepsini **start.spring.io arayüzünden** seç, elle yazma; doğru adlarla gelir): Spring Web, Thymeleaf, Spring Security, Spring Data JPA, Validation, Java Mail Sender, PostgreSQL Driver, Flyway Migration, Lombok, Spring Boot DevTools, Testcontainers, Docker Compose Support **seçme** (compose'u kendimiz çalıştırıyoruz)
3. İndirdiğin `pom.xml`'i kontrol et; eksikse ekle: Flyway'in PostgreSQL modülü (`flyway-database-postgresql`), Thymeleaf'in Spring Security eklentisi (`thymeleaf-extras-springsecurity6` ya da Spring Security 7 kullanılıyorsa onun karşılığı), `spring-security-test` (test), Testcontainers PostgreSQL modülü (test). Artifact adları Spring Boot sürümüne göre değişebilir; `./mvnw dependency:tree` ile çözüldüğünü doğrula.
4. Kök dizine `docker-compose.yml`:
   - `db`: `postgres:16`, port 5432, DB/kullanıcı/şifre `vetclinic`, volume `pgdata`
   - `mailpit`: `axllent/mailpit`, port 1025 (SMTP) ve 8025 (arayüz)
   - (`app` servisi Y2'de eklenecek)
5. `application.yml`: varsayılan profil `dev`; datasource `localhost:5432/vetclinic`; `spring.mail.host=localhost`, `port=1025`; `ddl-auto=validate`; `spring.flyway.locations` dev'de `classpath:db/migration,classpath:db/seed`.
6. Boş bir `V1__init.sql` henüz ekleme (Y2'de gelecek). Uygulamanın açıldığını görmek için geçici olarak Flyway'i `spring.flyway.enabled=false` ve `ddl-auto=none` yapabilirsin; Y2'de geri al.
7. Kök dizine `CLAUDE.md`, `docs/` altına bu dosyayı (`TASKS.md`) ve `.github/pull_request_template.md`'yi koy.
8. `.gitignore`'a `.idea/`, `target/`, `*.iml`, `.env` ekli olsun.
9. İlk commit + push. GitHub → Settings → Branches → `main` için kural: PR zorunlu, 1 onay, (CI gelince) status check zorunlu.
10. Ceylin'i **Collaborator** olarak davet et.

### Ceylin
1. Daveti kabul et, `git clone`, IntelliJ'de aç.
2. `docker compose up -d db mailpit`, `./mvnw spring-boot:run` → http://localhost:8080 açılıyor (giriş sayfası çıkabilir, normal).
3. `CLAUDE.md`'yi baştan sona oku.

**Faz 0 bitti sayılır:** ikinizde de uygulama açılıyor, Mailpit http://localhost:8025'te görünüyor.

---

# FAZ 1 – Tasarım

### Ortak toplantı (fazın başında, ~1 saat, birlikte)
Aşağıdakileri `CLAUDE.md` ve bu dosyadaki haliyle onaylayın; değişiklik varsa iki dosyaya da işleyin:
- [ ] Veritabanı şeması (Y2'deki tablo listesi)
- [ ] URL ve yetki haritası
- [ ] Sözleşmeler (`ContactNormalizer`, `NotificationService`, event'ler…)
- [ ] Enum değerleri (rol, tür, cinsiyet, ilan durumu, başvuru durumu, kanal)
- [ ] Seed verisinde kullanılacak demo kullanıcılar
- [ ] Spring Boot sürümü (Faz 0'da seçilen) ve `CLAUDE.md`'deki zaman/transaction kuralları (kural 5 ve 12)
- [ ] Y2a ve Y5 ilk commit'i için erken PR düzeni

## C1 – Gereksinimler ve davranış diyagramları (Ceylin)
**Branch:** `feature/c1-requirements-behavior-uml`

1. `docs/requirements.md`:
   - Aktörler: Ziyaretçi, Hayvan Sahibi, Veteriner, Sistem (zamanlayıcı)
   - **Fonksiyonel gereksinimler** numaralı (FR-01 …): OTP girişi, sahip portalı, aşı kartı, veteriner paneli, aşı yönetimi, sahiplendirme ilanı, başvuru, onay/sahiplik aktarımı, aşı hatırlatma
   - **Fonksiyonel olmayan gereksinimler** (NFR-01 …): güvenlik (OTP süresi, deneme/gönderim limitleri, IDOR), KVKK, kullanılabilirlik (mobil uyum), taşınabilirlik (Docker), bakım kolaylığı (katmanlı mimari, testler)
   - Tasarım kararları ve gerekçeleri: şifresiz giriş, kendi kendine kayıt yok, yaş yerine doğum tarihi, session vs JWT, 404 vs 403, SMS'in mock olması
2. `docs/uml/use-case.puml` (PlantUML): aktörler ve tüm kullanım durumları; `<<include>>` ile "Kod doğrula", `<<extend>>` ile "Kodu tekrar gönder".
3. `docs/uml/otp-login-sequence.puml`: Kullanıcı → LoginController → ContactNormalizer → OtpService → UserRepository → OtpCodeRepository → NotificationSenderFactory → EmailSender/MockSmsSender; ikinci kısım: kod girişi → doğrulama → SessionLoginService → rol bazlı yönlendirme. `alt` blokları: kullanıcı yok (aynı mesaj), süre dolmuş, yanlış kod, 5. yanlış deneme.
4. `docs/uml/adoption-activity.puml`: ziyaretçi ilanı inceler → başvuru formu → veteriner başvuruları inceler → (görüşme için RESERVED) → onay/ret → sahip var mı/yok mu (oluştur) → sahiplik aktarımı → bildirimler. Kulvarlar (swimlane): Ziyaretçi, Sistem, Veteriner.
5. Her diyagramın PNG'sini de `docs/uml/png/` altına üret (IntelliJ PlantUML eklentisi ya da `plantuml` jar).
6. `docs/uml/README.md`: diyagram listesi, kimin çizdiği, PNG'ye nasıl dönüştürüldüğü.

**Kabul:** diyagramlar hatasız render oluyor; gereksinim numaraları use case'lerle eşleşiyor; Yusuf PR'ı onayladı.

## Y1 – Yapı diyagramları (Yusuf)
**Branch:** `feature/y1-structure-uml`

1. `docs/uml/er.puml`: Y2'deki tüm tablolar, PK/FK, unique alanlar, ilişki çoklukları.
2. `docs/uml/class-domain.puml`: entity'ler ve enum'lar.
3. `docs/uml/class-notification.puml`: `NotificationSender` arayüzü, `EmailSender`, `MockSmsSender`, `NotificationSenderFactory`, `NotificationService` (Strategy + Factory), ileride eklenebilecek `NetgsmSmsSender` kesikli çizgiyle.
4. `docs/uml/class-adoption-state.puml`: `ListingState` arayüzü ve 5 durum sınıfı, `AdoptionListing` ile ilişkisi.
5. `docs/uml/adoption-state.puml` (state diyagramı): DRAFT → ACTIVE → RESERVED → ADOPTED; ACTIVE/RESERVED → CLOSED; RESERVED → ACTIVE; CLOSED → DRAFT (yeniden aç). Geçersiz geçişleri not olarak yaz.
6. `docs/uml/deployment.puml`: Tarayıcı → `app` container (Spring Boot, 8080) → `db` (PostgreSQL, 5432) ve `mailpit` (1025/8025).
7. PNG'ler `docs/uml/png/` altına; README'ye satırlarını ekle.
8. Ortak toplantı notlarını `docs/decisions.md`'ye yaz (tarih + karar + gerekçe).

**Kabul:** ER diyagramı Y2'deki şemayla birebir aynı; Ceylin PR'ı onayladı.

---

# FAZ 2 – Altyapı

## C2 – Layout, güvenlik iskeleti, ortak yardımcılar, CI (Ceylin)
**Branch:** `feature/c2-layout-security-common`

**Sayfa düzeni**
1. `templates/fragments/layout.html`: `head` (Bootstrap 5 CDN, `css/app.css`), `navbar`, `flash` (başarı/hata mesajları), `footer` (KVKK linki, iletişim).
2. Navbar rol bazlı (`sec:authorize`): herkes → Ana sayfa, Sahiplendirme; anonim → Giriş; OWNER → Hayvanlarım; VET → Panel, Sahipler, Hayvanlar, Aşılar, İlanlar; girişliyken kullanıcı adı + Çıkış (POST formu, CSRF'li).
3. `common.web.HomeController` + `index.html`: kısa tanıtım, "Sahiplendirme ilanlarını gör" ve "Hayvanımın aşı kartı" butonları, klinik iletişim bilgisi.
4. `templates/kvkk.html`: kısa aydınlatma metni (`/kvkk`, herkese açık).

**Güvenlik**
5. `auth.SecurityConfig`: URL haritası `CLAUDE.md`'deki gibi; `formLogin` kapalı; anonim kullanıcı korumalı sayfaya gelince `/login`'e yönlendir; `logout` POST `/logout` → `/?logout`; CSRF açık.
6. `auth.AppUserPrincipal` (id, displayName, role → `ROLE_VET`/`ROLE_OWNER`).
7. `auth.SessionLoginService.loginAs(user, req, res)`: `SecurityContext` oluşturur, **`HttpSessionSecurityContextRepository` ile session'a kaydeder**, `request.changeSessionId()` ile session fixation'ı engeller.
8. `auth.CurrentUserService`: `requireUserId()`, `requireUser()`, `currentUserId()`.
9. `auth.DevLoginController` (`@Profile("dev")`): `/dev/login` sayfası seed kullanıcılarını listeler; tıklanan kullanıcıyla `SessionLoginService` üzerinden giriş yapar. (OTP gelene kadar Yusuf'un panelini test etmesi için.)

**Ortak yardımcılar (`common`)**
10. `ContactNormalizer` + `ContactType` (EMAIL/PHONE) + `InvalidContactException`:
    - `@` varsa e-posta: trim + küçük harf + basit format kontrolü
    - Telefon: boşluk, `-`, `(`, `)` temizlenir; `0532…`, `532…`, `90532…`, `+90532…` → `+90532…`; sonuç `+90` + `5` ile başlayan 10 hane değilse hata
11. `AgeFormatter.format(birthDate)` (Clock kullanır): ≥1 yıl → "3 yaş"; 1–11 ay → "4 aylık"; <1 ay → "2 haftalık"; gelecek tarih → hata.
12. `vaccination.VaccinationStatus` (OVERDUE, DUE_SOON, OK, NONE) + `VaccinationStatusCalculator.statusOf(nextDueDate)`: null → NONE, bugünden önce → OVERDUE, ≤30 gün → DUE_SOON, diğer → OK.
13. `common.config.TimeConfig`: `Clock` bean'i (`Europe/Istanbul`).
14. `common.exception.NotFoundException`, `common.web.GlobalExceptionHandler` (`@ControllerAdvice`): NotFound → 404 sayfası; `InvalidContactException` → form hatası; diğer → 500 sayfası (stack trace kullanıcıya gösterilmez, loglanır).
15. `templates/error/404.html`, `403.html`, `500.html` (layout'u kullanır, ana sayfaya dön butonu).
16. `common.LogMasker`: `maskEmail("ceylin@gmail.com")` → `c*****@gmail.com`, `maskPhone("+905321234567")` → `+90532*****67`.

**CI**
17. `.github/workflows/ci.yml`: push ve PR'da; `actions/setup-java` (Temurin 21, maven cache); `./mvnw -B verify`.
18. Yusuf'a söyle: `main` branch kuralına CI'ı zorunlu status check olarak eklesin.

**Testler:** `ContactNormalizerTest` (tüm formatlar + geçersizler), `AgeFormatterTest` (sabit Clock), `VaccinationStatusCalculatorTest`, `LogMaskerTest`, `SecurityConfigTest` (MockMvc: anonim `/my/pets` → `/login`'e yönlendirme, OWNER `/vet` → 403, herkes `/` → 200).

**Kabul:** dev girişiyle VET ve OWNER olarak girilebiliyor ve navbar role göre değişiyor; olmayan bir URL'de özel 404 sayfası çıkıyor; CI yeşil.

## Y2 – Şema, entity'ler, seed, test altyapısı, Docker (Yusuf)
**Branch:** `feature/y2-schema-entities-seed`

> **Y2a (fazın ilk günü, ayrı küçük PR):** `V1__init.sql` (tüm tablolar), `User` entity'si + `Role` enum'u + `UserRepository`, `V1000__demo_data.sql`'in sadece kullanıcı kısmı, Faz 0'daki geçici Flyway ayarlarının geri alınması. Ceylin onaylasın, merge edilsin. Kalan maddeler (Y2b) aynı branch'te devam eder.

**1. `db/migration/V1__init.sql`** (Faz 0'daki geçici ayarları geri al):

| Tablo | Kolonlar |
|---|---|
| `users` | `id BIGSERIAL PK`, `full_name VARCHAR(100) NOT NULL`, `email VARCHAR(254) UNIQUE NULL`, `phone VARCHAR(13) UNIQUE NULL`, `role VARCHAR(10) NOT NULL CHECK (role IN ('VET','OWNER'))`, `active BOOLEAN NOT NULL DEFAULT TRUE`, `kvkk_consent_at TIMESTAMPTZ NULL`, `created_at TIMESTAMPTZ NOT NULL DEFAULT now()`, `CHECK (email IS NOT NULL OR phone IS NOT NULL)` |
| `pets` | `id`, `owner_id FK→users NULL` (sahipsizse NULL), `name VARCHAR(50) NOT NULL`, `species VARCHAR(10) NOT NULL` (CAT, DOG, BIRD, RABBIT, OTHER), `breed VARCHAR(60)`, `gender VARCHAR(10) NOT NULL` (MALE, FEMALE, UNKNOWN), `birth_date DATE NOT NULL`, `color VARCHAR(40)`, `microchip_no VARCHAR(20) UNIQUE NULL`, `is_neutered BOOLEAN NOT NULL DEFAULT FALSE`, `photo_url VARCHAR(500)`, `created_at` |
| `vaccines` | `id`, `name VARCHAR(80) NOT NULL UNIQUE`, `target_species VARCHAR(10) NULL` (NULL = hepsi), `repeat_interval_days INT NULL` (NULL = tek doz), `description VARCHAR(500)` |
| `vaccinations` | `id`, `pet_id FK NOT NULL`, `vaccine_id FK NOT NULL`, `administered_at DATE NOT NULL`, `next_due_date DATE NULL`, `vet_id FK→users NOT NULL`, `lot_number VARCHAR(40)`, `notes VARCHAR(500)`, `reminder_sent_at TIMESTAMPTZ NULL`, `created_at`; index `(next_due_date)` |
| `adoption_listings` | `id`, `pet_id FK UNIQUE NOT NULL`, `title VARCHAR(100) NOT NULL`, `description TEXT NOT NULL`, `status VARCHAR(10) NOT NULL` (DRAFT, ACTIVE, RESERVED, ADOPTED, CLOSED), `created_by FK→users NOT NULL`, `created_at`, `updated_at` |
| `adoption_applications` | `id`, `listing_id FK NOT NULL`, `applicant_name VARCHAR(100) NOT NULL`, `applicant_email VARCHAR(254) NULL`, `applicant_phone VARCHAR(13) NULL`, `message TEXT`, `kvkk_consent BOOLEAN NOT NULL`, `status VARCHAR(10) NOT NULL` (PENDING, APPROVED, REJECTED), `created_at`, `decided_at TIMESTAMPTZ NULL`; `CHECK (applicant_email IS NOT NULL OR applicant_phone IS NOT NULL)` |
| `otp_codes` | `id`, `user_id FK NOT NULL`, `channel VARCHAR(5) NOT NULL` (EMAIL, SMS), `destination VARCHAR(254) NOT NULL`, `code_hash VARCHAR(100) NOT NULL`, `expires_at TIMESTAMPTZ NOT NULL`, `attempt_count INT NOT NULL DEFAULT 0`, `used_at TIMESTAMPTZ NULL`, `created_at`; index `(destination, created_at)` |
| `sms_outbox` | `id`, `phone VARCHAR(13) NOT NULL`, `message VARCHAR(500) NOT NULL`, `created_at` |

**2. Entity'ler ve enum'lar** her biri kendi paketinde (`user.domain.User`, `pet.domain.Pet`, `vaccination.domain.Vaccine`/`Vaccination`, `adoption.domain.AdoptionListing`/`AdoptionApplication`, `auth.domain.OtpCode`, `notification.domain.SmsOutboxMessage`). Enum'lar `@Enumerated(STRING)`, ilişkiler `LAZY`. `created_at` veritabanındaki `DEFAULT now()` ile dolar (`insertable = false, updatable = false` + `@Generated`); `@CreationTimestamp` kullanılmaz (bkz. `CLAUDE.md` kural 5).

**3. Repository'ler** (sözleşmedeki metotlar dahil):
- `UserRepository`: `findByEmail`, `findByPhone`, `findAllByRoleOrderByFullName`, `existsByEmail`, `existsByPhone`
- `PetRepository`: `findByIdAndOwnerId`, `findAllByOwnerIdOrderByName`, `findAllByOwnerIsNull`
- `VaccineRepository`: `findAllByTargetSpeciesOrTargetSpeciesIsNullOrderByName`
- `VaccinationRepository`: `findAllByPetIdOrderByAdministeredAtDesc`, `findAllByNextDueDateBetween`
- `AdoptionListingRepository`: `findAllByStatusIn`, `findByPetId`
- `AdoptionApplicationRepository`: `findAllByListingIdOrderByCreatedAt`, `findAllByListingIdAndStatus`
- `OtpCodeRepository`: `findAllByUserIdAndUsedAtIsNull`, `findTopByDestinationOrderByCreatedAtDesc`, `countByDestinationAndCreatedAtAfter`
- `SmsOutboxRepository`: `findTop50ByOrderByCreatedAtDesc`

**4. `db/seed/V1000__demo_data.sql`** (sadece dev/docker):
- 2 VET (biri e-postalı, biri hem e-posta hem telefonlu)
- 6 OWNER: 2'si sadece e-postalı, 2'si sadece telefonlu, 2'si ikisi de; hepsi **normalize** (`+90532…`, küçük harf), `kvkk_consent_at` dolu
- 15 hayvan: 10'u sahipli (kedi, köpek, kuş, tavşan karışık; bir yavru: 3 aylık), 5'i sahipsiz
- Aşı kataloğu: Kuduz (365), Karma – köpek DHPPi (365), Lyme (365), Bronşin (365), Kedi karma FVRCP (365), FeLV (365), Leptospiroz (365)
- Aşı geçmişi: her sahipli hayvana 2–4 kayıt; en az 1 **gecikmiş** doz, 1 **önümüzdeki 7 gün içinde**, 1 **30 gün içinde**. Tarihleri `CURRENT_DATE - INTERVAL '…'` ile yaz ki demo her gün gerçekçi kalsın.
- 5 ilan (sahipsiz hayvanlar için): 3 ACTIVE, 1 RESERVED, 1 DRAFT
- 4 başvuru: 3 PENDING, 1 REJECTED

**5. Test altyapısı:** `src/test/.../AbstractIntegrationTest` (`@SpringBootTest`, `@Testcontainers`, `PostgreSQLContainer` + `@ServiceConnection`, profil `test`, seed yüklenmez). `application-test.yml`.

**6. Docker:** çok aşamalı `Dockerfile` (Maven build → JRE 21 imajı); `docker-compose.yml`'e `app` servisi (profil `docker`, `depends_on` db için healthcheck ile); `application-docker.yml` (host'lar `db` ve `mailpit`; **`spring.flyway.locations: classpath:db/migration,classpath:db/seed`** — bu satır yoksa Docker'da açılan uygulama boş gelir ve demo çalışmaz).

**Testler:** `PetRepositoryTest` (`findByIdAndOwnerId` başka sahibin hayvanında boş döner), `UserRepositoryTest` (unique ihlali), Flyway migration'ı temiz DB'de çalışıyor.

**Kabul:** `docker compose up --build` ile uygulama seed verisiyle açılıyor (`/dev/login` olmadığı için kontrolü DB'den yap: `docker compose exec db psql -U vetclinic -c "select count(*) from pets"` → 15); `ddl-auto=validate` hata vermiyor; ER diyagramıyla birebir aynı.

---

# FAZ 3 – Giriş ve veteriner paneli

## C3 – Bildirim katmanı + OTP girişi (Ceylin)
**Branch:** `feature/c3-notification-otp-login`

**Bildirim (`notification`) – Strategy + Factory**
1. `Channel` enum (EMAIL, SMS).
2. `NotificationSender` arayüzü: `Channel channel()`, `void send(String destination, String subject, String body)`.
3. `EmailSender`: `JavaMailSender`, gönderen `VetClinic <noreply@vetclinic.local>`, düz metin + basit HTML.
4. `MockSmsSender`: `sms_outbox` tablosuna yazar, logda numarayı **maskeli** gösterir.
5. `NotificationSenderFactory`: Spring'in enjekte ettiği `List<NotificationSender>`'dan `Map<Channel, NotificationSender>` kurar; `get(Channel)`. Yeni kanal = yeni sınıf, başka değişiklik yok.
6. `NotificationService`: `notifyUser(user, subject, body)` (e-posta varsa e-posta, yoksa SMS); `notifyContact(normalizedContact, subject, body)` (türünü `ContactNormalizer` ile bulur).
7. `/vet/sms-outbox` sayfası (VET): son 50 SMS (demo'da telefonla girişte kodu göstermek için).

**OTP (`auth`)**
8. `OtpCodeGenerator`: `SecureRandom`, 000000–999999, 6 haneye sıfırla tamamlanmış.
9. `OtpService.requestCode(rawContact)`:
   - normalize et (geçersizse form hatası: "Geçerli bir e-posta ya da cep telefonu girin")
   - kullanıcıyı bul (aktif değilse ya da yoksa **sessizce hiçbir şey yapma**)
   - kullanıcının kullanılmamış eski kodlarını geçersiz kıl
   - kodu üret, **BCrypt** (`PasswordEncoder`) ile hash'le, `expires_at = now + 5 dk` (Clock), kaydet
   - kanal: kullanıcı e-posta yazdıysa EMAIL, telefon yazdıysa SMS
   - mesaj: "VetClinic doğrulama kodunuz: 123456. Kod 5 dakika geçerlidir. Bu kodu kimseyle paylaşmayın."
10. `OtpService.verifyCode(rawContact, code)`:
    - destination'a ait en son, kullanılmamış, süresi dolmamış kodu bul
    - yanlışsa `attempt_count++`; 5'e ulaşınca kodu geçersiz kıl ("Çok fazla hatalı deneme, yeni kod isteyin")
    - doğruysa `used_at` doldur, `User` döndür
    - tüm hata durumlarında kullanıcıya genel mesaj: "Kod hatalı ya da süresi dolmuş"
    - **Dikkat:** metot `@Transactional`. Yanlış kodda exception fırlatılırsa `attempt_count++` da geri alınır ve 5 deneme limiti hiç çalışmaz. Bu yüzden `verifyCode` exception fırlatmaz, `VerifyResult` döndürür (`SUCCESS(user)`, `INVALID`, `LOCKED`); controller mesajı buna göre seçer.
11. `LoginController`:
    - `GET /login` → `login.html` (tek kutu: "E-posta veya cep telefonu")
    - `POST /login` → `requestCode`, iletişim bilgisini session'a koy, `/login/verify`'a yönlendir; mesaj her durumda aynı: "Bilgileriniz kayıtlıysa doğrulama kodu gönderildi."
    - `GET /login/verify` → `verify.html` (6 haneli kutu, maskeli hedef: "c*****@gmail.com adresine gönderildi")
    - `POST /login/verify` → doğruysa `SessionLoginService.loginAs` → VET: `/vet`, OWNER: `/my/pets`
    - `POST /login/resend` → yeni kod (limit C5'te eklenecek)
12. Navbar'daki "Giriş" artık `/login`'e gider; `/dev/login` sadece dev'de kalır.

**Testler:**
- `OtpServiceTest` (sabit Clock): doğru kod → kullanıcı döner; 5 dk + 1 sn → reddedilir; aynı kod ikinci kez → reddedilir; 5 yanlış → doğru kod da artık reddedilir; yeni kod isteyince eskisi geçersiz; kayıtsız kullanıcı → sender hiç çağrılmaz; DB'de kodun düz hali yok
- `NotificationSenderFactoryTest`: iki kanal da doğru sınıfı döndürür
- `MockSmsSenderTest`: `sms_outbox`'a satır yazılır
- `LoginFlowIT` (MockMvc + Testcontainers): kod isteme → DB'deki kodu test içinde bilinen bir kodla değiştir → doğrula → `/my/pets` 200
- `OtpAttemptIT` (Testcontainers, **mock değil gerçek DB**): 5 yanlış denemeden sonra DB'de `attempt_count = 5` ve doğru kod reddediliyor (rollback hatasını ancak bu test yakalar)

**Kabul:** e-postalı bir OWNER kodu Mailpit'te görüp giriyor; telefonlu bir OWNER kodu `/vet/sms-outbox`'ta (VET hesabıyla) görüp giriyor; VET `/vet`'e, OWNER `/my/pets`'e düşüyor; kayıtsız adreste aynı mesaj çıkıyor ve hiçbir şey gönderilmiyor.

## Y3 – Veteriner paneli: sahip ve hayvan yönetimi (Yusuf)
**Branch:** `feature/y3-vet-panel-owners-pets`

1. `GET /vet` – panel: kart olarak toplam sahip, toplam hayvan, 7 gün içinde aşısı gelen hayvan sayısı, bekleyen başvuru sayısı (şimdilik 0 olabilir), son eklenen 5 hayvan.
2. **Sahipler** (`user` paketi):
   - `GET /vet/owners` – liste + arama (ad, e-posta, telefon; telefon aramasında da `ContactNormalizer`), sayfalama (20'şer)
   - `GET/POST /vet/owners/new` – form: ad soyad, e-posta, telefon, KVKK onayı alındı (checkbox, zorunlu). Kurallar: en az biri dolu; `ContactNormalizer` ile normalize; e-posta/telefon başka kullanıcıda varsa alan hatası ("Bu telefon başka bir kayıtta kullanılıyor")
   - `GET /vet/owners/{id}` – detay: iletişim bilgileri, hayvanları, "Hayvan ekle" butonu (sahibi önceden seçili)
   - `GET/POST /vet/owners/{id}/edit` – düzenleme (aynı kurallar)
   - Pasifleştirme: `active=false` (silme yok; pasif kullanıcı giriş yapamaz)
3. **Hayvanlar** (`pet` paketi):
   - `GET /vet/pets` – liste, filtre: tür, sahipli/sahipsiz, arama (ad, mikroçip)
   - `GET/POST /vet/pets/new` – form: ad, tür, cins, cinsiyet, doğum tarihi (gelecek olamaz), renk, mikroçip (opsiyonel, unique), kısır mı, fotoğraf URL'si (opsiyonel), sahip (seçim kutusu ya da "Sahipsiz")
   - `GET /vet/pets/{id}` – detay: bilgiler, yaş (`AgeFormatter`), sahibi, aşı listesi (Y4'te dolacak; şimdilik boş alan), "İlana çıkar" butonu (sadece sahipsizse; Y5'te bağlanacak)
   - `GET/POST /vet/pets/{id}/edit`
4. Form nesneleri: `OwnerForm`, `PetForm` (Bean Validation); `OwnerService`, `PetService` (iş kuralları serviste).
5. Başarılı işlemlerde flash mesajı ("Kayıt eklendi").

**Testler:** `OwnerServiceTest` (normalize edilmiş kayıt, tekrar eden telefon hatası), `PetServiceTest` (gelecekteki doğum tarihi hatası), `OwnerControllerTest` (OWNER rolü `/vet/owners` → 403), `PetControllerIT` (VET hayvan ekler, listede görünür).

**Kabul:** veteriner yeni bir sahip ve hayvan ekleyebiliyor; `0532 123 45 67` girilen telefon DB'de `+905321234567` görünüyor; eklenen sahip hemen kodla giriş yapabiliyor.

---

# FAZ 4 – Sahip portalı ve aşılar

## C4 – Sahip portalı ve aşı kartı (Ceylin)
**Branch:** `feature/c4-owner-portal`

1. `GET /my/pets` – sahibin hayvanları kart olarak: fotoğraf (yoksa türe göre varsayılan ikon), ad, tür/cins, yaş (`AgeFormatter`), en yakın doz rozeti (OVERDUE kırmızı "Gecikti", DUE_SOON sarı "Yaklaşıyor", OK yeşil). Hayvan yoksa: "Kayıtlı hayvanınız yok, kliniğimizle iletişime geçin."
2. `pet.service.PetProfileFacade` (**Facade**): pet + aşı kayıtları + durum hesabı → tek `PetProfileDto`. İçeride `PetRepository.findByIdAndOwnerId` (yoksa `NotFoundException` → 404), `VaccinationRepository`, `VaccinationStatusCalculator`, `AgeFormatter` kullanır.
3. Durum kuralı: her aşı türü için **sadece en son kayıt** değerlendirilir (eski dozlar "tamamlandı" sayılır).
4. `GET /my/pets/{id}` – detay:
   - Üstte kimlik kartı: ad, tür, cins, cinsiyet, doğum tarihi + yaş, renk, mikroçip, kısırlık
   - "Yaklaşan ve geciken dozlar" kutusu (en üstte, renkli)
   - **Aşı kartı zaman çizelgesi:** yeniden eskiye; her satırda aşı adı, uygulama tarihi, sonraki doz, uygulayan veteriner, lot no; durum rozeti
   - "Yazdır" butonu (`@media print` CSS ile sade görünüm)
5. `GET /my/profile` – kendi iletişim bilgileri (salt okunur) + "Bilgileriniz yanlışsa kliniği arayın".

**Testler:** `PetProfileFacadeTest` (en son kayıt kuralı, OVERDUE/DUE_SOON hesabı, başka sahibin hayvanı → NotFound), `OwnerPortalControllerIT` (OWNER kendi hayvanını 200, olmayan ID 404).

**Kabul:** seed'deki bir sahip giriş yapınca hayvanlarını görüyor; gecikmiş aşı kırmızı, 7 gün içindeki sarı görünüyor; 3 aylık yavru "3 aylık" yazıyor; yazdırma görünümü düzgün.

## Y4 – Aşı kataloğu ve aşı kaydı (Yusuf)
**Branch:** `feature/y4-vaccinations`

1. `GET /vet/vaccines` – katalog listesi; `GET/POST /vet/vaccines/new` ve `/{id}/edit`: ad (unique), hedef tür (ya da hepsi), tekrar aralığı (gün, boş = tek doz), açıklama.
2. Veteriner hayvan detayında (`/vet/pets/{id}`) **aşı listesi** (zaman çizelgesi, durum rozetleriyle) ve **"Aşı ekle"** formu:
   - aşı seçimi **hayvanın türüne göre filtreli**
   - uygulama tarihi (varsayılan bugün, gelecek olamaz)
   - sonraki doz: `uygulama + repeat_interval_days` ile **otomatik dolar** (küçük bir JS ya da sunucuda hesap), elle değiştirilebilir, uygulama tarihinden önce olamaz; tek dozluk aşıda boş
   - lot no, notlar
   - `vet_id` = `CurrentUserService.requireUserId()`
3. Aşı kaydı düzenleme ve silme (silme onay sayfasıyla; yanlış girişler için).
4. `GET /vet/vaccinations/upcoming` – gecikmiş + 30 gün içinde dozu gelen hayvanlar: hayvan, sahip, telefon/e-posta, aşı, tarih; tarihe göre sıralı, gecikenler üstte.
5. `/vet` panelindeki "7 gün içinde aşısı gelen" kartını gerçek veriye bağla.
6. `VaccinationService`: tüm kurallar burada.

**Testler:** `VaccinationServiceTest` (otomatik sonraki doz hesabı, tek dozlu aşı, geçersiz tarih, türe uymayan aşı reddedilir), `VaccinationControllerIT` (VET aşı ekler → sahibin portalında görünür).

**Kabul:** veteriner bir köpeğe Kuduz eklediğinde sonraki doz 1 yıl sonrası olarak geliyor; kedi formunda köpek aşıları çıkmıyor; eklenen aşı sahibin portalında anında görünüyor.

**Faz 4 sonu – ortak ara demo:** ikiniz birlikte baştan sona deneyin: veteriner kodla giriş → sahip ekle → hayvan ekle → aşı ekle → çıkış → sahip telefonla giriş → aşı kartı. Bulunan hataları issue olarak açın.

---

# FAZ 5 – Güvenlik ve sahiplendirme

## C5 – Güvenlik sertleştirme (Ceylin)
**Branch:** `feature/c5-security-hardening`

1. **Tekrar gönderme limitleri** (`OtpRateLimiter`, `otp_codes` üzerinden, Clock ile):
   - aynı hedefe son koddan sonra **60 sn** geçmeden yeni kod yok → "Yeni kod için 42 saniye bekleyin"
   - aynı hedefe **saatte en fazla 5** kod → "Çok fazla kod istendi, 1 saat sonra tekrar deneyin"
   - limit aşıldığında da kayıtlı/kayıtsız ayrımı sızdırılmaz (kayıtsız adreste de aynı bekleme mesajı gösterilir; bunun için son istek zamanını session'da tutmak yeterli)
2. `verify.html`'de "Kodu tekrar gönder" butonu: 60 sn geri sayım (küçük JS), süre dolunca aktif.
3. **Loglarda maskeleme:** tüm `auth` ve `notification` loglarını gözden geçir; `LogMasker` kullan; kodun kendisi hiçbir yerde loglanmaz. `EmailSender`/`MockSmsSender` logları maskeli.
4. Sahip portalındaki tüm controller'ların `findByIdAndOwnerId` kullandığını doğrula; eksik varsa düzelt.
5. `application.yml`: `server.servlet.session.timeout=30m`, cookie `http-only`, `same-site=lax`.
6. Pasif kullanıcı (`active=false`): kod istenemez, mevcut session'ı da bir sonraki istekte düşer (`CurrentUserService` kontrol eder).

**Güvenlik testleri (`SecurityIT`):**
- OWNER A, OWNER B'nin hayvanı `/my/pets/{id}` → **404**
- OWNER → `/vet` → **403**; anonim → `/my/pets` → `/login`'e yönlendirme
- CSRF token'sız `POST /login/verify` → 403
- Girişten sonra session ID değişiyor (session fixation)
- 60 sn içinde ikinci kod isteği reddediliyor; saatte 6. istek reddediliyor
- Log çıktısında (OutputCaptureExtension) kod ve tam telefon geçmiyor

**Kabul:** tüm güvenlik testleri yeşil; elle denendiğinde bekleme mesajı ve geri sayım çalışıyor.

## Y5 – Sahiplendirme: ilanlar, State deseni, herkese açık sayfa, başvuru (Yusuf)
**Branch:** `feature/y5-adoption-listings`

**0. İlk commit (ayrı küçük PR):** `adoption.event.PetAdoptedEvent` ve `ApplicationRejectedEvent` record'ları (`CLAUDE.md`'deki imzayla). Ceylin C6'yı bunlarla yazacak.

**State deseni (`adoption.domain.state`)**
1. `ListingState` arayüzü: `publish()`, `reserve()`, `release()` (RESERVED→ACTIVE), `complete()` (→ADOPTED), `close()`, `reopen()` (CLOSED→DRAFT), `boolean acceptsApplications()`.
2. Sınıflar: `DraftState`, `ActiveState`, `ReservedState`, `AdoptedState`, `ClosedState`. İzin verilmeyen geçiş → `IllegalStateTransitionException` ("Sahiplenilmiş bir ilan tekrar yayına alınamaz").
3. `AdoptionListing.state()` → `status`'a göre doğru state nesnesi; geçişler sadece bu nesneler üzerinden.

**Veteriner tarafı**
4. `GET /vet/adoptions` – tüm ilanlar, duruma göre filtre, her satırda bekleyen başvuru sayısı.
5. `GET/POST /vet/adoptions/new?petId=` – **sadece sahipsiz ve ilanı olmayan** hayvan için; başlık, açıklama; DRAFT olarak oluşur. Hayvan detayındaki "İlana çıkar" butonunu buraya bağla.
6. `GET /vet/adoptions/{id}` – ilan detayı + durum butonları (sadece geçerli geçişler görünür) + başvuru listesi (Y6'da işlem butonları eklenecek).
7. Düzenleme (`/edit`): ADOPTED/CLOSED ilanlar düzenlenemez.

**Herkese açık taraf**
8. `GET /adoptions` – ACTIVE ve RESERVED ilanlar kart olarak (RESERVED'de "Görüşme sürecinde" rozeti); filtre: tür, cinsiyet, yaş aralığı (yavru <1, genç 1–3, yetişkin 3–8, yaşlı 8+); boşsa güzel bir boş durum mesajı.
9. `GET /adoptions/{id}` – fotoğraf, ad, tür, cins, yaş, cinsiyet, kısırlık, açıklama. DRAFT/CLOSED → 404. ADOPTED → "Bu dostumuz yuvasını buldu 🎉".
10. Başvuru formu (sadece `acceptsApplications()` true ise): ad soyad, e-posta, telefon (en az biri, `ContactNormalizer`), mesaj, KVKK onayı (zorunlu, `/kvkk` linki). `POST /adoptions/{id}/apply` → PENDING kayıt → "Başvurunuz alındı" sayfası.
11. Aynı iletişim bilgisiyle aynı ilana ikinci PENDING başvuru engellenir.
12. Ana sayfaya "Son ilanlar" bölümü (3 ACTIVE ilan).

**Testler:** `ListingStateTest` (**tüm geçiş matrisi**: 5 durum × 6 işlem, geçerliler geçer, geçersizler exception), `AdoptionServiceTest` (sahipli hayvan ilana çıkamaz, RESERVED başvuru almaz, tekrar başvuru engeli), `PublicAdoptionControllerIT` (anonim liste 200, DRAFT ilan 404, başvuru kaydı).

**Kabul:** giriş yapmamış bir ziyaretçi ilanları filtreleyip başvuru yapabiliyor; veteriner panelinde başvuru görünüyor; geçersiz durum geçişi butonu hiç görünmüyor, URL ile zorlanırsa hata mesajı çıkıyor.

---

# FAZ 6 – Olaylar, hatırlatmalar, sahiplik aktarımı

## C6 – Aşı hatırlatma ve event dinleyicileri (Ceylin)
**Branch:** `feature/c6-reminders-listeners`

**Aşı hatırlatma (Observer)**
1. `@EnableScheduling`; `vaccination.service.VaccineReminderJob`: `@Scheduled(cron = "0 0 9 * * *", zone = "Europe/Istanbul")`; `vetclinic.reminders.enabled` özelliğiyle açılıp kapanır (test profilinde kapalı).
2. Seçim kuralı: `next_due_date` bugün ile bugün+7 arasında, `reminder_sent_at IS NULL`, hayvanın sahibi var ve aktif, ve kayıt o hayvan+aşı için **en son kayıt**.
3. Her biri için `VaccineDueEvent(vaccinationId)` yayınla (job bildirim göndermez, sadece olayı duyurur).
4. `notification.listener.VaccineReminderListener` (`@EventListener`): `NotificationService.notifyUser` ile "Pamuk'un Kuduz aşısının zamanı yaklaşıyor: 05.11.2026. Randevu için: …" gönderir, sonra `reminder_sent_at`'i doldurur. Hata olursa logla, diğer hatırlatmalara devam et.
5. Demo için `POST /vet/reminders/run` (VET) → job'u hemen çalıştırır, "3 hatırlatma gönderildi" mesajı.

**Sahiplendirme olay dinleyicileri**
6. `notification.listener.AdoptionNotificationListener` (`@TransactionalEventListener(phase = AFTER_COMMIT)` **+ `@Transactional(propagation = Propagation.REQUIRES_NEW)`**; SMS gönderimi `sms_outbox`'a yazdığı için commit sonrası yeni transaction gerekir, yoksa yazma kaybolur ya da uygulama açılışta hata verir):
   - `PetAdoptedEvent` → yeni sahibe hoş geldin mesajı: "Tebrikler! Pamuk artık sizin. Aşı kartını görmek için: http://…/login". `newOwnerCreated` true ise ilk girişin nasıl yapılacağını da anlat.
   - `ApplicationRejectedEvent` → başvuru sahibine nazik ret mesajı (`notifyContact`).
7. Mesaj metinlerini tek bir yerde topla: `notification.MessageTemplates` (zaman kalırsa **Template Method**: ortak selamlama + imza, değişen gövde).

**Testler:** `VaccineReminderJobTest` (sabit Clock: 7 gün içindeki seçilir, 8. gün seçilmez, gönderilmiş olan tekrar seçilmez, eski kayıt seçilmez, sahipsiz hayvan seçilmez), `VaccineReminderListenerTest`, `AdoptionNotificationListenerIT` (event yayınlanınca Mailpit yerine mock sender'a mesaj gider; transaction rollback olursa gitmez).

**Kabul:** `/vet/reminders/run` sonrası Mailpit'te ve SMS kutusunda hatırlatmalar görünüyor; ikinci çalıştırmada aynı hatırlatma tekrar gitmiyor.

## Y6 – Başvuru onayı ve sahiplik aktarımı (Yusuf)
**Branch:** `feature/y6-adoption-approval`

1. `/vet/adoptions/{id}` başvuru listesine butonlar: **"Görüşmeye al"** (ilan ACTIVE → RESERVED), **"Onayla"**, **"Reddet"**; RESERVED'de **"Görüşme olumsuz, yayına geri al"** (RESERVED → ACTIVE).
2. `AdoptionService.approve(applicationId)` – **tek transaction**:
   1. başvuru PENDING mi, ilan ACTIVE ya da RESERVED mi kontrol et
   2. başvurunun e-posta/telefonu ile `users`'ta OWNER ara (önce e-posta, sonra telefon); yoksa yeni OWNER oluştur (ad başvurudan, `kvkk_consent_at = now`) → `newOwnerCreated = true`
      - **Çakışma kuralı:** var olan kullanıcı bulunduysa onun iletişim bilgileri **değiştirilmez** (başvurudaki diğer bilgi eklenmez). Yeni kullanıcı oluşturulurken başvurudaki e-posta ya da telefon başka bir kayıtta varsa o alan boş bırakılır (ör. e-posta A'da, telefon B'de olabilir; bu durumda zaten A bulunur, B'ye dokunulmaz).
   3. bulunan kullanıcı VET ise hata ("Bu iletişim bilgisi personel hesabına ait")
   4. `pet.owner = yeni sahip`
   5. ilan `state().complete()` → ADOPTED
   6. başvuru APPROVED, `decided_at = now`
   7. aynı ilandaki diğer PENDING başvurular REJECTED
   8. `PetAdoptedEvent` ve her ret için `ApplicationRejectedEvent` yayınla
3. `AdoptionService.reject(applicationId)` → REJECTED + `ApplicationRejectedEvent`.
4. Onay sonrası veterineri yeni sahibin detay sayfasına yönlendir: "Pamuk, Ayşe Yılmaz'a aktarıldı."
5. `/vet` panelindeki "bekleyen başvuru" kartını gerçek veriye bağla.
6. Sahiplenilen hayvanın aşı geçmişi korunur (yeni sahip portalında görür).

**Testler:** `AdoptionServiceTest` (var olan sahibe aktarım, yeni sahip oluşturma, diğer başvuruların reddi, ADOPTED ilanda onay hatası, VET iletişim bilgisi hatası, event'lerin yayınlandığı, **e-postası bir kullanıcıda telefonu başka kullanıcıda olan başvuru → unique hatası olmadan e-postadaki kullanıcıya aktarılır**), `AdoptionApprovalIT` (onay → yeni sahip kodla girer → hayvanı ve aşı geçmişini görür).

**Kabul:** ziyaretçi başvurur → veteriner onaylar → başvurana hoş geldin mesajı gelir → kodla girer → hayvan "Hayvanlarım"da; diğer başvuranlara ret mesajı gider; ilan herkese açık sayfada "yuvasını buldu" görünür.

---

# FAZ 7 – Teslime hazırlık

## C7 – Testler, README, demo senaryosu, rapor bölümleri (Ceylin)
**Branch:** `feature/c7-tests-docs`

1. **JaCoCo** ekle (`./mvnw verify` raporu üretsin); `auth`, `notification`, `pet`, `vaccination` servislerinde eksik testleri tamamla (hedef: servis katmanında %70+).
2. `/dev/login`'in sadece `dev` profilinde olduğunu, `docker` profilinde **olmadığını** test et.
3. `README.md`:
   - Proje tanımı ve ekran görüntüleri (ana sayfa, giriş, aşı kartı, sahiplendirme, panel)
   - Kurulum: tek komut `docker compose up --build`, adresler (8080, 8025)
   - Demo hesapları (seed'deki e-posta ve telefonlar) ve kodun nereden görüleceği (Mailpit / `/vet/sms-outbox`)
   - Mimari: paket yapısı, katmanlar, diyagram linkleri
   - **Tasarım desenleri tablosu:** desen → sınıf → dosya yolu → neden
   - Testleri çalıştırma
4. `docs/demo-script.md`: sunumda adım adım tıklanacaklar (aşağıdaki sunum akışı) + yedek plan (canlı demo bozulursa video).
5. Rapor bölümleri (`docs/report/` altında ya da ortak rapor dosyasında): kimlik doğrulama tasarımı, bildirim katmanı (Strategy/Factory), güvenlik önlemleri (OTP, IDOR, CSRF, session fixation, KVKK), hatırlatma (Observer), Facade.
6. C1 diyagramlarını son koda göre güncelle (özellikle sequence).

## Y7 – Docker son hali, arayüz cilası, rapor bölümleri, slaytlar (Yusuf)
**Branch:** `feature/y7-polish-docker-slides`

1. **Temiz klonla test:** başka bir klasöre klonla, `docker compose down -v`, `docker compose up --build` → her şey seed'le açılıyor mu?
2. Seed verisini gözden geçir: gerçekçi isimler, fotoğraflar (telifsiz, örn. Unsplash linkleri ya da `static/images/` altına), demo senaryosuna uygun aşı tarihleri.
3. Arayüz cilası: tüm sayfalar mobilde (375px) kontrol; tutarlı başlıklar, butonlar, boş durum mesajları; favicon ve logo.
4. Y1 diyagramlarını son koda göre güncelle (ER, class, state, deployment).
5. Rapor bölümleri: veritabanı tasarımı ve normalizasyon, sahiplendirme modülü, State deseni, Docker/dağıtım, Git iş akışı (PR geçmişinden ekran görüntüsü).
6. Sunum slaytları taslağı (12–15 slayt): problem → aktörler/use case → mimari → ER → giriş akışı (sequence) → desenler (Strategy, State, Observer, Facade) → güvenlik → canlı demo → test/CI → öğrendiklerimiz.

## Ortak bitiş (ikiniz birlikte)
- [ ] Demo videosu kaydı (3–4 dk, yedek olarak)
- [ ] Rapor birleştirme ve son okuma
- [ ] Sunum provası: her biriniz kendi modüllerini **ve** karşı tarafın en az bir modülünü anlatabilmeli
- [ ] `git tag v1.0 && git push --tags`

---

## Sunumdaki demo akışı (≈7 dk)
1. `docker compose up` ile her şey ayakta (önceden başlatılmış olsun).
2. **Ziyaretçi:** ana sayfa → sahiplendirme → filtre → bir ilana başvuru.
3. **Veteriner (e-postayla giriş):** Mailpit'te kod → panel → başvuruyu onayla.
4. **Yeni sahip (telefonla giriş):** `/vet/sms-outbox`'ta kod (ya da başvurdaki e-postayla Mailpit) → "Hayvanlarım" → aşı kartı.
5. **Güvenlik:** URL'deki hayvan ID'sini değiştir → 404. 5 yanlış kod → kod iptal.
6. **Hatırlatma:** `/vet/reminders/run` → Mailpit'te hatırlatma.
7. **Tasarım:** class diyagramında Strategy → "Gerçek SMS için sadece `NetgsmSmsSender` sınıfı eklenir."

## Kim neyi anlatır
| Ceylin | Yusuf |
|---|---|
| Gereksinimler, use case, sequence | ER, class, state, deployment |
| OTP girişi ve güvenlik önlemleri | Veritabanı tasarımı |
| Strategy + Factory (bildirim) | State (sahiplendirme) |
| Observer (hatırlatma, sahiplendirme olayları) | Sahiplik aktarımı (transaction) |
| Facade (aşı kartı) | Docker ve CI/Git akışı |
