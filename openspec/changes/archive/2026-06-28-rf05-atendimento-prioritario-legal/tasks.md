## 1. Banco de Dados e Migração

- [x] 1.1 Criar o script SQL de migração `V6__criar_tabela_log_auditoria.sql` para criar a tabela `log_auditoria`.

## 2. Entidades de Domínio e Camada de Persistência

- [x] 2.1 Criar a classe de domínio `AuditLog` no pacote `br.com.belloinfo.saap_mvp.domain.model`.
- [x] 2.2 Criar a interface de repositório `AuditLogRepository` em `br.com.belloinfo.saap_mvp.domain.repository`.
- [x] 2.3 Criar a entidade JPA `AuditLogEntity` em `br.com.belloinfo.saap_mvp.infrastructure.persistence.entity`.
- [x] 2.4 Atualizar o mapeador `CoreMapper` para incluir o mapeamento bidirecional da entidade `AuditLog`.
- [x] 2.5 Criar o repositório JPA `JpaAuditLogRepository` e o adaptador de persistência `AuditLogRepositoryAdapter`.

## 3. Modificações de Repositório Existentes

- [x] 3.1 Adicionar o método `Optional<Professional> findByUserId(UUID userId)` em `ProfessionalRepository` e sua implementação no adaptador.
- [x] 3.2 Adicionar a consulta `findByUserId` em `JpaProfessionalRepository`.
- [x] 3.3 Adicionar o método `Optional<Appointment> findNextInQueue(UUID professionalId, LocalDateTime start, LocalDateTime end)` em `AppointmentRepository` e sua implementação no adaptador.
- [x] 3.4 Adicionar a consulta customizada em `JpaAppointmentRepository` filtrando por profissional, status `ARRIVED`, data dentro do intervalo e ordenado por `priorityScore` crescente.

## 4. Camada de Aplicação (Use Cases e Regras de Negócio)

- [x] 4.1 Atualizar `CheckInAppointmentUseCase` para calcular e gravar `priorityScore`, atualizar status para `ARRIVED`, e persistir a entrada na auditoria.
- [x] 4.2 Criar o Use Case `CallNextPatientUseCase` para buscar o primeiro da fila presencial (`ARRIVED` com menor score), transicionar status para `CALLING`, persistir a alteração e gravar a auditoria.

## 5. Endpoints REST e Segurança (RBAC)

- [x] 5.1 Adicionar a chamada de auditoria no endpoint de check-in (`PUT /api/v1/appointments/{id}/check-in`), passando o IP de origem obtido da requisição HTTP e o usuário autenticado.
- [x] 5.2 Expor o endpoint `POST /api/v1/appointments/next` em `AppointmentController` permitindo apenas a role `ROLE_PROFESSIONAL` para chamar o próximo paciente, injetando o profissional logado e gravando no log de auditoria.

## 6. Testes Automatizados e Homologação

- [x] 6.1 Criar testes de unidade e integração para o fluxo de check-in com prioridade declarada/confirmada e prioridade inválida/revertida.
- [x] 6.2 Criar testes de integração para o Use Case de chamada do próximo paciente da fila presencial, validando ordenação por score.
- [x] 6.3 Executar o build completo com `./mvnw clean test` para certificar que todas as regras de negócio foram homologadas com sucesso.
