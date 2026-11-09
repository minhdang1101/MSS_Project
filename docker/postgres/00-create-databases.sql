-- Database-per-service: one database per microservice.
-- Safe to re-run: a database is only created when it does not exist yet (needs psql for \gexec).
SELECT format('CREATE DATABASE %I', db)
FROM unnest(ARRAY['catalog_db', 'member_db', 'loan_db', 'fine_db', 'notification_db']) AS db
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = db)
\gexec
