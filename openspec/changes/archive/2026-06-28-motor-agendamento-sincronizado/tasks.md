## 1. Banco de Dados e Migrações

- [x] 1.1 Listar os arquivos em `src/main/resources/db/migration/` e criar a migração sequencial `V3__criar_tabelas_agendamento.sql`.
- [x] 1.2 Definir na migração a tabela `agendamento` com campos para UUID (PK), `paciente_id` (FK), `profissional_id` (FK), `servico_id` (FK), `data_hora`, `status`, `forma_pagamento`, `version` (para concorrência otimista), e campos de auditoria de prioridade.
- [x] 1.3 Criar o índice de unicidade parcial `idx_agendamento_prof_data_hora_ativo` no script SQL.

## 2. Domínio e Persistência (JPA)

- [x] 2.1 Criar a entidade de domínio `Appointment` no pacote `br.com.belloinfo.saap_mvp.domain.model` com enums associados `AppointmentStatus` e `PaymentMethod`.
- [x] 2.2 Implementar o encapsulamento e validação da máquina de estados na classe `Appointment`.
- [x] 2.3 Criar a interface `AppointmentRepository` no pacote `domain.repository`.
- [x] 2.4 Criar a entidade JPA `AppointmentEntity` no pacote `infrastructure/persistence/entity`.
- [x] 2.5 Criar o repositório JPA `JpaAppointmentRepository` em `infrastructure/persistence/repository` com suporte a travas de leitura/escrita concorrente.
- [x] 2.6 Implementar o adaptador `AppointmentRepositoryAdapter` no pacote `infrastructure/persistence/adapter`.
- [x] 2.7 Atualizar a interface `CoreMapper` ou criar mapeadores para conversão entre `Appointment` e `AppointmentEntity`.

## 3. Regras de Negócio e Casos de Uso

- [x] 3.1 Implementar `BookAppointmentUseCase` validando a disponibilidade do slot com concorrência segura.
- [x] 3.2 Implementar os casos de uso para transição de estados: `ConfirmAppointmentUseCase`, `CancelAppointmentUseCase`, `CheckInAppointmentUseCase`, `StartAppointmentUseCase` e `CompleteAppointmentUseCase`.
- [x] 3.3 Implementar o caso de uso `ListAppointmentsUseCase` com filtros por data, profissional e paciente.

## 4. Controladores Web REST e Segurança

- [x] 4.1 Criar os DTOs de entrada e saída para requisições de agendamento e mudança de status.
- [x] 4.2 Criar o `AppointmentController` mapeando a rota `/api/v1/appointments`.
- [x] 4.3 Configurar regras de segurança RBAC com `@PreAuthorize` nos métodos do controller para limitar o acesso a `RECEPCIONISTA`, `ADMIN`, `PROFISSIONAL_SAUDE` e `PACIENTE`.

## 5. Testes Automatizados e Homologação

- [x] 5.1 Criar testes unitários para a máquina de estados e validações de transição de status na entidade `Appointment`.
- [x] 5.2 Criar testes de integração concorrentes usando `Testcontainers` com threads simultâneas competindo pelo mesmo slot.
- [x] 5.3 Criar testes de controlador (`MockMvc`) validando as regras de acesso RBAC e tratamento global de exceções.
