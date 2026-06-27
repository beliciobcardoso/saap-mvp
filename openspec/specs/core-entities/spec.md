# Core Entities Specification

Esta especificação define os requisitos de negócio e cenários para as entidades principais (core) do SAAP, incluindo Usuários, Pacientes, Profissionais de Saúde e Serviços, assim como o controle de acessos (RBAC) e a integridade de dados históricos através da exclusão lógica.

## Requirements

### Requirement: Cadastro de Usuário (User)
O sistema SHALL permitir o cadastro de usuários com credenciais de login, garantindo a unicidade do e-mail, criptografia da senha e associação a um papel de acesso (RBAC).

#### Scenario: Cadastro de usuário com dados válidos
- **WHEN** o administrador solicita o cadastro de um usuário informando e-mail único no formato válido, senha e papel "ADMIN"
- **THEN** o sistema SHALL persistir o usuário, salvar a senha criptografada e retornar o identificador único gerado (UUID)

#### Scenario: Cadastro de usuário com e-mail duplicado
- **WHEN** o administrador tenta cadastrar um usuário com um e-mail que já existe no sistema
- **THEN** o sistema SHALL impedir a criação e lançar um erro de validação de duplicidade

### Requirement: Cadastro de Paciente (Patient)
O sistema SHALL permitir o gerenciamento de pacientes contendo nome, CPF (Cadastro de Pessoas Físicas) único, número do cartão SUS (opcional, mas único se presente), e-mail (opcional), telefone, data de nascimento e status ativo/inativo.

#### Scenario: Cadastro de paciente com dados obrigatórios válidos
- **WHEN** o recepcionista cadastra um paciente informando nome completo, CPF válido e único, telefone válido e data de nascimento no passado
- **THEN** o sistema SHALL salvar o paciente com status ativo por padrão

#### Scenario: Cadastro de paciente com CPF duplicado
- **WHEN** o recepcionista tenta cadastrar um paciente com um CPF que já existe no sistema
- **THEN** o sistema SHALL impedir a criação e lançar um erro de validação de duplicidade

### Requirement: Cadastro de Profissional (Professional)
O sistema SHALL permitir o cadastro de profissionais de saúde contendo nome, registro profissional (ex: CRM) único, especialidade e papel correspondente.

#### Scenario: Cadastro de profissional com registro único
- **WHEN** o administrador cadastra um profissional informando nome, registro único e especialidade
- **THEN** o sistema SHALL salvar o profissional e associá-lo a um usuário de acesso correspondente

### Requirement: Cadastro de Serviço (Service)
O sistema SHALL permitir a definição dos serviços oferecidos pela clínica, contendo nome, descrição, duração em minutos e valor.

#### Scenario: Cadastro de serviço válido
- **WHEN** o administrador cadastra um serviço com nome, duração positiva e valor maior ou igual a zero
- **THEN** o sistema SHALL salvar o serviço ativo por padrão

### Requirement: Exclusão Lógica (Soft Delete) de Profissionais e Serviços
O sistema SHALL garantir que profissionais e serviços nunca sejam excluídos fisicamente do banco de dados, para preservar o histórico clínico e financeiro. Em vez disso, seu status ativo deve ser alterado para falso.

#### Scenario: Desativação de um profissional
- **WHEN** o administrador solicita a exclusão de um profissional ativo
- **THEN** o sistema SHALL alterar o atributo isActive do profissional para falso, mantendo seus registros intactos no banco de dados

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

