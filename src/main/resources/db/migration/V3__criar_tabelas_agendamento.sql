CREATE TABLE agendamento (
    id UUID PRIMARY KEY,
    paciente_id UUID NOT NULL REFERENCES paciente(id),
    profissional_id UUID NOT NULL REFERENCES profissional(id),
    servico_id UUID NOT NULL REFERENCES servico(id),
    data_hora TIMESTAMP NOT NULL,
    status VARCHAR(50) NOT NULL,
    forma_pagamento VARCHAR(50) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    priority_level VARCHAR(50) NOT NULL,
    priority_score BIGINT,
    priority_declared_at TIMESTAMP,
    priority_verified_by UUID,
    priority_notes VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx_agendamento_prof_data_hora_ativo
ON agendamento (profissional_id, data_hora)
WHERE status NOT IN ('CANCELLED', 'NO_SHOW');

CREATE INDEX idx_agendamento_paciente ON agendamento(paciente_id);
CREATE INDEX idx_agendamento_profissional ON agendamento(profissional_id);
CREATE INDEX idx_agendamento_data_hora ON agendamento(data_hora);
