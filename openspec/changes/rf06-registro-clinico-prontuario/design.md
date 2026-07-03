# Design: rf06-registro-clinico-prontuario

## Context

O sistema já possui o ciclo completo de atendimento presencial (RF05): check-in (`ARRIVED`), chamada (`CALLING`), início (`IN_PROGRESS`) e finalização (`COMPLETED` via `CompleteAppointmentUseCase`). Porém, a finalização hoje é uma transição "vazia" — nenhum registro clínico é produzido. O PRD (RF06, UC04/UC08) exige prontuário 1:1 com paciente, evolução 1:1 com agendamento, janela de edição restrita ao estado `IN_PROGRESS` e imutabilidade pós-`COMPLETED`. O RNF01 (já implementado na v1.1.0) fornece a infraestrutura de auditoria (`AuditService.log(...)`) e exige trilha para **toda leitura e escrita** de prontuário.

A arquitetura é hexagonal: `domain` (modelos puros + interfaces de repositório), `application` (use cases), `infrastructure` (JPA, web, security). Migrations Flyway numeradas — próxima é `V9`.

## Goals / Non-Goals

**Goals:**
- Entidades `MedicalRecord` (1:1 `Patient`) e `MedicalRecordEntry` (1:1 `Appointment`) isoladas das demais tabelas (minimização LGPD).
- Janela de edição: criar/editar evolução somente com agendamento `IN_PROGRESS`; imutável após `COMPLETED`.
- Transição `IN_PROGRESS` → `COMPLETED` condicionada à existência de evolução preenchida.
- Endpoints REST sob `/api/v1/medical-records`, acessíveis somente a `ROLE_PROFESSIONAL`.
- Auditoria de leitura, criação e edição de entradas de prontuário.

**Non-Goals:**
- Prescrições, anexos, exames ou modelos estruturados de evolução (evolução é texto livre no MVP).
- Assinatura digital ICP-Brasil / carimbo de tempo (validade legal no MVP é garantida por imutabilidade + auditoria).
- Acesso do paciente ao próprio prontuário (portal do paciente fora do escopo).
- Versionamento/histórico de edições da evolução durante a janela `IN_PROGRESS` (a auditoria registra que houve edição, não o diff).

## Decisions

1. **Prontuário criado sob demanda (lazy)**: `MedicalRecord` é criado automaticamente na primeira entrada de evolução do paciente, não no cadastro do paciente. Evita backfill de pacientes existentes e mantém a tabela mínima. Alternativa rejeitada: criar no cadastro — geraria migração de dados e acoplamento com `CreatePatientUseCase`.

2. **Imutabilidade derivada do estado do agendamento, sem flag própria**: a regra "editável" é `appointment.status == IN_PROGRESS`, verificada nos use cases. Não há coluna `locked`/`immutable` — o estado do agendamento é a fonte única de verdade, eliminando risco de dessincronização. Tentativa de edição fora da janela lança exceção de domínio mapeada para HTTP 409.

3. **`CompleteAppointmentUseCase` valida a evolução**: antes de `transitionTo(COMPLETED)`, o use case consulta `MedicalRecordEntryRepository.findByAppointmentId(...)`; se ausente ou com conteúdo em branco, lança exceção (HTTP 409). Isso implementa o gatilho do PRD ("Profissional preenche a evolução e fecha o prontuário") sem acoplar a máquina de estados do domínio ao repositório — a validação fica na camada de aplicação.

4. **Unicidade 1:1 garantida no banco**: `prontuario.paciente_id` com `UNIQUE`, `entrada_prontuario.agendamento_id` com `UNIQUE`. Corrida entre duas criações simultâneas resolve-se por constraint violation (mapeada para 409), sem lock pessimista.

5. **RBAC estrito a `ROLE_PROFESSIONAL`**: `SecurityConfig` restringe `/api/v1/medical-records/**` a `hasRole("PROFESSIONAL")`. `ROLE_ADMIN` e `ROLE_RECEP` **não** acessam (minimização LGPD — PRD atribui prontuário exclusivamente ao profissional). Adicionalmente, criação/edição exige que o profissional autenticado seja o profissional do agendamento.

6. **Auditoria síncrona no mesmo fluxo**: use cases de leitura e escrita chamam `AuditService.log(...)` com ações `MEDICAL_RECORD_READ`, `MEDICAL_RECORD_ENTRY_CREATED`, `MEDICAL_RECORD_ENTRY_UPDATED`, reutilizando o mecanismo existente (que já captura `usuario_id` e `ip_origem`).

7. **API REST**:
   - `GET /api/v1/medical-records/patients/{patientId}` → prontuário com entradas (auditado como leitura).
   - `POST /api/v1/medical-records/entries` → cria entrada `{appointmentId, evolution}` (cria o prontuário se não existir).
   - `PUT /api/v1/medical-records/entries/{entryId}` → edita evolução (somente na janela).

8. **Migration `V9__criar_tabelas_prontuario.sql`**: tabelas `prontuario` (id UUID, paciente_id UNIQUE FK, created_at) e `entrada_prontuario` (id UUID, prontuario_id FK, agendamento_id UNIQUE FK, profissional_id FK, evolucao TEXT NOT NULL, created_at, updated_at, version BIGINT para lock otimista, consistente com o padrão do projeto).

## Risks / Trade-offs

- **Volume de auditoria de leitura**: auditar toda leitura de prontuário gera crescimento da tabela de audit log. Aceito — exigência explícita do RNF01; mitigação futura via particionamento/retention fica fora do escopo.
- **Consulta sem evolução não pode ser completada**: atendimentos legados/interrompidos em `IN_PROGRESS` sem evolução ficarão bloqueados até o profissional preencher. Comportamento desejado pelo PRD, mas muda o fluxo atual de `CompleteAppointmentUseCase` — testes existentes desse fluxo precisarão ser atualizados.
- **Texto livre sem estrutura**: evolução como TEXT simples limita relatórios clínicos futuros; aceito no MVP (YAGNI).
- **Imutabilidade lógica, não física**: a proteção é na camada de aplicação (não há trigger no banco). Um acesso direto ao banco contorna a regra; mitigado pela trilha de auditoria e por acesso restrito ao banco em produção.
