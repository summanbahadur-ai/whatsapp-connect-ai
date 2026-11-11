-- Java-owned identity. No Better Auth/Supabase session or credential tables.
CREATE SCHEMA identity;
CREATE SCHEMA app;
CREATE FUNCTION app.current_user_id() RETURNS uuid LANGUAGE sql STABLE
  AS $$ SELECT nullif(current_setting('app.user_id', true), '')::uuid $$;

CREATE TABLE identity.users (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  name text NOT NULL,
  email text NOT NULL CHECK (length(email)>0),
  email_verified boolean NOT NULL DEFAULT false,
  image text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX users_email_case_insensitive ON identity.users(lower(email));

CREATE TABLE identity.password_credentials (
  user_id uuid PRIMARY KEY REFERENCES identity.users(id) ON DELETE CASCADE,
  password_hash text NOT NULL CHECK (length(password_hash)>0),
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE identity.verification_tokens (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL REFERENCES identity.users(id) ON DELETE CASCADE,
  purpose text NOT NULL CHECK (purpose IN ('password_reset','email_verification','email_change')),
  token_hash char(64) NOT NULL UNIQUE,
  payload jsonb NOT NULL DEFAULT '{}',
  expires_at timestamptz NOT NULL,
  consumed_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX verification_tokens_user ON identity.verification_tokens(user_id);
CREATE INDEX verification_tokens_expiry ON identity.verification_tokens(expires_at);

-- Spring Session JDBC's PostgreSQL schema, explicitly namespaced.
CREATE TABLE identity.spring_session (
  primary_id char(36) NOT NULL PRIMARY KEY,
  session_id char(36) NOT NULL,
  creation_time bigint NOT NULL,
  last_access_time bigint NOT NULL,
  max_inactive_interval integer NOT NULL,
  expiry_time bigint NOT NULL,
  principal_name varchar(100)
);
CREATE UNIQUE INDEX spring_session_session_id ON identity.spring_session(session_id);
CREATE INDEX spring_session_expiry ON identity.spring_session(expiry_time);
CREATE INDEX spring_session_principal ON identity.spring_session(principal_name);
CREATE TABLE identity.spring_session_attributes (
  session_primary_id char(36) NOT NULL REFERENCES identity.spring_session(primary_id) ON DELETE CASCADE,
  attribute_name varchar(200) NOT NULL,
  attribute_bytes bytea NOT NULL,
  PRIMARY KEY(session_primary_id,attribute_name)
);
