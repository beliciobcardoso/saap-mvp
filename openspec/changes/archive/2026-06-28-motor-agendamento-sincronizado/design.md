## Context

Atualmente, o SAAP MVP gerencia o CRUD básico de entidades core, mas não possui suporte para o ciclo de vida e reservas de agendamentos. A fim de permitir a operação real da clínica, precisamos expor endpoints para agendamentos, validar disponibilidade de horários e assegurar robustez sob acessos concorrentes para evitar *double-booking*.

## Goals / Non-Goals

**Goals:**
- Criar a modelagem e persistência para a entidade `Appointment` (`Agendamento`).
- Garantir isolamento transacional e unicidade de slot por profissional, impedindo double-booking.
- Implementar uma máquina de estados rígida para as transições de status da consulta com validação automática.
- Proteger os novos endpoints REST com controle RBAC granular.

**Non-Goals:**
- Implementar rotinas automáticas de follow-up (@Scheduled) ou fila de espera (waitlist) neste ciclo específico.
- Implementar a grade de horários de folga ou bloqueios de feriados do profissional (configurações avançadas de agenda).

## Decisions

### 1. Garantia de Unicidade e Prevenção de Double-Booking
- **Decisão**: Utilizar uma abordagem combinada em duas camadas:
  1. **Nível de Aplicação (JPA/Hibernate)**: Adicionar validação lógica nos Use Cases fazendo uma consulta prévia por agendamentos ativos no mesmo horário com `@Lock(LockModeType.PESSIMISTIC_WRITE)` na tabela/slot ou na tabela de profissional, ou usando transação serializável.
  2. **Nível de Banco de Dados (Flyway Migration)**: Criar um índice de unicidade parcial (Partial Unique Index) no PostgreSQL para impedir inserções duplicadas mesmo que a validação de aplicação sofra com condições de corrida extremas.
     ```sql
     CREATE UNIQUE INDEX idx_agendamento_prof_data_hora_ativo 
     ON agendamento (profissional_id, data_hora) 
     WHERE status NOT IN ('CANCELLED', 'NO_SHOW');
     ```
- **Razão**: A validação na aplicação fornece feedback e tratamento amigável de erros, enquanto o índice único no banco de dados garante integridade absoluta com baixo custo de performance.
- **Alternativas consideradas**: Apenas validação em memória (suscetível a condições de corrida) ou lock pessimista global (reduz consideravelmente o throughput da aplicação).

### 2. Máquina de Estados Encapsulada no Domínio
- **Decisão**: Encapsular as regras de transição de status em um método de domínio na entidade `Appointment` (`transitionTo(AppointmentStatus newStatus)`).
- **Razão**: Mantém a lógica de negócio pura no modelo de domínio (coesa com os princípios de DDD e Orientação a Objetos documentados no `docs/rascunho.md`), prevenindo que camadas de infraestrutura alterem o estado de forma inconsistente.
- **Alternativas consideradas**: Lógica de transição espalhada nos Use Cases. Descartada por violar o encapsulamento.

### 3. Autorização REST baseada em Security Rules (RBAC)
- **Decisão**: Anotar os métodos do `AppointmentController` com `@PreAuthorize`, validando papéis específicos:
  - Cadastro de consulta: `hasAnyRole('ADMIN', 'RECEPCIONISTA', 'PACIENTE')`
  - Check-in e confirmação: `hasAnyRole('ADMIN', 'RECEPCIONISTA')`
  - Início/Conclusão de consulta: `hasAnyRole('ADMIN', 'PROFISSIONAL_SAUDE')`
  - Visualização de agendas: Filtragem no repositório de acordo com o usuário autenticado (`ROLE_PROF` só vê seus pacientes, `ROLE_PATIENT` só vê suas próprias consultas).

## Risks / Trade-offs

- **[Risco] Alta Latência com Trava Pessimista**: Usar `@Lock(LockModeType.PESSIMISTIC_WRITE)` pode travar linhas de tabelas relacionadas, gerando deadlocks sob altíssima concorrência.
  - *Mitigação*: Garantir que as transações de reserva sejam extremamente curtas e rápidas. O uso do índice único parcial no PostgreSQL serve como uma segunda linha de defesa robusta e otimizada que não necessita de locks pessimistas longos na aplicação.
- **[Risco] Alterações Concorrentes de Status**: Duas secretárias tentando confirmar e cancelar a mesma consulta ao mesmo tempo.
  - *Mitigação*: Adicionar a anotação `@Version` na entidade `AppointmentEntity` para habilitar o controle de concorrência otimista (Optimistic Locking) para modificações de status.
