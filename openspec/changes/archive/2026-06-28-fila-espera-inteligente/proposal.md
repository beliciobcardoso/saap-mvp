## Why

Reduzir o tempo ocioso da clínica e otimizar a agenda médica ao realocar automaticamente horários liberados devido a cancelamentos de consultas para pacientes que aguardam na fila de espera, garantindo equidade pelo critério FIFO.

## What Changes

- **Novas Entidades**:
  - Introdução da entidade `WaitlistEntry` para representar os pacientes na fila de espera para um profissional e serviço específicos, registrando seu estado de prioridade, timestamp de entrada e status de ofertas.
- **Novas APIs e Fluxos Operacionais**:
  - Trigger automático no cancelamento de consultas: ao cancelar um agendamento, o sistema busca e oferece a vaga ao primeiro paciente qualificado na fila.
  - Endpoints públicos sem autenticação para aceitação/recusa da vaga via link seguro (`/api/v1/appointments/public/waitlist/accept` e `/api/v1/appointments/public/waitlist/decline`).
  - Worker em background para expiração de ofertas não respondidas dentro do timeout configurado (padrão 30 minutos).
- **Modificações**:
  - Acoplamento do Use Case de Cancelamento para disparar o Use Case de processamento da Fila de Espera se a configuração de auto-preenchimento estiver ativa.

## Capabilities

### New Capabilities
- `waitlist`: Gerenciamento da fila de espera (WaitlistEntry), processamento automático de vagas ociosas decorrentes de cancelamentos, disparo de notificações com tokens de ação e controle de expiração por timeout.

### Modified Capabilities

## Impact

- **Banco de Dados**: Nova tabela `fila_espera` para armazenar as entradas da fila de espera (`WaitlistEntry`).
- **Use Cases**: Modificação em `CancelAppointmentUseCase` para invocar o fluxo de processamento de fila.
- **REST Controller**: Novos endpoints públicos expostos em `AppointmentController` para processar ações do paciente da fila de espera.
- **Segurança**: Ajustes em `SecurityConfig` para permitir acesso anônimo nos callbacks da fila de espera.
