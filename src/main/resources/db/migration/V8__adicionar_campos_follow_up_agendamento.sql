-- Migration: V8__adicionar_campos_follow_up_agendamento.sql
-- Description: Adiciona campos para controlar o follow-up proativo de confirmações:
--   follow_up_sent_at: timestamp em que a notificação de follow-up foi enviada
--   follow_up_required: flag para sinalização manual da recepção quando o cancelamento automático está desabilitado

ALTER TABLE agendamento ADD COLUMN follow_up_sent_at TIMESTAMP;
ALTER TABLE agendamento ADD COLUMN follow_up_required BOOLEAN DEFAULT false NOT NULL;
