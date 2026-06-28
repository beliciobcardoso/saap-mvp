## Why

Consultas agendadas e não confirmadas representam um risco direto de ociosidade da agenda clínica. Sem um mecanismo proativo de follow-up, pacientes simplesmente não aparecem (no-show), bloqueando slots que poderiam ser aproveitados por outros pacientes. O sistema já possui a fila de espera (RF04) para preencher vagas canceladas, mas sem cancelamento automático por não-confirmação, o ciclo não fecha.

## What Changes

- **Novo worker agendado** (`@Scheduled`) que executa periodicamente para identificar agendamentos elegíveis para follow-up de confirmação.
- **Lógica de transição automática de status**: agendamentos que entram na janela de confirmação (48h antes) passam a `PENDING_RESPONSE`; aqueles que ultrapassam o prazo limite (24h antes) sem resposta são automaticamente cancelados (`CANCELLED`) se `autoCancelAfterNoResponse = true`.
- **Dois endpoints públicos** (sem autenticação JWT) para receber a resposta do paciente via link:
  - `GET /api/v1/appointments/public/confirm?token=...` → marca como `CONFIRMED`
  - `GET /api/v1/appointments/public/cancel?token=...` → marca como `CANCELLED`
- **Geração de tokens de ação JWT** seguros e com prazo de expiração, codificando o ID do agendamento e a ação permitida.
- **Notificação simulada** via `ConsoleNotificationService` (ponto de extensão para futura integração com e-mail/WhatsApp/SMS real).

## Capabilities

### New Capabilities
- `confirmation-follow-up`: Worker de follow-up de confirmações — varredura periódica, geração de tokens de ação, envio de notificação e transição automática para `CANCELLED` ou marcação de `followUpRequired`.

### Modified Capabilities
- `appointment-follow-up`: Extensão dos requisitos existentes para incluir as regras de cancelamento automático por não-resposta e o campo `followUpRequired` como sinalização visual para a recepção.
- `scheduling-engine`: A máquina de estados do agendamento recebe dois novos estados/transições: `PENDING` → `PENDING_RESPONSE` (quando entra na janela de confirmação) e `PENDING_RESPONSE` → `CANCELLED` (quando o prazo expira sem resposta).

## Impact

- **Novo scheduler**: classe `ConfirmationFollowUpScheduler` no pacote `infrastructure.scheduler`.
- **Novos use cases**: `TriggerFollowUpUseCase`, `ProcessConfirmationResponseUseCase`.
- **Domínio**: Adição dos estados `PENDING_RESPONSE` ao enum `AppointmentStatus`; adição dos campos `followUpRequired` (boolean) e `followUpSentAt` (LocalDateTime) na entidade `Appointment`.
- **Migration Flyway**: `V8__adicionar_campos_follow_up_agendamento.sql` para adicionar `follow_up_required` e `follow_up_sent_at` na tabela `agendamento`.
- **Endpoints públicos**: `AppointmentController` recebe dois novos endpoints GET não autenticados na rota `/public/`.
- **Reutilização**: O mecanismo de token JWT de ação (`AppointmentActionTokenService`) já existe — será reutilizado.
- **Configuração**: Leitura de `clinic.settings.confirmation-window-hours`, `follow-up-deadline-hours`, e `auto-cancel-after-no-response` do `application.yaml`.
