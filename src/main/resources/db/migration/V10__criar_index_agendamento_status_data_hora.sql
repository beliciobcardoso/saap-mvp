-- Index para otimizar queries do scheduler de follow-up
CREATE INDEX IF NOT EXISTS idx_agendamento_status_data_hora ON agendamento(status, data_hora);
