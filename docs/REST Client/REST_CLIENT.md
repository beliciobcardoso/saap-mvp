# Guia de Testes com REST Client — SAAP MVP

Este documento descreve a convenção adotada para testar os endpoints da API usando arquivos `.http`
(compatíveis com IntelliJ IDEA HTTP Client e VS Code REST Client).

---

## Estrutura dos Arquivos

Cada controller possui seu próprio arquivo `.http` dentro de `docs/REST Client/`:

```
docs/REST Client/
├── Auth.http           # POST /auth/login (público), POST /auth/logout (autenticado)
├── Users.http          # CRUD /users     (ADMIN)
├── Patients.http       # CRUD /patients  (ADMIN, RECEPTIONIST)
├── Professionals.http  # CRUD /professionals (ADMIN / ADMIN+RECEPTIONIST p/ leitura)
├── Services.http       # CRUD /services  (ADMIN / ADMIN+RECEPTIONIST p/ leitura)
├── Appointments.http   # Fluxos /appointments (ADMIN, RECEPTIONIST, PROFESSIONAL, PATIENT) + waitlist público
├── MedicalRecords.http # Prontuário /medical-records (PROFESSIONAL)
├── AuditLogs.http      # Logs /audit-logs paginados (ADMIN)
├── FullTest.http        # Fluxo end-to-end completo (todos os recursos)
└── REST_CLIENT.md      # Este guia
```

> **Regra:** Quando um novo controller for criado, crie o arquivo `.http` correspondente
> seguindo a mesma convenção descrita aqui.

---

## Pré-requisito: Bootstrap do Primeiro Usuário (banco limpo)

A API **não tem endpoint de self-register** (`AuthController` só expõe `/login` e `/logout`) e o
repositório **não tem seed de usuários via Flyway**. Isso cria um problema de ovo-e-galinha:
`POST /users` exige token de ADMIN, mas num banco recém-criado não existe nenhum ADMIN ainda para
gerar esse token.

Sempre que for testar contra um banco novo (ex.: primeira subida do `docker compose up`, banco
limpo local), siga esta sequência **nesta ordem**, antes de rodar qualquer outro arquivo `.http`:

### Passo 0 — Criar o primeiro ADMIN direto no banco (via SQL)

Necessário só uma vez por banco. Gere o hash bcrypt da senha:

```bash
python3 -c "import bcrypt; print(bcrypt.hashpw(b'SenhaForte123!', bcrypt.gensalt(10)).decode())"
```

Copie o hash gerado e insira na tabela `usuario` (ajuste o nome do serviço/container do Postgres
se for diferente de `postgres`):

```bash
docker compose exec -T postgres psql -U postgres -d saap_db -c "
INSERT INTO usuario (id, email, password, role, is_active, created_at, updated_at)
VALUES (gen_random_uuid(), 'john.nobody@email.com', '<HASH_GERADO_AQUI>', 'ADMIN', true, now(), now());
"
```

> `gen_random_uuid()` já está disponível pelas migrações do projeto (extensão `pgcrypto`). Se der
> erro de função inexistente, gere o UUID à parte (`uuidgen`) e cole o valor literal.

### Passo 1 — Logar com o ADMIN recém-criado

Rode a requisição `login_admin` de `Auth.http` com:
- E-mail: `john.nobody@email.com`
- Senha: `SenhaForte123!`

Isso popula `@token` com um JWT válido de ADMIN — usado como `Authorization: Bearer {{token}}`
pelos demais arquivos.

### Passo 2 — Criar os demais usuários de teste via API

Com o `@token` do Passo 1, use `POST /users` (ver `Users.http`) para criar cada usuário abaixo.
Todos são usados como **login** em algum arquivo `.http` da pasta — sem eles, o login naquele
arquivo falha com `401`:

| E-mail                  | Senha            | Role           | Usado em (login)                                           |
|--------------------------|------------------|----------------|-------------------------------------------------------------|
| `john.nobody@email.com` | `SenhaForte123!` | `ADMIN`        | Auth, Users, Patients, Professionals, Services, Appointments, MedicalRecords, AuditLogs |
| `ana.lima@saap.com`     | `Recepcao123!`   | `RECEPTIONIST` | Appointments, AuditLogs                                      |
| `dr.carlos@saap.com`    | `Medico123!`     | `PROFESSIONAL` | MedicalRecords                                               |
| `admin@saap.com`        | `adminPass123`   | `ADMIN`        | FullTest                                                     |
| `recep@saap.com`        | `password123`    | `RECEPTIONIST` | FullTest                                                     |
| `dr.pedro@saap.com`     | `Medico123!`     | `PROFESSIONAL` | FullTest                                                     |

Exemplo de request (repita trocando `email`/`password`/`role` pelos valores da tabela):

```http
POST {{baseUrl}}/users HTTP/1.1
Authorization: Bearer {{token}}
Content-Type: application/json

{
    "email": "ana.lima@saap.com",
    "password": "Recepcao123!",
    "role": "RECEPTIONIST"
}
```

### Passo 3 — Agora sim, rodar os arquivos `.http`

Com os 6 usuários da tabela acima existindo no banco, todos os arquivos da pasta podem ser
executados na ordem normal (login → CRUD → testes de segurança), incluindo `FullTest.http`.

> **Atenção — `FullTest.http` tem dois blocos `# @name login_admin`** (um com
> `john.nobody@email.com`, outro com `admin@saap.com`). O HTTP Client resolve
> `{{login_admin.response.body.token}}` para a **última** requisição executada com esse nome — o
> token usado no resto do arquivo acaba sendo o de `admin@saap.com`. Não quebra nada, mas é sutil;
> ao reescrever o arquivo prefira um `@name` único por login.

### Outros e-mails que aparecem nos arquivos (não precisam existir antes)

Estes são criados pela própria requisição do arquivo em que aparecem, ou são casos de teste
negativo — **não** devem ser pré-criados:

| E-mail                                                                              | Origem                                                              |
|--------------------------------------------------------------------------------------|-----------------------------------------------------------------------|
| `dr.joao@saap.com`, `dr.joao.jr@saap.com`                                            | Criados por `POST /professionals` dentro de `Professionals.http`     |
| `joao.pereira@email.com`, `maria.silva@email.com`, `maria.santos@email.com`, `negado@saap.com` | Pacientes criados por `POST /patients` dentro de `Patients.http`/`FullTest.http` |
| `recepcionista@saap.com`, `recepcionista.atualizado@saap.com`                        | Criado (POST) e depois renomeado (PUT) dentro de `Users.http`        |
| `incompleto@email.com`, `incompleto@saap.com`                                        | Teste negativo — payload incompleto, espera `400`                    |
| `nao.existe@email.com`                                                               | Teste negativo — login esperado `401`                                 |

---

## Convenção de Estrutura de um Arquivo `.http`

Cada arquivo segue este template padrão:

```http
### ==============================================================
### SAAP MVP - [Recurso]
### Base URL: http://localhost:8080/api/v1
### Rota base: /[recurso]
### Roles necessárias: [roles que têm acesso]
### ==============================================================

@hostname = http://localhost
@port = 8080
@host = {{hostname}}:{{port}}
@baseUrl = {{host}}/api/v1

###
# @name login_admin
POST {{baseUrl}}/auth/login HTTP/1.1
Content-Type: application/json

{
    "email": "john.nobody@email.com",
    "password": "SenhaForte123!"
}

@token = {{login_admin.response.body.token}}

### ==============================================================
### CRUD - [RECURSO]
### ==============================================================

###
# Criar novo [recurso]
# Esperado: 201 Created
# @name create_[recurso]
POST {{baseUrl}}/[recurso] HTTP/1.1
Authorization: Bearer {{token}}
Content-Type: application/json

{ ... }

@createdId = {{create_[recurso].response.body.id}}

### ... demais operações GET, PUT, DELETE

### ==============================================================
### TESTES DE SEGURANÇA
### ==============================================================

### ... casos de 401, 404, 400
```

---

## Fluxo de Execução

Para testar um recurso completo, execute as requisições **nesta ordem**:

1. **`login_admin`** — obtém o JWT e popula `@token` e `@createdId` automaticamente
2. **`create_[recurso]`** — cria o registro e captura o `id` retornado em `@createdId`
3. **`GET por ID`** — verifica se o registro foi criado corretamente
4. **`GET listar`** — verifica a listagem
5. **`PUT atualizar`** — testa a atualização
6. **`DELETE desativar`** — testa o soft-delete (retorna 204, não remove do banco)
7. **Testes de segurança** — testa 401, 404 e 400

---

## Captura Automática de Variáveis

O HTTP Client suporta captura automática de valores da resposta via `@name` + referência:

```http
# @name login_admin
POST {{baseUrl}}/auth/login HTTP/1.1
...

# Captura o token da resposta do "login_admin":
@token = {{login_admin.response.body.token}}

# @name create_patient
POST {{baseUrl}}/patients HTTP/1.1
...

# Captura o id do paciente criado:
@createdPatientId = {{create_patient.response.body.id}}
```

Isso elimina a necessidade de copiar e colar manualmente IDs e tokens entre as requisições.

---

## Mapeamento de Endpoints e Roles

| Método | Rota                        | Roles permitidas                  |
|--------|-----------------------------|-----------------------------------|
| POST   | `/auth/login`               | Pública (sem autenticação)        |
| GET    | `/users`                    | ADMIN                             |
| GET    | `/users/{id}`               | ADMIN                             |
| POST   | `/users`                    | ADMIN                             |
| PUT    | `/users/{id}`               | ADMIN                             |
| DELETE | `/users/{id}`               | ADMIN                             |
| GET    | `/patients`                 | ADMIN, RECEPTIONIST               |
| GET    | `/patients/{id}`            | ADMIN, RECEPTIONIST               |
| POST   | `/patients`                 | ADMIN, RECEPTIONIST               |
| PUT    | `/patients/{id}`            | ADMIN, RECEPTIONIST               |
| DELETE | `/patients/{id}`            | ADMIN, RECEPTIONIST               |
| GET    | `/professionals`            | ADMIN, RECEPTIONIST               |
| GET    | `/professionals/{id}`       | ADMIN, RECEPTIONIST               |
| POST   | `/professionals`            | ADMIN                             |
| PUT    | `/professionals/{id}`       | ADMIN                             |
| DELETE | `/professionals/{id}`       | ADMIN                             |
| GET    | `/services`                 | ADMIN, RECEPTIONIST               |
| GET    | `/services/{id}`            | ADMIN, RECEPTIONIST               |
| POST   | `/services`                 | ADMIN                             |
| PUT    | `/services/{id}`            | ADMIN                             |
| DELETE | `/services/{id}`            | ADMIN                             |
| POST   | `/appointments`             | ADMIN, RECEPTIONIST, PATIENT      |
| GET    | `/appointments`             | ADMIN, RECEPTIONIST, PROFESSIONAL, PATIENT |
| GET    | `/appointments/{id}`        | ADMIN, RECEPTIONIST, PROFESSIONAL, PATIENT |
| PUT    | `/appointments/{id}/confirm` | ADMIN, RECEPTIONIST               |
| PUT    | `/appointments/{id}/cancel`  | ADMIN, RECEPTIONIST, PATIENT      |
| PUT    | `/appointments/{id}/check-in`| ADMIN, RECEPTIONIST               |
| POST   | `/appointments/next`        | PROFESSIONAL                      |
| PUT    | `/appointments/{id}/start`   | ADMIN, PROFESSIONAL               |
| PUT    | `/appointments/{id}/complete`| ADMIN, PROFESSIONAL              |
| GET    | `/appointments/public/confirm` | Pública (sem autenticação)     |
| GET    | `/appointments/public/cancel`  | Pública (sem autenticação)     |
| GET    | `/appointments/public/waitlist/accept` | Pública (sem autenticação) |
| GET    | `/appointments/public/waitlist/decline`| Pública (sem autenticação) |
| POST   | `/auth/logout`                 | Qualquer role autenticada         |
| GET    | `/audit-logs?page=&size=`      | ADMIN (resposta paginada, `PageResponseDTO`) |
| GET    | `/medical-records/patients/{patientId}` | PROFESSIONAL             |
| POST   | `/medical-records/entries`     | PROFESSIONAL (só com agendamento IN_PROGRESS) |
| PUT    | `/medical-records/entries/{entryId}` | PROFESSIONAL (409 se agendamento já COMPLETED) |

---

## Respostas HTTP Esperadas

| Situação                             | Status HTTP |
|--------------------------------------|-------------|
| Recurso criado com sucesso           | `201 Created` |
| Operação bem-sucedida com resposta   | `200 OK` |
| Soft-delete (desativação)            | `204 No Content` |
| Request inválido / validação falhou  | `400 Bad Request` |
| Sem autenticação (sem token)         | `401 Unauthorized` |
| Token válido mas sem permissão       | `403 Forbidden` |
| Recurso não encontrado               | `404 Not Found` |
| Conflito de estado (ex: evolução clínica após COMPLETED, transição de status inválida) | `409 Conflict` |

---

## Adicionando um Novo Endpoint

Quando um novo controller for implementado:

1. Crie o arquivo `docs/REST Client/[Recurso].http`
2. Siga o template da seção **"Convenção de Estrutura"**
3. Inclua:
   - O bloco de login no topo
   - CRUD completo (POST, GET por ID, GET lista, PUT, DELETE)
   - Ao menos 3 testes de segurança (sem token, ID inválido, body inválido)
4. Atualize a tabela **"Mapeamento de Endpoints e Roles"** neste documento
