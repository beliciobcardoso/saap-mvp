# Tasks: rf06-registro-clinico-prontuario

## 1. Banco de Dados

- [x] 1.1 Criar migration `V9__criar_tabelas_prontuario.sql`: tabela `prontuario` (id UUID PK, paciente_id UUID UNIQUE NOT NULL FK → paciente, created_at) e tabela `entrada_prontuario` (id UUID PK, prontuario_id FK NOT NULL, agendamento_id UUID UNIQUE NOT NULL FK → agendamento, profissional_id FK NOT NULL, evolucao TEXT NOT NULL, created_at, updated_at, version BIGINT)

## 2. Domínio

- [x] 2.1 Criar modelos `MedicalRecord` e `MedicalRecordEntry` em `domain.model` (imutáveis, seguindo padrão dos modelos existentes)
- [x] 2.2 Criar interfaces `MedicalRecordRepository` e `MedicalRecordEntryRepository` em `domain.repository` (findByPatientId, findByAppointmentId, save)
- [x] 2.3 Criar exceção de domínio para janela de edição fechada (ex.: `MedicalRecordEntryLockedException`), mapeada para HTTP 409

## 3. Persistência

- [x] 3.1 Criar `MedicalRecordEntity` e `MedicalRecordEntryEntity` em `infrastructure.persistence.entity` (com `@Version` na entrada)
- [x] 3.2 Criar mappers entidade↔domínio em `infrastructure.persistence.mapper`
- [x] 3.3 Criar repositórios JPA e adapters em `infrastructure.persistence.repository` / `adapter`

## 4. Aplicação (Use Cases)

- [x] 4.1 `CreateMedicalRecordEntryUseCase`: valida agendamento `IN_PROGRESS`, valida profissional autenticado = profissional do agendamento, cria prontuário sob demanda se inexistente, persiste entrada, audita `MEDICAL_RECORD_ENTRY_CREATED`
- [x] 4.2 `UpdateMedicalRecordEntryUseCase`: valida janela `IN_PROGRESS` e profissional do agendamento, atualiza evolução, audita `MEDICAL_RECORD_ENTRY_UPDATED`; rejeita com 409 se agendamento `COMPLETED`
- [x] 4.3 `GetMedicalRecordByPatientUseCase`: retorna prontuário com entradas ordenadas por data decrescente, audita `MEDICAL_RECORD_READ`; 404 se paciente sem prontuário
- [x] 4.4 Alterar `CompleteAppointmentUseCase`: exigir entrada de evolução preenchida antes de `transitionTo(COMPLETED)`; rejeitar com 409 se ausente

## 5. Web (API REST)

- [x] 5.1 Criar DTOs de request/response e mapper em `infrastructure.web.dto` / `mapper`, com validação Bean Validation (`@NotBlank` na evolução, `@NotNull` no appointmentId)
- [x] 5.2 Criar `MedicalRecordController` com `GET /api/v1/medical-records/patients/{patientId}`, `POST /api/v1/medical-records/entries`, `PUT /api/v1/medical-records/entries/{entryId}`, documentado no Swagger
- [x] 5.3 Mapear novas exceções no `@ControllerAdvice` (janela fechada/evolução ausente → 409; profissional divergente → 403; prontuário inexistente → 404)

## 6. Segurança

- [x] 6.1 Restringir `/api/v1/medical-records/**` a `hasRole("PROFESSIONAL")` no `SecurityConfig`
- [x] 6.2 Implementar verificação "profissional autenticado é o profissional do agendamento" nos use cases de escrita

## 7. Testes

- [x] 7.1 Unitários: janela de edição (criação/edição em cada estado do agendamento), imutabilidade pós-`COMPLETED`, criação lazy do prontuário, validação de profissional divergente
- [x] 7.2 Unitários: `CompleteAppointmentUseCase` com e sem evolução preenchida (ajustar testes existentes do fluxo de finalização)
- [x] 7.3 Integração: unicidade 1:1 (paciente↔prontuário, agendamento↔entrada) via constraints, persistência e ordenação das entradas
- [x] 7.4 API/Segurança (MockMvc): 401 sem token, 403 para `ROLE_ADMIN`/`ROLE_RECEPTIONIST`, 403 para profissional divergente, 200/201 para `ROLE_PROFESSIONAL` do agendamento
- [x] 7.5 Verificar registro de auditoria nas operações de leitura e escrita de prontuário

## 8. Verificação Final (após concluir a implementação)

- [x] 8.1 Rodar a suíte completa de testes (`./mvnw clean verify`) e garantir 100% verde antes de seguir
- [x] 8.2 Corrigir qualquer regressão encontrada (especialmente nos testes existentes de `CompleteAppointmentUseCase` e `AppointmentController`)

## 9. Documentação e Versão

- [x] 9.1 Atualizar `README.md` com a seção de endpoints `/api/v1/medical-records`
- [x] 9.2 Atualizar `docs/PRD-SAAP.md`: marcar RF06 como `[Implementado]` e atualizar o status/versão do documento
- [x] 9.3 Alinhar e atualizar a versão do projeto para `1.2.0` nos três pontos hoje inconsistentes: `pom.xml` (atualmente `0.0.1-SNAPSHOT`), `OpenApiConfig` (atualmente `1.0.0`, exibida no Swagger UI) e `docs/PRD-SAAP.md` (atualmente `v1.1.0`)
