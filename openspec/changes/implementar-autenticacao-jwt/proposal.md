## Why

Atualmente, todos os endpoints das APIs REST do SAAP estão públicos e sem nenhum controle de acesso. Para proteger os dados dos pacientes (PHI) e assegurar a auditoria das ações, é necessário implementar autenticação via tokens JWT e autorização baseada em papéis (RBAC).

## What Changes

- **Login Endpoint:** Criação do endpoint `/api/v1/auth/login` para autenticação de usuários via e-mail e senha, retornando um token JWT de acesso.
- **Filtro de Segurança JWT:** Implementação de um filtro de requisições que intercepta chamadas HTTP, extrai o token JWT do cabeçalho `Authorization: Bearer <token>`, valida a assinatura/expiração e carrega o contexto do usuário.
- **Proteção dos Endpoints:** Ativação do Spring Security para exigir autenticação válida em todas as rotas `/api/v1/**` (exceto `/api/v1/auth/login`).
- **Controle de Acesso Baseado em Funções (RBAC):** Proteção de rotas específicas com base na role do usuário (ex: apenas `ADMIN` pode cadastrar serviços/profissionais; `RECEPCIONISTA` e `ADMIN` podem cadastrar pacientes; `MEDICO` tem acesso a relatórios e prontuários).

## Capabilities

### New Capabilities
- `user-auth`: Autenticação de usuários, geração e validação de tokens JWT, e aplicação de regras de autorização baseadas em perfis de acesso (RBAC).

### Modified Capabilities
<!-- Nenhuma especificação de comportamento das entidades core existentes está sendo alterada, apenas a segurança HTTP no acesso a elas. -->

## Impact

- **Segurança:** Configurações no `SecurityConfig.java` para habilitar segurança HTTP, filtros customizados de servlet e encriptação de senhas (BCryptPasswordEncoder).
- **Controladores REST:** Ajustes nos endpoints atuais de `/api/v1` para validação de segurança e injeção do usuário autenticado no contexto.
- **Dependências:** Adição de biblioteca de geração e verificação de tokens JWT (ex: `io.jsonwebtoken` ou `auth0` java-jwt) no `pom.xml`.
