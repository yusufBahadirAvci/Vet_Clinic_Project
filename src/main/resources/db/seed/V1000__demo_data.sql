-- Demo data, loaded only in the dev and docker profiles (classpath:db/seed).
-- Contacts are already normalized: e-mails in lower case, phones as +905XXXXXXXXX.
-- Y2a: users only. Pets, vaccines, vaccinations, listings and applications follow in V1001+.

INSERT INTO users (full_name, email, phone, role, kvkk_consent_at) VALUES
    -- Veterinarians
    ('Dr. Elif Yılmaz',  'elif.yilmaz@example.com',  NULL,            'VET',   now()),
    ('Dr. Murat Kaya',   'murat.kaya@example.com',   '+905321112233', 'VET',   now()),
    -- Owners: e-mail only
    ('Ayşe Demir',       'ayse.demir@example.com',   NULL,            'OWNER', now()),
    ('Mehmet Çelik',     'mehmet.celik@example.com', NULL,            'OWNER', now()),
    -- Owners: phone only
    ('Zeynep Arslan',    NULL,                       '+905334445566', 'OWNER', now()),
    ('Emre Şahin',       NULL,                       '+905427778899', 'OWNER', now()),
    -- Owners: e-mail and phone
    ('Fatma Öztürk',     'fatma.ozturk@example.com', '+905051234567', 'OWNER', now()),
    ('Burak Aydın',      'burak.aydin@example.com',  '+905469876543', 'OWNER', now());
