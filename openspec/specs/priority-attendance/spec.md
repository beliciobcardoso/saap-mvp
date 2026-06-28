# priority-attendance Specification

## Purpose
TBD - created by archiving change rf05-atendimento-prioritario-legal. Update Purpose after archive.
## Requirements
### Requirement: Validação Documental de Prioridade Legal no Check-In
O sistema SHALL permitir que a recepcionista realize o check-in presencial de um paciente e valide fisicamente/documentalmente a condição de prioridade legal declarada. Se a validação for positiva, o nível de prioridade legal informado é mantido e gravado. Se a validação for negativa, o nível de prioridade legal SHALL ser redefinido para P5 (NORMAL) com o respectivo motivo registrado. O status do agendamento SHALL ser atualizado para ARRIVED.

#### Scenario: Check-in presencial com prioridade validada positivamente
- **WHEN** a recepcionista realiza o check-in de um agendamento com prioridade declarada P1, confirmando a validação física e informando o documento comprovatório "Laudo de TEA"
- **THEN** o sistema SHALL atualizar o status do agendamento para ARRIVED, manter a prioridade legal como P1, e registrar as notas "Laudo de TEA" e o ID do usuário verificador

#### Scenario: Check-in presencial com prioridade declarada inválida ou rejeitada
- **WHEN** a recepcionista realiza o check-in de um agendamento com prioridade declarada P1, mas indica que a validação falhou e justifica "Ausência de laudo ou comprovante"
- **THEN** o sistema SHALL atualizar o status do agendamento para ARRIVED, redefinir a prioridade legal para P5 (NORMAL), e registrar as notas "Ausência de laudo ou comprovante" e o ID do usuário verificador

### Requirement: Cálculo do Score de Prioridade da Fila Presencial
O sistema SHALL calcular automaticamente o priorityScore de um agendamento ao realizar o check-in presencial. O cálculo SHALL obedecer à formula: priorityScore = (priorityLevelValue * 10^12) + checkInTimestamp, garantindo que a ordenação seja justa pelo nível de prioridade legal seguido pelo timestamp de chegada (FIFO dentro do mesmo nível).

#### Scenario: Cálculo do priorityScore com sucesso no check-in
- **WHEN** o check-in é realizado para um paciente com prioridade confirmada P2 no timestamp 1782739200000
- **THEN** o sistema SHALL salvar o priorityScore do agendamento como 2001782739200000

### Requirement: Chamada do Próximo Paciente da Fila pelo Profissional
O sistema SHALL permitir que um profissional de saúde chame o próximo paciente de sua fila presencial para o dia corrente. O sistema SHALL selecionar o agendamento em estado ARRIVED associado a esse profissional no dia de hoje com o menor priorityScore (Min-Heap), mudando seu status para CALLING.

#### Scenario: Chamada bem-sucedida do próximo paciente da fila presencial
- **WHEN** o profissional solicita a chamada do próximo paciente e existem agendamentos no estado ARRIVED para ele na data de hoje
- **THEN** o sistema SHALL selecionar o agendamento com menor priorityScore, alterar seu status para CALLING e retornar o agendamento selecionado

#### Scenario: Chamada do próximo paciente com a fila presencial vazia
- **WHEN** o profissional solicita a chamada do próximo paciente mas não há agendamentos no estado ARRIVED para ele na data de hoje
- **THEN** o sistema SHALL lançar uma exceção de negócio informando que a fila de atendimento está vazia e retornar status HTTP 400 (Bad Request)

### Requirement: Trilha de Auditoria Imutável de Alteração de Prioridades
O sistema SHALL registrar uma entrada de auditoria imutável na tabela log_auditoria para todas as ações críticas de check-in e alteração/validação de prioridades, registrando o timestamp, usuário executor, ação, recurso afetado (ID do agendamento) e IP de origem.

#### Scenario: Auditoria registrada ao realizar check-in com validação de prioridade
- **WHEN** o use case de check-in é executado e finalizado com sucesso
- **THEN** o sistema SHALL gravar uma entrada de auditoria na tabela log_auditoria com os dados do operador, data_hora e a alteração realizada

### Requirement: Controle de Acesso Baseado em Papéis (RBAC) para Endpoints da Fila
O sistema SHALL restringir as ações de check-in presencial e chamada de pacientes. O endpoint de check-in SHALL exigir a role ROLE_RECEP, e o endpoint de chamada do próximo paciente SHALL exigir a role ROLE_PROF.

#### Scenario: Usuário com perfil inadequado tenta realizar check-in
- **WHEN** um usuário autenticado com perfil ROLE_PROF tenta invocar o endpoint de check-in presencial
- **THEN** o sistema SHALL bloquear a requisição e retornar status HTTP 403 (Forbidden)

#### Scenario: Usuário com perfil adequado realiza check-in com sucesso
- **WHEN** um usuário autenticado com perfil ROLE_RECEP invoca o endpoint de check-in presencial com dados válidos
- **THEN** o sistema SHALL processar a requisição e retornar o agendamento atualizado com status HTTP 200 (OK)

