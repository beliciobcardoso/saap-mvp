# Implementation Plans — Lote 1 (Crítico: Segurança, Correção, CI, Deps)

Gerado pela auditoria `improve` em 2026-07-10, commit `a5a9a5a`.

Executar **em ordem** abaixo. Cada plano é independente, mas todos os 8 devem ser completados antes de passar para o Lote 2 (Performance/Tech Debt).

## Execução e Status

| # | Título | Prioridade | Esforço | Status | Notas |
|---|--------|-----------|---------|--------|-------|
| 001 | Remove hardcoded admin credentials from V10 migration | P1 | S | ✅ DONE | Deleta migração + cria script manual dev-only |
| 002 | Gate Swagger/OpenAPI documentation by profile | P1 | S | ✅ DONE | Adiciona config `springdoc.*.enabled` + `application-dev.yaml` |
| 003 | Add professional ownership check to medical record read | P1 | S | ✅ DONE | Enforcement LGPD/audit, valida se profissional tem acesso |
| 004 | Fix generic exception handler leaking raw error messages | P1 | S | TODO | Remove `ex.getMessage()` de 500 responses, sanitiza output |
| 005 | Add pessimistic lock to CallNextPatientUseCase | P1 | M | TODO | Previne race condition na fila de atendimento |
| 006 | Fix LoginRateLimitFilter to validate trusted proxies | P1 | M | TODO | Bloqueia X-Forwarded-For spoofing de taxa de login |
| 007 | Add GitHub Actions CI pipeline | P1 | M | TODO | `.github/workflows/test.yml` com `./mvnw clean verify` gate |
| 008 | Fix Testcontainers and Jackson version mismatch | P1 | S | TODO | Alinha Testcontainers 1.19.7 e Jackson 2.x, exclui 3.x |

### Status values
- **TODO** — não iniciado
- **IN PROGRESS** — executor trabalhando
- **DONE** — concluído, verificado contra done criteria
- **BLOCKED** — precisa esclarecimento ou encontrou STOP condition (ver coluna "Notas")
- **REJECTED** — achado foi fixado independentemente ou abordagem foi abandonada (ver "Considerados e Rejeitados")

## Dependências

**Execução linear (sem paralelo recomendado):**
- Todos os 8 planos têm risco baixo-médio e podem ser executados em série
- Se há múltiplos executores: planos 1, 2, 7, 8 (segurança + DX) são independentes e podem rodar em paralelo; depois 3, 4 (domínio + API); depois 5, 6 (concorrência + auth)
- Recomendação: executar 1-8 sequencialmente na primeira rodada, não há gain significativo de paralelismo para este lote

**Recomendação de merge:**
- Após DONE em planos 1-4 (críticos de segurança): merge + deploy pra staging
- Após DONE em planos 5-8: merge + deploy pra prod (com CI gate garantindo qualidade)

## Achados considerados e rejeitados

Nenhum neste lote — todos os achados 1-8 foram verificados em código real e confirmados.

## Instruções ao executor

1. **Leia o plano inteiro antes de começar** — o arquivo tem contexto (Current state, STOP conditions) que você precisará
2. **Execute cada step em ordem**, rode os comandos de verificação após cada step
3. **Se bater em STOP condition**: pare, copie exatamente o que você viu, e reporte (não improvise)
4. **Ao terminar**: rode a verificação final (`./mvnw clean verify` ou o que o plano pede)
5. **Atualize o status aqui**: substitua `TODO` por `DONE` (ou `BLOCKED` + detalhes se parou)

## Próximas fases (após Lote 1)

- **Lote 2** (Performance + Tech Debt Moderado): achados 9-16, quando Lote 1 estiver DONE
- **Lote 3** (Tech Debt + Docs): achados 17-23
- **Lote 4** (Direction — Features): 3 achados de direção (fila de espera, canais de notificação, UserRole.PATIENT)

## Verificação pré-execução

Antes de iniciar qualquer plano, confirme:

```bash
git status                                           # Worktree limpa?
git log --oneline -3                                 # Branch é `developer` (ou seu PR branch)?
./mvnw clean compile && ./mvnw clean test            # Build + testes passam agora?
```

Se algum falhar, fixe antes de começar os planos.

---

**Gerado por**: improve skill `/improve` (phase 4, advance planning)  
**Audit commit**: `a5a9a5a`  
**Tempo estimado (8 planos)**: 4-6 horas serial, ~2-3 horas se paralelizar  
**Risco geral Lote 1**: **BAIXO** — todos são S/M de esforço, segurança/qualidade, sem remodelação arquitetural
