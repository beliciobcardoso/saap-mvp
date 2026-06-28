## ADDED Requirements

### Requirement: Autenticação de Usuário via JWT
O sistema SHALL expor um endpoint de login que valide e-mail e senha de usuários ativos cadastrados no sistema, gerando um token de acesso seguro em formato JWT assinado digitalmente com tempo de expiração definido.

#### Scenario: Autenticação com credenciais válidas
- **WHEN** o cliente HTTP envia um POST para `/api/v1/auth/login` com e-mail cadastrado e senha correspondente correta
- **THEN** o sistema SHALL retornar status HTTP 200 (OK) contendo o token JWT e tempo de expiração no payload

#### Scenario: Autenticação com credenciais inválidas
- **WHEN** o cliente HTTP envia um POST para `/api/v1/auth/login` com e-mail incorreto ou senha incorreta
- **THEN** o sistema SHALL rejeitar a requisição, retornar status HTTP 401 (Unauthorized) e o JSON com a mensagem de erro correspondente

#### Scenario: Autenticação de usuário inativo
- **WHEN** o cliente HTTP envia um POST para `/api/v1/auth/login` com credenciais corretas de um usuário desativado logicamente
- **THEN** o sistema SHALL rejeitar a requisição, retornar status HTTP 401 (Unauthorized) e detalhar a falha de conta inativa

### Requirement: Autorização das Rotas HTTP e RBAC
O sistema SHALL interceptar todas as chamadas para endpoints protegidos sob o prefixo `/api/v1/`, extrair e validar o token JWT, e restringir o acesso a recursos específicos com base nos papéis (`UserRole`) do usuário autenticado.

#### Scenario: Acesso a recurso livre com token válido
- **WHEN** um usuário com papel `RECEPCIONISTA` autenticado envia um GET com token válido para `/api/v1/patients`
- **THEN** o sistema SHALL processar e retornar a lista de pacientes com status HTTP 200 (OK)

#### Scenario: Bloqueio de acesso a recurso restrito (RBAC)
- **WHEN** um usuário com papel `RECEPCIONISTA` autenticado envia um POST com token válido para `/api/v1/services`
- **THEN** o sistema SHALL bloquear o acesso e retornar status HTTP 403 (Forbidden)

#### Scenario: Requisição sem token em rota protegida
- **WHEN** um cliente HTTP envia uma requisição sem cabeçalho Authorization para `/api/v1/patients`
- **THEN** o sistema SHALL bloquear a requisição e retornar status HTTP 401 (Unauthorized)
