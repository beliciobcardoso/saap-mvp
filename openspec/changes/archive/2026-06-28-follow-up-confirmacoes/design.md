## Context

O SAAP MVP precisa automatizar o fluxo de confirmações de consultas marcadas como `PENDING`. Atualmente, as consultas dependem de intervenção manual da recepção para transição de status. A automação reduz o no-show e poupa tempo administrativo. Como os links de confirmação e cancelamento são acessados de fora da aplicação corporativa pelos pacientes (geralmente via e-mail), precisamos expor rotas públicas sem login completo do Spring Security, garantindo segurança contra adulterações através de Action Tokens seguros baseados em JWT auto-contidos.

## Goals / Non-Goals

**Goals:**
- Criar a infraestrutura de agendamento de tarefas no Spring Boot (`@Scheduled`).
- Desenvolver um worker automático rodando de hora em hora que filtre consultas `PENDING` marcadas para o dia seguinte.
- Implementar geração e decodificação segura de tokens JWT temporários de ação (`AppointmentActionToken`).
- Expor endpoints públicos para confirmação e cancelamento via query parameter `token`.
- Enviar notificações simuladas no console do sistema.

**Non-Goals:**
- Integrar com serviços reais de envio de e-mail (SMTP/SES) ou SMS/WhatsApp neste ciclo (notificação será mockada em console log).
- Permitir re-agendamento ou escolha de novos horários via interface pública (apenas confirmar ou cancelar).

## Decisions

### 1. Mecanismo de Execução Agendada (Background Scheduler)
- **Decisão**: Utilizar o `@Scheduled` nativo do Spring Boot associado a `@EnableScheduling`. O job rodará com a expressão cron `0 0 * * * *` (a cada hora cheia).
- **Razão**: Simplicidade e robustez para a escala atual do MVP.
- **Alternativas consideradas**: Quartz Scheduler ou Spring Batch (excessivos para um MVP com apenas uma tarefa em background).

### 2. Segurança via Tokens Auto-contidos (Action Tokens JWT)
- **Decisão**: Gerar tokens JWT compactos contendo o ID da consulta e a ação desejada (`confirm` ou `cancel`), assinados com a mesma chave secreta da aplicação e com tempo de expiração curto (24 horas).
- **Razão**: Dispensa o armazenamento de estado dos tokens no banco de dados, facilitando a validação stateless e segura sem login.
- **Alternativas consideradas**: Salvar tokens aleatórios (UUIDs) no banco de dados em uma tabela `action_token` (exige gerenciamento de ciclo de vida e limpezas recorrentes no banco).

### 3. Exposição de Endpoints Públicos
- **Decisão**: Adicionar a regra `.requestMatchers("/api/v1/appointments/public/**").permitAll()` no `SecurityConfig` do Spring Security. Os endpoints receberão a requisição GET e validarão internamente a integridade do token antes de executar a transição de estado da consulta.
- **Razão**: Permite acesso direto pelos links gerados sem exigir login do paciente no sistema. A integridade é garantida pela assinatura criptográfica do JWT.

## Risks / Trade-offs

- **[Risco] Envio duplicado de notificações**: Se o worker rodar a cada hora, ele pode processar a mesma consulta múltiplas vezes se não houver um controle de controle de envio.
  - *Mitigação*: Adicionar um campo `follow_up_sent` (booleano) na tabela `agendamento` para marcar que a notificação já foi disparada, evitando reprocessamentos desnecessários.
- **[Risco] Fraude ou alteração maliciosa**: Um atacante tentando confirmar/cancelar consultas de outros pacientes chutando IDs.
  - *Mitigação*: A validação criptográfica do token JWT impede qualquer alteração. Se o token for modificado, a assinatura falhará e a rota retornará 400 Bad Request.
