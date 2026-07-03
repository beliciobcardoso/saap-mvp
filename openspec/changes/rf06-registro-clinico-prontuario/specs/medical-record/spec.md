# medical-record Delta Specification

## ADDED Requirements

### Requirement: Prontuário Único por Paciente
O sistema SHALL manter no máximo um prontuário (`MedicalRecord`) por paciente, com relação 1:1 garantida por restrição de unicidade no banco de dados, criando o prontuário automaticamente na primeira entrada de evolução clínica do paciente.

#### Scenario: Primeira evolução clínica do paciente
- **WHEN** um profissional registra a primeira entrada de evolução para um paciente que ainda não possui prontuário
- **THEN** o sistema SHALL criar o prontuário do paciente e vincular a entrada a ele na mesma transação

#### Scenario: Evoluções subsequentes do mesmo paciente
- **WHEN** um profissional registra uma nova entrada de evolução para um paciente que já possui prontuário
- **THEN** o sistema SHALL vincular a entrada ao prontuário existente sem criar um novo

### Requirement: Entrada de Evolução 1:1 com Agendamento
O sistema SHALL permitir no máximo uma entrada de evolução (`MedicalRecordEntry`) por agendamento, garantida por restrição de unicidade no banco de dados.

#### Scenario: Tentativa de segunda entrada para o mesmo agendamento
- **WHEN** um profissional tenta criar uma segunda entrada de evolução para um agendamento que já possui uma
- **THEN** o sistema SHALL rejeitar a operação e retornar HTTP 409 (Conflict)

### Requirement: Janela de Edição Atrelada ao Estado do Agendamento
O sistema SHALL permitir a criação ou edição de uma entrada de evolução somente enquanto o agendamento correspondente estiver no estado `IN_PROGRESS`. Após a transição para `COMPLETED`, a entrada SHALL tornar-se imutável.

#### Scenario: Criação durante o atendimento
- **WHEN** o agendamento está `IN_PROGRESS` e o profissional do atendimento registra a evolução
- **THEN** o sistema SHALL persistir a entrada e retornar HTTP 201 (Created)

#### Scenario: Edição durante o atendimento
- **WHEN** o agendamento está `IN_PROGRESS` e o profissional edita a evolução já registrada
- **THEN** o sistema SHALL atualizar o conteúdo da entrada e retornar HTTP 200 (OK)

#### Scenario: Tentativa de criação fora da janela
- **WHEN** o agendamento não está no estado `IN_PROGRESS` (ex.: `CONFIRMED`, `COMPLETED`, `CANCELLED`) e é solicitada a criação de evolução
- **THEN** o sistema SHALL rejeitar a operação e retornar HTTP 409 (Conflict)

#### Scenario: Tentativa de edição após finalização
- **WHEN** o agendamento está `COMPLETED` e é solicitada a edição da entrada de evolução correspondente
- **THEN** o sistema SHALL rejeitar a operação sem alterar o registro e retornar HTTP 409 (Conflict)

### Requirement: Acesso Restrito a Profissionais de Saúde
O sistema SHALL restringir todos os endpoints de prontuário (`/api/v1/medical-records/**`) a usuários com `ROLE_PROFESSIONAL`. Adicionalmente, a criação e edição de evolução SHALL ser permitida apenas ao profissional vinculado ao agendamento correspondente.

#### Scenario: Acesso negado a perfil não profissional
- **WHEN** um usuário autenticado com `ROLE_ADMIN`, `ROLE_RECEPTIONIST` ou outro perfil não profissional acessa qualquer endpoint de prontuário
- **THEN** o sistema SHALL negar o acesso e retornar HTTP 403 (Forbidden)

#### Scenario: Profissional diferente do agendamento
- **WHEN** um usuário com `ROLE_PROFESSIONAL` tenta criar ou editar a evolução de um agendamento vinculado a outro profissional
- **THEN** o sistema SHALL negar a operação e retornar HTTP 403 (Forbidden)

#### Scenario: Acesso não autenticado
- **WHEN** uma requisição sem token JWT válido acessa qualquer endpoint de prontuário
- **THEN** o sistema SHALL retornar HTTP 401 (Unauthorized)

### Requirement: Auditoria de Acesso e Escrita de Prontuário
O sistema SHALL registrar na trilha de auditoria imutável toda leitura, inserção e alteração de dados de prontuário, contendo data/hora, usuário, ação, recurso afetado e IP de origem, conforme RNF01.

#### Scenario: Leitura de prontuário auditada
- **WHEN** um profissional consulta o prontuário de um paciente
- **THEN** o sistema SHALL registrar um log de auditoria com a ação `MEDICAL_RECORD_READ` e o identificador do recurso acessado

#### Scenario: Escrita de evolução auditada
- **WHEN** um profissional cria ou edita uma entrada de evolução
- **THEN** o sistema SHALL registrar um log de auditoria com a ação correspondente (`MEDICAL_RECORD_ENTRY_CREATED` ou `MEDICAL_RECORD_ENTRY_UPDATED`)

### Requirement: Consulta de Prontuário por Paciente
O sistema SHALL expor a consulta do prontuário de um paciente com suas entradas de evolução ordenadas da mais recente para a mais antiga.

#### Scenario: Paciente com prontuário existente
- **WHEN** um profissional consulta o prontuário de um paciente que possui entradas de evolução
- **THEN** o sistema SHALL retornar o prontuário com as entradas ordenadas por data decrescente e status HTTP 200 (OK)

#### Scenario: Paciente sem prontuário
- **WHEN** um profissional consulta o prontuário de um paciente que ainda não possui nenhuma evolução registrada
- **THEN** o sistema SHALL retornar HTTP 404 (Not Found) com mensagem indicando que o paciente ainda não possui prontuário
