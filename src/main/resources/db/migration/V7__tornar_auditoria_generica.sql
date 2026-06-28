ALTER TABLE log_auditoria ADD COLUMN recurso_id UUID;
ALTER TABLE log_auditoria ADD COLUMN recurso_tipo VARCHAR(100);

-- Preencher dados para registros existentes (retrocompatibilidade)
UPDATE log_auditoria 
SET recurso_id = agendamento_id, recurso_tipo = 'APPOINTMENT' 
WHERE agendamento_id IS NOT NULL;
