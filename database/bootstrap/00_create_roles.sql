-- Run as PostgreSQL administrator on the existing server, connected to postgres.
-- Creates only the new project's roles. No passwords or old database changes.
DO $$
BEGIN
  IF NOT EXISTS(SELECT FROM pg_roles WHERE rolname='wc_owner') THEN
    CREATE ROLE wc_owner NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS;
  END IF;
  IF NOT EXISTS(SELECT FROM pg_roles WHERE rolname='wc_auth') THEN
    CREATE ROLE wc_auth NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS;
  END IF;
  IF NOT EXISTS(SELECT FROM pg_roles WHERE rolname='wc_app') THEN
    CREATE ROLE wc_app NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS;
  END IF;
  IF NOT EXISTS(SELECT FROM pg_roles WHERE rolname='wc_worker') THEN
    CREATE ROLE wc_worker NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION BYPASSRLS;
  END IF;
END $$;
-- NOLOGIN is intentional until you set private passwords using the setup guide.
