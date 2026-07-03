-- RF06: Registro Clínico e Prontuário
-- Prontuário 1:1 com paciente; entrada de evolução 1:1 com agendamento.

CREATE TABLE prontuario (
    id UUID PRIMARY KEY,
    paciente_id UUID NOT NULL UNIQUE REFERENCES paciente(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE entrada_prontuario (
    id UUID PRIMARY KEY,
    prontuario_id UUID NOT NULL REFERENCES prontuario(id),
    agendamento_id UUID NOT NULL UNIQUE REFERENCES agendamento(id),
    profissional_id UUID NOT NULL REFERENCES profissional(id),
    evolucao TEXT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_entrada_prontuario_prontuario ON entrada_prontuario(prontuario_id);
CREATE INDEX idx_entrada_prontuario_profissional ON entrada_prontuario(profissional_id);
