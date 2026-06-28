# Documento de Requisitos de Produto (PRD)
## SAAP — Sistema de Agendamento de Atendimentos Profissionais
### Futura Implementação: Java 21 / Spring Boot 4.1.0 (WAR / Maven)

---

## 1. Controle do Documento
* **Status:** Aprovado / Implementado (v1.1.0)
* **Autor:** Antigravity (AI Coding Assistant)
* **Data:** 28 de Junho de 2026
* **Versão:** 1.1.0
* **Público-alvo:** Desenvolvedores, Engenheiros de Software e Product Owners

---

## 2. Visão Geral do Produto

### 2.1. Contexto e Justificativa
O **SAAP** é uma plataforma voltada à gestão inteligente de agendas e fluxos presenciais para clínicas, consultórios médicos e centros de saúde. A gestão ineficiente de horários, a ocorrência de *double-booking* e as taxas elevadas de *no-show* (ausências não justificadas) impactam financeiramente os estabelecimentos e prejudicam a experiência do paciente.

### 2.2. Objetivo do Produto
Estruturar uma solução em backend robusta, utilizando o ecossistema Java com Spring Boot, capaz de:
* Prevenir conflitos de horários por meio de validações transacionais rigorosas.
* Minimizar a ociosidade da agenda através de follow-up proativo e lista de espera automatizada.
* Organizar e otimizar o fluxo de atendimento presencial por meio de uma fila prioritária inteligente baseada na legislação federal de prioridades.
* Garantir conformidade total com a Lei Geral de Proteção de Dados (LGPD) no manuseio de dados clínicos sensíveis.

### 2.3. Stack Tecnológico Alvo
* **Linguagem:** Java 21 (LTS)
* **Framework Principal:** Spring Boot 4.1.0
* **Gerenciador de Build/Dependências:** Maven (pom.xml)
* **Empacotamento (Packaging):** WAR (Web Application Archive) para deploy externo
* **Configuração:** YAML (application.yml)
* **Banco de Dados:** PostgreSQL (Produção e Ambiente de Testes com Testcontainers)
* **Persistência:** Spring Data JPA / Hibernate
* **Segurança:** Spring Security + OAuth2 Resource Server (JWT)
* **Migração de Banco:** Flyway
* **Validações:** Hibernate Validator (Jakarta Validation)
* **Testes:** JUnit 5, Mockito, Testcontainers

---

## 3. Personas e Papéis de Acesso (RBAC)

O sistema deve implementar Controle de Acesso Baseado em Papéis (RBAC) no nível de endpoints e métodos do Spring Security:

| Persona/Papel | Prefixo Spring Role | Descrição de Acesso e Responsabilidades |
| :--- | :--- | :--- |
| **Administrador** | `ROLE_ADMIN` | Acesso irrestrito ao sistema. Gerencia cadastros (UC01), convênios (UC11), relatórios (UC07) e configurações globais da clínica (`ClinicSettings`). |
| **Recepcionista** | `ROLE_RECEP` | Responsável por gerenciar agendamentos (UC02), realizar check-in presencial (UC03), validar documentação de prioridade (UC09) e gerenciar a lista de espera (UC05). |
| **Profissional de Saúde** | `ROLE_PROF` | Acesso à própria agenda (UC09) e fila presencial. Autoridade para chamar pacientes, registrar o atendimento e preencher prontuários (UC04). |
| **Assistente** | `ROLE_ASSIST` | Papel de apoio operacional. Herda a visualização de agenda do profissional e gerencia a preparação do ambiente/materiais de exames (UC10). |
| **Paciente** | `ROLE_PATIENT` | Acesso ao portal do paciente (opcional). Pode solicitar agendamentos (UC02), cancelar (UC06), confirmar lembretes e consultar seu próprio histórico (UC08). |

---

## 4. Requisitos Funcionais (RF) e Regras de Negócio

### RF01: Cadastro de Entidades Core (UC01)
* **Descrição:** O sistema deve permitir o CRUD de Usuários, Pacientes, Profissionais e Serviços.
* **Regras de Negócio:**
  * O `Usuario` é a entidade de autenticação. Todo profissional deve ter um `Usuario` associado (1:1). Um `Usuario` pode ter um vínculo opcional (0..1) com um `Paciente` (caso um profissional seja atendido na clínica).
  * Exclusão lógica: Profissionais e Serviços não podem ser deletados fisicamente para preservar o histórico financeiro e clínico. Devem possuir a flag `isActive = false`.

### RF02: Motor de Agendamento Sincronizado (UC02)
* **Descrição:** Permitir a reserva de slots de tempo baseados na grade de horários (`Schedule`) de cada profissional.
* **Regras de Negócio (Prevenção de Concorrência):**
  * Um profissional não pode ter dois agendamentos no mesmo horário.
  * O sistema deve validar a disponibilidade antes de persistir o agendamento.
  * **Concorrência:** Para evitar double-booking sob alta carga, deve-se usar controle de concorrência otimista (`@Version` do JPA) ou bloqueio pessimista (`@Lock(LockModeType.PESSIMISTIC_WRITE)`) durante a transação de reserva.

### RF03: Follow-Up Proativo de Confirmações (UC14)
* **Descrição:** Rotina automática para envio de notificações e cancelamento preventivo de consultas não confirmadas.
* **Regras de Negócio:**
  * O sistema executa uma tarefa agendada (ex: a cada hora via `@Scheduled`) para buscar agendamentos com data-hora próxima.
  * Ao entrar na janela `confirmationWindowHours` (ex: 48h antes da consulta), dispara uma notificação (WhatsApp/E-mail/SMS) com status `PENDING_RESPONSE`.
  * Se o paciente responder "SIM", o status do agendamento passa para `CONFIRMED`.
  * Se o paciente responder "NÃO", passa para `CANCELLED`.
  * Se o prazo `followUpDeadlineHours` (ex: 24h antes da consulta) for atingido sem resposta:
    * Se `autoCancelAfterNoResponse = true` na configuração da clínica (`ClinicSettings`), o agendamento é automaticamente marcado como `CANCELLED` (motivo: no-show de confirmação).
    * Se `false`, o sistema marca o agendamento com `followUpRequired = true`, gerando um alerta visual no painel da recepcionista.

### RF04: Fila de Espera Inteligente (UC05)
* **Descrição:** Alocação dinâmica de slots ociosos para pacientes na fila de espera.
* **Regras de Negócio:**
  * Quando um agendamento é cancelado (via UC06 ou por falta de resposta no follow-up), se `waitlistAutoFill = true`, o sistema busca o primeiro paciente na lista de espera (`WaitlistEntry` com ordenação FIFO).
  * O paciente da vez recebe uma notificação automática oferecendo a vaga. Ele terá um prazo (`waitlistCycleTimeoutMinutes` - padrão 30 min) para aceitar.
  * Caso aceite, o agendamento é gerado e ele é removido da fila.
  * Caso expire ou recuse, o sistema passa a vaga para o próximo paciente da lista. Este ciclo repete-se até o limite configurado em `waitlistMaxCycles`.

### RF05: Atendimento Prioritário Legal (UC09) `[Implementado - v1.1.0]`
* **Descrição:** Gerenciamento da fila de atendimento presencial pós-check-in com base na Lei Federal 10.048/2000.
* **Regras de Negócio:**
  * **Declaração vs. Validação:** O paciente pode declarar prioridade ao agendar. No entanto, no check-in presencial, a Recepcionista **deve obrigatoriamente validar física ou documentalmente** a condição de prioridade (ex: idade, laudo de TEA, carteira de PcD, comprovante de doação de sangue dentro de 120 dias).
  * Se a validação for positiva, o nível de prioridade é mantido/definido e os campos `priorityVerifiedBy` (ID da recepcionista) e `priorityNotes` (tipo de documento apresentado) são gravados.
  * Se a validação for negativa, a prioridade é revertida para `NORMAL` (P5), registrando a justificativa em `priorityNotes`.
  * **Algoritmo da Fila Prioritária (Min-Heap):**
    A ordem de chamada no período baseia-se em uma fila de prioridade ordenada por um `priorityScore` numérico composto:
    
    $$\text{priorityScore} = (\text{priorityLevel} \times 10^{12}) + \text{timestampCheckIn}$$
    
    *Os níveis de prioridade são:*
    * `P1 (1)`: Pessoa com deficiência, Transtorno do Espectro Autista (TEA), mobilidade reduzida.
    * `P2 (2)`: Idoso (60+), Doador de sangue com comprovante.
    * `P3 (3)`: Gestante, Lactante, Criança de colo.
    * `P4 (4)`: Obesidade (IMC $\ge$ 40).
    * `P5 (5)`: Normal.
    
    *Em Java, o cálculo do score e a ordenação devem suportar precisão de inteiros longos (`BigInt`/`Long` no JPA, `BigInteger` ou `long` convencional no código Java). O tie-breaker é natural através da ordenação do timestamp (FIFO para mesma prioridade).*

### RF06: Registro Clínico e Prontuário (UC04 / UC08)
* **Descrição:** Abertura e preenchimento de evolução clínica durante a consulta.
* **Regras de Negócio:**
  * Um prontuário (`MedicalRecord`) é 1:1 com o `Paciente`.
  * Cada consulta gera uma entrada (`MedicalRecordEntry`), que é obrigatoriamente 1:1 com o `Agendamento` correspondente.
  * A evolução de prontuário só pode ser editada ou inserida se o agendamento estiver no estado `IN_PROGRESS` (iniciado pelo profissional). Após finalizado (`COMPLETED`), a entrada torna-se **imutável** para fins de validade legal de prontuários médicos.

---

## 5. Requisitos Não Funcionais (RNF)

### RNF01: LGPD e Segurança de Dados `[Implementado - v1.1.0]`
* **Minimização de Dados:** Dados sensíveis de saúde (prontuários, diagnósticos, prescrições) devem ser armazenados de forma isolada (`MedicalRecordEntry`).
* **Segurança:** Trânsito de dados protegido por TLS/HTTPS. Senhas criptografadas com BCrypt.
* **Trilha de Auditoria (Audit Trail):** Toda inserção, alteração ou leitura de dados de prontuário e alterações de prioridade de fila devem ser registradas em uma tabela de log de auditoria imutável contendo: `data_hora`, `usuario_id`, `acao`, `recurso_afetado` e `ip_origem`. Além disso, o sistema deve auditar todo cadastro, atualização, desativação (soft delete) e logins dos usuários.

### RNF02: Concorrência e Performance
* **Tempo de Resposta:** Endpoints de leitura devem responder em menos de 100ms (p95) e de gravação em menos de 300ms.
* **Double-Booking:** O sistema deve garantir zero ocorrências de double-booking através de isolamento transacional no banco de dados.

### RNF03: Alta Disponibilidade das Notificações
* **Resiliência:** O serviço de envio de notificações (`NotificacaoService`) deve rodar de forma assíncrona. Falhas na API do provedor (WhatsApp/SMS) não podem bloquear a resposta da aplicação web ao usuário. Implementar mecanismo de retentativa com backoff exponencial.

---

## 6. Proposta de Arquitetura em Spring Boot

O projeto utilizará a **Clean Architecture** (ou Arquitetura Hexagonal), mantendo o núcleo de regras de negócio independente dos frameworks externos e da persistência.

Como o projeto utiliza empacotamento **WAR**, a classe principal do Spring Boot deve estender `SpringBootServletInitializer` para permitir a inicialização correta em servidores externos de Servlet (como o Apache Tomcat):

```java
package com.saap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
public class SaapApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(SaapApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(SaapApplication.class, args);
    }
}
```

### 6.1. Divisão de Pacotes (Estrutura de Pastas)
```text
br.com.belloinfo.saap
│
├── domain                  # Modelos de domínio puros (Entidades de Negócio)
│   ├── model               # Ex: Appointment, Patient, User
│   └── valueobject         # Ex: PriorityScore, ConfirmationStatus
│
├── usecase                 # Casos de uso do sistema (Lógica de Aplicação)
│   ├── appointment         # Ex: BookAppointmentUseCase, ConfirmPresenceUseCase
│   └── waitlist            # Ex: WaitlistProcessorUseCase
│
├── infrastructure          # Adaptadores e Configurações de Frameworks
│   ├── security            # Configurações do Spring Security, JWT, RBAC
│   ├── persistence         # Entidades JPA, Repositories (Spring Data)
│   ├── messaging           # Envio de notificações (E-mail, WhatsApp, SMS)
│   ├── scheduler           # Tarefas agendadas (@Scheduled do Spring)
│   └── web                 # Controllers (REST API), DTOs, ExceptionHandlers
```

### 6.2. Dependências do Projeto (Maven `pom.xml`)

O gerenciamento de dependências no Maven será configurado com as seguintes dependências chave no `pom.xml`:

```xml
<dependencies>
    <!-- Web e Servlet (WAR Packaging) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
        <!-- Exclui o Tomcat embutido na geração do WAR para deploy externo -->
        <exclusions>
            <exclusion>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-tomcat</artifactId>
            </exclusion>
        </exclusions>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-tomcat</artifactId>
        <scope>provided</scope>
    </dependency>

    <!-- Persistência e Banco de Dados -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-core</artifactId>
    </dependency>
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-database-postgresql</artifactId>
    </dependency>

    <!-- Segurança e Autenticação -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
    </dependency>

    <!-- Validação de Dados -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Comunicações e Notificações -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-mail</artifactId>
    </dependency>

    <!-- Utilitários -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
    <dependency>
        <groupId>org.mapstruct</groupId>
        <artifactId>mapstruct</artifactId>
        <version>1.5.5.Final</version>
    </dependency>
    <dependency>
        <groupId>org.mapstruct</groupId>
        <artifactId>mapstruct-processor</artifactId>
        <version>1.5.5.Final</version>
        <scope>provided</scope>
    </dependency>

    <!-- Monitoramento e Observabilidade -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>

    <!-- Testes -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.springframework.security</groupId>
        <artifactId>spring-security-test</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>junit-jupiter</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>postgresql</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### 6.3. Configuração do Projeto (`application.yml`)

O arquivo de propriedades do Spring Boot será definido no formato YAML (`src/main/resources/application.yml`):

```yaml
spring:
  application:
    name: saap-backend
  profiles:
    active: dev

  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://localhost:5432/saap}
    username: ${DATABASE_USER:postgres}
    password: ${DATABASE_PASSWORD:postgres}
    driver-class-name: org.postgresql.Driver

  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
    show-sql: false

  flyway:
    enabled: true
    baseline-on-migrate: true

  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${JWT_ISSUER_URI:https://auth.saap.com}
          jwk-set-uri: ${JWT_JWK_SET_URI:https://auth.saap.com/.well-known/jwks.json}

# Configurações do Negócio / Motor do SAAP
clinic:
  settings:
    confirmation-window-hours: 48
    follow-up-deadline-hours: 24
    auto-cancel-after-no-response: false
    waitlist-auto-fill: true
    waitlist-cycle-timeout-minutes: 30
    waitlist-max-cycles: 3
```

### 6.4. Mapeamento de Entidades JPA (Exemplo de Estrutura)

Para traduzir a modelagem lógica relacional (DBML/Prisma) para o Spring Boot, usaremos mapeamentos JPA baseados nas seguintes entidades exemplo:

#### JPA Entity: `Appointment`
```java
package com.saap.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "agendamento")
@Getter
@Setter
public class AppointmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dateTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "forma_pagamento", nullable = false)
    private PaymentMethod paymentMethod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_id", nullable = false)
    private PatientEntity patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profissional_id", nullable = false)
    private ProfessionalEntity professional;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "servico_id", nullable = false)
    private ServiceEntity service;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "convenio_id")
    private HealthPlanEntity healthPlan;

    // Campos de Auditoria de Prioridade
    @Enumerated(EnumType.STRING)
    @Column(name = "priority_level", nullable = false)
    private PriorityLevel priorityLevel;

    @Column(name = "priority_score")
    private Long priorityScore; // Armazena (priorityLevel * 10^12) + timestampCheckIn

    @Column(name = "priority_declared_at")
    private LocalDateTime priorityDeclaredAt;

    @Column(name = "priority_verified_by")
    private UUID priorityVerifiedBy;

    @Column(name = "priority_notes")
    private String priorityNotes;

    // Controle de Concorrência
    @Version
    private Long version;
}
```

---

## 7. Modelagem de Estados (Lifecycle de Agendamento)

A entidade `Appointment` deve obedecer rigorosamente a transições de estado controladas. Qualquer transição ilegal deve disparar uma `IllegalStateException` genérica em nível de aplicação, capturada por um `@ControllerAdvice`.

| Estado Atual | Estado Destino Permitido | Gatilho / Condição |
| :--- | :--- | :--- |
| `PENDING` | `CONFIRMED` | Confirmação por parte do Paciente (WhatsApp/E-mail) ou recepção. |
| `PENDING` | `CANCELLED` | Cancelamento ativo pelo paciente ou expiração do deadline de confirmação. |
| `CONFIRMED` | `ARRIVED` (Check-in) | Paciente chega fisicamente. A recepção valida e define o `priorityScore`. |
| `CONFIRMED` | `CANCELLED` | Cancelamento ativo antes do horário da consulta. |
| `CONFIRMED` | `NO_SHOW` | Consulta expira sem a realização de check-in presencial no período correspondente. |
| `ARRIVED` | `CALLING` | Profissional aciona a chamada no painel (extração da Priority Queue). |
| `CALLING` | `IN_PROGRESS` | Profissional clica em "Iniciar Atendimento" no consultório. |
| `IN_PROGRESS` | `COMPLETED` | Profissional preenche a evolução e fecha o prontuário. |
| `ARRIVED` | `CANCELLED` | Desistência justificada do paciente após o check-in mas antes do atendimento. |

---

## 8. Estratégia de Testes e Validação

Para garantir a qualidade e a resiliência do sistema com Spring Boot, a pirâmide de testes será assim estruturada:

1. **Testes Unitários:**
   * Foco: Lógica do Score de Prioridade (`priorityScore`) e máquina de estados do `Appointment`.
   * Ferramentas: JUnit 5, AssertJ.
2. **Testes de Integração (Repositories & Services):**
   * Foco: Validação de concorrência (*double-booking*), concorrência otimista com `@Version` e transições transacionais.
   * Ferramentas: Spring Boot Test, Testcontainers (PostgreSQL ativo em container para isolar e simular transações reais e travas pessimistas).
3. **Testes de API / Segurança:**
   * Foco: Validação do RBAC em endpoints específicos (ex: somente `ROLE_PROF` acessa prontuários).
   * Ferramentas: MockMvc, `@WithMockUser`.
