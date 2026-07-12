## 1. Migração de Banco de Dados

- [ ] 1.1 Criar `V8__adicionar_campos_follow_up_agendamento.sql` com os campos `follow_up_sent_at TIMESTAMP` e `follow_up_required BOOLEAN DEFAULT FALSE` na tabela `agendamento`
- [ ] 1.2 Verificar sequência Flyway (`V7` → `V8`) e validar com `./mvnw flyway:validate`

## 2. Domínio — Enum e Entidade

- [ ] 2.1 Adicionar `PENDING_RESPONSE` ao enum `AppointmentStatus`
- [ ] 2.2 Adicionar campos `followUpSentAt` (`LocalDateTime`, nullable) e `followUpRequired` (`boolean`, default `false`) à classe de domínio `Appointment`
- [ ] 2.3 Atualizar `AppointmentEntity` (JPA) com os novos campos mapeados às colunas do banco
- [ ] 2.4 Atualizar os mappers `AppointmentMapper` (domínio ↔ JPA entity)
- [ ] 2.5 Atualizar a máquina de estados (validação de transições) para aceitar `PENDING` → `PENDING_RESPONSE`, `PENDING_RESPONSE` → `CONFIRMED` e `PENDING_RESPONSE` → `CANCELLED`

## 3. Configurações

- [ ] 3.1 Adicionar as propriedades `clinic.settings.confirmation-window-hours` (default 48), `clinic.settings.follow-up-deadline-hours` (default 24) e `clinic.settings.auto-cancel-after-no-response` (default `true`) em `application.yaml`
- [ ] 3.2 Criar ou atualizar a classe `ClinicSettings` com `@ConfigurationProperties(prefix = "clinic.settings")` para injetar essas configurações

## 4. Use Cases de Domínio

- [ ] 4.1 Criar `TriggerFollowUpUseCase` — porta de entrada (interface) com método `execute()` que busca agendamentos elegíveis, gera tokens, dispara notificação e persiste a mudança de status para `PENDING_RESPONSE`
- [ ] 4.2 Criar `ProcessMissedDeadlinesUseCase` — porta de entrada com método `execute()` que busca agendamentos `PENDING_RESPONSE` com prazo expirado e os cancela ou marca `followUpRequired`
- [ ] 4.3 Criar `ConfirmAppointmentByTokenUseCase` — valida o token JWT, verifica o estado `PENDING_RESPONSE`, transiciona para `CONFIRMED`
- [ ] 4.4 Criar `CancelAppointmentByTokenUseCase` — valida o token JWT, verifica o estado `PENDING_RESPONSE`, transiciona para `CANCELLED`

## 5. Repositório

- [ ] 5.1 Adicionar método `findEligibleForFollowUp(LocalDateTime windowStart, LocalDateTime windowEnd)` à interface de porta de saída `AppointmentRepository` (busca por `PENDING`, `followUpSentAt IS NULL` dentro da janela)
- [ ] 5.2 Adicionar método `findPendingResponsePastDeadline(LocalDateTime deadline)` (busca por `PENDING_RESPONSE` com `followUpSentAt` antes do deadline)
- [ ] 5.3 Implementar os métodos na classe de adaptador JPA `JpaAppointmentRepository`

## 6. Scheduler (Infraestrutura)

- [ ] 6.1 Criar `ConfirmationFollowUpScheduler` com `@Component` e habilitar `@EnableScheduling` na aplicação
- [ ] 6.2 Implementar método `sendFollowUpNotifications()` com `@Scheduled(cron = "0 0 * * * *")` que invoca `TriggerFollowUpUseCase`
- [ ] 6.3 Implementar método `processMissedDeadlines()` com `@Scheduled(cron = "0 30 * * * *")` que invoca `ProcessMissedDeadlinesUseCase`

## 7. Controller — Endpoints Públicos

- [ ] 7.1 Adicionar endpoint `GET /api/v1/appointments/public/confirm?token=...` sem `@PreAuthorize` que invoca `ConfirmAppointmentByTokenUseCase`
- [ ] 7.2 Adicionar endpoint `GET /api/v1/appointments/public/cancel?token=...` sem `@PreAuthorize` que invoca `CancelAppointmentByTokenUseCase`
- [ ] 7.3 Configurar o `SecurityFilterChain` para permitir as rotas `/api/v1/appointments/public/**` sem autenticação

## 8. Testes

- [ ] 8.1 Testes unitários de `TriggerFollowUpUseCase` (mock do repositório e notificação; verifica transição de estado, token gerado e `followUpSentAt` preenchido)
- [ ] 8.2 Testes unitários de `ProcessMissedDeadlinesUseCase` (cenários `autoCancelAfterNoResponse = true` e `false`)
- [ ] 8.3 Testes unitários de `ConfirmAppointmentByTokenUseCase` e `CancelAppointmentByTokenUseCase` (token válido, token inválido/expirado, estado inválido)
- [ ] 8.4 Testes de integração dos endpoints públicos via `MockMvc` (200 OK na confirmação, 200 OK no cancelamento, 400 com token inválido, 409 com estado inválido)
- [ ] 8.5 Rodar `./mvnw clean test` e garantir `BUILD SUCCESS`

## 9. Documentação

- [ ] 9.1 Atualizar `docs/REST Client/Appointments.http` com as requisições de teste para os endpoints públicos de confirmação e cancelamento (usando token obtido via console log)
- [ ] 9.2 Atualizar `docs/REST Client/FullTest.http` com a etapa de follow-up no fluxo completo
- [ ] 9.3 Atualizar `docs/PRD-SAAP.md` para marcar RF03 (UC14) como **Implementado**
