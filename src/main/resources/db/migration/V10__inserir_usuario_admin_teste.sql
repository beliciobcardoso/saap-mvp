-- Migration V10: Inserir usuário admin de teste para IA fazer testes da aplicação
-- Email: john.nobody@email.com
-- Senha: SenhaForte123!
-- Role: ADMIN

INSERT INTO usuario (
  id,
  email,
  senha,
  nome,
  role,
  ativo,
  data_criacao,
  data_atualizacao
) VALUES (
  '550e8400-e29b-41d4-a716-446655440000'::uuid,
  'john.nobody@email.com',
  '$2b$12$OA4lISRwboc6gpvmaynEseeThIj00jjVOuQBatMob2IvYtDu/9icW',
  'John Admin',
  'ADMIN',
  true,
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP
) ON CONFLICT (email) DO NOTHING;
