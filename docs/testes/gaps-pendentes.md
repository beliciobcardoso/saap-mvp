# Gaps de Teste Pendentes

> Criado em 04/07/2026. Lista de cobertura de teste ainda não validada manualmente, identificada após execução de [teste-fluxo-follow-up.md](./teste-fluxo-follow-up.md) e [teste-fluxo-outros-fluxos.md](./teste-fluxo-outros-fluxos.md).
> Cada item deve ser marcado `[x]` só após execução real (não suíte automatizada) e resultado documentado.

- [x] **1. Scheduler real disparando sozinho** — Agendamento `9967f1ad-...` criado 17:15:48 `PENDING`; cron real (`*/30 * * * * *`) flipou pra `PENDING_RESPONSE` sozinho às 17:16:00, sem SQL manual. ✅ Confirmado.
- [x] **2. Auto-cancelamento por não-resposta** — Agendamento `725025fa-...` (dateTime 05/07 12:00, `PENDING_RESPONSE`) virou `CANCELLED` sozinho às 17:30 (job `processMissedDeadlines`), sem SQL manual. `follow_up_required=false` — confirma que `auto-cancel-after-no-response=true` está ativo e usa o branch de cancelamento automático, não o de flag manual. ✅ Confirmado.
- [x] **3. Fila com múltiplos pacientes/prioridades** — 3 agendamentos check-in em ordem P5→P3→P1 (invertida). `next` chamou na ordem correta P1→P3→P5 (priorityScore ascendente). ✅ Confirmado.
- [x] **4. Endpoints de listagem/paginação** — Achado original: `GET` com `?page=0&size=3` ignorava os parâmetros, retornava array completo sem wrapper. **Implementado em 04/07/2026** (mesmo dia): `PageResult`/`PageResponseDTO`/`PaginationSupport`, aplicado aos 6 endpoints de listagem. ✅ Confirmado ao vivo (default `size=20`, clamp `size≤100`, `page≥0`) + `mvn test` 272/272.
- [x] **5. Validação de campo obrigatório faltando** — `POST /patients` e `POST /appointments` vazios → `400` com `fields` detalhando cada campo faltante. ✅ Confirmado.
- [x] **6. Rate limit de login** — 6 tentativas seguidas com credencial errada → 5x `401`, 6ª `429`. ✅ Confirmado.
- [x] **7. Concorrência em agendamento** — 5 requisições simultâneas pro mesmo slot: 1x `201`, 4x `409`. Banco confirma 1 única linha. ✅ Sem duplo booking.
- [x] **8. Oferta de waitlist expirada por tempo** — `WaitlistTimeoutScheduler` (roda a cada minuto) expirou a oferta sozinho (`OFFERED→EXPIRED`) sem SQL manual. Tentativa de aceite pós-expiração → `409` "oferta não está mais ativa". ✅ Confirmado (achado de timezone documentado abaixo).

## Regra
Cada item só fecha com teste real executado (curl/SQL contra app rodando), não só teste automatizado (`mvn test`) — esses já passam e não bastam pra fechar o gap.

## Status Final

**8/8 gaps fechados em 04/07/2026.** Detalhes completos em [teste-fluxo-outros-fluxos.md](./teste-fluxo-outros-fluxos.md#-fechamento-dos-gaps-pendentes-04072026-continuação).

Achado colateral não listado originalmente: descoberto desvio de 3h entre horário do JVM (local, UTC-3) e `NOW()` do Postgres (UTC) ao montar o teste do gap 8 — documentado como risco operacional, não bug funcional (app é internamente consistente).
