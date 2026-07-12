-- Usuário admin de teste (senha: adminPass123)
INSERT INTO "usuario" (id, email, password, role, is_active, created_at, updated_at)
VALUES (
  '550e8400-e29b-41d4-a716-446655440001',
  'admin@saap.com',
  '$2a$10$QLewVA97Pq/ymBSvsIGyC.Yc3bFIqanQQrGE6MvpRTJ1hnYPN3NyS',
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
  '$2a$10$sabC9OA2oW6F.n2.AyrU7.jlDN2mzMcX2q7LELZO1wMIQV.riziRq',
  'RECEPTIONIST',
  true,
  NOW(),
  NOW()
) ON CONFLICT DO NOTHING;

-- Paciente de teste
INSERT INTO "paciente" (id, name, cpf, phone, birth_date, email, is_active, created_at, updated_at)
VALUES (
  '550e8400-e29b-41d4-a716-446655440010',
  'João Silva',
  '12345678901',
  '11999999999',
  '1990-01-01',
  'patient@example.com',
  true,
  NOW(),
  NOW()
) ON CONFLICT DO NOTHING;

-- Profissional de teste
INSERT INTO "profissional" (id, name, email, phone, registration_number, role, is_active, created_at, updated_at)
VALUES (
  '550e8400-e29b-41d4-a716-446655440020',
  'Dr. Pedro',
  'dr.pedro@saap.com',
  '11988888888',
  'CRM-12345',
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
