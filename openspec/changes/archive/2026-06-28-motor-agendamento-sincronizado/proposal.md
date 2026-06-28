## Why

Atualmente, o SAAP MVP não possui regras ou estruturas para gerenciar os agendamentos de consultas de forma estruturada e concorrente. A ausência desta funcionalidade impossibilita o controle das agendas dos profissionais de saúde, a validação de disponibilidade e expõe o sistema ao risco de *double-booking* (dois agendamentos no mesmo horário para o mesmo profissional). Implementar o Motor de Agendamento Sincronizado resolve este problema e estabelece a base para o fluxo de atendimento presencial e lista de espera.

## What Changes

- Criação da estrutura de dados para suporte a Agendamentos (`Appointment` / `Agendamento`).
- Implementação de máquina de estados para controle rígido do ciclo de vida de um agendamento.
- Desenvolvimento das regras de validação de conflito de horários (disponibilidade do profissional).
- Implementação de controle de concorrência ativa (trava pessimista ou otimista) para garantir que requisições concorrentes não criem agendamentos duplicados.
- Criação dos endpoints REST para criação, cancelamento, alteração de status e consulta de agendamentos.
- Cobertura de testes unitários e de integração (usando Testcontainers PostgreSQL) simulando cenários de alta concorrência.

## Capabilities

### New Capabilities
- `scheduling-engine`: Gerenciamento e validação concorrente de slots de agendamento e ciclo de vida de consultas.

### Modified Capabilities
<!-- Nenhuma especificação existente necessita de modificação direta em seus requisitos de negócio neste ciclo. -->

## Impact

- **Banco de Dados**: Criação de novas tabelas de banco de dados (`agendamento` e tabelas de relacionamento associadas) via migração sequencial do Flyway.
- **Domínio**: Introdução da entidade `Appointment` no pacote de domínio.
- **Segurança**: Proteção de rotas `/api/v1/appointments/**` com regras de autorização RBAC (ex: recepcionistas podem cadastrar, profissionais de saúde visualizam a própria agenda).
- **Infraestrutura**: Adição de travas de concorrência na camada de persistência JPA.
