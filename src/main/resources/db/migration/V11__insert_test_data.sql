-- Usuário admin de teste (senha: adminPass123)
INSERT INTO "usuario" (id, email, password, role, is_active, created_at, updated_at)
VALUES (
  '550e8400-e29b-41d4-a716-446655440001',
  'admin@saap.com',
  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcg7b3XeKeUxWdeS86E36P4/F0m',
  'ADMIN',
  true,
  NOW(),
  NOW()
) ON CONFLICT DO NOTHING;

-- Usuário recepcionista de teste (senha: password123)
INSERT INTO "usuario" (id, email, password, role, is_active, created_at, updated_at)
VALUES (
  '550e8400-e29b-41d4-a716-446655440002',
  'recep@saap.com',
  '$2a$10$Yw3XN.aGwpMdPpyMPCf1veOqP.rKyp7HflPYJpx2.i8yMCp8K2yWu',
  'RECEPTIONIST',
  true,
  NOW(),
  NOW()
) ON CONFLICT DO NOTHING;

-- Paciente de teste
INSERT INTO "paciente" (id, name, cpf, phone, email, is_active, created_at, updated_at)
VALUES (
  '550e8400-e29b-41d4-a716-446655440010',
  'João Silva',
  '12345678901',
  '11999999999',
  'patient@example.com',
  true,
  NOW(),
  NOW()
) ON CONFLICT DO NOTHING;

-- Profissional de teste
INSERT INTO "profissional" (id, name, role, is_active, created_at, updated_at)
VALUES (
  '550e8400-e29b-41d4-a716-446655440020',
  'Dr. Pedro',
  'PROFESSIONAL',
  true,
  NOW(),
  NOW()
) ON CONFLICT DO NOTHING;

-- Serviço de teste
INSERT INTO "servico" (id, name, description, duration_minutes, price, is_active, created_at, updated_at)
VALUES (
  '550e8400-e29b-41d4-a716-446655440030',
  'Consulta Geral',
  'Consulta clínica geral',
  30,
  150.00,
  true,
  NOW(),
  NOW()
) ON CONFLICT DO NOTHING;
