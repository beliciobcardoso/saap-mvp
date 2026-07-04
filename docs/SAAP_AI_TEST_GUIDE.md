# SAAP MVP - Guia Completo de Teste para IA (Playwright)

## 📋 Sumário Executivo

Documento único para IA ler e testar **toda a aplicação SAAP MVP** via frontend (Playwright). Cobre:
- Verificação de que a aplicação está rodando
- Autenticação JWT
- Todos os atores (roles) e permissões
- Todos os endpoints REST com exemplos de request/response
- Fluxo CREATE, READ, UPDATE, DELETE para cada entidade
- Fluxo completo de agendamento (máquina de estados)
- Prontuário clínico e regra de imutabilidade
- Auditoria
- Checklist de testes obrigatórios
- Dados de teste pré-configurados
- Erros comuns e soluções

---

## 🚀 Parte 1: Verificação da Aplicação

### 1.1 Verificar que está Rodando

```bash
curl http://localhost:8080/swagger-ui.html
# Deve retornar HTML do Swagger UI

curl http://localhost:8080/actuator/health
# Deve retornar {"status":"UP"}
```

---

## 🔐 Parte 2: Autenticação JWT

### 2.1 Fluxo de Login

**Endpoint:** `POST /api/v1/auth/login`
**Acesso:** Público (sem autenticação prévia)

**Usuário Admin de Teste (pré-cadastrado via migration):**
```json
{
  "email": "john.nobody@email.com",
  "password": "SenhaForte123!"
}
```

**Request:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john.nobody@email.com",
    "password": "SenhaForte123!"
  }'
```

**Response Esperada:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "john.nobody@email.com",
    "name": "John Admin",
    "role": "ADMIN",
    "active": true
  }
}
```

### 2.2 Usar Token em Requisições Subsequentes

**Salve o token** da resposta acima.

**Header obrigatório para endpoints protegidos:**
```
Authorization: Bearer <TOKEN_AQUI>
```

**Exemplo com curl:**
```bash
TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

curl -X GET http://localhost:8080/api/v1/users \
  -H "Authorization: Bearer $TOKEN"
```

### 2.3 Via Swagger UI

1. Acesse http://localhost:8080/swagger-ui.html
2. Chame `POST /api/v1/auth/login` com as credenciais de teste
3. Copie o `token` retornado
4. Clique em **Authorize** no topo da página, cole o token, confirme
5. Todos os endpoints protegidos passam a usar esse token automaticamente

---

## 👥 Parte 3: Atores (Roles) e Permissões

Existem **5 roles** no sistema:

| Role | Descrição | Permissões |
|------|-----------|-----------|
| **ADMIN** | Administrador do sistema | Gerenciar usuários, profissionais, serviços; listar auditoria; acesso total |
| **RECEPTIONIST** | Recepcionista da clínica | Gerenciar agendamentos (agendar, confirmar, check-in, cancelar); listar pacientes/profissionais/serviços |
| **PROFESSIONAL** | Médico/Profissional de saúde | Ler/escrever prontuário; chamar próximo paciente (fila); iniciar/completar atendimento |
| **ASSISTANT** | Assistente administrativo | Gerenciar pacientes (criar/editar/desativar); listar pacientes |
| **PATIENT** | Paciente | Agendar consulta própria; cancelar agendamento próprio; ler prontuário próprio |

**Usuário Admin de Teste (já existe no banco via migration V10):**
- Email: `john.nobody@email.com`
- Senha: `SenhaForte123!`
- Role: `ADMIN`
- Status: `ACTIVE`
- ID: `550e8400-e29b-41d4-a716-446655440000`

Para testar os outros 4 papéis (RECEPTIONIST, PROFESSIONAL, ASSISTANT, PATIENT), a IA deve criar
um usuário para cada role usando o endpoint `POST /api/v1/users` autenticado como ADMIN (ver Parte 4.1),
depois fazer login com cada um deles e repetir os testes relevantes.

---

## 📊 Parte 4: Entidades e Fluxo CRUD Completo

### 4.1 USUÁRIOS (`/api/v1/users`)

**Requer:** `ADMIN`

#### 4.1.1 CREATE - Cadastrar Novo Usuário

```bash
TOKEN="<seu_token>"

curl -X POST http://localhost:8080/api/v1/users \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "receptionist01@clinica.com",
    "password": "SenhaForte123!",
    "name": "Maria Receptionist",
    "role": "RECEPTIONIST"
  }'
```

**Response (201 Created):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440001",
  "email": "receptionist01@clinica.com",
  "name": "Maria Receptionist",
  "role": "RECEPTIONIST",
  "active": true,
  "createdAt": "2026-07-03T10:00:00Z"
}
```

**Roles Válidos para Criação:** `ADMIN`, `RECEPTIONIST`, `PROFESSIONAL`, `ASSISTANT`, `PATIENT`

> Repita a criação para os 4 roles restantes (`PROFESSIONAL`, `ASSISTANT`, `PATIENT` e mais um `ADMIN`
> se desejar) para ter usuários de teste cobrindo todos os atores.

#### 4.1.2 READ - Buscar Usuário por ID

```bash
USER_ID="550e8400-e29b-41d4-a716-446655440001"

curl -X GET http://localhost:8080/api/v1/users/$USER_ID \
  -H "Authorization: Bearer $TOKEN"
```

**Response (200 OK):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440001",
  "email": "receptionist01@clinica.com",
  "name": "Maria Receptionist",
  "role": "RECEPTIONIST",
  "active": true,
  "createdAt": "2026-07-03T10:00:00Z"
}
```

#### 4.1.3 LIST - Listar Todos os Usuários Ativos

```bash
curl -X GET http://localhost:8080/api/v1/users \
  -H "Authorization: Bearer $TOKEN"
```

**Response (200 OK):**
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "john.nobody@email.com",
    "name": "John Admin",
    "role": "ADMIN",
    "active": true
  },
  {
    "id": "550e8400-e29b-41d4-a716-446655440001",
    "email": "receptionist01@clinica.com",
    "name": "Maria Receptionist",
    "role": "RECEPTIONIST",
    "active": true
  }
]
```

#### 4.1.4 UPDATE - Atualizar Usuário

```bash
curl -X PUT http://localhost:8080/api/v1/users/$USER_ID \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "receptionist01.updated@clinica.com",
    "password": "NovaSenha456!",
    "name": "Maria Silva Receptionist",
    "role": "RECEPTIONIST"
  }'
```

**Response (200 OK):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440001",
  "email": "receptionist01.updated@clinica.com",
  "name": "Maria Silva Receptionist",
  "role": "RECEPTIONIST",
  "active": true
}
```

#### 4.1.5 DELETE - Desativar Usuário (Soft Delete)

```bash
curl -X DELETE http://localhost:8080/api/v1/users/$USER_ID \
  -H "Authorization: Bearer $TOKEN"
```

**Response (204 No Content)**
- Usuário marcado como `active: false` no banco (não é deletado fisicamente)
- Não mais listado em `GET /api/v1/users`

---

### 4.2 PACIENTES (`/api/v1/patients`)

**Requer:** `ADMIN` ou `ASSISTANT` (para CREATE, UPDATE, DELETE)
**Leitura:** `ADMIN`, `ASSISTANT`, `RECEPTIONIST`, `PROFESSIONAL`

#### 4.2.1 CREATE - Cadastrar Novo Paciente

**Validações Obrigatórias:**
- `cpf`: Formato válido (11 dígitos, passa na validação de CPF, `@CPF` custom)
- `susNumber`: Exatamente 15 dígitos (ou vazio)
- `email`: Formato de e-mail válido
- `phone`: Não vazio
- `birthDate`: Data no passado (YYYY-MM-DD)

```bash
curl -X POST http://localhost:8080/api/v1/patients \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "João da Silva",
    "cpf": "12345678910",
    "susNumber": "123456789012345",
    "email": "joao.silva@email.com",
    "phone": "11987654321",
    "birthDate": "1990-05-15"
  }'
```

**Response (201 Created):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440002",
  "name": "João da Silva",
  "cpf": "123.456.789-10",
  "susNumber": "123456789012345",
  "email": "joao.silva@email.com",
  "phone": "11987654321",
  "birthDate": "1990-05-15",
  "active": true,
  "createdAt": "2026-07-03T10:10:00Z"
}
```

#### 4.2.2 READ - Buscar Paciente por ID

```bash
PATIENT_ID="550e8400-e29b-41d4-a716-446655440002"

curl -X GET http://localhost:8080/api/v1/patients/$PATIENT_ID \
  -H "Authorization: Bearer $TOKEN"
```

**Response (200 OK):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440002",
  "name": "João da Silva",
  "cpf": "123.456.789-10",
  "susNumber": "123456789012345",
  "email": "joao.silva@email.com",
  "phone": "11987654321",
  "birthDate": "1990-05-15",
  "active": true
}
```

#### 4.2.3 LIST - Listar Todos os Pacientes Ativos

```bash
curl -X GET http://localhost:8080/api/v1/patients \
  -H "Authorization: Bearer $TOKEN"
```

#### 4.2.4 UPDATE - Atualizar Paciente

```bash
curl -X PUT http://localhost:8080/api/v1/patients/$PATIENT_ID \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "João Pedro da Silva",
    "cpf": "12345678910",
    "susNumber": "123456789012345",
    "email": "joao.silva.updated@email.com",
    "phone": "11987654322",
    "birthDate": "1990-05-15"
  }'
```

**Response (200 OK):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440002",
  "name": "João Pedro da Silva",
  "email": "joao.silva.updated@email.com",
  "phone": "11987654322"
}
```

#### 4.2.5 DELETE - Desativar Paciente

```bash
curl -X DELETE http://localhost:8080/api/v1/patients/$PATIENT_ID \
  -H "Authorization: Bearer $TOKEN"
```

**Response (204 No Content)**

---

### 4.3 PROFISSIONAIS (`/api/v1/professionals`)

**Requer:** `ADMIN` (para CREATE, UPDATE, DELETE)
**Leitura:** `ADMIN`, `RECEPTIONIST`

#### 4.3.1 CREATE - Cadastrar Novo Profissional

**Validações:**
- `crm`: Registro de Conselho Profissional (ex: CRM para médicos)
- `specialty`: Especialidade médica (enum)
- `email`: Formato válido
- `phone`: Não vazio
- `userId`: **obrigatório para o fluxo de prontuário** — UUID do usuário (role `PROFESSIONAL`, ver Parte 4.1) que vai logar como esse profissional. Sem esse vínculo, `GET/POST/PUT /api/v1/medical-records/*` retornam `400 Bad Request` ("Profissional não cadastrado para o usuário logado"), pois o controller resolve o profissional autenticado via `findByUserId`.

```bash
curl -X POST http://localhost:8080/api/v1/professionals \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Dra. Ana Cardiology",
    "crm": "123456",
    "specialty": "CARDIOLOGY",
    "email": "ana.cardio@clinica.com",
    "phone": "11987654321",
    "userId": "'$PROFESSIONAL_USER_ID'"
  }'
```

**Response (201 Created):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440003",
  "name": "Dra. Ana Cardiology",
  "crm": "123456",
  "specialty": "CARDIOLOGY",
  "email": "ana.cardio@clinica.com",
  "phone": "11987654321",
  "userId": "550e8400-e29b-41d4-a716-446655440099",
  "active": true,
  "createdAt": "2026-07-03T10:20:00Z"
}
```

**Specialties Válidas:**
- `CARDIOLOGY` - Cardiologia
- `DERMATOLOGY` - Dermatologia
- `ORTHOPEDICS` - Ortopedia
- `PEDIATRICS` - Pediatria
- `PSYCHIATRY` - Psiquiatria
- `GENERAL_PRACTICE` - Clínica Geral

#### 4.3.2 READ - Buscar Profissional por ID

```bash
PROF_ID="550e8400-e29b-41d4-a716-446655440003"

curl -X GET http://localhost:8080/api/v1/professionals/$PROF_ID \
  -H "Authorization: Bearer $TOKEN"
```

#### 4.3.3 LIST - Listar Todos os Profissionais Ativos

```bash
curl -X GET http://localhost:8080/api/v1/professionals \
  -H "Authorization: Bearer $TOKEN"
```

#### 4.3.4 UPDATE - Atualizar Profissional

```bash
curl -X PUT http://localhost:8080/api/v1/professionals/$PROF_ID \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Dra. Ana Santos Cardiology",
    "crm": "123456",
    "specialty": "CARDIOLOGY",
    "email": "ana.santos@clinica.com",
    "phone": "11987654322"
  }'
```

#### 4.3.5 DELETE - Desativar Profissional

```bash
curl -X DELETE http://localhost:8080/api/v1/professionals/$PROF_ID \
  -H "Authorization: Bearer $TOKEN"
```

---

### 4.4 SERVIÇOS (`/api/v1/services`)

**Requer:** `ADMIN` (para CREATE, UPDATE, DELETE)
**Leitura:** `ADMIN`, `RECEPTIONIST`, `PATIENT`

#### 4.4.1 CREATE - Cadastrar Novo Serviço

**Validações:**
- `name`: Obrigatório, não vazio
- `durationMinutes`: Inteiro >= 1
- `price`: Decimal >= 0

```bash
curl -X POST http://localhost:8080/api/v1/services \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Consulta Cardiologia",
    "description": "Consulta de cardiologia com eletrocardiograma",
    "durationMinutes": 60,
    "price": 350.00
  }'
```

**Response (201 Created):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440004",
  "name": "Consulta Cardiologia",
  "description": "Consulta de cardiologia com eletrocardiograma",
  "durationMinutes": 60,
  "price": 350.00,
  "active": true,
  "createdAt": "2026-07-03T10:30:00Z"
}
```

#### 4.4.2 READ - Buscar Serviço por ID

```bash
SERVICE_ID="550e8400-e29b-41d4-a716-446655440004"

curl -X GET http://localhost:8080/api/v1/services/$SERVICE_ID \
  -H "Authorization: Bearer $TOKEN"
```

#### 4.4.3 LIST - Listar Todos os Serviços Ativos

```bash
curl -X GET http://localhost:8080/api/v1/services \
  -H "Authorization: Bearer $TOKEN"
```

#### 4.4.4 UPDATE - Atualizar Serviço

```bash
curl -X PUT http://localhost:8080/api/v1/services/$SERVICE_ID \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Consulta Cardiologia - Acompanhamento",
    "description": "Consulta de cardiologia com eletrocardiograma e ecografia",
    "durationMinutes": 90,
    "price": 450.00
  }'
```

#### 4.4.5 DELETE - Desativar Serviço

```bash
curl -X DELETE http://localhost:8080/api/v1/services/$SERVICE_ID \
  -H "Authorization: Bearer $TOKEN"
```

---

### 4.5 AGENDAMENTOS (`/api/v1/appointments`)

**Requer:** Permissões variadas conforme ação (ver cada subitem)

#### 4.5.1 CREATE - Agendar Consulta (Book Appointment)

**Requer:** `RECEPTIONIST` ou `PATIENT` (próprio)

**Regra de data:** `startTime` deve ser no futuro (mínimo recomendado: +24h da data atual).

```bash
PROFESSIONAL_ID="550e8400-e29b-41d4-a716-446655440003"
SERVICE_ID="550e8400-e29b-41d4-a716-446655440004"
PATIENT_ID="550e8400-e29b-41d4-a716-446655440002"

curl -X POST http://localhost:8080/api/v1/appointments \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "patientId": "'$PATIENT_ID'",
    "professionalId": "'$PROFESSIONAL_ID'",
    "serviceId": "'$SERVICE_ID'",
    "startTime": "2026-07-04T14:00:00",
    "priority": "NORMAL",
    "description": "Paciente com dor no peito"
  }'
```

**Priority Válidas:** `NORMAL`, `HIGH`, `URGENT`

**Response (201 Created):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440005",
  "patientId": "550e8400-e29b-41d4-a716-446655440002",
  "professionalId": "550e8400-e29b-41d4-a716-446655440003",
  "serviceId": "550e8400-e29b-41d4-a716-446655440004",
  "startTime": "2026-07-04T14:00:00",
  "status": "PENDING",
  "priority": "NORMAL",
  "description": "Paciente com dor no peito",
  "createdAt": "2026-07-03T10:40:00Z"
}
```

#### 4.5.2 READ - Buscar Agendamento por ID

```bash
APPOINTMENT_ID="550e8400-e29b-41d4-a716-446655440005"

curl -X GET http://localhost:8080/api/v1/appointments/$APPOINTMENT_ID \
  -H "Authorization: Bearer $TOKEN"
```

#### 4.5.3 LIST - Listar Agendamentos (com filtros)

```bash
# Listar todos os agendamentos
curl -X GET http://localhost:8080/api/v1/appointments \
  -H "Authorization: Bearer $TOKEN"

# Com filtros (query params opcionais)
curl -X GET "http://localhost:8080/api/v1/appointments?professionalId=$PROFESSIONAL_ID&patientId=$PATIENT_ID&fromDate=2026-07-03&toDate=2026-07-10" \
  -H "Authorization: Bearer $TOKEN"
```

**Query Params:**
- `professionalId`: UUID (filtrar por profissional)
- `patientId`: UUID (filtrar por paciente)
- `fromDate`: YYYY-MM-DD (data inicial, inclusive)
- `toDate`: YYYY-MM-DD (data final, inclusive)

#### 4.5.4 CONFIRM - Confirmar Agendamento

**Requer:** `RECEPTIONIST`
**Transição:** `PENDING` → `CONFIRMED`

```bash
curl -X PUT http://localhost:8080/api/v1/appointments/$APPOINTMENT_ID/confirm \
  -H "Authorization: Bearer $TOKEN"
```

**Response (200 OK):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440005",
  "status": "CONFIRMED",
  "patientId": "550e8400-e29b-41d4-a716-446655440002",
  "professionalId": "550e8400-e29b-41d4-a716-446655440003",
  "serviceId": "550e8400-e29b-41d4-a716-446655440004",
  "startTime": "2026-07-04T14:00:00"
}
```

#### 4.5.5 CHECK-IN - Registrar Presença

**Requer:** `RECEPTIONIST`
**Transição:** `CONFIRMED` → `ARRIVED`
**Observação:** Validação documental de prioridade legal (ex: idoso, gestante, deficiência)

```bash
curl -X PUT http://localhost:8080/api/v1/appointments/$APPOINTMENT_ID/check-in \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "hasCompanyDocument": false,
    "hasConditionDocument": false,
    "notes": "Paciente chegou no horário"
  }'
```

**Response (200 OK):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440005",
  "status": "ARRIVED",
  "priority": "NORMAL",
  "queuePosition": 1
}
```

#### 4.5.6 NEXT - Chamar Próximo Paciente (Fila)

**Requer:** `PROFESSIONAL`
**Transição:** `ARRIVED` → `CALLING`
**Obrigatório:** este passo é pré-requisito de `/start` — `/start` só aceita transição a partir de `CALLING`, nunca direto de `ARRIVED`.
**Lógica:** Retorna próximo paciente aguardando na fila baseado em score de prioridade

```bash
curl -X POST http://localhost:8080/api/v1/appointments/next \
  -H "Authorization: Bearer $TOKEN"
```

**Response (200 OK):**
```json
{
  "appointmentId": "550e8400-e29b-41d4-a716-446655440005",
  "patientName": "João da Silva",
  "patientAge": 33,
  "patientPhone": "11987654321",
  "priority": "NORMAL",
  "waitTime": 5,
  "priority_score": 100
}
```

#### 4.5.7 START - Iniciar Atendimento

**Requer:** `PROFESSIONAL`
**Transição:** `CALLING` → `IN_PROGRESS`
**Pré-requisito:** chamar `POST /appointments/next` (4.5.6) antes — chamar `/start` direto a partir de `ARRIVED` retorna `400 Bad Request` ("Transição de estado inválida de ARRIVED para IN_PROGRESS").

```bash
curl -X PUT http://localhost:8080/api/v1/appointments/$APPOINTMENT_ID/start \
  -H "Authorization: Bearer $TOKEN"
```

**Response (200 OK):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440005",
  "status": "IN_PROGRESS",
  "startedAt": "2026-07-04T14:05:00Z"
}
```

#### 4.5.8 COMPLETE - Completar Agendamento

**Requer:** `PROFESSIONAL`
**Transição:** `IN_PROGRESS` → `COMPLETED`
**Obrigatório:** Incluir evolução clínica em `clinicalEvolution`

```bash
curl -X PUT http://localhost:8080/api/v1/appointments/$APPOINTMENT_ID/complete \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "clinicalEvolution": "Paciente apresenta sintomas de insuficiência cardíaca. Prescrito losartana 50mg/dia. Agendar novo atendimento em 30 dias."
  }'
```

**Response (200 OK):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440005",
  "status": "COMPLETED",
  "completedAt": "2026-07-04T14:50:00Z",
  "clinicalEvolution": "Paciente apresenta sintomas de insuficiência cardíaca..."
}
```

#### 4.5.9 CANCEL - Cancelar Agendamento

**Requer:** `RECEPTIONIST` ou `PATIENT` (próprio)
**Transição válida:** `PENDING` ou `CONFIRMED` → `CANCELLED`

```bash
curl -X PUT http://localhost:8080/api/v1/appointments/$APPOINTMENT_ID/cancel \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "cancellationReason": "Paciente solicitou cancelamento - emergência pessoal"
  }'
```

**Response (200 OK):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440005",
  "status": "CANCELLED",
  "cancellationReason": "Paciente solicitou cancelamento - emergência pessoal",
  "cancelledAt": "2026-07-03T11:30:00Z"
}
```

#### Máquina de Estados Completa

```
PENDING → CONFIRMED → ARRIVED → CALLING → IN_PROGRESS → COMPLETED
   ↓            ↓         ↓
CANCELLED   CANCELLED  CANCELLED
                (CONFIRMED também pode ir a NO_SHOW)
```

Transições válidas (`Appointment.transitionTo`): `PENDING→{CONFIRMED, CANCELLED, PENDING_RESPONSE}`, `PENDING_RESPONSE→{CONFIRMED, CANCELLED}`, `CONFIRMED→{ARRIVED, CANCELLED, NO_SHOW}`, `ARRIVED→{CALLING, CANCELLED}`, `CALLING→{IN_PROGRESS}`, `IN_PROGRESS→{COMPLETED}`. `COMPLETED`, `CANCELLED`, `NO_SHOW` são estados finais.

---

### 4.6 PRONTUÁRIO / REGISTRO CLÍNICO (`/api/v1/medical-records`)

**Requer:** `PROFESSIONAL` (acesso exclusivo). Toda leitura e escrita é auditada.
**Pré-requisito obrigatório:** o registro de `Professional` usado no atendimento precisa ter o campo `userId` apontando para o usuário logado (ver 4.3.1). O controller resolve o profissional autenticado via `findByUserId`; sem esse vínculo, qualquer chamada retorna `400 Bad Request` ("Profissional não cadastrado para o usuário logado").

#### 4.6.1 GET - Consultar Prontuário do Paciente

```bash
curl -X GET http://localhost:8080/api/v1/medical-records/patients/$PATIENT_ID \
  -H "Authorization: Bearer $TOKEN"
```

**Response (200 OK)** — evoluções ordenadas da mais recente para a mais antiga:
```json
{
  "patientId": "550e8400-e29b-41d4-a716-446655440002",
  "patientName": "João da Silva",
  "entries": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440006",
      "appointmentId": "550e8400-e29b-41d4-a716-446655440005",
      "professionalName": "Dra. Ana Cardiology",
      "clinicalEvolution": "Paciente apresenta sintomas de insuficiência cardíaca. Prescrito losartana 50mg/dia.",
      "createdAt": "2026-07-04T14:50:00Z",
      "updatedAt": "2026-07-04T14:50:00Z"
    }
  ]
}
```

#### 4.6.2 CREATE - Registrar Evolução Clínica

**Pré-requisito:** Agendamento em status `IN_PROGRESS`. Somente o profissional do atendimento pode registrar.
**Observação:** Cria o prontuário do paciente sob demanda, se ainda não existir.

```bash
curl -X POST http://localhost:8080/api/v1/medical-records/entries \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "appointmentId": "'$APPOINTMENT_ID'",
    "clinicalEvolution": "Paciente com pressão arterial controlada. Sintomas aliviados. Manter medicação. Retorno em 15 dias."
  }'
```

**Response (201 Created):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440006",
  "appointmentId": "550e8400-e29b-41d4-a716-446655440005",
  "patientId": "550e8400-e29b-41d4-a716-446655440002",
  "clinicalEvolution": "Paciente com pressão arterial controlada...",
  "createdAt": "2026-07-04T14:50:00Z"
}
```

#### 4.6.3 UPDATE - Editar Evolução Clínica

**Condição:** Agendamento ainda em status `IN_PROGRESS`.
**Bloqueio (regra crítica):** Após o agendamento ser `COMPLETED`, a entrada torna-se **imutável** — qualquer tentativa de edição retorna **HTTP 409 Conflict**.

```bash
ENTRY_ID="550e8400-e29b-41d4-a716-446655440006"

curl -X PUT http://localhost:8080/api/v1/medical-records/entries/$ENTRY_ID \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "clinicalEvolution": "Paciente com pressão arterial controlada. Sintomas completamente aliviados. Manter medicação. Retorno em 30 dias."
  }'
```

**Response (200 OK) — se ainda IN_PROGRESS:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440006",
  "appointmentId": "550e8400-e29b-41d4-a716-446655440005",
  "clinicalEvolution": "Paciente com pressão arterial controlada...",
  "updatedAt": "2026-07-04T14:55:00Z"
}
```

**Response (409 Conflict) — se o agendamento já está COMPLETED:** editar não é permitido.

---

### 4.7 AUDITORIA (`/api/v1/audit-logs`)

**Requer:** `ADMIN`
**Acesso:** Leitura apenas

#### 4.7.1 LIST - Listar Logs de Auditoria

```bash
curl -X GET http://localhost:8080/api/v1/audit-logs \
  -H "Authorization: Bearer $TOKEN"
```

**Response (200 OK)** — ordenados por data decrescente:
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440010",
    "action": "CADASTRO_USUARIO",
    "entityId": "550e8400-e29b-41d4-a716-446655440001",
    "entityType": "USER",
    "performedBy": "john.nobody@email.com",
    "ipAddress": "127.0.0.1",
    "timestamp": "2026-07-03T10:00:30Z"
  },
  {
    "id": "550e8400-e29b-41d4-a716-446655440011",
    "action": "CADASTRO_PACIENTE",
    "entityId": "550e8400-e29b-41d4-a716-446655440002",
    "entityType": "PATIENT",
    "performedBy": "john.nobody@email.com",
    "ipAddress": "127.0.0.1",
    "timestamp": "2026-07-03T10:10:15Z"
  }
]
```

**Ações Auditadas:**
- `CADASTRO_USUARIO` / `ATUALIZACAO_USUARIO` / `DESATIVACAO_USUARIO`
- `CADASTRO_PACIENTE` / `ATUALIZACAO_PACIENTE` / `DESATIVACAO_PACIENTE`
- `CADASTRO_PROFISSIONAL` / `ATUALIZACAO_PROFISSIONAL` / `DESATIVACAO_PROFISSIONAL`
- `CADASTRO_SERVICO` / `ATUALIZACAO_SERVICO` / `DESATIVACAO_SERVICO`
- Leitura e escrita de prontuário (medical records) também são auditadas

---

## ✅ Parte 5: Roteiro Completo de Testes (Checklist)

**Objetivo:** A IA deve executar TODOS os testes abaixo e validar que funcionam, cobrindo os 5 atores.

### 5.1 Preparação

- [ ] **Setup:** Aplicação rodando em `http://localhost:8080`
- [ ] **Login:** Obter token JWT com usuário admin (`john.nobody@email.com` / `SenhaForte123!`)
- [ ] **Header:** Todos os requests protegidos incluem `Authorization: Bearer <TOKEN>`
- [ ] **Criar usuários de teste** para os outros 4 roles (RECEPTIONIST, PROFESSIONAL, ASSISTANT, PATIENT)
- [ ] **Vincular o usuário PROFESSIONAL** ao cadastro em `/api/v1/professionals` via campo `userId` (ver 4.3.1) — obrigatório para os testes de prontuário (5.7) funcionarem

### 5.2 Testes de Usuários

- [ ] **CREATE:** Cadastrar novo usuário (cada um dos 5 roles)
- [ ] **READ:** Buscar usuário por ID
- [ ] **LIST:** Listar todos os usuários ativos
- [ ] **UPDATE:** Atualizar dados do usuário
- [ ] **DELETE:** Desativar usuário (soft delete)

### 5.3 Testes de Pacientes

- [ ] **CREATE:** Cadastrar novo paciente com CPF e SUS válidos
- [ ] **READ:** Buscar paciente por ID
- [ ] **LIST:** Listar todos os pacientes
- [ ] **UPDATE:** Atualizar dados do paciente
- [ ] **DELETE:** Desativar paciente
- [ ] **Validação:** Testar CPF inválido (deve falhar com 400)
- [ ] **Validação:** Testar SUS inválido — não 15 dígitos (deve falhar com 400)

### 5.4 Testes de Profissionais

- [ ] **CREATE:** Cadastrar profissional com especialidade válida
- [ ] **READ:** Buscar profissional por ID
- [ ] **LIST:** Listar todos os profissionais
- [ ] **UPDATE:** Atualizar especialidade/CRM
- [ ] **DELETE:** Desativar profissional

### 5.5 Testes de Serviços

- [ ] **CREATE:** Cadastrar serviço com duração > 0 e preço >= 0
- [ ] **READ:** Buscar serviço por ID
- [ ] **LIST:** Listar todos os serviços
- [ ] **UPDATE:** Atualizar preço e duração
- [ ] **DELETE:** Desativar serviço

### 5.6 Testes de Agendamentos (Fluxo Completo)

**Pré-requisito:** Ter paciente, profissional e serviço cadastrados

- [ ] **CREATE:** Agendar consulta (status = PENDING)
- [ ] **CONFIRM:** Confirmar agendamento (status = CONFIRMED)
- [ ] **CHECK-IN:** Registrar presença (status = ARRIVED)
- [ ] **NEXT:** Chamar próximo paciente da fila (status = CALLING, retorna appointmentId) — **obrigatório antes de START**
- [ ] **START:** Iniciar atendimento (status = IN_PROGRESS, só a partir de CALLING)
- [ ] **COMPLETE:** Completar consulta com evolução clínica (status = COMPLETED)
- [ ] **CANCEL (fluxo alternativo):** Cancelar agendamento em PENDING/CONFIRMED
- [ ] **LIST com filtros:** Testar `professionalId`, `patientId`, `fromDate`, `toDate`

### 5.7 Testes de Prontuário

**Pré-requisito:** Ter agendamento IN_PROGRESS e depois COMPLETED

- [ ] **GET:** Consultar prontuário do paciente
- [ ] **CREATE:** Registrar evolução clínica durante agendamento IN_PROGRESS
- [ ] **UPDATE:** Editar evolução clínica enquanto status ainda é IN_PROGRESS
- [ ] **Bloqueio:** Tentar editar entrada com agendamento já COMPLETED (deve retornar **HTTP 409**)

### 5.8 Testes de Auditoria

- [ ] **LIST:** Listar todos os logs de auditoria
- [ ] **Validação:** Confirmar que CREATE/UPDATE/DELETE aparecem nos logs com timestamp, usuário e IP

### 5.9 Testes de Controle de Acesso (RBAC)

**Usar os usuários criados em 5.1 para cada role e validar permissões:**

- [ ] **ADMIN:** Acesso total (criar/editar/deletar usuários, listar auditoria)
- [ ] **RECEPTIONIST:** Pode gerenciar agendamentos e listar pacientes/profissionais/serviços (não pode criar usuários)
- [ ] **PROFESSIONAL:** Pode ler/escrever prontuário, chamar próximo paciente, iniciar/completar atendimento
- [ ] **ASSISTANT:** Pode gerenciar pacientes (não pode gerenciar profissionais/serviços)
- [ ] **PATIENT:** Pode agendar/cancelar consulta própria e ler prontuário próprio
- [ ] **Negação:** Tentar acessar endpoint protegido sem token (deve retornar **HTTP 401**)
- [ ] **Negação:** Usar token de RECEPTIONIST para acessar endpoint exclusivo de ADMIN (deve retornar **HTTP 403**)

### 5.10 Testes de Validação

- [ ] **Email inválido:** Tentar criar usuário/paciente com email malformado (deve falhar)
- [ ] **CPF duplicado:** Tentar criar dois pacientes com mesmo CPF (deve falhar)
- [ ] **Campo obrigatório:** Omitir campo obrigatório (deve falhar com mensagem específica no campo `fields`)
- [ ] **Duração negativa:** Tentar criar serviço com duração < 1 minuto (deve falhar)
- [ ] **Preço negativo:** Tentar criar serviço com preço < 0 (deve falhar)
- [ ] **Data retroativa:** Tentar agendar para data no passado (deve falhar)

### 5.11 Cenários Reais de Ponta a Ponta

**Cenário 1: Agendamento Normal**
1. Recepcionista agenda consulta (PENDING)
2. Recepcionista confirma (CONFIRMED)
3. Paciente chega e faz check-in (CHECKED_IN)
4. Profissional chama próximo paciente
5. Profissional inicia atendimento (IN_PROGRESS)
6. Profissional conclui com evolução clínica (COMPLETED)
7. Validar que evolução está no prontuário do paciente

**Cenário 2: Cancelamento**
1. Agendar consulta
2. Confirmar
3. Cancelar com motivo
4. Validar status = CANCELLED

**Cenário 3: Imutabilidade de Prontuário**
1. Criar agendamento e completar com evolução clínica
2. Tentar editar a entrada depois do COMPLETED (deve retornar **HTTP 409**)

---

## 🔗 Parte 6: URLs Importantes

| Recurso | URL |
|---------|-----|
| **Swagger UI** | http://localhost:8080/swagger-ui.html |
| **API Base** | http://localhost:8080/api/v1 |
| **Login** | `POST /api/v1/auth/login` |
| **Health Check** | http://localhost:8080/actuator/health |

---

## 🔎 Parte 7: Referência Rápida de Endpoints

### Autenticação
- `POST /api/v1/auth/login` — Login com email/senha

### Usuários (`ADMIN`)
- `POST /api/v1/users` — Criar usuário
- `GET /api/v1/users/{id}` — Buscar usuário por UUID
- `GET /api/v1/users` — Listar usuários ativos
- `PUT /api/v1/users/{id}` — Atualizar usuário
- `DELETE /api/v1/users/{id}` — Desativação lógica (soft delete)

### Pacientes (`ADMIN`, `ASSISTANT`; leitura também `RECEPTIONIST`, `PROFESSIONAL`)
- `POST /api/v1/patients` — Cadastrar paciente (valida CPF e e-mail)
- `GET /api/v1/patients/{id}` — Buscar paciente por UUID
- `GET /api/v1/patients` — Listar pacientes ativos
- `PUT /api/v1/patients/{id}` — Atualizar dados cadastrais
- `DELETE /api/v1/patients/{id}` — Desativação lógica (soft delete)

### Profissionais (`ADMIN`; leitura também `RECEPTIONIST`)
- `POST /api/v1/professionals` — Cadastrar profissional (CRM, especialidade)
- `GET /api/v1/professionals/{id}` — Buscar profissional por UUID
- `GET /api/v1/professionals` — Listar profissionais ativos
- `PUT /api/v1/professionals/{id}` — Atualizar dados cadastrais
- `DELETE /api/v1/professionals/{id}` — Desativação lógica (soft delete)

### Serviços (`ADMIN`; leitura também `RECEPTIONIST`, `PATIENT`)
- `POST /api/v1/services` — Cadastrar serviço/procedimento
- `GET /api/v1/services/{id}` — Buscar serviço por UUID
- `GET /api/v1/services` — Listar serviços ativos
- `PUT /api/v1/services/{id}` — Atualizar serviço
- `DELETE /api/v1/services/{id}` — Desativação lógica (soft delete)

### Agendamentos e Fila Presencial
- `POST /api/v1/appointments` — Agendar (`RECEPTIONIST`, `PATIENT` próprio)
- `GET /api/v1/appointments/{id}` — Buscar por UUID
- `GET /api/v1/appointments` — Listar com filtros (professionalId, patientId, fromDate, toDate)
- `PUT /api/v1/appointments/{id}/confirm` — Confirmar (`RECEPTIONIST`)
- `PUT /api/v1/appointments/{id}/cancel` — Cancelar (`RECEPTIONIST`, `PATIENT` próprio)
- `PUT /api/v1/appointments/{id}/check-in` — Check-in com validação documental (`RECEPTIONIST`)
- `POST /api/v1/appointments/next` — Chamar próximo paciente da fila (`PROFESSIONAL`)
- `PUT /api/v1/appointments/{id}/start` — Iniciar atendimento (`PROFESSIONAL`)
- `PUT /api/v1/appointments/{id}/complete` — Concluir com evolução clínica obrigatória (`PROFESSIONAL`)

### Prontuário e Registro Clínico (`PROFESSIONAL`, exclusivo — auditado)
- `GET /api/v1/medical-records/patients/{patientId}` — Consultar prontuário (mais recente primeiro)
- `POST /api/v1/medical-records/entries` — Registrar evolução de agendamento `IN_PROGRESS`
- `PUT /api/v1/medical-records/entries/{entryId}` — Editar evolução enquanto `IN_PROGRESS`; após `COMPLETED` é imutável (**HTTP 409**)

### Auditoria (`ADMIN`, exclusivo)
- `GET /api/v1/audit-logs` — Listar logs ordenados por data decrescente

---

## 📝 Parte 8: Dados de Teste Pré-configurados

### Usuário Admin (Pré-criado no banco via migration V10)
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "email": "john.nobody@email.com",
  "password": "SenhaForte123!",
  "name": "John Admin",
  "role": "ADMIN",
  "active": true
}
```

### CPFs Válidos para Testes (Formato: 11 dígitos)
```
12345678910    - João da Silva
98765432109    - Maria da Silva
55555555555    - Teste genérico
```

### Especialidades de Profissionais
```
CARDIOLOGY - Cardiologia
DERMATOLOGY - Dermatologia
ORTHOPEDICS - Ortopedia
PEDIATRICS - Pediatria
PSYCHIATRY - Psiquiatria
GENERAL_PRACTICE - Clínica Geral
```

### Prioridades de Agendamento
```
NORMAL - Prioridade normal (score: 100)
HIGH - Prioridade alta (score: 200)
URGENT - Urgência (score: 300)
```

### Status de Agendamento
```
PENDING - Agendado, aguardando confirmação
PENDING_RESPONSE - Aguardando resposta do paciente (ex: lista de espera)
CONFIRMED - Confirmado pela recepção
ARRIVED - Paciente fez check-in (chegou)
CALLING - Chamado pelo profissional via /appointments/next
IN_PROGRESS - Atendimento em andamento
COMPLETED - Atendimento finalizado
CANCELLED - Cancelado
NO_SHOW - Paciente não compareceu
```

---

## ⚠️ Parte 9: Erros Comuns e Soluções

| Erro | Causa | Solução |
|------|-------|---------|
| `401 Unauthorized` | Token ausente ou expirado | Fazer novo login, obter novo token |
| `403 Forbidden` | Usuário sem permissão (`@PreAuthorize`) para o endpoint | Verificar role do usuário |
| `400 Bad Request` | Validação falhou | Verificar formato dos campos obrigatórios (ver campo `fields` na resposta) |
| `404 Not Found` | Recurso não existe | Verificar UUID do recurso |
| `409 Conflict` | Tentativa de editar entrada de prontuário com agendamento já COMPLETED | Não é permitido; criar novo agendamento se necessário |
| `Database Connection Error` | PostgreSQL não está rodando | Iniciar PostgreSQL ou Docker |

### Formato Padrão de Erro (GlobalExceptionHandler)

```json
{
  "timestamp": "2026-06-27T14:00:00.123Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/patients",
  "fields": {
    "cpf": "O CPF informado é inválido"
  }
}
```

---

## 🎯 Conclusão

Este documento fornece **instrução completa e única** para uma IA testar a aplicação SAAP MVP via
frontend/API. A IA deve:

1. Verificar a aplicação na **Parte 1**
2. Autenticar na **Parte 2**
3. Exercitar todos os endpoints na **Parte 4** (CREATE, READ, UPDATE, DELETE em cada entidade, com todos os 5 atores)
4. Executar o checklist completo da **Parte 5**
5. Documentar o resultado de cada teste (sucesso/falha, status HTTP retornado)

**Tempo estimado:** 2-4 horas para a suíte completa (incluindo validações e edge cases)

---

**Gerado em:** 2026-07-03
**Atualizado em:** 2026-07-04 — removido setup/build (não aplicável ao teste via frontend), corrigidos status de agendamento (ARRIVED/CALLING em vez de CHECKED_IN) e passo obrigatório `/appointments/next` antes de `/start`, adicionado `userId` obrigatório na criação de profissional para o fluxo de prontuário
**Versão:** SAAP MVP v1.1
**Autor:** Sistema de Documentação Automática
