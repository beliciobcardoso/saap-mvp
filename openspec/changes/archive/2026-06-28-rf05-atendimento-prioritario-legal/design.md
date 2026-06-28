## Context

Atualmente, o SAAP possui a estrutura core de agendamentos e transição de estados, mas carece da funcionalidade completa de Atendimento Prioritário Legal (RF05/UC09). Embora a entidade `Appointment` e o caso de uso `CheckInAppointmentUseCase` possuam suporte básico para check-in e cálculo do score de prioridade, precisamos:
1. Validar e testar rigorosamente o fluxo de check-in e reversão de prioridade quando a verificação documental falha.
2. Criar a funcionalidade para profissionais de saúde (`ROLE_PROFESSIONAL`) solicitarem a chamada do próximo paciente da fila presencial do dia.
3. Implementar o registro imutável da trilha de auditoria para alterações de prioridade legal, salvando as ações de auditoria no banco de dados.
4. Proteger todos os endpoints usando Spring Security e RBAC.

## Goals / Non-Goals

**Goals:**
- Implementar a migração de banco de dados (`V6__criar_tabela_log_auditoria.sql`) para a tabela de log de auditoria.
- Criar a entidade de domínio `AuditLog` e seu mapeamento na camada de persistência.
- Criar o caso de uso `CallNextPatientUseCase` para chamar o próximo paciente com base no menor `priorityScore` (fila de prioridade inteligente).
- Integrar a gravação de logs de auditoria no `CheckInAppointmentUseCase` e no `CallNextPatientUseCase`.
- Expor e proteger os endpoints no `AppointmentController`:
  - `PUT /api/v1/appointments/{id}/check-in` (exige `ROLE_RECEPTIONIST`)
  - `POST /api/v1/appointments/next` (exige `ROLE_PROFESSIONAL`)
- Garantir cobertura de testes unitários e de integração de 100% para os novos fluxos.

**Non-Goals:**
- Implementação de painel de senhas ou frontend visual para exibição da chamada.
- Alteração no ciclo de vida existente do agendamento (máquina de estados), exceto a transição para `ARRIVED` e `CALLING`.
- Gestão de prontuários clínicos (UC04 / UC08), que será tratada em outra especificação de produto (RF06).

## Decisions

### 1. Obtenção do Profissional Logado na Chamada do Próximo Paciente
- **Decisão**: O endpoint de chamada do próximo paciente (`POST /api/v1/appointments/next`) não aceitará um `professionalId` no payload. Em vez disso, o sistema recuperará o email do usuário autenticado no contexto do Spring Security, buscará o `User` correspondente, e obterá o `Professional` associado a esse `User`.
- **Alternativa Considerada**: Passar o `professionalId` no corpo da requisição.
- **Razão da Escolha**: Segurança. Evita que um profissional cometa fraudes ou erros chamando pacientes de outros profissionais.

### 2. Algoritmo da Fila de Prioridades (Min-Heap no Banco)
- **Decisão**: Utilizar ordenação natural na consulta JPA para recuperar o agendamento com menor `priorityScore` e menor `dateTime` do dia atual.
- **Alternativa Considerada**: Implementar um `PriorityQueue` em memória na JVM.
- **Razão da Escolha**: Consistência e persistência de dados. Um heap na memória da JVM seria perdido em reinicializações do servidor e seria complexo de sincronizar em ambiente multi-instâncias (horizontal scaling). Delegar a ordenação ao PostgreSQL usando índices é performático e seguro.

### 3. Trilha de Auditoria no Banco de Dados
- **Decisão**: Criar a tabela `log_auditoria` via Flyway e persistir as alterações críticas de prioridade usando JPA síncrono.
- **Alternativa Considerada**: Escrever em logs de arquivo (SLF4J/Logback).
- **Razão da Escolha**: Conformidade com o PRD que exige tabela de auditoria inviolável e passível de consultas e relatórios administrativos futuros. O registro síncrono garante que se a transação do log falhar, a transação do negócio também seja desfeita, garantindo a integridade dos dados da auditoria.

## Risks / Trade-offs

- **[Risco] Múltiplas Chamadas Concorrentes pelo Mesmo Profissional**
  - **Mitigação**: O Use Case de chamada do paciente utilizará bloqueio pessimista (`PESSIMISTIC_WRITE` ou `@Version`) ao transicionar o agendamento para `CALLING`, garantindo que um mesmo paciente não seja chamado por dois profissionais simultaneamente.
- **[Risco] Divergências de Fuso Horário no Check-in Timestamp**
  - **Mitigação**: O timestamp do check-in utilizado para calcular o score será gerado usando o fuso UTC ou o relógio do servidor de banco de dados (`Instant.now().toEpochMilli()`), evitando inconsistências decorrentes do fuso horário da máquina cliente.
