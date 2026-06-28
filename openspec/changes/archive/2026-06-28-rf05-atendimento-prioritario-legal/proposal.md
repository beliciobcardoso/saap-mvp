## Why

Garantir conformidade com a Lei Federal 10.048/2000 no atendimento presencial do SAAP, estruturando um fluxo inteligente de fila de prioridades onde as declarações de prioridade sejam obrigatoriamente validadas pela recepção de forma auditável e os pacientes sejam chamados de forma otimizada e justa.

## What Changes

- **Check-in e Validação de Prioridade**:
  - Fluxo obrigatório de validação de documentos de prioridade durante o check-in presencial realizado por recepcionistas (`ROLE_RECEP`).
  - Atualização do status do agendamento para `ARRIVED`, persistindo o nível de prioridade confirmado (`PriorityLevel` P1 a P5), observações do documento apresentado, e o ID da recepcionista responsável.
- **Algoritmo de Score da Fila Prioritária**:
  - Cálculo automático do `priorityScore` no check-in: `(priorityLevel * 10^12) + checkInTimestamp`.
  - Ordenação natural da fila de chamada baseada no menor score (Min-Heap no banco de dados).
- **Chamada do Próximo Paciente**:
  - Funcionalidade para profissionais de saúde (`ROLE_PROF`) solicitarem a chamada do próximo paciente da fila presencial (`CALLING`), buscando o agendamento com menor score para o dia e profissional logado.
- **Trilha de Auditoria Imutável**:
  - Registro detalhado e inviolável de todas as validações e alterações de prioridade na tabela de auditoria (`log_auditoria`).

## Capabilities

### New Capabilities
- `priority-attendance`: Registro de check-in presencial com validação documental de prioridades, cálculo do score de prioridade da fila, chamada do próximo paciente pelo profissional e auditoria de alterações.

### Modified Capabilities

## Impact

- **Banco de Dados**:
  - Nova tabela `log_auditoria` para registro imutável das ações críticas de auditoria.
- **Use Cases**:
  - Criação de `CheckInAppointmentUseCase` para gerenciar a chegada e validação de prioridades.
  - Criação de `CallNextPatientUseCase` para gerenciar a chamada de pacientes pelo profissional.
- **Endpoints**:
  - `POST /api/v1/appointments/{id}/check-in` para realização de check-in com parâmetros de validação de prioridade.
  - `POST /api/v1/appointments/next` para profissional chamar o próximo paciente.
- **Segurança**:
  - Proteção de endpoints utilizando Spring Security com RBAC, restringindo check-in para `ROLE_RECEP` e chamada para `ROLE_PROF`.
