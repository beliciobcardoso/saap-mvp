# SAAP MVP

Este é o produto mínimo viável (MVP) do sistema **SAAP**, desenvolvido utilizando o ecossistema Spring Boot com Java 21.

## 🚀 Tecnologias Utilizadas

O projeto foi construído utilizando as seguintes tecnologias e dependências principais:

- **Java 21**
- **Spring Boot 4.1.x**
- **Spring Web MVC** — Criação de APIs REST e endpoints web.
- **Spring Security** — Mecanismos de autenticação e autorização robustos.
- **Spring Data JPA** — Integração simplificada com o banco de dados via Hibernate.
- **PostgreSQL** — Banco de dados relacional (Driver runtime).
- **Flyway Migration** — Controle de versão e migração de esquemas de banco de dados.
- **Spring Boot Actuator** — Monitoramento, métricas e auditoria da integridade da aplicação.
- **Spring Boot DevTools** — Reload rápido e facilidades para ambiente de desenvolvimento.
- **Hibernate Validation** — Validação declarativa de beans/DTOs (`jakarta.validation`).
- **Lombok** — Redução de código boilerplate (getters, setters, construtores, etc.).
- **Maven** — Gerenciador de dependências e build.

---

## 🛠️ Pré-requisitos

Para executar e desenvolver este projeto localmente, você precisará de:

- **JDK 21** instalado e configurado nas variáveis de ambiente.
- **Maven 3.9+** (ou utilizar o Maven Wrapper `./mvnw` incluso no projeto).
- **PostgreSQL** instalado e em execução local ou via Docker.

---

## ⚙️ Configuração Local

1. Instale as dependências e compile o projeto para garantir que tudo está correto:
   ```bash
   ./mvnw clean compile
   ```

2. Crie o banco de dados no PostgreSQL (por exemplo, `saap_db`).

3. Configure a conexão com o banco de dados. Crie um arquivo `src/main/resources/application-local.yaml` (que já está configurado no `.gitignore` para não expor suas credenciais) com a seguinte estrutura básica:
   ```yaml
   spring:
     datasource:
       url: jdbc:postgresql://localhost:5432/saap_db
       username: seu_usuario
       password: sua_senha
       driver-class-name: org.postgresql.Driver
     jpa:
       hibernate:
         ddl-auto: validate
       show-sql: true
       properties:
         hibernate:
           format_sql: true
     flyway:
       enabled: true
   ```

---

## 🏃 Como Executar

Para iniciar a aplicação em modo de desenvolvimento com hot-reload ativo (via DevTools):

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

A aplicação estará disponível em `http://localhost:8080` (ou na porta configurada).

---

## 📦 Empacotamento (Build)

Como o projeto está configurado para empacotamento em formato **WAR**, execute o comando abaixo para gerar o artefato pronto para deploy:

```bash
./mvnw clean package
```

O arquivo `.war` resultante será gerado dentro do diretório `target/`.

---

## 📁 Estrutura de Pastas Principal

```text
saap-mvp/
├── .mvn/                     # Configurações do Maven Wrapper
├── src/
│   ├── main/
│   │   ├── java/             # Código-fonte Java da aplicação
│   │   │   └── br/com/belloinfo/saap_mvp/
│   │   │       ├── SaapMvpApplication.java   # Classe principal (Bootstrap)
│   │   │       └── ServletInitializer.java   # Configuração para deploy em Servidores Web externos (WAR)
│   │   └── resources/
│   │       ├── db/migration/ # Scripts SQL de migração do Flyway
│   │       └── application.yaml  # Configurações globais da aplicação
│   └── test/                 # Testes unitários e de integração
├── pom.xml                   # Definição de dependências e plugins do Maven
└── .gitignore                # Arquivos ignorados no controle de versão
```
