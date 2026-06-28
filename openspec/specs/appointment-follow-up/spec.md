# appointment-follow-up Specification

## Purpose
TBD - created by archiving change follow-up-confirmacoes. Update Purpose after archive.
## Requirements
### Requirement: Processamento de Worker Agendado para Confirmação
O sistema SHALL executar um worker agendado a cada 1 hora para selecionar todos os agendamentos agendados para o dia seguinte que estejam com o status `PENDING`. Para cada registro elegível, o sistema SHALL gerar um token JWT seguro contendo o ID do agendamento e a ação permitida, e disparar uma notificação simulada por e-mail com os links públicos de confirmação e cancelamento.

#### Scenario: Envio com sucesso para agendamentos pendentes do dia seguinte
- **WHEN** o worker agendado executa a varredura periódica e encontra agendamentos elegíveis com data de início para o dia seguinte e status `PENDING`
- **THEN** o sistema SHALL gerar tokens seguros exclusivos e disparar e-mails simulados contendo os links públicos correspondentes

#### Scenario: Ignorar agendamentos em outros status ou datas futuras distantes
- **WHEN** o worker agendado encontra agendamentos no dia seguinte com status `CONFIRMED` ou agendamentos em `PENDING` para datas que não sejam do dia seguinte
- **THEN** o sistema SHALL ignorar esses registros e não enviar notificações de confirmação

### Requirement: Endpoints Públicos de Confirmação e Cancelamento por Link
O sistema SHALL expor endpoints HTTP GET públicos para receber os callbacks das ações do paciente contendo o token de ação seguro na query parameter. O sistema SHALL validar o token, decodificar o ID do agendamento e a ação pretendida (confirmar/cancelar), e realizar a transição de estado no banco de dados sem exigir autenticação completa do usuário.

#### Scenario: Confirmação com sucesso via link público
- **WHEN** o paciente acessa o link público `/api/v1/appointments/public/confirm` com um token válido e ativo
- **THEN** o sistema SHALL atualizar o status do agendamento para `CONFIRMED`, salvar no banco de dados e retornar mensagem de sucesso

#### Scenario: Cancelamento com sucesso via link público
- **WHEN** o paciente acessa o link público `/api/v1/appointments/public/cancel` com um token válido e ativo
- **THEN** o sistema SHALL atualizar o status do agendamento para `CANCELLED`, salvar no banco de dados e retornar mensagem de sucesso

#### Scenario: Erro com token inválido ou expirado
- **WHEN** o paciente acessa os endpoints públicos com um token adulterado, expirado ou corrompido
- **THEN** o sistema SHALL rejeitar a operação e retornar erro HTTP 400 (Bad Request)

