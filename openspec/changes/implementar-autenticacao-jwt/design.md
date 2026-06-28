## Context

Atualmente, o SAAP MVP conta com um `SecurityConfig` simplificado que expõe todas as rotas `/api/**` publicamente. Com a implementação de novas rotas sensíveis que envolvem dados de pacientes, precisamos proteger a aplicação utilizando Spring Security com autenticação stateless baseada em tokens JWT (JSON Web Tokens) e controle de acesso RBAC.

## Goals / Non-Goals

**Goals:**
- Implementar criptografia de senhas usando `BCryptPasswordEncoder`.
- Adicionar a biblioteca `com.auth0:java-jwt` ao `pom.xml` para gerenciar tokens JWT de forma simplificada e segura.
- Criar o endpoint de autenticação `/api/v1/auth/login` recebendo e-mail/senha e retornando o token JWT.
- Criar o filtro customizado `JwtAuthenticationFilter` para interceptar, extrair e validar tokens em rotas protegidas.
- Habilitar proteção de nível de método com `@PreAuthorize` nos controladores REST atuais.

**Non-Goals:**
- Implementar fluxo de "Esqueci minha senha" ou recuperação de conta neste ciclo.
- Implementar login social (OAuth2/Google) ou autenticação multifator (MFA).
- Implementar Tokens de Atualização (Refresh Tokens).

## Decisions

### 1. Biblioteca JWT: com.auth0:java-jwt
- **Decisão:** Utilizar a biblioteca da Auth0 (`com.auth0:java-jwt`) no lugar da JJWT (`io.jsonwebtoken`).
- **Razão:** A biblioteca Auth0 possui uma API mais direta, fluida e com menos dependências transitivas necessárias para configuração no Spring Boot 3/4.
- **Alternativas consideradas:** `io.jsonwebtoken` (JJWT). Descartada por necessitar de três dependências distintas (`jjwt-api`, `jjwt-impl` e `jjwt-jackson`) e APIs mais complexas.

### 2. Autenticação Stateless com Filtro Customizado
- **Decisão:** Desativar sessões HTTP padrão (`SessionCreationPolicy.STATELESS`) e registrar um `JwtAuthenticationFilter` herdando de `OncePerRequestFilter` antes do `UsernamePasswordAuthenticationFilter`.
- **Razão:** APIs REST devem ser preferencialmente stateless e escaláveis. O filtro customizado garante que todas as requisições para endpoints protegidos passem pela validação do token JWT.

### 3. Configuração de Variáveis Sensíveis via .env
- **Decisão:** Configurar a chave secreta do JWT (`JWT_SECRET`) e tempo de expiração via variáveis de ambiente expostas no `application.yaml` e injetadas localmente pelo arquivo `.env`.
- **Razão:** Manter a chave secreta segura e mutável por ambiente sem expô-la no repositório de controle de versão.

### 4. Controle de Acesso Baseado em Funções (RBAC) nos Controladores
- **Decisão:** Ativar `@EnableMethodSecurity` no Spring Security e anotar os endpoints com `@PreAuthorize("hasRole('...')")`.
- **Razão:** Permite um controle fino e legível de permissões diretamente nos métodos do controlador REST.
  - Exemplo: `@PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")` no método de cadastro de pacientes.

## Risks / Trade-offs

- **[Risco] Senhas Legadas em Aberto no Banco de Teste:** O banco local ou os testes podem conter usuários com senhas em texto puro que falharão no login com o `BCryptPasswordEncoder`.
  - *Mitigação:* Atualizar scripts de migração do Flyway e instâncias de sementes de teste para salvar as senhas pré-criptografadas com BCrypt.
- **[Risco] Expiração de Tokens em Testes de Integração:** Tokens JWT estáticos mockados em testes podem expirar com o tempo.
  - *Mitigação:* Criar um utilitário de teste em `BaseIntegrationTest` ou gerar tokens em tempo de execução com expiração estendida durante a fase de testes.
