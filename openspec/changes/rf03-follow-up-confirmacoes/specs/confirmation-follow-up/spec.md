## ADDED Requirements

### Requirement: Worker de Varredura para Envio de Notificações de Confirmação
O sistema SHALL executar um método `@Scheduled` a cada hora que identifica todos os agendamentos com status `PENDING` cuja data de início está dentro da janela de confirmação configurada (`clinic.settings.confirmation-window-hours`, padrão 48h). Para cada agendamento elegível que ainda não recebeu notificação (`followUpSentAt IS NULL`), o sistema SHALL: (1) gerar um token de ação JWT com expiração igual ao prazo de resposta; (2) disparar uma notificação simulada via `ConsoleNotificationService` com os links de confirmação e cancelamento; (3) transicionar o status do agendamento para `PENDING_RESPONSE`; (4) gravar o timestamp de envio em `followUpSentAt`.

#### Scenario: Envio bem-sucedido para agendamentos elegíveis
- **WHEN** o worker executa e encontra agendamentos com status `PENDING`, data de início dentro da janela de confirmação e `followUpSentAt IS NULL`
- **THEN** o sistema SHALL gerar tokens JWT de confirmação e cancelamento, disparar notificação simulada, atualizar o status para `PENDING_RESPONSE` e gravar `followUpSentAt = now()`

#### Scenario: Ignorar agendamentos já notificados
- **WHEN** o worker encontra agendamentos com status `PENDING_RESPONSE` (notificação já enviada) dentro da janela de confirmação
- **THEN** o sistema SHALL ignorar esses registros e não gerar novos tokens ou notificações

#### Scenario: Ignorar agendamentos fora da janela de confirmação
- **WHEN** o worker encontra agendamentos `PENDING` com data de início além da janela de confirmação
- **THEN** o sistema SHALL ignorar esses registros nessa execução

---

### Requirement: Worker de Processamento de Deadlines de Resposta
O sistema SHALL executar um método `@Scheduled` a cada hora que identifica todos os agendamentos com status `PENDING_RESPONSE` cuja data de início é menor ou igual ao prazo limite de resposta configurado (`clinic.settings.follow-up-deadline-hours`, padrão 24h). Se `clinic.settings.auto-cancel-after-no-response = true`, o sistema SHALL cancelar o agendamento automaticamente (status → `CANCELLED`). Se `false`, o sistema SHALL marcar `followUpRequired = true` para sinalização manual pela recepção.

#### Scenario: Cancelamento automático habilitado — prazo expirado sem resposta
- **WHEN** `autoCancelAfterNoResponse = true` e o agendamento está em `PENDING_RESPONSE` com data de início ≤ agora + deadlineHours
- **THEN** o sistema SHALL transicionar o status do agendamento para `CANCELLED` e gravar o motivo de cancelamento como `NO_RESPONSE_FOLLOW_UP`

#### Scenario: Cancelamento automático desabilitado — prazo expirado sem resposta
- **WHEN** `autoCancelAfterNoResponse = false` e o agendamento está em `PENDING_RESPONSE` com data de início ≤ agora + deadlineHours
- **THEN** o sistema SHALL marcar `followUpRequired = true` no agendamento sem alterar o status, para processamento manual pela recepção

---

### Requirement: Transição de Estado PENDING → PENDING_RESPONSE
O sistema SHALL suportar a transição de estado `PENDING` → `PENDING_RESPONSE` na máquina de estados do agendamento, representando que o paciente foi notificado e aguarda confirmação.

#### Scenario: Transição válida ao enviar notificação de follow-up
- **WHEN** o worker de follow-up processa um agendamento elegível com status `PENDING`
- **THEN** o sistema SHALL alterar o status para `PENDING_RESPONSE` e persistir a mudança

#### Scenario: Tentativa de transição inválida a partir de estado não-PENDING
- **WHEN** é solicitada a transição `PENDING_RESPONSE` a partir de um agendamento com status diferente de `PENDING`
- **THEN** o sistema SHALL lançar uma exceção de transição inválida e abortar a operação sem alterar o registro
