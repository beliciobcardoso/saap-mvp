## ADDED Requirements

### Requirement: Prevenção de Conflito de Horários (Double-booking)
O sistema SHALL impedir a criação de dois agendamentos no mesmo horário para o mesmo profissional de saúde, garantindo a unicidade e disponibilidade do slot antes da persistência.

#### Scenario: Agendamento em horário disponível
- **WHEN** o usuário solicita o cadastro de um agendamento para um profissional em um slot livre e disponível de sua grade
- **THEN** o sistema SHALL persistir o agendamento com o status inicial `PENDING` e retornar os dados cadastrados com status 201 (Created)

#### Scenario: Tentativa de agendamento em horário ocupado
- **WHEN** o usuário tenta cadastrar um agendamento para um profissional em um slot de horário que já possui um agendamento ativo (não cancelado)
- **THEN** o sistema SHALL impedir o cadastro, rejeitar a transação e retornar um erro HTTP 409 (Conflict)

### Requirement: Máquina de Estados e Transições do Agendamento
O sistema SHALL forçar a máquina de estados definida para o ciclo de vida do agendamento, validando cada transição e rejeitando qualquer alteração ilegal com uma exceção de negócio específica (`IllegalStateException`).

#### Scenario: Transição válida de confirmação
- **WHEN** o status do agendamento é `PENDING` e é solicitada a confirmação da consulta
- **THEN** o sistema SHALL alterar o status para `CONFIRMED` e persistir a mudança no banco de dados

#### Scenario: Transição inválida de início de consulta direta
- **WHEN** o status do agendamento é `PENDING` e é solicitado o início direto da consulta (`IN_PROGRESS`)
- **THEN** o sistema SHALL lançar uma exceção de transição inválida e abortar a operação sem alterar o registro

### Requirement: Controle de Transação Concorrente sob Alta Carga
O sistema SHALL usar mecanismos de isolamento transacional (como bloqueio pessimista `@Lock` ou versionamento otimista `@Version` no JPA) para assegurar que requisições simultâneas e paralelas concorrendo pelo mesmo slot resultem em apenas uma reserva com sucesso.

#### Scenario: Duas requisições simultâneas para o mesmo slot
- **WHEN** duas requisições HTTP paralelas chegam simultaneamente para reservar o mesmo slot de horário de um profissional
- **THEN** o sistema SHALL permitir apenas um agendamento com sucesso e retornar status HTTP 409 (Conflict) ou erro transacional para a segunda requisição
