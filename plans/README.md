# Implementation Plans — Todos os Lotes

Gerados pela auditoria `improve` em 2026-07-10, commit `a5a9a5a`.

---

## LOTE 1 — Crítico (Segurança, Correção, CI, Deps) — 8/8 DONE ✅

Executar em ordem. Cada plano é independente mas todos devem ser completados antes de Lote 2.

| # | Título | Prioridade | Esforço | Status | Notas |
|---|--------|-----------|---------|--------|-------|
| 001 | Remove hardcoded admin credentials from V10 migration | P1 | S | ✅ DONE | Deleta migração + script manual dev-only |
| 002 | Gate Swagger/OpenAPI documentation by profile | P1 | S | ✅ DONE | Config `springdoc.*.enabled` + `application-dev.yaml` |
| 003 | Add professional ownership check to medical record read | P1 | S | ✅ DONE | LGPD/audit compliance |
| 004 | Fix generic exception handler leaking raw error messages | P1 | S | ✅ DONE | Remove `ex.getMessage()` de 500 responses |
| 005 | Add pessimistic lock to CallNextPatientUseCase | P1 | M | ✅ DONE | Race condition na fila |
| 006 | Fix LoginRateLimitFilter to validate trusted proxies | P1 | M | ✅ DONE | Bloqueia X-Forwarded-For spoofing |
| 007 | Add GitHub Actions CI pipeline | P1 | M | ✅ DONE | `.github/workflows/test.yml` com gate |
| 008 | Fix Testcontainers and Jackson version mismatch | P1 | S | ✅ DONE | Alinha 1.19.7 e Jackson 2.x |

---

## LOTE 2 — Performance + Tech Debt Moderado — 0/8 TODO

Começar DEPOIS que Lote 1 = DONE. Executar em ordem.

| # | Título | Prioridade | Esforço | Status | Notas |
|---|--------|-----------|---------|--------|-------|
| 009 | Move action tokens from GET query string to POST body | P2 | M | ✅ DONE | Previne logging de tokens |
| 010 | Add Redis-backed token blacklist for production | P2 | L | ✅ DONE | Multi-instance safety |
| 011 | Paginate ListAuditLogsUseCase to avoid full user table scans | P2 | S | TODO | findByIdIn() instead of findAll() |
| 012 | Fix CoreMapper asymmetric medical record entries mapping | P2 | S | TODO | Lazy collection bloat |
| 013 | Replace string-matching exception handling with typed exceptions | P2 | S | TODO | ScheduleConflictException instead of message check |
| 014 | Refactor AuthController to follow Controller→UseCase convention | P2 | M | TODO | Cria LoginUseCase |
| 015 | Extract getAuthenticatedUserEmail helper to avoid duplication | P2 | S | TODO | SecurityUtils |
| 016 | Archive completed openspec/changes/rf03-follow-up-confirmacoes | P2 | S | TODO | Move para archive/ |

---

## LOTE 3 — Tech Debt + Docs — 0/7 TODO

Começar DEPOIS que Lote 2 = DONE.

| # | Título | Prioridade | Esforço | Status | Notas |
|---|--------|-----------|---------|--------|-------|
| 017 | Remove hardcoded database password from docs | P2 | S | TODO | docs/ERROS-E-SOLUCOES.md |
| 018 | Remove unused repository port methods | P3 | S | TODO | Dead code cleanup |
| 019 | Replace Thread.sleep in tests with injectable Clock | P3 | S | TODO | Flaky test fix |
| 020 | Add Spotless code formatting and linting | P3 | M | TODO | Maven + CI gate |
| 021 | Document required JWT_SECRET and ACTION_TOKEN_SECRET | P3 | S | TODO | .env.example + README |
| 022 | Add correlation/trace IDs to logging via MDC | P3 | M | TODO | Request tracing + audit |
| 023 | Add database index (status, data_hora) for follow-up scheduler | P3 | S | TODO | Flyway V11 |

---

## LOTE 4 — Direction (Features) — 0/3 TODO

Estas são opções de novas features, não bug fixes. Começar DEPOIS que Lote 3 = DONE, ou em paralelo se prioridade for alta.

| # | Título | Prioridade | Esforço | Status | Evidence |
|---|--------|-----------|---------|--------|----------|
| 024 | Implement use case for patients to join the waitlist | P2 | M | TODO | Grep: zero `new WaitlistEntry()`, manual SQL inserts needed |
| 025 | Implement real notification channels (WhatsApp, SMS, Email) | P2 | L | TODO | ConsoleNotificationService only, PRD RNF03 unmet |
| 026 | Implement PATIENT user role for authenticated patient access | P2 | M | TODO | UserRole.PATIENT defined but never used (zero @PreAuthorize) |

---

## Execução e Convenções

### Status values
- **TODO** — não iniciado
- **IN PROGRESS** — executor trabalhando
- **DONE** — concluído, verificado contra done criteria
- **BLOCKED** — STOP condition encontrada (ver notas)
- **REJECTED** — achado fixado independentemente ou abordagem abandonada

### Dependências entre lotes
- Lote 1 DEVE estar DONE antes de Lote 2
- Lote 2 DEVE estar DONE antes de Lote 3
- Lote 4 pode rodar em paralelo com Lote 3 (features vs tech debt)

### Dentro de cada lote
- Planos são geralmente independentes
- Execute em ordem numérica para consistência
- Se um plano fica BLOCKED, pule e continue com os próximos
- Volte aos BLOCKED depois, ou reporte ao final da sessão

---

## Instruções ao Executor

1. **Leia o plano inteiro antes de começar** — tem STOP conditions e scope que importa
2. **Execute cada step em ordem**, rode commands de verification após cada step
3. **Se bater em STOP condition**: pare, copie exatamente o que viu, reporte
4. **Ao terminar**: rode verificação final (./mvnw clean verify ou o que o plano pede)
5. **Atualize status aqui**: TODO → DONE (ou BLOCKED + motivo)

---

## Resumo por Tipo

**Segurança (5)**: 001, 002, 003, 004, 006  
**Correção (1)**: 005  
**Performance (4)**: 011, 012, 023  
**DX + Tooling (5)**: 007, 020, 021, 022, 016  
**Deps (1)**: 008  
**Tech Debt (6)**: 013, 014, 015, 017, 018, 019  
**Features (3)**: 024, 025, 026  

---

## Tempo Estimado

| Lote | Total | Serial | Paralelo |
|------|-------|--------|----------|
| 1 (8 DONE) | — | 4-6h | 2-3h |
| 2 (8) | 8-12h | — | — |
| 3 (7) | 6-10h | — | — |
| 4 (3) | 6-12h | — | — |
| **Total** | **28-50h** | **serial** | **paralelo** |

---

**Gerado por**: improve skill `/improve` (phase 4, advance planning)  
**Audit commit**: `a5a9a5a`  
**Risco geral**: BAIXO-MÉDIO (1-8 críticos com risco baixo, 9-26 médio)
