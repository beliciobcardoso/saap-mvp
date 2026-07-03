# scheduling-engine Delta Specification

## MODIFIED Requirements

### Requirement: Máquina de Estados e Transições do Agendamento
O sistema SHALL forçar a máquina de estados definida para o ciclo de vida do agendamento, validando cada transição e rejeitando qualquer alteração ilegal com uma exceção de negócio específica (`IllegalStateException`). A transição `IN_PROGRESS` → `COMPLETED` SHALL exigir, como pré-condição, a existência de uma entrada de evolução clínica preenchida para o agendamento.

#### Scenario: Transição válida de confirmação
- **WHEN** o status do agendamento é `PENDING` e é solicitada a confirmação da consulta
- **THEN** o sistema SHALL alterar o status para `CONFIRMED` e persistir a mudança no banco de dados

#### Scenario: Transição inválida de início de consulta direta
- **WHEN** o status do agendamento é `PENDING` e é solicitado o início direto da consulta (`IN_PROGRESS`)
- **THEN** o sistema SHALL lançar uma exceção de transição inválida e abortar a operação sem alterar o registro

#### Scenario: Finalização com evolução preenchida
- **WHEN** o status do agendamento é `IN_PROGRESS`, existe entrada de evolução clínica preenchida para o agendamento e é solicitada a finalização da consulta
- **THEN** o sistema SHALL alterar o status para `COMPLETED` e persistir a mudança, tornando a evolução imutável

#### Scenario: Tentativa de finalização sem evolução
- **WHEN** o status do agendamento é `IN_PROGRESS`, não existe entrada de evolução clínica para o agendamento e é solicitada a finalização da consulta
- **THEN** o sistema SHALL rejeitar a operação, manter o status `IN_PROGRESS` e retornar HTTP 409 (Conflict)
