# Guia de Testes com REST Client — SAAP MVP

Este documento descreve a convenção adotada para testar os endpoints da API usando arquivos `.http`
(compatíveis com IntelliJ IDEA HTTP Client e VS Code REST Client).

---

## Estrutura dos Arquivos

Cada controller possui seu próprio arquivo `.http` dentro de `docs/REST Client/`:

```
docs/REST Client/
├── Auth.http           # POST /auth/login (público)
├── Users.http          # CRUD /users     (ADMIN)
├── Patients.http       # CRUD /patients  (ADMIN, RECEPTIONIST)
├── Professionals.http  # CRUD /professionals (ADMIN / ADMIN+RECEPTIONIST p/ leitura)
├── Services.http       # CRUD /services  (ADMIN / ADMIN+RECEPTIONIST p/ leitura)
├── Appointments.http   # Fluxos /appointments (ADMIN, RECEPTIONIST, PROFESSIONAL, PATIENT)
└── REST_CLIENT.md      # Este guia
```

> **Regra:** Quando um novo controller for criado, crie o arquivo `.http` correspondente
> seguindo a mesma convenção descrita aqui.

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

## Usuário de Teste Padrão

| Campo    | Valor                      |
|----------|----------------------------|
| E-mail   | `john.nobody@email.com`    |
| Senha    | `SenhaForte123!`           |
| Role     | `ADMIN`                    |

> Este usuário foi inserido via seeding no banco de dados local.
> Para ambientes de teste (Testcontainers), os testes criam seus próprios usuários via `@BeforeEach`.

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
| PUT    | `/appointments/{id}/complete`| ADMIN, PROFESSIONAL               |

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
