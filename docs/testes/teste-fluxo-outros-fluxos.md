# Teste dos Demais Fluxos (via Swagger/OpenAPI)

> **Data:** 04/07/2026
> **Objetivo:** Validar os fluxos não cobertos em [teste-fluxo-follow-up.md](./teste-fluxo-follow-up.md) (RF03/RF06): fila de espera (waitlist), CRUDs de domínio, prontuário, audit-logs e autenticação.
> **Método:** endpoints identificados via `GET /v3/api-docs` (Swagger), executados contra a instância real da aplicação (porta 8080, banco `postgis`).

> **Legenda:** `✅` = confirmado OK, ou achado já corrigido e revalidado. `ℹ️` = observação informativa, não é bug. **Nenhum item deste documento está em aberto** — todo achado marcado como problema foi corrigido e testado novamente (ver [Correções Aplicadas](#-correções-aplicadas-04072026)).

---

## 📋 Resumo dos Testes

| # | Fluxo | Resultado |
|---|-------|-----------|
| 1 | Fila de espera — oferta automática + aceite | ✅ **SUCESSO** |
| 2 | Fila de espera — oferta automática + recusa | ✅ **SUCESSO** |
| 3 | CRUD Pacientes | ✅ **SUCESSO** |
| 4 | CRUD Serviços | ✅ **SUCESSO** |
| 5 | CRUD Usuários (+ login/logout) | ✅ **SUCESSO** |
| 6 | CRUD Profissionais | ✅ **SUCESSO** (achado corrigido — ver seção 5) |
| 7 | Prontuário (listar/editar entrada) | ✅ **SUCESSO** |
| 8 | Audit Logs | ✅ **SUCESSO** |
| 9 | Logout com blacklist de token | ✅ **SUCESSO** |

---

## 🧪 Pré-requisitos

Mesmos do [teste-fluxo-follow-up.md](./teste-fluxo-follow-up.md), mais:

- **Profissional (login):** `dr.carlos@saap.com` / `Medico123!`
- Tokens de ação (fila de espera) gerados manualmente com PyJWT (HS256), reaproveitando o mesmo `ACTION_TOKEN_SECRET` — ver detalhe na seção 1.

---

## 1️⃣ Fluxo de Fila de Espera (Waitlist)

Não existe endpoint de criação pública de entrada na fila — ela é inserida diretamente (`INSERT INTO fila_espera ... status='WAITING'`) e a oferta é disparada automaticamente pelo `ProcessWaitlistSlotOfferUseCase` quando um agendamento do mesmo profissional+serviço é **cancelado via admin** (`PUT /appointments/{id}/cancel`).

### Cenário 1: Aceite

| Etapa | Ação | Resultado |
|-------|------|-----------|
| 1 | Criar agendamento `ee074249-...` (07/07 09:00) | ✅ `201` `PENDING` |
| 2 | Inserir entrada `WAITING` na fila (mesmo profissional/serviço) | ✅ |
| 3 | `PUT /appointments/ee074249.../cancel` (Admin) | ✅ `200` → `CANCELLED` |
| 4 | Verificar fila → `OFFERED`, `offered_appointment_time = 07/07 09:00` | ✅ |
| 5 | Gerar token `accept-waitlist` (sub = id da entrada) e chamar `GET /appointments/public/waitlist/accept?token=` | ✅ `200` — "Vaga da fila de espera aceita e agendamento confirmado com sucesso!" |
| 6 | Verificar: entrada → `ACCEPTED`; **novo agendamento** `df47ef8d-...` criado como `CONFIRMED` | ✅ |
| 7 | Reaceitar mesmo token (oferta não mais ativa) | ✅ `400 Bad Request` — "Esta oferta de vaga não está mais ativa" |

### Cenário 2: Recusa

| Etapa | Ação | Resultado |
|-------|------|-----------|
| 1-4 | Repetir 1-4 acima com agendamento `3654f3fc-...` (07/07 10:00) e entrada `01e81085-...` | ✅ `OFFERED` |
| 5 | Gerar token `decline-waitlist` e chamar `GET /appointments/public/waitlist/decline?token=` | ✅ `200` |
| 6 | Token inválido | ✅ `400 Bad Request` |
| 7 | Redeclinar mesmo token | ✅ `400 Bad Request` |

> **✅ Achado (já corrigido — ver [Correções Aplicadas](#-correções-aplicadas-04072026), item 1):** no teste original, o fluxo de fila de espera retornava `400 Bad Request` tanto para token inválido quanto para oferta já processada, diferente do fluxo de agendamento (RF03), que já retornava `409 Conflict` para estado incorreto. Corrigido: waitlist agora também retorna `409` pra erro de estado, `400` só pra token inválido — validado em [Fechamento dos Gaps Pendentes](#-fechamento-dos-gaps-pendentes-04072026-continuação).

> **Estrutura do token gerado:** idêntica ao token de follow-up — HS256, `iss=saap-action-token`, `sub=<waitlistEntryId>`, `action=accept-waitlist|decline-waitlist`, `exp=+24h` — porém o campo de negócio que efetivamente expira a oferta é `offer_expires_at` na tabela `fila_espera` (janela de ~30 min a partir da oferta), validado à parte do JWT.

---

## 2️⃣ CRUD Pacientes (`/api/v1/patients`)

| Operação | Resultado |
|----------|-----------|
| `POST` com CPF duplicado | ✅ `409 Conflict` — "CPF já cadastrado" |
| `POST` com CPF válido novo | ✅ `201 Created` |
| `GET /{id}` | ✅ `200 OK` |
| `PUT /{id}` | ✅ `200 OK` |
| `DELETE /{id}` | ✅ `204 No Content` |
| `GET /{id}` pós-delete | ✅ `200 OK`, `"active": false` |

> **Observação:** delete de paciente é **soft-delete** (registro permanece consultável com `active:false`).

---

## 3️⃣ CRUD Serviços (`/api/v1/services`)

`POST` → `201` · `GET` → `200` · `PUT` → `200` · `DELETE` → `204`. Sem divergências.

---

## 4️⃣ CRUD Usuários (`/api/v1/users`) + Autenticação

| Operação | Resultado |
|----------|-----------|
| `GET /users` (listar) | ✅ `200 OK` |
| `POST /users` (role `RECEPTIONIST`) | ✅ `201 Created` |
| Login com credenciais recém-criadas | ✅ `200 OK`, token válido |
| `GET /users/{id}` | ✅ `200 OK` |
| `DELETE /users/{id}` | ✅ `204 No Content` |
| Login pós-delete | ✅ `401 Unauthorized` — "Usuário inexistente ou senha inválida" |

> **✏️ Correção (04/07/2026):** a observação original deste teste dizia "hard-delete". **Estava errada.** Confirmado no código (`DeactivateUserUseCase` chama `user.deactivate()` + `save()`, nunca `.delete()`) e no banco: os 3 usuários de teste "deletados" nesta sessão continuam na tabela `usuario` com `is_active = false`. É **soft-delete**, igual a pacientes/serviços/profissionais — o `401` no login pós-delete acontece porque o login checa `active=true`, não porque o registro sumiu. O engano original veio de inferir o mecanismo só pela resposta HTTP (`204` + login falhando), sem checar o banco.

---

## 5️⃣ CRUD Profissionais (`/api/v1/professionals`)

| Operação | Resultado |
|----------|-----------|
| `POST` (vinculado a `userId` de usuário `PROFESSIONAL`) | ✅ `201 Created` |
| `GET /{id}` | ✅ `200 OK` |
| `PUT /{id}` **sem** campo `userId` no corpo | ✅ `200 OK` — vínculo preservado (teste original tinha achado bug aqui, já corrigido) |
| `DELETE /{id}` | ✅ `204 No Content` |

> **✅ Achado já corrigido (04/07/2026):** no teste original, `PUT /professionals/{id}` tratava o corpo como substituição completa e omitir `userId` desvinculava o profissional do usuário de login. Corrigido em `UpdateProfessionalUseCase.java` (ver [Correções Aplicadas](#-correções-aplicadas-04072026), item 2) — agora só sobrescreve `userId` se o campo vier preenchido. Reexecutei o teste: vínculo preservado quando o campo é omitido.

---

## 6️⃣ Prontuário (`/api/v1/medical-records`)

| Operação | Resultado |
|----------|-----------|
| `GET /medical-records/patients/{patientId}` com token **ADMIN** | ✅ `403 Forbidden` (teste original tinha achado bug aqui — retornava `401`, já corrigido) |
| `GET /medical-records/patients/{patientId}` com token **PROFESSIONAL** | ✅ `200 OK` |
| `PUT /medical-records/entries/{entryId}` com atendimento **não** `IN_PROGRESS` | ✅ `409 Conflict` — "Evolução clínica imutável: o atendimento não está mais em andamento (IN_PROGRESS)" |
| `PUT /medical-records/entries/{entryId}` com atendimento **`IN_PROGRESS`** | ✅ `200 OK` — edição aplicada |

> **✅ Achado já corrigido (04/07/2026):** endpoints de prontuário são restritos à role `PROFESSIONAL` por `hasRole()` direto no `SecurityConfig` (não por `@PreAuthorize`) — Admin recebia `401` em vez de `403`. Corrigido — ver achado #3 na seção [Achados Originais](#-achados-originais-verificados-no-código-fonte-antes-da-correção) (bug era restrito a essa rota e `/actuator/**`, não à API inteira) e reconfirmado em `403` na validação pós-fix.

---

## 7️⃣ Audit Logs (`/api/v1/audit-logs`)

`GET /audit-logs` (Admin) → ✅ `200 OK`, retorna eventos como `FINALIZACAO_ATENDIMENTO`, `MEDICAL_RECORD_*` com `userId`, `userEmail`, `recursoId`, `recursoTipo`, `ipAddress`.

---

## 8️⃣ Logout e Blacklist de Token (`/api/v1/auth/logout`)

| Etapa | Resultado |
|-------|-----------|
| `POST /auth/logout` com token válido | ✅ `204 No Content` |
| Reusar o mesmo token em qualquer endpoint autenticado | ✅ `401 Unauthorized` — "Não autorizado: token ausente ou inválido" |

Confirma que o `TokenBlacklistService` invalida o JWT imediatamente após logout, mesmo dentro da janela de expiração original.

---

## ✅ Correções Aplicadas (04/07/2026)

Os 3 achados abaixo foram corrigidos e revalidados:

| # | Arquivo | Mudança |
|---|---------|---------|
| 1 | `AppointmentController.java` | `publicWaitlistAccept`/`publicWaitlistDecline` agora têm o mesmo try/catch de `publicConfirm`/`publicCancel` (`IllegalArgumentException→400`, `IllegalStateException→409`) |
| 2 | `UpdateProfessionalUseCase.java` | `existing.setUserId(...)` só executa se `updated.getUserId() != null` — omitir o campo no PUT preserva o vínculo |
| 3 | `SecurityConfig.java` | Adicionado `.accessDeniedHandler(...)` retornando `403` JSON — corrige `/actuator/**` e `/medical-records/**` (únicas 2 rotas com `hasRole()` direto no matcher de URL); endpoints via `@PreAuthorize` (maioria da API) já retornavam `403` corretamente antes do fix — ver correção de escopo na seção de achados |

**Validação pós-fix:**
- Reexecutado cenário de waitlist (accept + reaceitar) → estado incorreto agora retorna `409` (era `400`); token inválido continua `400`.
- `PUT /professionals/{id}` sem `userId` → vínculo preservado (era zerado).
- Role errada em `/actuator/env` (ADMIN) e em `/medical-records/patients/{id}` (ADMIN tentando endpoint `PROFESSIONAL`) → ambos agora `403` (eram `401`); token ausente/blacklisted continua `401` (comportamento preservado).
- Regressão completa RF03 (confirm + cancel) e RF06 (check-in → complete) → sem alterações de comportamento.
- Suíte automatizada: `mvn test` → **272/272 testes, 0 falhas, `BUILD SUCCESS`** (inclui `SecurityIntegrationTest`, `WaitlistPublicControllerIntegrationTest`, `UpdateProfessionalUseCaseTest`).

---

## 📌 Achados Originais (verificados no código-fonte antes da correção)

> Números de linha abaixo refletem o estado do código **antes** dos fixes da seção anterior — podem estar deslocados em 1-2 linhas na versão atual (o código cresceu com os try/catch e o null-check adicionados). Buscar pelo nome do método/campo em vez do número exato de linha.

### 1. Inconsistência de status HTTP — fila de espera vs. agendamento

**Causa raiz confirmada:** `AppointmentController.publicConfirm`/`publicCancel` (linhas 173 e 185) têm try/catch local que mapeia `IllegalStateException` → `409`. Os métodos irmãos `publicWaitlistAccept`/`publicWaitlistDecline` (linhas 197 e 203), **não têm esse try/catch** — a exceção cai no `GlobalExceptionHandler.handleIllegalStateException` (linha 137-154), que por padrão retorna `400` e só sobe pra `409` se a mensagem contiver a substring `"Horário indisponível"` (regra criada pra um cenário totalmente diferente — conflito de horário na criação de agendamento).

Resultado: "oferta não está mais ativa" (erro de estado, waitlist) → `400`; "não é possível confirmar agendamento com status: X" (erro de estado, agendamento) → `409`. Mesma categoria de erro, dois status diferentes, por causa de um try/catch que existe em dois métodos e falta nos outros dois do mesmo controller.

**Fix sugerido:** replicar o try/catch de `publicConfirm`/`publicCancel` em `publicWaitlistAccept`/`publicWaitlistDecline`, ou remover os quatro catches locais e ajustar o handler global pra tratar `IllegalStateException` como `409` por padrão (o que é semanticamente mais correto — `400` deveria ficar reservado a erro do cliente na requisição, não a conflito de estado do servidor).

### 2. `PUT /professionals/{id}` desvincula usuário silenciosamente

**Causa raiz confirmada:** `UpdateProfessionalUseCase` linha 31 — `existing.setUserId(updated.getUserId())` — sobrescreve incondicionalmente, sem checar null. `ProfessionalRequestDTO.userId` (linha 26) é o único campo do DTO **sem** `@NotNull` (todos os outros campos são obrigatórios), ou seja, o próprio contrato já assume que pode vir null — mas o use case não trata esse caso, tratando "omitido" como "remover vínculo".

**Impacto real:** qualquer PUT feito por um formulário de edição que não reenvie `userId` (comum quando o campo é só exibido, não editável) apaga o vínculo profissional↔usuário e derruba o login desse profissional sem aviso, sem log de auditoria específico pra isso, sem erro na resposta (`200 OK` normal).

**Fix sugerido:** em `UpdateProfessionalUseCase`, só sobrescrever `userId` se `updated.getUserId() != null`; ou tornar o campo `@NotNull` no DTO de update se o vínculo deve sempre ser reafirmado explicitamente.

### 3. `403 Forbidden` inacessível — mas só nas 2 rotas com `hasRole()` direto no `SecurityConfig` (correção ao achado original)

**✏️ Correção de escopo (04/07/2026):** a versão original deste achado dizia que o problema era **sistêmico, em toda a aplicação**. Isso estava **errado por generalização excessiva**. Ao auditar o próprio documento, encontrei um teste pré-existente (`SecurityIntegrationTest.shouldReturnForbiddenWhenReceptionistAccessesUsers`, no repo desde o commit `46dcce7`, muito antes desta sessão) que testa `GET /api/v1/users` com role errada e **já esperava e recebia `403`** — contradizendo a alegação de que "nunca dava 403 em lugar nenhum". Investiguei a causa da divergência:

- A aplicação tem `@EnableMethodSecurity` ativo. A maioria dos endpoints (`/users`, `/services`, `/professionals`, `/appointments/*/confirm|cancel|check-in|...`) é protegida via **`@PreAuthorize` em nível de método/classe** no controller. Uma `AccessDeniedException` vinda daí é lançada **dentro do ciclo do `DispatcherServlet`** (o proxy AOP do method-security envolve a chamada do método do controller) — por isso ela chega normalmente ao `@RestControllerAdvice` e cai em `GlobalExceptionHandler.handleAccessDeniedException`, que **sempre existiu** no código (não foi criado por mim) e **sempre devolveu `403` corretamente**, mesmo antes de qualquer fix desta sessão.
- Só **duas rotas** no `SecurityConfig` usam `hasRole(...)` **direto no matcher de URL** (`authorizeHttpRequests`), fora do mecanismo de `@PreAuthorize`: `/api/v1/medical-records/**` (`hasRole("PROFESSIONAL")`) e `/actuator/**` (`hasRole("ADMIN")`). Só essas duas de fato sofriam do bug: a `AccessDeniedException` aí é lançada pelo `AuthorizationFilter`, **antes do `DispatcherServlet` existir** — não passa pelo `@RestControllerAdvice` de jeito nenhum, só por um `accessDeniedHandler` configurado no próprio `SecurityConfig`, que não existia.

**Escopo real do bug:** `GET /actuator/**` e `/api/v1/medical-records/**` com role incorreta devolviam `401` em vez de `403`. Todos os outros ~15 endpoints protegidos por `@PreAuthorize` (users, services, professionals, appointments, etc.) **nunca tiveram esse problema**.

**Causa raiz:** `SecurityConfig` configurava `.exceptionHandling().authenticationEntryPoint(...)` sem `.accessDeniedHandler(...)`. Sem esse handler explícito, uma `AccessDeniedException` originada no `AuthorizationFilter` (fora do MVC) não tinha pra onde ir e o comportamento observado acabava caindo no `401` do `authenticationEntryPoint`.

**Impacto (real, não "toda a API"):** só quem chamava `/actuator/**` ou `/medical-records/**` com role errada via `401` em vez de `403` — inconsistente com o resto da API, que já diferenciava os dois casos corretamente.

**Fix aplicado:** `.accessDeniedHandler(...)` adicionado ao `SecurityConfig`, cobrindo especificamente essas 2 rotas — as demais já estavam corretas e continuam via `GlobalExceptionHandler.handleAccessDeniedException` (inalterado).

---

## 🔎 Fechamento dos Gaps Pendentes (04/07/2026, continuação)

Ver [gaps-pendentes.md](./gaps-pendentes.md) para o checklist completo. Testes executados contra os jobs/cron **reais** da aplicação, sem UPDATE manual simulando o resultado — apenas montando a pré-condição e deixando o scheduler agir sozinho.

### Scheduler real (`AppointmentFollowUpScheduler`)
Agendamento criado às 17:15:48 em `PENDING`; o cron real (`*/30 * * * * *`) moveu pra `PENDING_RESPONSE` sozinho às **17:16:00** — sem qualquer UPDATE manual. Job confirmado ativo e funcional.

### Fila com múltiplas prioridades
3 agendamentos check-in feito propositalmente fora de ordem (P5 primeiro, P1 por último). `POST /appointments/next` chamado 3x devolveu exatamente P1 → P3 → P5 — confirma `priorityScore` (nível×10¹² + timestamp de check-in) funcionando como projetado, prioridade clínica sempre vence ordem de chegada.

### Paginação — ✅ Implementada em 04/07/2026
Achado original: `GET /patients?page=0&size=3` (e demais listagens) ignorava os query params, sempre retornando o array completo sem envelope de paginação.

**Implementado:** `PageResult<T>` (domain), `PageResponseDTO<T>` (web) e `PaginationSupport` (infra, clamp de `size` em 100 e `page` em 0, default `size=20`). Aplicado nos 6 endpoints de listagem: `GET /patients`, `/services`, `/professionals`, `/users`, `/appointments`, `/audit-logs`. Resposta agora no formato `{"content":[...], "page":0, "size":20, "totalElements":N, "totalPages":N}`.

Efeito colateral positivo: o filtro "somente ativos" de `patients` e `users` (antes feito em memória via `.stream().filter()` depois de carregar a tabela inteira) passou a ser feito na query (`findByActiveTrue`), necessário pra `totalElements` bater com a paginação real.

**Validação:** `mvn test` → 270/270 (11 arquivos de teste ajustados pra nova assinatura/formato). Testado ao vivo contra app real: `size` default 20, `size=99999→100` (clamp), `page=-1→0`, páginas diferentes retornam conteúdo diferente. Regressão completa RF03+RF06 sem quebra.

> **✅ Gap de cobertura encontrado e corrigido (04/07/2026):** a contagem caiu de 272→270 porque o filtro "só ativos" de `patients`/`users` saiu da camada de use case (testada com mock) e virou query (`findByActiveTrue`) — mas nenhum teste de integração cobria isso pra Patient/User (só existia pra Professional/Service em `RepositoryIntegrationTest`). Corrigido: adicionados `shouldExcludeInactivePatientFromFindActive` e `shouldExcludeInactiveUserFromFindActive` em `RepositoryIntegrationTest.java`, restaurando a contagem pra **272/272** (número final e definitivo do projeto).

### Validação de campo obrigatório
`POST` sem corpo em `/patients` e `/appointments` → `400` com `fields` por campo (`MethodArgumentNotValidException` tratado corretamente). Sem achados.

### Rate limit de login
5 tentativas com senha errada → `401`; 6ª tentativa → `429`. Proteção contra brute-force confirmada ativa.

### Concorrência
5 requisições simultâneas pro mesmo profissional/horário → 1×`201`, 4×`409` "Horário indisponível". Banco confirma 1 única linha — sem duplo agendamento sob concorrência real (não só no teste automatizado `AppointmentConcurrencyIntegrationTest`).

### Waitlist — expiração real + achado de timezone
`WaitlistTimeoutScheduler` (roda a cada minuto) expirou a oferta sozinho (`OFFERED→EXPIRED`), sem SQL manual. Tentativa de aceite pós-expiração → `409` corretamente (graças ao fix #1 acima).

> **ℹ️ Nota informativa (não é bug, não precisa correção):** durante a montagem do teste, descobri que o JVM da aplicação roda em horário **local (America/Sao_Paulo, UTC-3)**, enquanto o Postgres `NOW()` retorna **UTC**. A coluna `fila_espera.offer_expires_at` é `timestamp without time zone`, escrita via `LocalDateTime.now()` (Java/local). Como toda leitura/escrita da aplicação usa o mesmo relógio Java, o fluxo funcional real **não quebra**. Mas qualquer query manual, script de DBA, relatório ou serviço externo que compare essa coluna contra `NOW()`/`CURRENT_TIMESTAMP` do Postgres terá um desvio de **3 horas** — na prática, ao tentar simular a expiração via `UPDATE ... SET offer_expires_at = NOW() - INTERVAL '1 hour'`, a oferta continuou válida porque "1h atrás em UTC" ainda estava no futuro em horário local. Vale documentar essa convenção (ou padronizar timestamps do banco para `timestamptz`) pra evitar erro de operação/suporte no futuro.

### Auto-cancelamento por não-resposta (`processMissedDeadlines`)
Agendamento `725025fa-...` (dateTime 05/07 12:00, dentro da janela de deadline) setado `PENDING_RESPONSE` com prazo já vencido. Job roda fixo no minuto 30 de cada hora (não configurável via `.env`, cron hardcoded em `AppointmentFollowUpScheduler`). Às **17:30** virou `CANCELLED` sozinho, sem SQL manual, com `follow_up_required=false` — confirma `auto-cancel-after-no-response=true` ativo e usando o branch de cancelamento automático (não o de flag pra ação manual).

---

## 📊 Resumo Final — Gaps Fechados

| Gap | Resultado |
|---|---|
| 1. Scheduler real (follow-up) | ✅ Cron real disparou sozinho, `PENDING→PENDING_RESPONSE` em 12s |
| 2. Auto-cancelamento por deadline | ✅ Job das :30 cancelou sozinho, sem SQL manual |
| 3. Fila com prioridades múltiplas | ✅ Ordem P1→P3→P5 correta mesmo com check-in invertido |
| 4. Paginação | ✅ Implementada 04/07/2026 — antes params eram ignorados |
| 5. Validação de campo obrigatório | ✅ `400` com detalhe por campo |
| 6. Rate limit de login | ✅ 6ª tentativa → `429` |
| 7. Concorrência | ✅ 5 requisições simultâneas → 1 vence, 4× `409`, sem duplicata |
| 8. Waitlist — oferta expirada | ✅ Scheduler expira sozinho; aceite pós-expiração → `409` |

Achado colateral: desvio de 3h entre relógio do JVM (local) e `NOW()` do Postgres (UTC) — documentado como risco operacional acima, não como bug (app é self-consistent).
