## MODIFIED Requirements

### Requirement: Processamento de Worker Agendado para Confirmação
O sistema SHALL executar um worker agendado a cada 1 hora para selecionar todos os agendamentos com status `PENDING` cujas datas de início estejam dentro da janela de confirmação configurada (`clinic.settings.confirmation-window-hours`, padrão 48h) **e que ainda não tenham sido notificados** (`followUpSentAt IS NULL`). Para cada registro elegível, o sistema SHALL gerar um token JWT seguro contendo o ID do agendamento e a ação permitida, disparar uma notificação simulada por e-mail com os links públicos de confirmação e cancelamento, transicionar o status para `PENDING_RESPONSE` e registrar o timestamp de envio em `followUpSentAt`.

#### Scenario: Envio com sucesso para agendamentos pendentes elegíveis
- **WHEN** o worker agendado executa a varredura periódica e encontra agendamentos elegíveis com status `PENDING` dentro da janela de confirmação e `followUpSentAt IS NULL`
- **THEN** o sistema SHALL gerar tokens seguros exclusivos, disparar e-mails simulados com os links públicos, transicionar o status para `PENDING_RESPONSE` e gravar `followUpSentAt`

#### Scenario: Ignorar agendamentos já notificados ou com outros status
- **WHEN** o worker agendado encontra agendamentos com status diferente de `PENDING`, agendamentos com `followUpSentAt` já preenchido, ou agendamentos `PENDING` fora da janela de confirmação
- **THEN** o sistema SHALL ignorar esses registros e não enviar notificações

---

### Requirement: Endpoints Públicos de Confirmação e Cancelamento por Link
O sistema SHALL expor endpoints HTTP GET públicos para receber os callbacks das ações do paciente contendo o token de ação seguro na query parameter. O sistema SHALL validar o token, decodificar o ID do agendamento e a ação pretendida (confirmar/cancelar), e realizar a transição de estado no banco de dados **a partir do estado `PENDING_RESPONSE`** sem exigir autenticação completa do usuário.

#### Scenario: Confirmação com sucesso via link público
- **WHEN** o paciente acessa o link público `/api/v1/appointments/public/confirm` com um token válido e ativo e o agendamento está em status `PENDING_RESPONSE`
- **THEN** o sistema SHALL atualizar o status do agendamento para `CONFIRMED`, salvar no banco de dados e retornar mensagem de sucesso

#### Scenario: Cancelamento com sucesso via link público
- **WHEN** o paciente acessa o link público `/api/v1/appointments/public/cancel` com um token válido e ativo e o agendamento está em status `PENDING_RESPONSE`
- **THEN** o sistema SHALL atualizar o status do agendamento para `CANCELLED`, salvar no banco de dados e retornar mensagem de sucesso

#### Scenario: Erro com token inválido ou expirado
- **WHEN** o paciente acessa os endpoints públicos com um token adulterado, expirado ou corrompido
- **THEN** o sistema SHALL rejeitar a operação e retornar erro HTTP 400 (Bad Request)

#### Scenario: Erro com agendamento em estado inválido para resposta via link
- **WHEN** o paciente acessa os endpoints públicos com um token válido mas o agendamento já está em status diferente de `PENDING_RESPONSE` (ex.: já cancelado, já confirmado)
- **THEN** o sistema SHALL rejeitar a operação e retornar erro HTTP 409 (Conflict) com mensagem explicativa
