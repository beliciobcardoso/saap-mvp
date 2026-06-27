## Why

Para implementar os fluxos de agendamento, atendimento e controle de prioridades do SAAP, é necessário primeiro estabelecer a base de dados e os modelos de domínio do sistema. Este change inicializa as entidades centrais (*core*) que servirão de fundação para todas as regras de negócio subsequentes.

## What Changes

- Criação dos modelos de domínio puro para as entidades principais: `User`, `Patient`, `Professional` e `Service`.
- Criação dos mapeamentos JPA correspondentes para persistência no banco de dados PostgreSQL.
- Criação do script de migração Flyway inicial para criação das tabelas correspondentes no banco de dados.
- Criação das portas de repositórios (interfaces no domínio) e suas implementações JPA na camada de infraestrutura.

## Capabilities

### New Capabilities
- `core-entities`: Implementação dos modelos de domínio, mapeamento de banco de dados (JPA), scripts de migração (Flyway) e repositórios básicos para as entidades fundamentais do sistema (`User`, `Patient`, `Professional` e `Service`).

### Modified Capabilities

## Impact

- **Pacotes Afetados:** `br.com.belloinfo.saap_mvp.domain` (novas subpastas/classes) e `br.com.belloinfo.saap_mvp.infrastructure.persistence` (novas classes JPA, repositories e migrations).
- **Banco de Dados:** Criação das tabelas `usuario`, `paciente`, `profissional` e `servico` via script Flyway (`V1__criar_tabelas_core.sql`).
- **Segurança:** Configuração inicial de relacionamento entre `User` e os perfis de acesso (RBAC).
