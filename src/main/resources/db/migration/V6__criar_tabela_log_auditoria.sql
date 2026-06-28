CREATE TABLE log_auditoria (
    id UUID PRIMARY KEY,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usuario_id UUID NOT NULL REFERENCES usuario(id),
    acao VARCHAR(255) NOT NULL,
    agendamento_id UUID REFERENCES agendamento(id),
    ip_origem VARCHAR(50) NOT NULL
);

CREATE INDEX idx_log_auditoria_usuario ON log_auditoria(usuario_id);
CREATE INDEX idx_log_auditoria_agendamento ON log_auditoria(agendamento_id);
