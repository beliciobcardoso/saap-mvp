## Context

Atualmente, quando um agendamento é cancelado no SAAP-MVP (seja por clique do paciente, ação da recepção ou no-show de confirmação), o slot de data/hora correspondente é simplesmente liberado na grade de horários do profissional. Isso gera tempo ocioso e exige que pacientes interessados fiquem monitorando manualmente por vagas livres.
Este design especifica a Fila de Espera Inteligente (UC05), que gerencia automaticamente a alocação dinâmica desses slots ociosos para pacientes cadastrados em uma lista de espera FIFO.

## Goals / Non-Goals

**Goals:**
- Criar a entidade e a tabela para armazenar as intenções de espera dos pacientes (`WaitlistEntry`).
- Integrar o trigger de cancelamento para oferecer a vaga de forma automatizada ao paciente líder da fila correspondente (mesmo profissional e serviço).
- Oferecer endpoints públicos sem autenticação segura com prazo de 30 minutos para aceitação/recusa.
- Implementar um scheduler periódico para tratar expirações de ofertas de vaga e repassar para o próximo da fila.

**Non-Goals:**
- Permitir que pacientes mudem de posição na fila manualmente (a ordem é estritamente FIFO/timestamp de cadastro).
- Gerenciar remunerações ou pagamentos antecipados na fila de espera.

## Decisions

### 1. Modelo de Dados: Entidade `WaitlistEntry`
Criação da tabela `fila_espera` no banco de dados e mapeamento das classes correspondentes na camada de domínio e infraestrutura.
- **Alternativa A**: Armazenar em uma coluna de JSON no cadastro do profissional.
- **Alternativa B (Escolhida)**: Criar uma tabela relacional dedicada para `WaitlistEntry` com relacionamentos claros (Paciente, Profissional, Serviço). Permite indexação eficiente de consultas FIFO e alteração de status individual sem locks globais.

### 2. Mecanismo de Disparo de Oferta
Como acoplar o cancelamento com o processamento da fila.
- **Alternativa A**: Publicação de eventos assíncronos (Spring events / EventBus).
- **Alternativa B (Escolhida)**: Acoplamento síncrono transacional chamando o Use Case `ProcessWaitlistOfferUseCase` diretamente no fluxo de cancelamento. Isso garante consistência imediata da transação do banco de dados (se o cancelamento falhar, a fila não é notificada).

### 3. Mecanismo de Ações Públicas via JWT
A aceitação ou recusa da oferta de vaga deve ocorrer de forma anônima e rápida via e-mail/notificação.
- **Alternativa A**: Exigir login completo no sistema.
- **Alternativa B (Escolhida)**: Geração de tokens JWT seguros auto-contidos específicos para a fila de espera contendo o ID da entrada da fila e a ação (`accept` ou `decline`). Os endpoints correspondentes em `/api/v1/appointments/public/waitlist/**` serão liberados de autenticação no Spring Security.

## Risks / Trade-offs

- **Concorrência no Slot de Agendamento** → Se a recepcionista agendar manualmente um paciente na mesma vaga liberada enquanto o paciente da fila de espera está no meio do seu timeout de 30 minutos.
  - *Mitigação*: No Use Case de aceitação (`accept`), o sistema validará se a vaga ainda está disponível (sem consultas ativas naquele profissional/horário). Se não estiver, retornará uma resposta amigável informando que a vaga foi preenchida administrativamente e manterá o paciente ativo na fila de espera para a próxima oportunidade.
