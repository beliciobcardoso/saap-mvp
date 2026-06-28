# waitlist Specification

## Purpose
TBD - created by archiving change fila-espera-inteligente. Update Purpose after archive.
## Requirements
### Requirement: Registro de Entrada na Fila de Espera
O sistema SHALL permitir o cadastro de um paciente em uma fila de espera associando-o a um profissional e um serviço específico, mantendo a entrada ativa (`isActive = true`), o status `WAITING` e registrando o timestamp de criação para ordenação FIFO.

#### Scenario: Inserção com sucesso na fila de espera
- **WHEN** uma solicitação de cadastro na fila de espera for feita com paciente, profissional e serviço válidos
- **THEN** o sistema SHALL registrar a entrada com `status = WAITING`, `isActive = true` e o timestamp atual

### Requirement: Disparo Automático de Oferta na Desistência
Ao cancelar uma consulta (mudança de status para `CANCELLED`), o sistema SHALL buscar a entrada ativa na fila de espera mais antiga (FIFO) correspondente ao mesmo profissional e serviço. O sistema SHALL mudar seu status para `OFFERED`, calcular a expiração da oferta (30 minutos) e enviar uma notificação simulada com os links públicos de aceitação e recusa da vaga.

#### Scenario: Consulta cancelada e oferta disparada para o primeiro da fila
- **WHEN** um agendamento é cancelado e existe paciente ativo na fila de espera para o mesmo profissional e serviço
- **THEN** o sistema SHALL marcar o primeiro da fila como `OFFERED`, definir o timeout de expiração para 30 minutos e disparar notificação simulada de oferta

#### Scenario: Consulta cancelada e fila de espera vazia
- **WHEN** um agendamento é cancelado mas não existem pacientes ativos na fila de espera para aquele profissional e serviço
- **THEN** o sistema SHALL apenas registrar o cancelamento da consulta sem disparar ofertas

### Requirement: Aceitação de Vaga da Fila de Espera via Callback Público
O sistema SHALL expor um endpoint público GET `/api/v1/appointments/public/waitlist/accept` para receber os cliques de aceitação. O sistema SHALL validar o token de ação seguro na query parameter. Se o token for válido e a oferta não tiver expirado ou sido cancelada, o sistema SHALL criar um novo agendamento com status `CONFIRMED` para o paciente no horário oferecido, atualizando o status da entrada para `ACCEPTED` e `isActive = false`.

#### Scenario: Aceitação de vaga com sucesso
- **WHEN** o paciente da fila clica no link de aceitação `/api/v1/appointments/public/waitlist/accept` com um token de oferta ativa válido
- **THEN** o sistema SHALL atualizar a entrada para `status = ACCEPTED` e `isActive = false`, criar o novo agendamento no horário liberado com status `CONFIRMED` e retornar mensagem de confirmação

### Requirement: Recusa de Vaga da Fila de Espera via Callback Público
O sistema SHALL expor um endpoint público GET `/api/v1/appointments/public/waitlist/decline` para receber os cliques de recusa. O sistema SHALL validar o token na query parameter, atualizar a entrada para `status = DECLINED` e `isActive = false`, e acionar imediatamente o disparo da oferta para o próximo paciente da fila para o mesmo slot ocioso.

#### Scenario: Recusa de vaga com sucesso
- **WHEN** o paciente da fila clica no link de recusa `/api/v1/appointments/public/waitlist/decline` com um token válido
- **THEN** o sistema SHALL atualizar a entrada para `status = DECLINED` e `isActive = false` e buscar o próximo paciente da fila para disparar a oferta imediatamente

### Requirement: Expiração de Oferta por Timeout
O sistema SHALL executar um worker agendado periodicamente para verificar ofertas com status `OFFERED` cujo prazo limite tenha expirado. O sistema SHALL marcar essas entradas como `EXPIRED` e `isActive = false`, e disparar automaticamente a oferta para o próximo paciente da fila.

#### Scenario: Expiração por tempo limite atingido
- **WHEN** o worker de timeout executa e detecta uma oferta `OFFERED` com timestamp atual maior que o tempo limite definido
- **THEN** o sistema SHALL atualizar a entrada para `status = EXPIRED` e `isActive = false` e oferecer a vaga para o próximo paciente da fila

