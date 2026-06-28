-- Migration: V4__adicionar_coluna_follow_up_agendamento.sql
-- Description: Adiciona coluna para controlar se o lembrete de confirmação de agendamento já foi enviado

ALTER TABLE agendamento ADD COLUMN follow_up_sent BOOLEAN DEFAULT false NOT NULL;
