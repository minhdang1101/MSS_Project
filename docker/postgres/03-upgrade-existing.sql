-- Brings a database created from an OLDER 01-schema.sql / 02-seed.sql up to the current entities WITHOUT deleting rows.
-- Hibernate's ddl-auto=update cannot do this by itself: it will not add a NOT NULL column to a table that already
-- has rows ("column ... contains null values"), so the fine and fine_policy tables were left without their new columns.
--
-- Safe to run any number of times, and a no-op on a database created from the current 01-schema.sql.
-- Run with psql (uses \connect):  psql -U <user> -f 03-upgrade-existing.sql
--
-- What changed since the first schema:
--   fine_policy  (no change; lost_book_fee was removed again)
--   fine         + book_id, due_at, returned_at, policy_id, version; status is UNPAID or PAID
--   closed_day   dropped (not part of the design)
--   loan         + due_soon_notified_at
--   book         + category, language; new table book_cover (cover pictures)
--   member       + password_hash (reader login)
--   notification + read_at (reader marks a notification as read)
--                + source_event_id, attempts, last_error, created_at, updated_at (sending and retries)

-- ===================== catalog_db =====================
\connect catalog_db

ALTER TABLE book ADD COLUMN IF NOT EXISTS category varchar(100);
ALTER TABLE book ADD COLUMN IF NOT EXISTS language varchar(50);
-- Books that existed before the column are in Vietnamese unless the librarian says otherwise
UPDATE book SET language = 'Tiếng Việt' WHERE language IS NULL;
-- Genres of the demo books from 02-seed.sql (only rows still without one)
UPDATE book SET category = CASE isbn
    WHEN '978-604-3-55012-4' THEN 'Khoa học'
    WHEN '978-604-9-40881-5' THEN 'Công nghệ'
    ELSE 'Văn học Việt Nam' END
 WHERE category IS NULL AND isbn IN ('978-604-1-08765-2', '978-604-2-11203-7', '978-604-9-77310-1',
                                     '978-604-3-55012-4', '978-604-1-90024-8', '978-604-9-40881-5', '978-604-7-66190-3');

CREATE TABLE IF NOT EXISTS book_cover (
    book_id      bigint PRIMARY KEY REFERENCES book (id),
    content_type varchar(30) NOT NULL CHECK (content_type IN ('image/jpeg', 'image/png')),
    data         bytea       NOT NULL,
    updated_at   timestamp(6) with time zone NOT NULL
);

-- ===================== member_db =====================
\connect member_db

-- Passwords, sign-in and the forgot-password e-mails moved to Keycloak: member keeps only the reader's profile.
-- Librarian accounts now live in Keycloak too (TT-0001 is created by the realm file), so their rows (and the old
-- librarian table) go. Run this ONLY after Keycloak is up with the "library" realm: once the password columns are
-- dropped, the old member-service (main branch) can no longer let anybody sign in.
DO $
BEGIN
    IF to_regclass('public.librarian') IS NOT NULL THEN
        DROP TABLE librarian;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'member' AND column_name = 'role') THEN
        DELETE FROM member WHERE role = 'LIBRARIAN';
    END IF;
END $;
ALTER TABLE member DROP COLUMN IF EXISTS password_hash;
ALTER TABLE member DROP COLUMN IF EXISTS role;
ALTER TABLE member DROP COLUMN IF EXISTS reset_code_hash;
ALTER TABLE member DROP COLUMN IF EXISTS reset_code_expires_at;
ALTER TABLE member DROP COLUMN IF EXISTS reset_code_attempts;
ALTER TABLE member DROP COLUMN IF EXISTS reset_code_sent_at;

-- ===================== loan_db =====================
\connect loan_db

ALTER TABLE loan ADD COLUMN IF NOT EXISTS due_soon_notified_at timestamp(6) with time zone;

-- ===================== fine_db =====================
\connect fine_db

-- fine_policy: lost_book_fee is not part of the fine rules any more
ALTER TABLE fine_policy DROP COLUMN IF EXISTS lost_book_fee;

-- fine: add the columns nullable first, fill the existing rows, then make them required
ALTER TABLE fine ADD COLUMN IF NOT EXISTS book_id     bigint;
ALTER TABLE fine ADD COLUMN IF NOT EXISTS due_at      timestamp(6) with time zone;
ALTER TABLE fine ADD COLUMN IF NOT EXISTS returned_at timestamp(6) with time zone;
ALTER TABLE fine ADD COLUMN IF NOT EXISTS policy_id   bigint;
ALTER TABLE fine ADD COLUMN IF NOT EXISTS version     bigint;

-- A fine is only created when a loan is returned late, so it was calculated at the return moment, and the loan was
-- due overdue_days before that.
UPDATE fine SET returned_at = calculated_at WHERE returned_at IS NULL AND status <> 'ACCRUING';
-- ACCRUING no longer exists: a fine whose book is still out is UNPAID with no returned_at
UPDATE fine SET status = 'UNPAID' WHERE status = 'ACCRUING';
UPDATE fine SET due_at = calculated_at - overdue_days * interval '1 day' WHERE due_at IS NULL;
-- The rules in force: the active policy (else the newest one)
UPDATE fine SET policy_id = (SELECT id FROM fine_policy ORDER BY active DESC, effective_from DESC, id LIMIT 1)
 WHERE policy_id IS NULL;
-- book_id lives in loan_db, so SQL here cannot look it up. Only the demo rows from 02-seed.sql are known
-- (loan 7 -> book 7, loan 8 -> book 1). Any other row stays NULL and the SET NOT NULL below fails loudly
-- instead of guessing: fix those rows by hand, then run this file again.
UPDATE fine SET book_id = CASE loan_id WHEN 7 THEN 7 WHEN 8 THEN 1 END
 WHERE book_id IS NULL AND loan_id IN (7, 8);
UPDATE fine SET version = 0 WHERE version IS NULL;

ALTER TABLE fine ALTER COLUMN book_id   SET NOT NULL;
ALTER TABLE fine ALTER COLUMN due_at    SET NOT NULL;
ALTER TABLE fine ALTER COLUMN policy_id SET NOT NULL;

-- Only UNPAID and PAID remain (WAIVED rows, if any, make this fail loudly: settle them by hand first)
ALTER TABLE fine DROP CONSTRAINT IF EXISTS fine_status_check;
ALTER TABLE fine ADD CONSTRAINT fine_status_check CHECK (status IN ('UNPAID', 'PAID'));

DROP TABLE IF EXISTS closed_day;

-- ===================== notification_db =====================
\connect notification_db

ALTER TABLE notification ADD COLUMN IF NOT EXISTS read_at timestamp(6) with time zone;
ALTER TABLE notification ADD COLUMN IF NOT EXISTS source_event_id varchar(36);
ALTER TABLE notification ADD COLUMN IF NOT EXISTS attempts integer NOT NULL DEFAULT 0;
ALTER TABLE notification ADD COLUMN IF NOT EXISTS last_error varchar(500);
ALTER TABLE notification ADD COLUMN IF NOT EXISTS created_at timestamp(6) with time zone NOT NULL DEFAULT now();
ALTER TABLE notification ADD COLUMN IF NOT EXISTS updated_at timestamp(6) with time zone NOT NULL DEFAULT now();
CREATE INDEX IF NOT EXISTS idx_notification_member_scheduled ON notification (member_id, scheduled_at DESC);
