## Why

Atualmente, o SAAP possui apenas as entidades de domínio puro e a infraestrutura de banco de dados (persistência JPA). Para permitir a integração com a interface do usuário (ou sistemas externos) e tornar as entidades operacionais ponta a ponta, precisamos expor os fluxos de CRUD (Cadastro, Consulta, Atualização e Deleção) através de endpoints HTTP (REST APIs), encapsulados por Use Cases de aplicação.

## What Changes

- **Camada de Casos de Uso (Application Layer):** Criação dos casos de uso para gerenciar o CRUD básico de Usuários, Pacientes, Profissionais e Serviços, orquestrando as regras de domínio e chamando os adaptadores de persistência.
- **REST API Endpoints:** Criação de controllers Spring MVC (`@RestController`) para expor as URLs:
  - `/api/users`
  - `/api/patients`
  - `/api/professionals`
  - `/api/services`
- **Camada de DTOs e Mapeamento:** Criação de Data Transfer Objects (DTOs) específicos para requisição e resposta de cada entidade, prevenindo o vazamento de entidades JPA/domínio e garantindo o desacoplamento.
- **Validação com Hibernate Validator:** Adicionar validações de payload (como CPF brasileiro válido de 11 dígitos, formato de e-mail, celular, e campos obrigatórios) via anotações Jakarta Validation.
- **Tratamento Global de Erros:** Implementar um handler de exceções centralizado (`@RestControllerAdvice`) para interceptar erros de validação e restrições de banco de dados, retornando responses com formato JSON padronizado e HTTP Status apropriados (ex: `400 Bad Request` para validação, `409 Conflict` para duplicidade, `442` ou `404 Not Found` para ausência de dados).

## Capabilities

### New Capabilities
<!-- None -->

### Modified Capabilities
- `core-entities`: Especificar os contratos de API REST (endpoints, payloads, HTTP status) e regras de validação associadas ao gerenciamento das entidades core.

## Impact

- **Camada de Apresentação/Web:** Nova estrutura de pacotes `infrastructure.web.controller` e `infrastructure.web.dto`.
- **Camada de Aplicação:** Novo pacote `application.usecase` contendo os fluxos de negócios/CRUD.
- **Testes:** Adicionados testes de integração usando `@WebMvcTest` ou `MockMvc` para as rotas HTTP e testes de validação do Spring.
