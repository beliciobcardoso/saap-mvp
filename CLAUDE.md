# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

# Preferências do Usuário

## Idioma
- **Sempre interagir em português brasileiro (pt-br)** em toda a interface e respostas.

## Regras de Workflow (Graphify e Contexto)
- **Consulta Obrigatória:** Antes de responder dúvidas sobre código ou modificar o projeto, consulte SEMPRE o graphify (`graphify-out/GRAPH_REPORT.md` ou rodando o `/graphify --update`) para obter o contexto da arquitetura.
- **Atualização Automática:** Sempre que alterar código ou arquivos do projeto, rode o graphify (`/graphify --update`) de forma automatizada para refletir as mudanças.
- **CLAUDE.md Vivo:** Atualize este arquivo (`CLAUDE.md`) em conjunto com as alterações do projeto para manter as regras sempre sincronizadas com a evolução do sistema.

## Especificações (OpenSpec)
- Mudanças de funcionalidade seguem o fluxo OpenSpec em `openspec/` (`changes/` para propostas em andamento, `specs/` para specs consolidadas: `appointment-follow-up`, `core-entities`, `priority-attendance`, `scheduling-engine`, `user-auth`, `waitlist`). Use as skills `opsx:*` / `openspec-*` para propor, continuar, verificar e arquivar mudanças.

# Visão Geral

SAAP MVP — Sistema de Agendamento e Atendimento de Pacientes para clínicas de saúde. Backend Java 21 / Spring Boot 4.1.x em arquitetura Hexagonal/Clean, empacotado como WAR. PostgreSQL + Flyway, JWT stateless, RBAC por `@EnableMethodSecurity`.

# Comandos

```bash
./mvnw clean compile          # compila e baixa dependências
./mvnw spring-boot:run        # roda local em http://localhost:${PORT:-8080}
./mvnw clean test             # testes (sobe PostgreSQL via Testcontainers — exige Docker rodando)
./mvnw test -Dtest=NomeDaClasse                 # uma classe de teste
./mvnw test -Dtest=NomeDaClasse#nomeDoMetodo    # um método específico
./mvnw clean verify           # testes + gate de cobertura JaCoCo (mínimo 80% de linhas, falha o build abaixo disso)
./mvnw clean package          # gera o .war em target/
```

- Configuração local: `cp .env.example .env` e preencher `JWT_SECRET`/`ACTION_TOKEN_SECRET` (≥32 chars, `openssl rand -hex 32`, **devem ser diferentes um do outro**).
- Não é preciso criar o banco manualmente: `DatabaseInitializerListener` cria o `DB_NAME` no Postgres se não existir na primeira subida.
- Swagger UI em `/swagger-ui.html` (auth via botão "Authorize" com o Bearer JWT retornado por `/api/v1/auth/login`).
- Ambiente local tem alias `rtk` para comandos de shell (proxy que comprime output p/ economizar tokens) — prefira `rtk mvn ...` / `rtk git ...` quando disponível.

# Arquitetura

Hexagonal/Clean Architecture, isolando regras de negócio de infraestrutura. Pacote raiz: `br.com.belloinfo.saap_mvp`.

```
domain/            # Camada pura, sem dependência de Spring
  model/           # Entidades de domínio (Appointment, Patient, Professional, Service, User, WaitlistEntry, MedicalRecord...)
  repository/      # Portas de saída (interfaces de repositório)
  valueobject/     # Enums: UserRole, ProfessionalRole, AppointmentStatus, PriorityLevel, WaitlistStatus, PaymentMethod
  exception/       # Exceções de domínio (ResourceNotFoundException, MedicalRecordConflictException)

application/       # Casos de uso (um por operação, ex: BookAppointmentUseCase, CheckInAppointmentUseCase)
  usecase/
  service/         # AppointmentActionTokenService, AuditService, NotificationService

infrastructure/    # Adaptadores (Spring/JPA/Web)
  persistence/     # entity/ (JPA), repository/ (Spring Data JpaRepository), adapter/ (implementa as portas do domain), mapper/CoreMapper (MapStruct: domain <-> JPA entity)
  web/             # controller/, dto/ (records imutáveis com Bean Validation), mapper/WebMapper (domain <-> DTO), validation/ (@CPF custom), exception/GlobalExceptionHandler
  security/        # SecurityConfig, JwtAuthenticationFilter, LoginRateLimitFilter, TokenService, TokenBlacklistService
  scheduler/       # AppointmentFollowUpScheduler, WaitlistTimeoutScheduler (jobs @Scheduled)
  database/        # DatabaseInitializerListener (auto-cria o schema/DB)
  config/          # ClinicSettings, SaapProperties (propriedades da clínica: janela de confirmação, prazo de follow-up etc.)
```

Fluxo típico de requisição: `Controller` recebe `DTO` → chama `UseCase` → `UseCase` opera sobre entidades de `domain/model` via portas de `domain/repository` → `RepositoryAdapter` (infrastructure/persistence) traduz para `JPA Entity` via `CoreMapper` → `Controller` traduz o retorno de domínio para DTO de resposta via `WebMapper`.

Todas as rotas REST usam prefixo global `/api/v1`.

## Migrações de banco (Flyway)

`src/main/resources/db/migration/V1__...` a `V10__...`, aplicadas em ordem sequencial. Nova migração = novo arquivo `V{n+1}__descricao_snake_case.sql`; nunca editar migração já aplicada.

# Regras de Negócio Importantes

## Máquina de estados do Appointment (`Appointment.transitionTo`)

```
PENDING -> CONFIRMED | CANCELLED | PENDING_RESPONSE
PENDING_RESPONSE -> CONFIRMED | CANCELLED
CONFIRMED -> ARRIVED | CANCELLED | NO_SHOW
ARRIVED -> CALLING | CANCELLED
CALLING -> IN_PROGRESS
IN_PROGRESS -> COMPLETED
COMPLETED | CANCELLED | NO_SHOW -> (terminais, nenhuma transição válida)
```
Transição inválida lança `IllegalStateException`. Qualquer novo fluxo de status deve passar por este método, não setar `status` diretamente.

## Fila presencial / Prioridade legal

`checkIn()` calcula `priorityScore = priorityLevel.getValue() * 1_000_000_000_000L + checkInTimestamp` — maior nível de prioridade (P1 > P5, `PriorityLevel` é 1..5) sempre ganha de qualquer diferença de horário de chegada. `CallNextPatientUseCase` usa esse score para decidir o próximo paciente. Prioridade só é confirmada no check-in (por `PriorityVerifiedBy`), não na reserva do agendamento.

## Prontuário / Registro Clínico

- Acesso exclusivo a `ROLE_PROFESSIONAL` (`/api/v1/medical-records/**`), toda leitura/escrita é auditada (RNF01, via `AuditService`/`AuditLogRepository`).
- Evolução clínica só pode ser criada/editada enquanto o agendamento está `IN_PROGRESS`. Após `COMPLETED`, a entrada é imutável — tentativa de edição retorna HTTP 409 (`MedicalRecordConflictException`).

## Fila de espera (Waitlist)

`WaitlistStatus`: `WAITING -> OFFERED -> ACCEPTED | DECLINED | EXPIRED`. `WaitlistTimeoutScheduler` expira ofertas não respondidas; `ProcessWaitlistSlotOfferUseCase`/`AcceptWaitlistOfferUseCase`/`DeclineWaitlistOfferUseCase` implementam o ciclo de oferta de vaga.

## Segurança

- JWT stateless (`JwtAuthenticationFilter`), sem sessão HTTP. Dois segredos distintos: `JWT_SECRET` (login) e `ACTION_TOKEN_SECRET` (links de confirmação/cancelamento por e-mail, sem necessidade de login — ver `AppointmentActionTokenService` e rotas públicas `/api/v1/appointments/public/**`).
- Papéis (`UserRole`): `ADMIN`, `RECEPTIONIST`, `PROFESSIONAL`, `ASSISTANT`, `PATIENT`. Autorização fina via `@EnableMethodSecurity` nos use cases/controllers além das regras grosseiras em `SecurityConfig` (ex: `/actuator/**` só `ADMIN`, `/api/v1/medical-records/**` só `PROFESSIONAL`).
- `LoginRateLimitFilter` limita tentativas de login; `TokenBlacklistService` invalida tokens (logout).
- CPF validado por `@CPF` (`CpfValidator`) com verificação real dos dígitos verificadores, não só formato.

# Planos Concluídos

- ✅ **Lote 1 (010-016)**: Unique timestamps, repository fixes, correlation tracing, Redis token blacklist, security hardening
- ✅ **Lote 2 (017-020)**: State machine, medical records access control, waitlist timeout scheduler
- ✅ **Lote 3 (021-024)**: Clinic configuration, follow-up scheduler, priority scoring, check-in
- ✅ **Plan 025**: Notification channels (Email SMTP, WhatsApp via Twilio, NotificationOrchestrator)
- ✅ **Plan 025 - Extensão**: Botões interativos de WhatsApp (Content API) para fila de espera, com webhook de resposta e paridade de mensagens entre e-mail/WhatsApp/links públicos

## Plan 025 - Notificações Reais

Implementado suporte multi-canal para notificações:

- **EmailNotificationService**: Spring Mail com SMTP (compatível com OCI Email Delivery); `sendHtml()` para e-mails HTML ricos (botões estilizados "Aceitar"/"Recusar" na oferta de fila de espera)
- **WhatsAppNotificationService**: Twilio WhatsApp Business API; `sendQuickReply()` para envio de Content Templates com botões interativos via Content API
- **NotificationOrchestrator**: Coordenador de canais com fallback gracioso; identifica canais via `AopUtils.getTargetClass(channel).getSimpleName()` (necessário porque `@Async` faz o Spring envolver o bean em proxy — usar `channel.getClass()` direto retornava o nome da classe proxy, não da classe real); `notifyVia(channelName, ...)` permite disparo para um canal específico por nome
- **NotificationServiceImpl**: Implementa contrato de domínio com templates de follow-up e waitlist, incluindo o HTML da oferta de fila de espera e o roteamento por canal (email → `sendHtml`, WhatsApp → `sendQuickReply` quando há Content SID configurado, senão texto simples via orquestrador)
- Configuração: `app.notifications.enabled`, `MAIL_HOST/PORT/USERNAME/PASSWORD`, variáveis Twilio (`TWILIO_ACCOUNT_SID/AUTH_TOKEN/FROM_NUMBER/WAITLIST_CONTENT_SID/WEBHOOK_BASE_URL/WEBHOOK_VALIDATE_SIGNATURE`)
- Validação de recipients null, logging, operações `@Async`
- **Canal SMS removido** (`SmsNotificationService` excluído): o WhatsApp com Content Templates/botões cobre o mesmo caso de uso com melhor experiência (botão clicável em vez de responder SMS em texto livre)

### Extensão: Botões Interativos de WhatsApp para Fila de Espera

Feature completa de resposta a ofertas de fila de espera diretamente por clique em botão do WhatsApp (Aceitar/Recusar), sem precisar abrir o link de e-mail:

- **`WhatsAppWebhookController`** (`POST /api/v1/notifications/whatsapp/webhook`, `permitAll` no `SecurityConfig`): recebe `From` + `ButtonPayload` do Twilio, valida a assinatura da requisição (`com.twilio.security.RequestValidator`), identifica a oferta de fila de espera ativa mais recente pelo telefone do remetente, gera um action token interno e reaproveita `AcceptWaitlistOfferUseCase`/`DeclineWaitlistOfferUseCase` — garantindo que a resposta por botão produza o mesmo efeito e a mesma mensagem de confirmação que o link público de e-mail. Responde em TwiML.
- **`WaitlistEntryRepository.findMostRecentOfferedByPatientPhone`**: nova consulta (interface de domínio + `WaitlistEntryRepositoryAdapter` + `JpaWaitlistEntryRepository.findFirstByPatientPhoneAndStatusAndActiveTrueOrderByOfferedAppointmentTimeDesc`) usada pelo webhook para resolver qual oferta o clique se refere.
- **Correção de bug**: os endpoints públicos de magic-link (`GET /api/v1/appointments/public/confirm|cancel|waitlist/accept|waitlist/decline`) haviam sido convertidos incorretamente para `POST` com token no corpo; revertidos para `GET` com token na query string, pois são links clicáveis de e-mail/WhatsApp (não podem exigir corpo de requisição). `ActionTokenRequestDTO` (usado só pela variante POST) removido por não ter mais uso.
- **Troubleshooting de assinatura Twilio (documentado em `docs/twilio.md`)**: erro 11200 do Twilio causado por `TWILIO_WEBHOOK_BASE_URL` desatualizado (apontando para túnel Cloudflare já encerrado), fazendo a assinatura calculada localmente não bater com a enviada pelo Twilio → `403`. Distinto do erro 11210 (DNS/túnel morto). Fix: manter `TWILIO_WEBHOOK_BASE_URL` sincronizado com a URL do túnel ativo e reiniciar o servidor após qualquer mudança.
- Testado ponta a ponta com cliques reais em botões do WhatsApp (Aceitar e Recusar), confirmando mensagem de resposta e efeito no banco (`WaitlistStatus`, `is_active`) via API real do Twilio (Messages API) — nenhum mock usado.
- Ver `docs/twilio.md` (setup, Content Templates, variáveis, troubleshooting) e `docs/NOTIFICATION_TESTING.md` (protocolo de teste passo a passo, incluindo geração de oferta nova para testar botões de uso único).

# Testes

- `BaseIntegrationTest` sobe um `PostgreSQLContainer` (Testcontainers, `postgres:16-alpine`) real e usa perfil `test`; toda classe de teste de integração deve estendê-la. Docker precisa estar rodando.
- `ddl-auto=validate` nos testes de integração — o schema vem das migrações Flyway reais, não do Hibernate; alterar entidade sem migração correspondente quebra os testes de integração.
- Convenção: `*UseCaseTest` (unitário, mocka repositórios) vs `*IntegrationTest`/`*IT` (sobe contexto Spring completo + Testcontainers).
- Cobertura mínima de 80% de linhas verificada pelo JaCoCo no `mvnw verify` (falha o build se abaixo).
- **Status atual**: 274/274 testes passando, 80%+ de cobertura JaCoCo
