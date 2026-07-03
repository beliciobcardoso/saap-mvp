# Proposal: rf06-registro-clinico-prontuario

## Why

O RF06 é o último requisito funcional do PRD sem implementação. Hoje o ciclo de atendimento fecha (`IN_PROGRESS` → `COMPLETED` via `CompleteAppointmentUseCase`) sem nenhum registro clínico da consulta — o profissional não tem onde documentar a evolução do paciente. Sem prontuário, o sistema não cumpre a finalidade clínica do atendimento nem os requisitos legais de validade de prontuários médicos (imutabilidade pós-atendimento) e de LGPD (RNF01: isolamento de dados sensíveis de saúde e trilha de auditoria de todo acesso a prontuário).

## What Changes

- **Novas entidades de domínio**: `MedicalRecord` (prontuário, relação 1:1 com `Patient`) e `MedicalRecordEntry` (evolução clínica, relação 1:1 com `Appointment`), armazenadas de forma isolada conforme RNF01.
- **Regra de janela de edição**: uma entrada de evolução só pode ser criada ou editada enquanto o agendamento correspondente estiver no estado `IN_PROGRESS`. Após a transição para `COMPLETED`, a entrada torna-se **imutável** (qualquer tentativa de alteração é rejeitada).
- **Fechamento do ciclo de atendimento**: a transição `IN_PROGRESS` → `COMPLETED` passa a exigir que a evolução clínica da consulta tenha sido preenchida (o profissional "preenche a evolução e fecha o prontuário", conforme tabela de estados do PRD).
- **Novos endpoints REST** sob `/api/v1/medical-records`, restritos a `ROLE_PROFESSIONAL` (RBAC): consulta de prontuário por paciente, criação e edição de entrada de evolução vinculada a um agendamento.
- **Auditoria de prontuário**: toda leitura, inserção e alteração de dados de prontuário é registrada via `AuditService` existente (exigência explícita do RNF01).
- **Migration Flyway** `V9` criando as tabelas `prontuario` e `entrada_prontuario`.

## Capabilities

### New Capabilities
- `medical-record`: Registro clínico e prontuário — prontuário 1:1 com paciente, entradas de evolução 1:1 com agendamento, janela de edição atrelada ao estado do agendamento, imutabilidade pós-`COMPLETED`, acesso restrito a `ROLE_PROFESSIONAL` e auditoria de leitura/escrita.

### Modified Capabilities
- `scheduling-engine`: A transição `IN_PROGRESS` → `COMPLETED` ganha pré-condição — só é permitida se existir entrada de evolução preenchida para o agendamento.

## Impact

- **Domínio**: novos modelos `MedicalRecord` e `MedicalRecordEntry` em `domain.model`; novos repositórios `MedicalRecordRepository` e `MedicalRecordEntryRepository` em `domain.repository`.
- **Aplicação**: novos use cases `GetMedicalRecordByPatientUseCase`, `CreateMedicalRecordEntryUseCase`, `UpdateMedicalRecordEntryUseCase`; alteração em `CompleteAppointmentUseCase` (validar existência da evolução antes de completar).
- **Persistência**: entidades JPA `MedicalRecordEntity` e `MedicalRecordEntryEntity`, mappers e adapters em `infrastructure.persistence`; migration `V9__criar_tabelas_prontuario.sql`.
- **Web**: novo `MedicalRecordController`, DTOs e mapper em `infrastructure.web`; tratamento de erros para janela de edição fechada (409) e imutabilidade (409/422).
- **Segurança**: regras de rota em `SecurityConfig` restringindo `/api/v1/medical-records/**` a `ROLE_PROFESSIONAL`.
- **Auditoria**: chamadas a `AuditService` nos use cases de leitura e escrita de prontuário.
- **Testes**: unitários (regras de janela de edição e imutabilidade), integração (persistência e transições) e API/segurança (RBAC — somente `ROLE_PROFESSIONAL`), seguindo a pirâmide do PRD.
