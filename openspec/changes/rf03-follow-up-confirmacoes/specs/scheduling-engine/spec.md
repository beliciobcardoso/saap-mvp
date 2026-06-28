## ADDED Requirements

### Requirement: Estado PENDING_RESPONSE na Máquina de Estados do Agendamento
O sistema SHALL suportar o estado `PENDING_RESPONSE` como um estado válido no enum `AppointmentStatus`, representando agendamentos que receberam notificação de follow-up e aguardam resposta do paciente. As transições válidas a partir de `PENDING_RESPONSE` são: → `CONFIRMED` (paciente confirmou via link), → `CANCELLED` (paciente cancelou via link ou prazo expirou com `autoCancelAfterNoResponse = true`). Transições ilegais a partir de `PENDING_RESPONSE` SHALL ser rejeitadas com `IllegalStateException`.

#### Scenario: Transição PENDING_RESPONSE → CONFIRMED
- **WHEN** um agendamento em status `PENDING_RESPONSE` recebe a ação de confirmação via endpoint público
- **THEN** o sistema SHALL transicionar o status para `CONFIRMED` e persistir a mudança

#### Scenario: Transição PENDING_RESPONSE → CANCELLED
- **WHEN** um agendamento em status `PENDING_RESPONSE` recebe a ação de cancelamento (via link do paciente ou por deadline automático)
- **THEN** o sistema SHALL transicionar o status para `CANCELLED` e persistir a mudança

#### Scenario: Transição ilegal a partir de PENDING_RESPONSE
- **WHEN** é solicitada uma transição inválida a partir de um agendamento em `PENDING_RESPONSE` (ex.: diretamente para `IN_PROGRESS`)
- **THEN** o sistema SHALL lançar `IllegalStateException` e abortar sem alterar o registro
