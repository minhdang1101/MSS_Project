-- Demo data shared across services. IDs are fixed so cross-service references line up
-- (loan.member_id -> member.id, loan.book_id/copy_id -> book.id/book_copy.id, fine.loan_id -> loan.id).
-- Dates are relative to now() so loans are always "due soon" or "overdue" whenever you seed.
-- Safe to re-run: existing rows are left untouched (ON CONFLICT DO NOTHING).
-- Run with psql (uses \connect):  psql -U <user> -f 02-seed.sql

-- ===================== member_db =====================
\connect member_db

INSERT INTO member (id, library_card_no, full_name, email, phone, status, joined_at, created_at, updated_at) VALUES
  (1, 'TV-0412', 'Phạm Minh Khôi', 'khoi.pham@example.com', '0901000412', 'SUSPENDED', '2024-09-05', now(), now()),
  (2, 'TV-0087', 'Lê Hoài An',     'an.le@example.com',     '0901000087', 'ACTIVE',    '2023-02-14', now(), now()),
  (3, 'TV-1130', 'Đỗ Bảo Ngọc',    'ngoc.do@example.com',   '0901001130', 'ACTIVE',    '2025-09-01', now(), now()),
  (4, 'TV-0256', 'Trần Quốc Việt', 'viet.tran@example.com', '0901000256', 'ACTIVE',    '2023-08-20', now(), now()),
  (5, 'TV-0901', 'Vũ Thanh Hà',    'ha.vu@example.com',     '0901000901', 'ACTIVE',    '2024-03-11', now(), now())
ON CONFLICT DO NOTHING;
SELECT setval(pg_get_serial_sequence('member', 'id'), (SELECT max(id) FROM member));

-- The first librarian (role LIBRARIAN; the staff code is stored as the card number). Dev password: Admin@12345
-- Dev only: this password is public in git, so never use this seed on a real installation.
INSERT INTO member (library_card_no, full_name, email, status, joined_at, role, password_hash, created_at, updated_at) VALUES
  ('TT-0001', 'Thủ thư mặc định', 'tt-0001@library.local', 'ACTIVE', CURRENT_DATE, 'LIBRARIAN',
   '$2a$10$D03cLeNmbDzLZ0I.qJqCCOcmt2YoXCd0xT1jXF03m3y3vZCEN0IoW', now(), now())
ON CONFLICT (library_card_no) DO NOTHING;

-- ===================== catalog_db =====================
\connect catalog_db

-- available_copies = total_copies minus the open loans seeded in loan_db below
INSERT INTO book (id, isbn, title, author, publisher, publication_year, category, language, format, total_copies, available_copies, status, version, created_at, updated_at) VALUES
  (1, '9786041087652', 'Số đỏ',                  'Vũ Trọng Phụng',  'NXB Văn học',  2016, 'Văn học Việt Nam', 'Tiếng Việt', 'PHYSICAL', 4,  3,  'AVAILABLE',   0, now(), now()),
  (2, '9786042112037', 'Dế mèn phiêu lưu ký',    'Tô Hoài',         'NXB Kim Đồng', 2019, 'Văn học Việt Nam', 'Tiếng Việt', 'PHYSICAL', 6,  4,  'AVAILABLE',   0, now(), now()),
  (3, '9786049773101', 'Nỗi buồn chiến tranh',   'Bảo Ninh',        'NXB Trẻ',      2018, 'Văn học Việt Nam', 'Tiếng Việt', 'DIGITAL',  10, 9,  'AVAILABLE',   0, now(), now()),
  (4, '9786043550124', 'Lược sử thời gian',      'Stephen Hawking', 'NXB Trẻ',      2020, 'Khoa học', 'Tiếng Việt', 'PHYSICAL', 3,  2,  'AVAILABLE',   0, now(), now()),
  (5, '9786041900248', 'Truyện Kiều',            'Nguyễn Du',       'NXB Văn học',  2015, 'Văn học Việt Nam', 'Tiếng Việt', 'PHYSICAL', 8,  7,  'AVAILABLE',   0, now(), now()),
  (6, '9786049408815', 'Cơ sở dữ liệu phân tán', 'Lê Văn Sơn',      'NXB Giáo dục', 2021, 'Công nghệ', 'Tiếng Việt', 'DIGITAL',  25, 25, 'AVAILABLE',   0, now(), now()),
  (7, '9786047661903', 'Tắt đèn',                'Ngô Tất Tố',      'NXB Văn học',  2017, 'Văn học Việt Nam', 'Tiếng Việt', 'PHYSICAL', 5,  5,  'AVAILABLE',   0, now(), now())
ON CONFLICT DO NOTHING;
SELECT setval(pg_get_serial_sequence('book', 'id'), (SELECT max(id) FROM book));

-- Physical copies only; digital books are licences counted in total_copies
INSERT INTO book_copy (id, book_id, barcode, shelf_location, status) VALUES
  (1,  1, 'BC-0001-01', 'A1-03', 'AVAILABLE'),
  (2,  1, 'BC-0001-02', 'A1-03', 'ON_LOAN'),
  (3,  1, 'BC-0001-03', 'A1-03', 'AVAILABLE'),
  (4,  1, 'BC-0001-04', 'A1-03', 'AVAILABLE'),
  (5,  2, 'BC-0002-01', 'B2-01', 'ON_LOAN'),
  (6,  2, 'BC-0002-02', 'B2-01', 'AVAILABLE'),
  (7,  2, 'BC-0002-03', 'B2-01', 'AVAILABLE'),
  (8,  2, 'BC-0002-04', 'B2-01', 'AVAILABLE'),
  (9,  2, 'BC-0002-05', 'B2-01', 'ON_LOAN'),
  (10, 2, 'BC-0002-06', 'B2-01', 'AVAILABLE'),
  (11, 4, 'BC-0004-01', 'C3-07', 'ON_LOAN'),
  (12, 4, 'BC-0004-02', 'C3-07', 'AVAILABLE'),
  (13, 4, 'BC-0004-03', 'C3-07', 'AVAILABLE'),
  (14, 5, 'BC-0005-01', 'A1-01', 'AVAILABLE'),
  (15, 5, 'BC-0005-02', 'A1-01', 'AVAILABLE'),
  (16, 5, 'BC-0005-03', 'A1-01', 'AVAILABLE'),
  (17, 5, 'BC-0005-04', 'A1-01', 'AVAILABLE'),
  (18, 5, 'BC-0005-05', 'A1-01', 'AVAILABLE'),
  (19, 5, 'BC-0005-06', 'A1-01', 'ON_LOAN'),
  (20, 5, 'BC-0005-07', 'A1-01', 'AVAILABLE'),
  (21, 5, 'BC-0005-08', 'A1-01', 'AVAILABLE'),
  (22, 7, 'BC-0007-01', 'A2-05', 'AVAILABLE'),
  (23, 7, 'BC-0007-02', 'A2-05', 'AVAILABLE'),
  (24, 7, 'BC-0007-03', 'A2-05', 'AVAILABLE'),
  (25, 7, 'BC-0007-04', 'A2-05', 'AVAILABLE'),
  (26, 7, 'BC-0007-05', 'A2-05', 'AVAILABLE')
ON CONFLICT DO NOTHING;
SELECT setval(pg_get_serial_sequence('book_copy', 'id'), (SELECT max(id) FROM book_copy));

-- ===================== loan_db =====================
\connect loan_db

INSERT INTO loan (id, idempotency_key, member_id, book_id, copy_id, loan_type, borrowed_at, due_at, returned_at, status, version, created_at, updated_at) VALUES
  -- open loans: 3 overdue, 3 due within 3 days
  (1, 'seed-loan-1', 1, 1, 2,    'PHYSICAL', now() - interval '28 days', now() - interval '14 days', NULL, 'OVERDUE',  0, now(), now()),
  (2, 'seed-loan-2', 2, 2, 9,    'PHYSICAL', now() - interval '21 days', now() - interval '7 days',  NULL, 'OVERDUE',  0, now(), now()),
  (3, 'seed-loan-3', 3, 3, NULL, 'DIGITAL',  now() - interval '12 days', now() + interval '2 days',  NULL, 'BORROWED', 0, now(), now()),
  (4, 'seed-loan-4', 4, 4, 11,   'PHYSICAL', now() - interval '13 days', now() + interval '1 day',   NULL, 'BORROWED', 0, now(), now()),
  (5, 'seed-loan-5', 5, 5, 19,   'PHYSICAL', now() - interval '11 days', now() + interval '3 days',  NULL, 'BORROWED', 0, now(), now()),
  (6, 'seed-loan-6', 1, 2, 5,    'PHYSICAL', now() - interval '57 days', now() - interval '43 days', NULL, 'OVERDUE',  0, now(), now()),
  -- returned late: these produced the fines seeded in fine_db
  (7, 'seed-loan-7', 2, 7, 22,   'PHYSICAL', now() - interval '40 days', now() - interval '26 days', now() - interval '22 days', 'RETURNED', 0, now(), now()),
  (8, 'seed-loan-8', 5, 1, 1,    'PHYSICAL', now() - interval '30 days', now() - interval '16 days', now() - interval '14 days', 'RETURNED', 0, now(), now())
ON CONFLICT DO NOTHING;
SELECT setval(pg_get_serial_sequence('loan', 'id'), (SELECT max(id) FROM loan));

-- ===================== fine_db =====================
\connect fine_db

-- 3.000đ/day after 1 grace day, capped at 150.000đ per loan
INSERT INTO fine_policy (id, daily_rate, grace_days, max_amount, active, effective_from) VALUES
  (1, 3000.00, 1, 150000.00, true, '2026-01-01')
ON CONFLICT DO NOTHING;
SELECT setval(pg_get_serial_sequence('fine_policy', 'id'), (SELECT max(id) FROM fine_policy));

-- amount = (overdue_days - grace_days) * daily_rate
-- book_id, due_at and returned_at repeat what loans 7 and 8 hold in loan_db (a fine keeps its own copy)
INSERT INTO fine (id, loan_id, member_id, book_id, due_at, returned_at, policy_id,
                  overdue_days, daily_rate, amount, status, calculated_at, paid_at, version) VALUES
  (1, 7, 2, 7, now() - interval '26 days', now() - interval '22 days', 1,
      4, 3000.00, 9000.00, 'PAID',   now() - interval '22 days', now() - interval '21 days', 0),
  (2, 8, 5, 1, now() - interval '16 days', now() - interval '14 days', 1,
      2, 3000.00, 3000.00, 'UNPAID', now() - interval '14 days', NULL, 0)
ON CONFLICT DO NOTHING;
SELECT setval(pg_get_serial_sequence('fine', 'id'), (SELECT max(id) FROM fine));

-- ===================== notification_db =====================
\connect notification_db

INSERT INTO notification (id, idempotency_key, member_id, loan_id, type, channel, recipient, subject, message, scheduled_at, sent_at, status) VALUES
  (1, 'DUE_SOON:3', 3, 3, 'DUE_SOON', 'EMAIL', 'ngoc.do@example.com',   'Sách sắp đến hạn trả', 'Quyền đọc "Nỗi buồn chiến tranh" sẽ hết hạn sau 2 ngày.',         now() - interval '1 day',   now() - interval '1 day',   'SENT'),
  (2, 'DUE_SOON:4', 4, 4, 'DUE_SOON', 'SMS',   '0901000256',            'Sách sắp đến hạn trả', 'Hạn trả "Lược sử thời gian" là ngày mai.',                         now() - interval '2 hours', now() - interval '2 hours', 'SENT'),
  (3, 'DUE_SOON:5', 5, 5, 'DUE_SOON', 'EMAIL', 'ha.vu@example.com',     'Sách sắp đến hạn trả', 'Hạn trả "Truyện Kiều" còn 3 ngày.',                                now(),                      NULL,                       'PENDING'),
  (4, 'OVERDUE:1',  1, 1, 'OVERDUE',  'EMAIL', 'khoi.pham@example.com', 'Sách đã quá hạn trả',  '"Số đỏ" đã quá hạn. Phạt 3.000đ mỗi ngày sau 1 ngày ân hạn.',      now() - interval '13 days', now() - interval '13 days', 'SENT'),
  (5, 'OVERDUE:2',  2, 2, 'OVERDUE',  'EMAIL', 'an.le@example.com',     'Sách đã quá hạn trả',  '"Dế mèn phiêu lưu ký" đã quá hạn. Phạt 3.000đ mỗi ngày sau 1 ngày ân hạn.', now() - interval '6 days', now() - interval '6 days', 'SENT'),
  (6, 'OVERDUE:6',  1, 6, 'OVERDUE',  'EMAIL', 'khoi.pham@example.com', 'Sách đã quá hạn trả',  '"Dế mèn phiêu lưu ký" đã quá hạn. Phạt 3.000đ mỗi ngày sau 1 ngày ân hạn.', now() - interval '42 days', now() - interval '42 days', 'SENT')
ON CONFLICT DO NOTHING;
SELECT setval(pg_get_serial_sequence('notification', 'id'), (SELECT max(id) FROM notification));
