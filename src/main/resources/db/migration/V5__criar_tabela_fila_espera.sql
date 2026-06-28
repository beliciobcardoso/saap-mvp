CREATE TABLE fila_espera (
    id UUID PRIMARY KEY,
    paciente_id UUID NOT NULL REFERENCES paciente(id),
    profissional_id UUID NOT NULL REFERENCES profissional(id),
    servico_id UUID NOT NULL REFERENCES servico(id),
    status VARCHAR(50) NOT NULL,
    offered_appointment_time TIMESTAMP,
    offer_expires_at TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_fila_espera_profissional_servico_ativo
ON fila_espera (profissional_id, servico_id)
WHERE is_active = TRUE;
