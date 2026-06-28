-- Remove serviços duplicados mantendo apenas um deles (o que foi criado primeiro / ID menor)
DELETE FROM servico a USING servico b
WHERE a.id > b.id 
  AND a.name = b.name 
  AND a.is_active = true 
  AND b.is_active = true;

-- Cria o índice único parcial com segurança para garantir unicidade em serviços ativos futuros
-- IF NOT EXISTS garante idempotência caso o índice já tenha sido criado anteriormente
CREATE UNIQUE INDEX IF NOT EXISTS idx_servico_name_active ON servico(name) WHERE is_active = true;
