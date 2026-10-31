-- Demo pets, vaccine catalog, vaccination history, adoption listings and applications.
-- Loaded only in the dev and docker profiles (classpath:db/seed). Depends on the users from V1000.
-- All dates are relative to CURRENT_DATE so the demo stays realistic on any day.
-- Foreign keys are looked up by unique names/contacts instead of hard-coded ids.

-- ---------------------------------------------------------------------------
-- Pets: 10 owned by the V1000 owners, 5 without an owner (for adoption)
-- ---------------------------------------------------------------------------
INSERT INTO pets (owner_id, name, species, breed, gender, birth_date, color, microchip_no, is_neutered)
SELECT u.id, x.name, x.species, x.breed, x.gender,
       (CURRENT_DATE - x.age::interval)::date, x.color, x.microchip_no, x.neutered
FROM (VALUES
    -- owner contact (NULL = no owner), name, species, breed, gender, age, color, microchip, neutered
    ('ayse.demir@example.com',   'Max',      'DOG',    'Golden Retriever',     'MALE',   '4 years 2 months',  'Golden',          '792097000100001', TRUE),
    ('ayse.demir@example.com',   'Luna',     'CAT',    'British Shorthair',    'FEMALE', '2 years 5 months',  'Grey',            '792097000100002', TRUE),
    ('mehmet.celik@example.com', 'Rocky',    'DOG',    'German Shepherd',      'MALE',   '6 years 1 month',   'Black and tan',   '792097000100003', FALSE),
    ('+905334445566',            'Mia',      'CAT',    'Domestic Shorthair',   'FEMALE', '3 months',          'Tabby',           NULL,              FALSE),
    ('+905334445566',            'Kiwi',     'BIRD',   'Budgerigar',           'MALE',   '2 years 3 months',  'Green and yellow', NULL,             FALSE),
    ('+905427778899',            'Bella',    'DOG',    'Beagle',               'FEMALE', '3 years 7 months',  'Tricolor',        '792097000100004', TRUE),
    ('fatma.ozturk@example.com', 'Snowball', 'RABBIT', 'Holland Lop',          'FEMALE', '2 years 1 month',   'White',           NULL,              TRUE),
    ('fatma.ozturk@example.com', 'Oliver',   'CAT',    'Turkish Van',          'MALE',   '5 years 4 months',  'White',           '792097000100005', TRUE),
    ('burak.aydin@example.com',  'Charlie',  'DOG',    'Labrador Retriever',   'MALE',   '1 year 6 months',   'Chocolate',       '792097000100006', FALSE),
    ('burak.aydin@example.com',  'Simba',    'CAT',    'Persian',              'MALE',   '7 years 8 months',  'Cream',           '792097000100007', TRUE),
    (NULL,                       'Buddy',    'DOG',    'Mixed breed',          'MALE',   '2 years',           'Brown',           '792097000100008', TRUE),
    (NULL,                       'Daisy',    'DOG',    'Kangal mix',           'FEMALE', '1 year 2 months',   'Fawn',            '792097000100009', TRUE),
    (NULL,                       'Shadow',   'CAT',    'Domestic Shorthair',   'MALE',   '3 years',           'Black',           '792097000100010', TRUE),
    (NULL,                       'Ginger',   'CAT',    'Domestic Shorthair',   'FEMALE', '8 months',          'Orange',          NULL,              FALSE),
    (NULL,                       'Thumper',  'RABBIT', 'Mixed breed',          'MALE',   '1 year',            'Brown and white', NULL,              FALSE)
) AS x(owner_contact, name, species, breed, gender, age, color, microchip_no, neutered)
LEFT JOIN users u ON u.role = 'OWNER' AND (u.email = x.owner_contact OR u.phone = x.owner_contact);

-- ---------------------------------------------------------------------------
-- Vaccine catalog (target_species NULL = all species)
-- ---------------------------------------------------------------------------
INSERT INTO vaccines (name, target_species, repeat_interval_days, description) VALUES
    ('Rabies',                    NULL,  365, 'Protects against rabies. Required for dogs and cats; given to other species when advised by the vet.'),
    ('Dog Combo (DHPPi)',         'DOG', 365, 'Combined dog vaccine against distemper, hepatitis, parvovirus and parainfluenza.'),
    ('Lyme',                      'DOG', 365, 'Protects dogs against Lyme disease transmitted by ticks.'),
    ('Kennel Cough (Bordetella)', 'DOG', 365, 'Protects dogs against infectious tracheobronchitis (kennel cough).'),
    ('Leptospirosis',             'DOG', 365, 'Protects dogs against leptospirosis, a bacterial infection spread through water and urine.'),
    ('Cat Combo (FVRCP)',         'CAT', 365, 'Combined cat vaccine against feline viral rhinotracheitis, calicivirus and panleukopenia.'),
    ('FeLV',                      'CAT', 365, 'Protects cats against the feline leukemia virus.');

-- ---------------------------------------------------------------------------
-- Vaccination history (owned pets only, 2-4 records each)
--   OVERDUE:            Max / Rabies (-15), Bella / Dog Combo (-25)
--   due within 7 days:  Oliver / Rabies (+2), Mia / Cat Combo (+3), Luna / Cat Combo (+5)
--   due within 30 days: Mia / FeLV (+10), Rocky / Rabies + Dog Combo (+20), Snowball / Rabies (+25)
-- Kiwi (bird) and Snowball (rabbit) have two Rabies doses: the older dose's next_due_date is the
-- date the newer dose was given (booster on time). Only the latest dose per vaccine counts
-- for the status (docs/decisions.md, decision 12).
-- ---------------------------------------------------------------------------
INSERT INTO vaccinations (pet_id, vaccine_id, administered_at, next_due_date, vet_id, lot_number, notes)
SELECT p.id, v.id, CURRENT_DATE - x.given_days_ago, CURRENT_DATE + x.due_in_days, u.id, x.lot_number, x.notes
FROM (VALUES
    -- pet, vaccine, given N days ago, next dose in N days (negative = past), vet e-mail, lot, notes
    ('Max',      'Rabies',                    380, -15, 'murat.kaya@example.com',  'RB-24-1187', 'Owner informed that the booster is due.'),
    ('Max',      'Dog Combo (DHPPi)',         200, 165, 'elif.yilmaz@example.com', 'DH-25-0412', NULL),
    ('Max',      'Leptospirosis',             200, 165, 'elif.yilmaz@example.com', 'LP-25-0219', NULL),
    ('Luna',     'Cat Combo (FVRCP)',         360,   5, 'elif.yilmaz@example.com', 'FV-24-0933', NULL),
    ('Luna',     'Rabies',                    100, 265, 'murat.kaya@example.com',  'RB-25-0371', NULL),
    ('Rocky',    'Rabies',                    345,  20, 'murat.kaya@example.com',  'RB-24-1240', NULL),
    ('Rocky',    'Dog Combo (DHPPi)',         345,  20, 'murat.kaya@example.com',  'DH-24-1102', NULL),
    ('Rocky',    'Lyme',                      150, 215, 'elif.yilmaz@example.com', 'LY-25-0088', 'Lives in a garden with ticks.'),
    ('Rocky',    'Kennel Cough (Bordetella)', 150, 215, 'elif.yilmaz@example.com', 'KC-25-0154', NULL),
    ('Mia',      'Cat Combo (FVRCP)',          25,   3, 'elif.yilmaz@example.com', 'FV-25-0611', 'Kitten series, first dose. Booster in 4 weeks.'),
    ('Mia',      'FeLV',                       25,  10, 'elif.yilmaz@example.com', 'FL-25-0302', 'Kitten series, first dose.'),
    ('Kiwi',     'Rabies',                    665, -300, 'murat.kaya@example.com', 'RB-23-0990', NULL),
    ('Kiwi',     'Rabies',                    300,  65, 'murat.kaya@example.com',  'RB-24-1301', 'Booster given on time.'),
    ('Bella',    'Dog Combo (DHPPi)',         390, -25, 'elif.yilmaz@example.com', 'DH-24-0877', NULL),
    ('Bella',    'Rabies',                    120, 245, 'murat.kaya@example.com',  'RB-25-0405', NULL),
    ('Snowball', 'Rabies',                    705, -340, 'elif.yilmaz@example.com', 'RB-23-0815', NULL),
    ('Snowball', 'Rabies',                    340,  25, 'elif.yilmaz@example.com', 'RB-24-1199', 'Booster given on time.'),
    ('Oliver',   'Cat Combo (FVRCP)',         200, 165, 'murat.kaya@example.com',  'FV-25-0256', NULL),
    ('Oliver',   'FeLV',                      200, 165, 'murat.kaya@example.com',  'FL-25-0117', NULL),
    ('Oliver',   'Rabies',                    363,   2, 'murat.kaya@example.com',  'RB-24-1275', NULL),
    ('Charlie',  'Dog Combo (DHPPi)',          60, 305, 'elif.yilmaz@example.com', 'DH-25-0733', NULL),
    ('Charlie',  'Rabies',                     60, 305, 'elif.yilmaz@example.com', 'RB-25-0698', NULL),
    ('Charlie',  'Kennel Cough (Bordetella)',  60, 305, 'elif.yilmaz@example.com', 'KC-25-0340', 'Goes to a dog daycare.'),
    ('Simba',    'Cat Combo (FVRCP)',         320,  45, 'murat.kaya@example.com',  'FV-24-1018', NULL),
    ('Simba',    'Rabies',                     50, 315, 'murat.kaya@example.com',  'RB-25-0752', NULL)
) AS x(pet_name, vaccine_name, given_days_ago, due_in_days, vet_email, lot_number, notes)
JOIN pets p     ON p.name = x.pet_name AND p.owner_id IS NOT NULL
JOIN vaccines v ON v.name = x.vaccine_name
JOIN users u    ON u.email = x.vet_email AND u.role = 'VET';

-- ---------------------------------------------------------------------------
-- Adoption listings for the 5 pets without an owner: 3 ACTIVE, 1 RESERVED, 1 DRAFT
-- ---------------------------------------------------------------------------
INSERT INTO adoption_listings (pet_id, title, description, status, created_by, created_at, updated_at)
SELECT p.id, x.title, x.description, x.status, u.id,
       now() - x.created_days_ago * INTERVAL '1 day', now() - x.updated_days_ago * INTERVAL '1 day'
FROM (VALUES
    ('Buddy',   'Buddy, a cheerful companion',
     'Buddy is a friendly two-year-old mixed-breed dog. He is neutered, vaccinated, loves long walks and gets along well with children.',
     'ACTIVE',   'murat.kaya@example.com',  21, 21),
    ('Shadow',  'Shadow is looking for a quiet home',
     'Shadow is a calm three-year-old black cat. He is neutered and litter trained, and prefers a quiet home without other pets.',
     'ACTIVE',   'elif.yilmaz@example.com', 14, 14),
    ('Thumper', 'Thumper the curious rabbit',
     'Thumper is a playful one-year-old rabbit. He needs a spacious cage, fresh hay every day and some time to run around.',
     'ACTIVE',   'elif.yilmaz@example.com',  9,  9),
    ('Daisy',   'Daisy, a gentle giant in the making',
     'Daisy is a young Kangal mix. She is neutered, very loyal and needs a home with a garden and an experienced owner.',
     'RESERVED', 'murat.kaya@example.com',  30,  3),
    ('Ginger',  'Ginger (draft)',
     'Ginger is an eight-month-old orange cat. Health check and neutering are planned before the listing is published.',
     'DRAFT',    'elif.yilmaz@example.com',  2,  2)
) AS x(pet_name, title, description, status, vet_email, created_days_ago, updated_days_ago)
JOIN pets p  ON p.name = x.pet_name AND p.owner_id IS NULL
JOIN users u ON u.email = x.vet_email AND u.role = 'VET';

-- ---------------------------------------------------------------------------
-- Adoption applications: 3 PENDING, 1 REJECTED
-- Ayşe Demir applies with the e-mail of her existing owner account, so approving it in Y6
-- demonstrates the "existing owner" path; the others demonstrate "new owner is created".
-- ---------------------------------------------------------------------------
INSERT INTO adoption_applications
    (listing_id, applicant_name, applicant_email, applicant_phone, message, kvkk_consent, status, created_at, decided_at)
SELECT l.id, x.applicant_name, x.applicant_email, x.applicant_phone, x.message, TRUE, x.status,
       now() - x.created_days_ago * INTERVAL '1 day',
       CASE WHEN x.decided_days_ago IS NULL THEN NULL ELSE now() - x.decided_days_ago * INTERVAL '1 day' END
FROM (VALUES
    ('Buddy',  'Ayşe Demir',  'ayse.demir@example.com',  NULL,
     'We already have a dog and a big garden. Max would love a friend to play with.',            'PENDING',  5, NULL::int),
    ('Buddy',  'Kerem Yıldız', 'kerem.yildiz@example.com', NULL,
     'I live in a small studio flat but I would really like to adopt Buddy.',                    'REJECTED', 12, 10),
    ('Shadow', 'Selin Koç',   NULL,                       '+905551112233',
     'I work from home and my flat is very quiet. I think Shadow would be happy here.',          'PENDING',  4, NULL),
    ('Daisy',  'Can Polat',   'can.polat@example.com',    '+905372223344',
     'We have a house with a large garden and I have raised a Kangal before.',                   'PENDING',  6, NULL)
) AS x(pet_name, applicant_name, applicant_email, applicant_phone, message, status, created_days_ago, decided_days_ago)
JOIN pets p              ON p.name = x.pet_name
JOIN adoption_listings l ON l.pet_id = p.id;

-- ---------------------------------------------------------------------------
-- Sanity check: fail the migration if a lookup above silently dropped rows (e.g. a typo).
-- ---------------------------------------------------------------------------
DO $$
BEGIN
    IF (SELECT count(*) FROM pets) <> 15
        OR (SELECT count(*) FROM pets WHERE owner_id IS NOT NULL) <> 10
        OR (SELECT count(*) FROM vaccines) <> 7
        OR (SELECT count(*) FROM vaccinations) <> 25
        OR (SELECT count(*) FROM adoption_listings) <> 5
        OR (SELECT count(*) FROM adoption_applications) <> 4 THEN
        RAISE EXCEPTION 'V1001 demo data is incomplete; check the name/contact lookups';
    END IF;
END $$;
