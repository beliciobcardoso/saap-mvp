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
