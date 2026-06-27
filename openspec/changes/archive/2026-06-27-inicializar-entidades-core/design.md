## Context

O SAAP está no estágio inicial de desenvolvimento. Para suportar as regras de negócio de agendamento e atendimento, precisamos criar os modelos e a estrutura de dados base para os atores principais do sistema: `User` (Usuário), `Patient` (Paciente), `Professional` (Profissional de Saúde) e `Service` (Serviço).

## Goals / Non-Goals

**Goals:**
- Definir os modelos de domínio puro de `User`, `Patient` (incluindo CPF e Cartão SUS), `Professional` e `Service` na camada `domain`.
- Configurar os mapeamentos JPA e entidades de banco de dados (`*Entity`) correspondentes na camada `infrastructure.persistence` (garantindo unicidade de CPF e SUS).
- Criar a migração Flyway inicial (`V1__criar_tabelas_core.sql`) para criar o esquema de tabelas correspondente no PostgreSQL.
- Implementar os repositórios (ports no domínio, adapters em infraestrutura de persistência) com suporte a exclusão lógica (*soft delete*) para profissionais e serviços.
- Desacoplar modelos de domínio e entidades de banco de dados usando mappers (MapStruct).

**Non-Goals:**
- Implementar lógica de autenticação ativa (JWT, login endpoints) ou filtros de segurança complexos neste momento (apenas a estrutura de `User` e a criptografia com BCrypt).
- Implementar as entidades de agendamento (`Appointment`), fila de prioridades presencial, ou prontuário clínico (serão tratadas em changes futuros).

## Decisions

### 1. Separação de Classes de Domínio e Entidades JPA
- **Decisão:** Criar classes de modelo puras no pacote `domain.model` (ex: `Patient`, `User`) livre de qualquer dependência do Spring Data ou Hibernate, e classes correspondentes no pacote `infrastructure.persistence.entity` (ex: `PatientEntity`, `UserEntity`) mapeadas com anotações JPA. A conversão entre elas será realizada pelo **MapStruct**.
- **Razão:** Garante conformidade estrita com a Clean Architecture. Se precisarmos trocar o JPA/Hibernate por outra ferramenta de persistência no futuro, o núcleo de negócios do sistema não será afetado.
- **Alternativa Considerada:** Colocar anotações JPA diretamente nas classes de domínio. Embora reduza o número de classes, acopla o domínio às ferramentas de persistência, violando a separação de responsabilidades.

### 2. Padrão de Exclusão Lógica (Soft Delete) para Professional e Service
- **Decisão:** Utilizar o campo `is_active` (boolean) nas tabelas `profissional` e `servico`. Nas entidades JPA correspondentes, utilizaremos a anotação `@SQLRestriction("is_active = true")` (ou `@Where` nas versões mais antigas do Hibernate) para que todas as buscas automáticas de repositories filtrem apenas registros ativos por padrão. As operações de deleção serão customizadas para atualizar `is_active` para false.
- **Razão:** O histórico financeiro e clínico precisa ser mantido. A exclusão física de profissionais ou serviços geraria chaves estrangeiras quebradas ou perda de rastreabilidade.
- **Alternativa Considerada:** Permitir exclusão física e manter tabelas de log separadas para histórico. Isso aumentaria significativamente a complexidade de modelagem e consultas.

### 3. Autenticação e Criptografia
- **Decisão:** A senha do usuário (`User`) será persistida de forma criptografada usando o `BCryptPasswordEncoder` do Spring Security. O campo `role` será baseado no enum `UserRole` representando os perfis RBAC do sistema (`ADMIN`, `RECEPTIONIST`, `PROFESSIONAL`, `ASSISTANT`, `PATIENT`).
- **Razão:** Segurança obrigatória e conformidade inicial com a LGPD no manuseio de credenciais.

### 4. Nomenclatura de Atributos Booleanos (active vs isActive)
- **Decisão:** Declarar atributos booleanos sem o prefixo verbal "is" nas classes Java (ex: `private boolean active;` em vez de `private boolean isActive;`), garantindo que o Lombok gere os métodos `isActive()` como getter e `setActive(boolean)` como setter.
- **Razão:** Previne conflitos de compilação no MapStruct (erros de propriedades não mapeadas/unmapped target property) e problemas potenciais de desserialização (como no Jackson). O MapStruct resolve a propriedade pelo nome `active`, o que entra em conflito direto com atributos declarados fisicamente com a palavra literal `isActive`. O mapeamento físico da coluna no banco de dados com JPA continua usando `@Column(name = "is_active")`, preservando a consistência do schema do banco.

## Risks / Trade-offs

- **[Risco] Sobrecarga de Mapeamento (Boilerplate):** A separação entre Model e Entity adiciona classes duplicadas e a necessidade de mappers.
  - *Mitigação:* Uso do Lombok para geração automática de construtores, getters e setters, e MapStruct para geração automática do código de conversão (mapeamento) em tempo de compilação.
- **[Risco] Inconsistência na Exclusão Lógica:** Consultas nativas SQL escritas manualmente na persistência podem esquecer de filtrar por `is_active = true`.
  - *Mitigação:* Revisar e padronizar o uso de Queries do Spring Data JPA e mapear devidamente testes de integração para verificar o comportamento do soft delete.
