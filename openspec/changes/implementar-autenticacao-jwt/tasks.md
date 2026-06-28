## 1. Configuração e Dependências

- [x] 1.1 Adicionar a dependência do `com.auth0:java-jwt` no `pom.xml`.
- [x] 1.2 Declarar as variáveis `JWT_SECRET` e `JWT_EXPIRATION` no `.env.example` e configurá-las no `application.yaml`.
## 2. Criptografia e Serviços de Token

- [x] 2.1 Configurar o bean `BCryptPasswordEncoder` e atualizar o fluxo de criação de usuários para encriptar senhas antes de salvar no banco.
- [x] 2.2 Criar a classe `TokenService` responsável por gerar e decodificar/validar os tokens JWT de forma segura.
## 3. Integração com Spring Security e UserDetailsService

- [x] 3.1 Implementar `UserDetailsService` customizado buscando os dados de login através do `UserRepository` do projeto.
- [x] 3.2 Implementar o filtro customizado `JwtAuthenticationFilter` herdando de `OncePerRequestFilter` para interceptar requisições HTTP e validar o token JWT.
## 4. Configuração de Segurança Global

- [x] 4.1 Atualizar `SecurityConfig` para ativar `@EnableWebSecurity` e `@EnableMethodSecurity`.
- [x] 4.2 Configurar o `SecurityFilterChain` definindo sessões como `STATELESS`, liberando a rota pública de login e inserindo o filtro `JwtAuthenticationFilter` no pipeline.
## 5. Controladores REST e DTOs de Login

- [x] 5.1 Criar os registros imutáveis `LoginRequestDTO` e `LoginResponseDTO`.
- [x] 5.2 Implementar o `AuthController` exposto em `/api/v1/auth/login` para realizar a autenticação e retornar o token JWT gerado.
- [x] 5.3 Anotar os endpoints existentes (`UserController`, `PatientController`, `ProfessionalController`, `ServiceController`) com `@PreAuthorize` aplicando o controle RBAC de acordo com os papéis.

## 6. Testes Automatizados

- [x] 6.1 Criar testes unitários para a classe `TokenService`.
- [x] 6.2 Adicionar utilitário de geração de tokens JWT em `BaseIntegrationTest` para injetar credenciais válidas nos testes existentes.
- [x] 6.3 Criar testes de integração para o `AuthController` e verificar o bloqueio de segurança em endpoints protegidos (status 401 e 403).
