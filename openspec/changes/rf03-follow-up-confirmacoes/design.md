## Context

O sistema SAAP possui agendamentos com lifecycle controlado (PENDING → CONFIRMED → ARRIVED → ...). Atualmente, os agendamentos ficam indefinidamente em `PENDING` sem qualquer interação proativa com o paciente, criando risco de no-show. O mecanismo de tokens de ação JWT (`AppointmentActionTokenService`) e o `ConsoleNotificationService` já existem — são reutilizados aqui. As configurações de janela de tempo são lidas de `clinic.settings.*` no `application.yaml`.

## Goals / Non-Goals

**Goals:**
- Implementar worker `@Scheduled` que, a cada hora, identifica agendamentos elegíveis e dispara notificações de confirmação.
- Permitir que o paciente confirme ou cancele a consulta via link público (sem autenticação JWT).
- Cancelar automaticamente agendamentos sem resposta quando o prazo expira (se `autoCancelAfterNoResponse = true`).
- Sinalizar `followUpRequired = true` para a recepção quando o cancelamento automático estiver desligado.

**Non-Goals:**
- Integração com provedores reais de e-mail, WhatsApp ou SMS (fica no `ConsoleNotificationService` simulado).
- Interface de front-end para visualização de `followUpRequired`.
- Reenvio de notificações múltiplas (o sistema envia uma única notificação por agendamento elegível).

## Decisions

### Decision: Novo estado `PENDING_RESPONSE` na máquina de estados
**Escolha**: Adicionar o estado `PENDING_RESPONSE` ao enum `AppointmentStatus`.
**Alternativa descartada**: Usar um campo booleano auxiliar `notificationSent` no estado `PENDING`. Descartada porque não captura semanticamente que o paciente está "aguardando responder" e complica as queries de filtragem do scheduler.
**Rationale**: O estado `PENDING_RESPONSE` deixa inequívoco o ciclo de vida e permite consultas simples e precisas (`WHERE status = 'PENDING_RESPONSE'`) no deadline checker.

### Decision: Separação em dois workers (`@Scheduled`)
**Escolha**: Dois métodos `@Scheduled` distintos — `sendFollowUpNotifications()` (a cada hora) e `processMissedDeadlines()` (a cada hora, offset de 30min).
**Alternativa descartada**: Um único scheduler que faz os dois trabalhos. Descartada porque mistura responsabilidades e dificulta o teste unitário isolado.
**Rationale**: Separação de preocupações. Cada worker tem uma responsabilidade única e testável independentemente.

### Decision: Reutilização de `AppointmentActionTokenService`
O serviço de token JWT de ação já possui `generateToken(appointmentId, action, expirationMs)` — será reutilizado para gerar os tokens de confirmação/cancelamento. A expiração do token é calculada como `followUpDeadlineHours * 3600 * 1000`.

### Decision: Campos `followUpSentAt` e `followUpRequired` na entidade `Appointment`
Adicionados via migração Flyway `V8`. Alternativa (tabela de log separada) descartada por complexidade desnecessária — ambos os campos são atributos diretos do agendamento.

### Decision: Configurações via `@ConfigurationProperties`
Os parâmetros (`confirmationWindowHours`, `followUpDeadlineHours`, `autoCancelAfterNoResponse`) são injetados via a classe `ClinicSettings` (ou similar), lida de `clinic.settings.*` do YAML. Evita hardcoding e permite ajuste por ambiente.

## Risks / Trade-offs

- **Relógio do servidor**: O scheduler usa `LocalDateTime.now()` — se o servidor estiver em fuso UTC e o banco em America/Sao_Paulo, pode haver desvio. Mitigação: garantir que `spring.jpa.properties.hibernate.jdbc.time_zone` e o TZ do container estejam alinhados.
- **Duplicação de notificações**: Se o servidor reiniciar durante a janela de confirmação, o scheduler poderá disparar notificações repetidas para o mesmo agendamento. Mitigação: o campo `followUpSentAt` é verificado antes do envio — só envia se `followUpSentAt IS NULL`.
- **Cancelamento abrupto**: Um token de cancelamento gerado pode ser usado pelo paciente *após* o atendimento ter sido iniciado (race condition). Mitigação: o `ProcessConfirmationResponseUseCase` verifica o estado atual antes de transicionar — só cancela se `PENDING_RESPONSE`.

## Migration Plan

1. Criar `V8__adicionar_campos_follow_up_agendamento.sql` com os campos `follow_up_sent_at` e `follow_up_required`.
2. Adicionar `PENDING_RESPONSE` ao enum `AppointmentStatus`.
3. Atualizar `Appointment` com os dois novos campos.
4. Atualizar `AppointmentEntity` com os mapeamentos JPA.
5. Implementar workers e use cases.
6. Expor endpoints públicos (sem `@PreAuthorize`).
7. Rodar `./mvnw clean test`.
