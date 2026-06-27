## ADDED Requirements

### Requirement: Interface REST API das Entidades Core
O sistema SHALL expor endpoints REST para permitir o cadastro, busca, atualização e desativação de Usuários, Pacientes, Profissionais e Serviços de forma isolada do modelo de persistência.

#### Scenario: Criação bem-sucedida de paciente via API
- **WHEN** o cliente HTTP envia um payload JSON válido via POST para `/api/v1/patients`
- **THEN** o sistema SHALL persistir o paciente, retornar status HTTP 201 (Created) e o corpo com os dados criados (incluindo UUID)

#### Scenario: Remoção lógica de profissional via API
- **WHEN** o cliente HTTP envia uma requisição DELETE para `/api/v1/professionals/{id}`
- **THEN** o sistema SHALL executar a deleção lógica (soft delete), desativando o profissional, e retornar status HTTP 204 (No Content)

### Requirement: Validação de Payload da API
O sistema SHALL validar todas as requisições de entrada de dados (DTOs) na camada web antes de repassá-las aos casos de uso, utilizando restrições de validação padrão do Jakarta Validation.

#### Scenario: Cadastro de paciente com CPF de tamanho inválido
- **WHEN** o cliente envia uma requisição POST para `/api/v1/patients` com o campo CPF contendo menos ou mais de 11 dígitos
- **THEN** o sistema SHALL rejeitar a requisição e retornar status HTTP 400 (Bad Request) detalhando a falha de validação do campo

#### Scenario: Cadastro de paciente com e-mail em formato incorreto
- **WHEN** o cliente envia uma requisição POST para `/api/v1/patients` com o campo email no formato "email_invalido.com"
- **THEN** o sistema SHALL rejeitar a requisição e retornar status HTTP 400 (Bad Request) detalhando a falha de validação do campo

### Requirement: Tratamento de Erros da API
O sistema SHALL interceptar exceções de negócios e violações de integridade de banco de dados (duplicidade) em um controlador global de exceções, retornando respostas padronizadas.

#### Scenario: Cadastro de usuário com e-mail duplicado via API
- **WHEN** o cliente envia um POST para `/api/v1/users` com um e-mail que já existe no banco de dados
- **THEN** o sistema SHALL capturar a falha de unicidade, retornar status HTTP 409 (Conflict) e um JSON estruturado com a descrição do erro
