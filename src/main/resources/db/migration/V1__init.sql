-- VetClinic initial schema. Must match docs/uml/er.puml.
-- Never edit this file after it is merged; add a new V{n}__*.sql instead.

CREATE TABLE users (
    id              BIGSERIAL    PRIMARY KEY,
    full_name       VARCHAR(100) NOT NULL,
    email           VARCHAR(254) NULL,
    phone           VARCHAR(13)  NULL,
    role            VARCHAR(10)  NOT NULL,
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    kvkk_consent_at TIMESTAMPTZ  NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT users_email_uq   UNIQUE (email),
    CONSTRAINT users_phone_uq   UNIQUE (phone),
    CONSTRAINT users_role_chk   CHECK (role IN ('VET', 'OWNER')),
    CONSTRAINT users_contact_chk CHECK (email IS NOT NULL OR phone IS NOT NULL)
);

CREATE TABLE pets (
    id           BIGSERIAL    PRIMARY KEY,
    owner_id     BIGINT       NULL REFERENCES users (id),
    name         VARCHAR(50)  NOT NULL,
    species      VARCHAR(10)  NOT NULL,
    breed        VARCHAR(60)  NULL,
    gender       VARCHAR(10)  NOT NULL,
    birth_date   DATE         NOT NULL,
    color        VARCHAR(40)  NULL,
    microchip_no VARCHAR(20)  NULL,
    is_neutered  BOOLEAN      NOT NULL DEFAULT FALSE,
    photo_url    VARCHAR(500) NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pets_microchip_no_uq UNIQUE (microchip_no),
    CONSTRAINT pets_species_chk CHECK (species IN ('CAT', 'DOG', 'BIRD', 'RABBIT', 'OTHER')),
    CONSTRAINT pets_gender_chk  CHECK (gender IN ('MALE', 'FEMALE', 'UNKNOWN'))
);

CREATE TABLE vaccines (
    id                   BIGSERIAL    PRIMARY KEY,
    name                 VARCHAR(80)  NOT NULL,
    target_species       VARCHAR(10)  NULL,
    repeat_interval_days INT          NULL,
    description          VARCHAR(500) NULL,
    CONSTRAINT vaccines_name_uq UNIQUE (name),
    CONSTRAINT vaccines_target_species_chk
        CHECK (target_species IS NULL OR target_species IN ('CAT', 'DOG', 'BIRD', 'RABBIT', 'OTHER'))
);

CREATE TABLE vaccinations (
    id               BIGSERIAL    PRIMARY KEY,
    pet_id           BIGINT       NOT NULL REFERENCES pets (id),
    vaccine_id       BIGINT       NOT NULL REFERENCES vaccines (id),
    administered_at  DATE         NOT NULL,
    next_due_date    DATE         NULL,
    vet_id           BIGINT       NOT NULL REFERENCES users (id),
    lot_number       VARCHAR(40)  NULL,
    notes            VARCHAR(500) NULL,
    reminder_sent_at TIMESTAMPTZ  NULL,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX vaccinations_next_due_date_idx ON vaccinations (next_due_date);

CREATE TABLE adoption_listings (
    id          BIGSERIAL    PRIMARY KEY,
    pet_id      BIGINT       NOT NULL REFERENCES pets (id),
    title       VARCHAR(100) NOT NULL,
    description TEXT         NOT NULL,
    status      VARCHAR(10)  NOT NULL,
    created_by  BIGINT       NOT NULL REFERENCES users (id),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT adoption_listings_pet_id_uq UNIQUE (pet_id),
    CONSTRAINT adoption_listings_status_chk
        CHECK (status IN ('DRAFT', 'ACTIVE', 'RESERVED', 'ADOPTED', 'CLOSED'))
);

CREATE TABLE adoption_applications (
    id              BIGSERIAL    PRIMARY KEY,
    listing_id      BIGINT       NOT NULL REFERENCES adoption_listings (id),
    applicant_name  VARCHAR(100) NOT NULL,
    applicant_email VARCHAR(254) NULL,
    applicant_phone VARCHAR(13)  NULL,
    message         TEXT         NULL,
    kvkk_consent    BOOLEAN      NOT NULL,
    status          VARCHAR(10)  NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    decided_at      TIMESTAMPTZ  NULL,
    CONSTRAINT adoption_applications_contact_chk
        CHECK (applicant_email IS NOT NULL OR applicant_phone IS NOT NULL),
    CONSTRAINT adoption_applications_status_chk
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
);

CREATE TABLE otp_codes (
    id            BIGSERIAL    PRIMARY KEY,
    user_id       BIGINT       NOT NULL REFERENCES users (id),
    channel       VARCHAR(5)   NOT NULL,
    destination   VARCHAR(254) NOT NULL,
    code_hash     VARCHAR(100) NOT NULL,
    expires_at    TIMESTAMPTZ  NOT NULL,
    attempt_count INT          NOT NULL DEFAULT 0,
    used_at       TIMESTAMPTZ  NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT otp_codes_channel_chk CHECK (channel IN ('EMAIL', 'SMS'))
);

CREATE INDEX otp_codes_destination_created_at_idx ON otp_codes (destination, created_at);

CREATE TABLE sms_outbox (
    id         BIGSERIAL    PRIMARY KEY,
    phone      VARCHAR(13)  NOT NULL,
    message    VARCHAR(500) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
