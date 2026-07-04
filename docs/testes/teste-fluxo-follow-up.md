# Teste do Fluxo de Follow-up (RF03)

> **Data:** 04/07/2026
> **Objetivo:** Validar o fluxo completo de notificação e confirmação/cancelamento de agendamentos pelo paciente via links públicos.

---

## 📋 Resumo dos Testes

| # | Teste | Endpoint | Status |
|---|-------|----------|--------|
| 1 | Confirmar presença via link público | `GET /appointments/public/confirm?token=` | ✅ **SUCESSO** |
| 2 | Cancelar consulta via link público | `GET /appointments/public/cancel?token=` | ✅ **SUCESSO** |

---

## 🧪 Pré-requisitos

### Variáveis de Ambiente (`.env`)

```env
JWT_SECRET=dcf8f3763d104530d14c20873303c55d2fce1ffed30db80eee9cf37490851ce3
ACTION_TOKEN_SECRET=a3b8e7f2d1c904e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9
JWT_EXPIRATION=86400000
ACTION_TOKEN_EXPIRATION=86400000
FOLLOW_UP_CRON=*/30 * * * * *
```

> **Nota:** O `.env` não é carregado automaticamente pelo Spring Boot. É necessário exportar as variáveis antes de iniciar ou usar um mecanismo como `export $(grep -v '^#' .env | xargs) && ./mvnw spring-boot:run`.

### Dados Base

- **Admin:** `john.nobody@email.com` / `SenhaForte123!`
- **Recepcionista:** `ana.lima@saap.com` / `Recepcao123!`
- **Profissional:** Dr. Carlos Mendes (`5134ffb1-6a1e-4d55-8abb-538ce717cfba`) — login `dr.carlos@saap.com` / `Medico123!`
- **Paciente:** Carlos Augusto Teste (`3ae569b4-5883-4b07-8d21-4790d28963b8`)
- **Serviço:** Consulta Clínica Geral (`07800639-bfdb-45e6-94d2-fb98bc7495ea`)

---

## 🔄 Fluxo Completo

### Etapa 1: Criar Agendamento PENDING

```http
POST /api/v1/appointments
Authorization: Bearer <admin-token>
Content-Type: application/json

{
  "patientId": "3ae569b4-5883-4b07-8d21-4790d28963b8",
  "professionalId": "5134ffb1-6a1e-4d55-8abb-538ce717cfba",
  "serviceId": "07800639-bfdb-45e6-94d2-fb98bc7495ea",
  "dateTime": "2026-07-05T10:00:00",
  "paymentMethod": "PIX"
}
```

**Resultado:** `201 Created`
```json
{
  "id": "3c3f1e60-b3a5-4fa5-96e4-50d71271d89c",
  "status": "PENDING",
  "dateTime": "2026-07-05T10:00:00"
}
```

### Etapa 2: Transicionar para PENDING_RESPONSE

O scheduler (`AppointmentFollowUpScheduler`) ou ação manual via SQL:

```sql
UPDATE agendamento 
SET status = 'PENDING_RESPONSE', 
    follow_up_sent = true, 
    follow_up_sent_at = NOW() 
WHERE id = '3c3f1e60-b3a5-4fa5-96e4-50d71271d89c';
```

**Resultado:** `UPDATE 1`

### Etapa 3: Gerar Token JWT (pela aplicação)

O `AppointmentActionTokenService` gera tokens JWT com:

| Campo | Valor |
|-------|-------|
| **Algoritmo** | HMAC256 |
| **Secret** | `ACTION_TOKEN_SECRET` |
| **Issuer (iss)** | `saap-action-token` |
| **Subject (sub)** | UUID do agendamento |
| **Claim "action"** | `confirm` ou `cancel` |
| **Expiração** | 24h |

Exemplo de payload do token:
```json
{
  "iss": "saap-action-token",
  "sub": "3c3f1e60-b3a5-4fa5-96e4-50d71271d89c",
  "action": "confirm",
  "exp": 1783261693
}
```

### Etapa 4: Confirmar Presença

```http
GET /api/v1/appointments/public/confirm?token=<confirm-token>
```

**Resultado:** ✅ `200 OK`
```
Presença confirmada com sucesso!
```

**Transição de estado:** `PENDING_RESPONSE` → `CONFIRMED`

### Etapa 5: Cancelar Consulta

```http
GET /api/v1/appointments/public/cancel?token=<cancel-token>
```

**Resultado:** ✅ `200 OK`
```
Consulta cancelada com sucesso!
```

**Transição de estado:** `PENDING_RESPONSE` → `CANCELLED`

---

## 📸 Logs do ConsoleNotificationService

Quando o scheduler dispara a notificação, os logs exibem:

```
=== [NOTIFICAÇÃO ENVIADA] ===
Para: carlos.teste@email.com
Assunto: Confirmação de Consulta - SAAP
Olá, Carlos Augusto Teste, você possui uma consulta marcada para amanhã 
(2026-07-05T10:00) com o profissional Dr. Carlos Mendes.
Por favor, confirme ou cancele sua presença através dos links abaixo:
Confirmar presença: http://localhost:8080/api/v1/appointments/public/confirm?token=eyJ...
Cancelar consulta:  http://localhost:8080/api/v1/appointments/public/cancel?token=eyJ...
=============================
```

---

## ⚙️ Configuração do Scheduler

O `AppointmentFollowUpScheduler` está configurado no `application.yaml`:

```yaml
clinic:
  settings:
    confirmation-window-hours: 48   # Janela para disparar notificação
    follow-up-deadline-hours: 24    # Prazo para resposta do paciente
    auto-cancel-after-no-response: true  # Cancelar automaticamente se não responder

saap:
  scheduler:
    follow-up:
      cron: "*/30 * * * * *"      # A cada 30 segundos (ambiente dev)
```

### Jobs do Scheduler

| Job | Horário | Descrição |
|-----|---------|-----------|
| `sendFollowUpNotifications` | Conforme cron | Notifica PENDING dentro da janela → PENDING_RESPONSE |
| `processMissedDeadlines` | Minuto 30 de cada hora | Processa PENDING_RESPONSE com prazo expirado |

---

## ✅ Cenários Testados

### Cenário 1: Confirmação via Link Público
- ✅ Token válido → `200 OK` → Status `CONFIRMED`
- ✅ Token inválido → `400 Bad Request` → "Token de ação inválido ou expirado"
- ✅ Agendamento em status incorreto → `409 Conflict`

### Cenário 2: Cancelamento via Link Público
- ✅ Token válido → `200 OK` → Status `CANCELLED`
- ✅ Token inválido → `400 Bad Request`
- ✅ Agendamento em status incorreto → `409 Conflict`

### Cenário 3: Não Resposta do Paciente
- Com `auto-cancel-after-no-response: true` → Cancelamento automático
- Com `auto-cancel-after-no-response: false` → Marcado como `followUpRequired` para ação manual

---

## 🔄 Fluxo de Check-in e Atendimento (RF06)

> **Data:** 04/07/2026
> **Objetivo:** Validar o fluxo completo de check-in e atendimento presencial (check-in → chamar → iniciar → preencher prontuário → completar).

### ✅ Resultado: FLUXO COMPLETO EXECUTADO COM SUCESSO

Todas as etapas do fluxo foram executadas e validadas com sucesso em **04/07/2026**.

| Etapa | Ação | Quem | Transição | Resultado |
|-------|------|------|-----------|-----------|
| 1 | Confirmar agendamento | Admin | `PENDING → CONFIRMED` | ✅ `200 OK` |
| 2 | Check-in presencial | Recepcionista | `CONFIRMED → ARRIVED` | ✅ `200 OK` |
| 3 | Chamar próximo da fila | Profissional | `ARRIVED → CALLING` | ✅ `200 OK` |
| 4 | Iniciar consulta | Profissional | `CALLING → IN_PROGRESS` | ✅ `200 OK` |
| 5 | Preencher evolução clínica | Profissional | — | ✅ `201 Created` |
| 6 | Concluir consulta | Profissional | `IN_PROGRESS → COMPLETED` | ✅ `200 OK` |

---

### 📋 Detalhamento das Etapas

#### Etapa 1: Confirmar Agendamento (Admin)

```http
PUT /api/v1/appointments/{id}/confirm
Authorization: Bearer <admin-token>
```

**Resultado:** ✅ `200 OK` — `PENDING → CONFIRMED`

#### Etapa 2: Check-in (Recepcionista)

```http
PUT /api/v1/appointments/{id}/check-in
Authorization: Bearer <receptionist-token>
Content-Type: application/json

{
  "verifiedLevel": "P5",
  "notes": "Check-in realizado, paciente presente na recepção."
}
```

**Resultado:** ✅ `200 OK` — `CONFIRMED → ARRIVED`

> O sistema calcula automaticamente o `priorityScore` com base na prioridade declarada e verificação documental.

#### Etapa 3: Chamar Próximo Paciente (Profissional)

```http
POST /api/v1/appointments/next
Authorization: Bearer <professional-token>
```

**Resultado:** ✅ `200 OK` — `ARRIVED → CALLING`

> **Regra de negócio:** O `findNextInQueue` busca agendamentos **do dia atual** (`dateTime BETWEEN startOfDay AND endOfDay`), ordenados por `priorityScore` ascendente (menor score = maior prioridade).

#### Etapa 4: Iniciar Consulta (Profissional)

```http
PUT /api/v1/appointments/{id}/start
Authorization: Bearer <professional-token>
```

**Resultado:** ✅ `200 OK` — `CALLING → IN_PROGRESS`

#### Etapa 5: Preencher Evolução Clínica (Profissional)

```http
POST /api/v1/medical-records/entries
Authorization: Bearer <professional-token>
Content-Type: application/json

{
  "appointmentId": "{id}",
  "evolution": "Paciente apresenta melhora significativa. Pressão arterial 120x80 mmHg."
}
```

**Resultado:** ✅ `201 Created` — Registro criado com sucesso

> **Obrigatório:** A API **bloqueia** a finalização da consulta sem evolução clínica (HTTP 409 — `MedicalRecordConflictException`).

#### Etapa 6: Concluir Consulta (Profissional)

```http
PUT /api/v1/appointments/{id}/complete
Authorization: Bearer <professional-token>
```

**Resultado:** ✅ `200 OK` — `IN_PROGRESS → COMPLETED`

---

### 📊 Mapa de Transições de Estado

```
PENDING ──→ CONFIRMED ──→ ARRIVED ──→ CALLING ──→ IN_PROGRESS ──→ COMPLETED
    │             │            │            │              │        
    └──→ CANCELLED ←───────────┴────────────┴──────────────┴──→ CANCELLED
                                   ↗
                          PENDING_RESPONSE
```

### 🧪 Agendamento de Teste (04/07/2026)

| Campo | Valor |
|-------|-------|
| **ID** | `daa6c144-4324-45a7-8223-092d5e06407a` |
| **Paciente** | Carlos Augusto Teste |
| **Profissional** | Dr. Carlos Mendes |
| **Serviço** | Consulta Clínica Geral (R$ 150,00 - 30 min) |
| **Data/Hora** | 04/07/2026 às 16:00 |
| **Pagamento** | PIX |
| **Status Final** | ✅ `COMPLETED` |

### ⚙️ Detalhe Técnico do `findNextInQueue`

```java
// AppointmentRepositoryAdapter.java
public Optional<Appointment> findNextInQueue(UUID professionalId, LocalDateTime start, LocalDateTime end) {
    return jpaAppointmentRepository
        .findFirstByProfessionalIdAndStatusAndDateTimeBetweenOrderByPriorityScoreAsc(
            professionalId,
            AppointmentStatus.ARRIVED,
            start,   // Início do dia atual (00:00:00)
            end      // Fim do dia atual (23:59:59.999999999)
        ).map(mapper::toDomain);
}
```

---

## 📌 Observações

1. **Notificação:** Atualmente via `ConsoleNotificationService` (apenas logs). Para e-mail real, implementar a interface `NotificationService`.
2. **Tokens:** Utilizam chave separada (`ACTION_TOKEN_SECRET`), diferente do JWT de autenticação (`JWT_SECRET`).
3. **Issuer:** O issuer dos tokens de ação é `saap-action-token` (diferente de `saap-api` usado nos tokens de autenticação).
4. **Cron:** Em desenvolvimento, usar `*/30 * * * * *` para testar rapidamente. Em produção, ajustar para `0 0 * * * *` (a cada hora).
5. **Fila do dia:** O `CallNextPatientUseCase` busca apenas agendamentos do dia atual. Agendamentos futuros entram na fila no próprio dia da consulta após o check-in.

---

## ✅ Execução de Validação Real (04/07/2026)

Fluxo reexecutado ponta a ponta contra a instância da aplicação em execução (porta 8080, banco `postgis`), confirmando os dois fluxos (RF03 e RF06) sem regressões.

### Correção de Documentação

- **Login:** o endpoint real é `POST /api/v1/auth/login` (não `/auth/login`).

### RF03 — Follow-up

| Cenário | Agendamento | Resultado |
|---------|-------------|-----------|
| Confirmar (token válido) | `8d90ad04-3e5e-4ce1-90f4-01280df8a0a3` | ✅ `200 OK` → `CONFIRMED` |
| Confirmar (token inválido) | — | ✅ `400 Bad Request` |
| Confirmar (status incorreto, já `CONFIRMED`) | `8d90ad04-...` | ✅ `409 Conflict` |
| Cancelar (token válido) | `5ba216cf-7890-41e2-8e71-d7978365b519` | ✅ `200 OK` → `CANCELLED` |
| Cancelar (token inválido) | — | ✅ `400 Bad Request` |
| Cancelar (status incorreto, já `CANCELLED`) | `5ba216cf-...` | ✅ `409 Conflict` |

> **Nota:** como o scheduler não seria aguardado em tempo real, os tokens de ação (`confirm`/`cancel`) foram gerados manualmente com PyJWT (HS256, `iss=saap-action-token`, `sub=<appointmentId>`, `exp=+24h`) usando o `ACTION_TOKEN_SECRET` do `.env` — payload idêntico ao produzido por `AppointmentActionTokenService`.

### RF06 — Check-in e Atendimento

| Etapa | Agendamento `1a555782-b3ed-49ab-8bd1-f94214ddf3e1` | Resultado |
|-------|-----------------------------------------------------|-----------|
| Confirmar (Admin) | `PENDING → CONFIRMED` | ✅ `200 OK` |
| Check-in (Recepcionista) | `CONFIRMED → ARRIVED` | ✅ `200 OK` |
| Chamar próximo (Profissional) | `ARRIVED → CALLING` | ✅ `200 OK` |
| Iniciar consulta (Profissional) | `CALLING → IN_PROGRESS` | ✅ `200 OK` |
| Preencher evolução clínica | — | ✅ `201 Created` |
| Concluir consulta (Profissional) | `IN_PROGRESS → COMPLETED` | ✅ `200 OK` |

### Guarda de Negócio Confirmada

Agendamento `e46adac5-5ef1-4217-a3aa-fd97252e0200` levado até `IN_PROGRESS` e finalizado **sem** preencher evolução clínica:

```json
{"status":409,"error":"Conflict","message":"Não é possível finalizar o atendimento sem a evolução clínica preenchida"}
```

✅ Bloqueio confirmado — `MedicalRecordConflictException` disparada corretamente.
