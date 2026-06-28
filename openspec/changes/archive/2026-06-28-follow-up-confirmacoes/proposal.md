## Why

Atualmente, as consultas agendadas permanecem com o status `PENDING` até que uma recepcionista confirme manualmente ou que o paciente compareça. Isso resulta em uma alta taxa de ausência (*no-show*). O sistema necessita de um mecanismo automático em background para disparar e-mails de confirmação proativa para consultas do dia seguinte, permitindo que os pacientes confirmem ou cancelem suas consultas através de links dedicados sem intervenção humana.

## What Changes

- Criar um worker agendado (`@Scheduled`) executado a cada 1 hora para buscar consultas elegíveis (status `PENDING` marcadas para o dia seguinte).
- Implementar um serviço de notificação simulada que envia e-mails contendo links de confirmação e cancelamento.
- Expor dois novos endpoints públicos de callback para processar a interação do paciente com os links de confirmação/cancelamento:
  - `GET /api/v1/appointments/public/confirm?token=<token>` (altera status para `CONFIRMED`)
  - `GET /api/v1/appointments/public/cancel?token=<token>` (altera status para `CANCELLED`)
- Implementar a geração e validação de tokens JWT auto-contidos de curta duração contendo o ID do agendamento e a ação pretendida, garantindo que os links públicos sejam seguros e não requeiram login completo no sistema.

## Capabilities

### New Capabilities
- `appointment-follow-up`: Rotinas de envio de follow-up proativo e processamento de callbacks públicos para confirmação/cancelamento de consultas.

### Modified Capabilities

## Impact

- Afeta `AppointmentController` com a adição de rotas públicas.
- Nova infraestrutura de agendamento de tarefas (`@EnableScheduling` e `@Scheduled`).
- Configurações do Spring Security para expor rotas públicas específicas do fluxo de confirmação.
