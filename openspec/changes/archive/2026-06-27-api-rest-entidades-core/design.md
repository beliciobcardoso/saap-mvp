## Context

Com a camada de domínio puro e a persistência (JPA) prontas, precisamos criar a ponte que expõe essas operações para a web. Na Clean/Hexagonal Architecture, isso significa criar Casos de Uso (Application Service layer) e Controllers REST (Presentation/Infrastructure layer).

## Goals / Non-Goals

**Goals:**
- Implementar Casos de Uso (Use Cases) no pacote `application.usecase` para criar, buscar por ID, listar ativos, atualizar e deletar logicamente cada entidade core.
- Criar a camada de DTOs (`*Request`, `*Response`) em `infrastructure.web.dto`.
- Implementar os Controllers REST em `infrastructure.web.controller` com as rotas HTTP corretas.
- Configurar validações sintáticas rigorosas (Jakarta Validation) nos DTOs de entrada.
- Implementar validador de CPF brasileiro customizado auto-contido.
- Implementar um Handler Global de Exceções (`GlobalExceptionHandler`) para unificar o formato de erros HTTP JSON.

**Non-Goals:**
- Implementar autenticação via token JWT ou autorização RBAC ativa por interceptador do Spring Security nesta alteração (será tratada no change de autenticação).
- Implementar endpoints para agendamento de consultas ou prontuários.

## Decisions

### 1. Pacotes e Estrutura de Camadas (Hexagonal)
- **Decisão:** Separar rigorosamente as responsabilidades:
  - `application.usecase`: Use cases injetando portas de domínio (interfaces de repositório), sem dependência de anotações do Spring REST.
  - `infrastructure.web.controller`: Controllers Spring Boot injetando Use Cases.
  - `infrastructure.web.dto`: Classes de Request/Response imutáveis (usando records do Java 16+ ou classes com anotações Lombok `@Value`/`@Builder`).
  - `infrastructure.web.mapper`: MapStruct Mappers específicos para DTOs.
- **Razão:** Desacoplamento arquitetural. Os DTOs e Controllers contêm regras HTTP (como serialização JSON e status codes), enquanto os Use Cases orquestram regras de aplicação pura.

### 2. DTO Mappers Isolados
- **Decisão:** Criar um mapper MapStruct dedicado para a camada Web (ex: `WebMapper`) isolado do `CoreMapper` usado na persistência.
- **Razão:** Os DTOs de entrada/saída contêm formatações e agrupamentos diferentes da entidade física do banco ou do modelo de domínio. O desacoplamento evita conflitos de nomes ou vazamento de lógica.

### 3. Validador de CPF Customizado e Auto-contido
- **Decisão:** Escrever uma anotação `@CPF` e sua classe validadora associada `CpfValidator` que implementa `ConstraintValidator<CPF, String>`, realizando a validação de dígitos verificadores (checksum do CPF).
- **Razão:** O Spring Validation (Hibernate Validator) não possui um validador de CPF brasileiro nativo. Criar uma validação customizada evita dependências de terceiros (como Stella) no POM, mantendo a compilação do projeto isolada e performática.

### 4. Tratamento Global de Exceções
- **Decisão:** Criar a classe `GlobalExceptionHandler` anotada com `@RestControllerAdvice` que intercepta exceções comuns do framework e do banco, retornando o corpo `ErrorResponse` no formato:
  ```json
  {
    "timestamp": "2026-06-27T10:00:00",
    "status": 400,
    "error": "Bad Request",
    "message": "Validation failed",
    "path": "/api/patients",
    "fields": {
      "cpf": "O CPF informado é inválido"
    }
  }
  ```
- **Razão:** Oferece respostas de erro amigáveis, legíveis e padronizadas para o cliente (mobile ou frontend), evitando o vazamento de stack traces internas em ambiente de produção (LGPD/Segurança).

## Risks / Trade-offs

- **[Risco] Bloqueio indevido por validação de CPF nos testes:** CPFs gerados aleatoriamente sem dígitos corretos quebram testes de integração.
  - *Mitigação:* Usar CPFs válidos gerados pelo algoritmo nos testes automatizados.
