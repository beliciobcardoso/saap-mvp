## 1. Banco de Dados e Migração

- [x] 1.1 Criar o arquivo de migração sequencial `V4__adicionar_coluna_follow_up_agendamento.sql` adicionando a coluna `follow_up_sent` (boolean, default false) na tabela `agendamento`.

## 2. Entidades de Domínio e Camada de Persistência

- [x] 2.1 Adicionar a propriedade `followUpSent` (boolean) na classe de domínio `Appointment`.
- [x] 2.2 Atualizar a entidade JPA `AppointmentEntity` mapeando o novo campo correspondente.
- [x] 2.3 Adicionar o método de busca `findByStatusAndDateTimeBetweenAndFollowUpSentFalse` no repositório de agendamentos.
- [x] 2.4 Atualizar o mapeador `CoreMapper` para converter o novo campo entre as representações.

## 3. Serviços Core (Token e Notificação)

- [x] 3.1 Criar o serviço `AppointmentActionTokenService` para geração e decodificação segura de tokens JWT auto-contidos para ações de confirmação/cancelamento públicas.
- [x] 3.2 Criar a interface `NotificationService` e a implementação `ConsoleNotificationService` para simular o disparo de e-mails com links no console.
- [x] 3.3 Criar o caso de uso `SendFollowUpNotificationsUseCase` contendo a lógica de busca de agendamentos `PENDING` para o dia seguinte, envio de e-mails simulados e atualização do status para `follow_up_sent = true`.

## 4. Agendamento em Background (Worker Scheduler)

- [x] 4.1 Criar/atualizar a configuração para habilitar o agendador do Spring (`@EnableScheduling`).
- [x] 4.2 Criar o worker `AppointmentFollowUpScheduler` com método anotado com `@Scheduled` executando o job de envio a cada 1 hora.

## 5. Endpoints REST Públicos e Segurança

- [x] 5.1 Criar os endpoints GET públicos `/api/v1/appointments/public/confirm` e `/api/v1/appointments/public/cancel` no `AppointmentController` para processar os cliques do paciente.
- [x] 5.2 Atualizar o `SecurityConfig` liberando acesso público sem autenticação para a rota `/api/v1/appointments/public/**`.

## 6. Testes Automatizados e Homologação

- [x] 6.1 Criar testes unitários para o `AppointmentActionTokenService` validando expiração e integridade dos tokens JWT.
- [x] 6.2 Criar testes de integração para o caso de uso `SendFollowUpNotificationsUseCase` e o worker agendado.
- [x] 6.3 Criar testes de controlador REST verificando que as rotas públicas realizam as transições de status da consulta com tokens válidos e rejeitam tokens inválidos.
