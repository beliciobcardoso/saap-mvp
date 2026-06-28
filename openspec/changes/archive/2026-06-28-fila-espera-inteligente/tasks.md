## 1. Banco de Dados e Migração

- [x] 1.1 Listar migrações existentes e criar o script SQL sequencial `V5__criar_tabela_fila_espera.sql` para criar a tabela `fila_espera`.

## 2. Entidades de Domínio e Camada de Persistência

- [x] 2.1 Criar o enum `WaitlistStatus` com os valores `WAITING`, `OFFERED`, `ACCEPTED`, `DECLINED`, `EXPIRED`.
- [x] 2.2 Criar a classe de domínio `WaitlistEntry` e mapeá-la em `CoreMapper`.
- [x] 2.3 Criar a entidade JPA `WaitlistEntryEntity` mapeando os campos de auditoria e status.
- [x] 2.4 Criar a interface de repositório `WaitlistEntryRepository` na camada de domínio.
- [x] 2.5 Criar o repositório JPA `JpaWaitlistEntryRepository` e o adaptador de persistência `WaitlistEntryRepositoryAdapter`.

## 3. Serviços de Fila de Espera e Casos de Uso

- [x] 3.1 Criar o Use Case `ProcessWaitlistSlotOfferUseCase` para buscar o primeiro da fila (FIFO) e gerar a oferta de vaga.
- [x] 3.2 Atualizar o Use Case `CancelAppointmentUseCase` para chamar o `ProcessWaitlistSlotOfferUseCase` quando uma consulta for cancelada.
- [x] 3.3 Criar o Use Case `AcceptWaitlistOfferUseCase` para receber a confirmação de vaga, validar o token e criar o agendamento correspondente.
- [x] 3.4 Criar o Use Case `DeclineWaitlistOfferUseCase` para marcar a oferta como recusada e chamar o fluxo de oferta para o próximo paciente da fila.

## 4. Agendamento em Background (Worker Scheduler)

- [x] 4.1 Criar o worker `WaitlistTimeoutScheduler` com método `@Scheduled` para buscar e expirar ofertas que ultrapassaram o timeout (30 minutos).

## 5. Endpoints REST Públicos e Segurança

- [x] 5.1 Expor endpoints públicos `/api/v1/appointments/public/waitlist/accept` e `/api/v1/appointments/public/waitlist/decline` no `AppointmentController`.
- [x] 5.2 Atualizar o `SecurityConfig` para permitir acesso sem autenticação na rota `/api/v1/appointments/public/waitlist/**`.

## 6. Testes Automatizados e Homologação

- [x] 6.1 Criar testes de integração para o Use Case de processamento e expiração da fila de espera.
- [x] 6.2 Criar testes de controlador REST verificando que as rotas públicas realizam as transições corretas do status da oferta e criam os agendamentos.
