# SAAP MVP - Sistema de Agendamento e Atendimento de Pacientes

Este é o Produto Mínimo Viável (MVP) do **SAAP** (Sistema de Agendamento e Atendimento de Pacientes), uma plataforma desenvolvida utilizando **Java 21** e **Spring Boot 4.1.x** seguindo os princípios de **Clean Architecture / Hexagonal Architecture**.

O objetivo do sistema é suportar as operações principais de uma clínica de saúde, incluindo o gerenciamento de usuários, cadastro e controle de pacientes (com validação de CPF), cadastro de profissionais de saúde, catálogo de serviços médicos e controle de acessos (RBAC).

---

## ⚙️ Arquitetura do Projeto (Hexagonal / Clean)

A estrutura de código é dividida rigorosamente para isolar as regras de negócio de detalhes de infraestrutura (como banco de dados e frameworks web):

```text
src/main/java/br/com/belloinfo/saap_mvp/
├── domain/                         # Camada de Domínio Puro (Sem acoplamento com Spring)
│   ├── model/                      # Entidades de Domínio (User, Patient, Professional, Service)
│   ├── repository/                 # Portas de Saída (Interfaces de Repositório)
│   └── valueobject/                # Value Objects e Enums (UserRole, ProfessionalRole)
│
├── application/                    # Camada de Aplicação (Regras de Caso de Uso)
│   └── usecase/                    # Casos de Uso específicos de CRUD das Entidades Core
│
├── infrastructure/                 # Camada de Infraestrutura e Adaptadores (Spring/JPA/Web)
│   ├── database/                   # Inicializadores e Listeners (Auto-criação do Banco de Dados)
│   ├── persistence/                # Implementação JPA, Entidades Físicas e Repositórios JpaRepository
│   ├── security/                   # Configuração de Autenticação e Spring Security
│   └── web/                        # Adaptadores de Entrada (REST Controllers, DTOs, Exception Handler)
│       ├── config/                 # Configuração de Rotas Web (Prefixo Global /api/v1)
│       ├── controller/             # Controladores REST das Entidades Core
│       ├── dto/                    # Data Transfer Objects imutáveis (Records) com Validações
│       ├── exception/              # Manipulador Global de Exceções (GlobalExceptionHandler)
│       ├── mapper/                 # Mapeadores do MapStruct (WebMapper)
│       └── validation/             # Anotações de Validação customizadas (ex: @CPF para pacientes)
```

---

## 🛠️ Tecnologias Utilizadas

- **Java 21** (Uso de Records, Pattern Matching, Local Variable Type Inference)
- **Spring Boot 4.1.x** (Spring Web MVC, Spring Security, Spring Data JPA, Spring Actuator, Spring DevTools)
- **PostgreSQL** & **Flyway Migration** (Versionamento de banco de dados e migrações SQL)
- **MapStruct 1.5.5** (Mapeamento performático compile-time entre DTOs e entidades)
- **Lombok** (Geração automática de construtores/boilerplate de domínio)
- **Jakarta Validation** (Validações automáticas de payloads de entrada)
- **Testcontainers** & **JUnit 5** (Testes de integração rodando banco PostgreSQL real em container)

---

## ⚙️ Configuração Local

### Pré-requisitos
- **JDK 21** instalado e configurado.
- **Docker** em execução (para rodar os testes de integração com Testcontainers).
- **PostgreSQL** instalado (se for rodar localmente sem Docker).

### Passo a Passo

1. Compile o projeto e baixe as dependências:
   ```bash
   ./mvnw clean compile
   ```

2. Crie o arquivo `.env` de configuração na raiz do projeto baseado no modelo:
   ```bash
   cp .env.example .env
   ```

3. Abra o arquivo `.env` e configure as credenciais do seu banco de dados local:
   - `DB_HOST`, `DB_PORT`, `DB_NAME`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`.
   - `PORT`: Define a porta que o servidor Tomcat subirá (padrão: `8080`).

   > [!TIP]
   > **Criação Automática do Banco:** Você não precisa rodar comandos SQL no PostgreSQL para criar o banco de dados. Na primeira inicialização, um Listener do Spring (`DatabaseInitializerListener`) criará automaticamente o banco de dados especificado em `DB_NAME` caso ele não exista no servidor.

---

## 🏃 Como Executar a Aplicação

Inicie a aplicação localmente:
```bash
./mvnw spring-boot:run
```
O servidor estará em execução em `http://localhost:<PORT>` (geralmente `http://localhost:8080`).

---

## 🧪 Como Executar os Testes

O projeto utiliza **Testcontainers** para subir uma instância real do PostgreSQL em Docker durante os testes de integração. Certifique-se de que o Docker está rodando localmente e execute:

```bash
./mvnw clean test
```

---

## 📦 Empacotamento (Build)

Para gerar o artefato empacotado em formato **WAR** (pronto para deploy em servidores como Tomcat externo):

```bash
./mvnw clean package
```
O arquivo `.war` resultante será gerado na pasta `target/`.

---

## 🌐 Endpoints da API REST (Versão 1.0)

Todas as rotas REST são expostas automaticamente com o prefixo global `/api/v1`.

### 👥 Usuários (`/api/v1/users`)
- `POST /api/v1/users` - Cadastra um novo usuário.
- `GET /api/v1/users/{id}` - Busca usuário por UUID.
- `GET /api/v1/users` - Lista todos os usuários ativos.
- `PUT /api/v1/users/{id}` - Atualiza dados do usuário.
- `DELETE /api/v1/users/{id}` - Desativação lógica (soft delete).

### 🏥 Pacientes (`/api/v1/patients`)
- `POST /api/v1/patients` - Cadastra um paciente (valida CPF e formato de e-mail).
- `GET /api/v1/patients/{id}` - Busca paciente por UUID.
- `GET /api/v1/patients` - Lista todos os pacientes ativos.
- `PUT /api/v1/patients/{id}` - Atualiza dados cadastrais.
- `DELETE /api/v1/patients/{id}` - Desativação lógica (soft delete).

### 🩺 Profissionais (`/api/v1/professionals`)
- `POST /api/v1/professionals` - Cadastra profissional de saúde (CRM, especialidade).
- `GET /api/v1/professionals/{id}` - Busca profissional por UUID.
- `GET /api/v1/professionals` - Lista profissionais ativos.
- `PUT /api/v1/professionals/{id}` - Atualiza dados cadastrais.
- `DELETE /api/v1/professionals/{id}` - Desativação lógica (soft delete).

### 💼 Serviços (`/api/v1/services`)
- `POST /api/v1/services` - Cadastra um novo tipo de serviço/procedimento.
- `GET /api/v1/services/{id}` - Busca serviço por UUID.
- `GET /api/v1/services` - Lista serviços ativos.
- `PUT /api/v1/services/{id}` - Atualiza dados do serviço.
- `DELETE /api/v1/services/{id}` - Desativação lógica (soft delete).

---

## 🛡️ Tratamento de Erros e Resposta

O manipulador global de exceções captura falhas e retorna payloads JSON padronizados, por exemplo:

```json
{
  "timestamp": "2026-06-27T14:00:00.123Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/patients",
  "fields": {
    "cpf": "O CPF informado é inválido"
  }
}
```
