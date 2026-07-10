-- 🚨 DEVELOPMENT ONLY — NEVER RUN THIS IN PRODUCTION 🚨
-- This script creates a test ADMIN user for local development.
-- Run this manually after creating the database, if needed:
--   psql -U postgres -d saap_db -f docs/dev-setup/admin-user-setup.sql
-- Then immediately CHANGE the password in production before deploying.

INSERT INTO usuario (id, nome, email, cpf, senha, is_active, role, created_at, updated_at)
VALUES (
  gen_random_uuid(),
  'Admin Teste',
  'admin@test.saap.local',
  '00000000000',
  '[bcrypt hash here — generate fresh for your dev env]',
  true,
  'ADMIN',
  now(),
  now()
)
ON CONFLICT DO NOTHING;
