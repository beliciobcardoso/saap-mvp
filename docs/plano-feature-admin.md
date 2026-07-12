# Plano — Criação Automática do Usuário Admin na Inicialização com Notificação por E-mail

## Objetivo
Garantir que, ao subir a aplicação pela primeira vez, exista um usuário administrador seguro (`role = ADMIN`). A senha inicial/temporária será gerada aleatoriamente no startup do servidor, exibida nos logs e enviada por e-mail (usando o OCI Email Delivery). A alteração de senha será obrigatória no primeiro login. Enquanto a senha não for trocada, reinicializações do servidor gerarão uma nova senha cíclica e a enviarão novamente por e-mail.

---

## Variáveis de Ambiente Necessárias (`.env`)
No arquivo `.env`, serão definidas as seguintes configurações:

```env
# --- Administrador Geral ---
EMAIL_ADMIN=admin@saap.com

# --- EMAIL (OCI Email Delivery via SMTP) ---
SMTP_HOST=smtp.email.us-ashburn-1.oci.oraclecloud.com
SMTP_PORT=587
SMTP_USER=ocid1.user.oc1..aaaaaa...
SMTP_PASSWORD=senha_smtp_oci
EMAIL_FROM=suporte@saap.com
```

---

## Regras da Feature

1. **Verificação no Startup (Startup Listener):**
   - Ao iniciar o contexto da aplicação, verificar se o usuário com e-mail `EMAIL_ADMIN` e role `ADMIN` existe no banco.
   - Caso não exista: criar o usuário, definir status `needs_password_change = true`, gerar uma senha forte aleatória, criptografá-la no banco, logar a senha em claro e enviá-la por e-mail para o administrador.
   - Caso exista e `needs_password_change == true` (ainda não realizou login/troca): gerar nova senha aleatória forte, atualizar no banco de dados, logar em claro e reenviar por e-mail.
   - Caso exista e `needs_password_change == false`: não fazer nada (idempotente).

2. **Envio Resiliente de E-mail:**
   - Enviar a senha gerada ao e-mail cadastrado em `EMAIL_ADMIN` através do **OCI Email Delivery** (configurado via SMTP do Spring Mail).
   - Se o envio de e-mail falhar (ex: credenciais ausentes ou erro de rede), registrar o erro no log, mas permitir que o servidor suba normalmente (a senha continuará disponível nos logs do servidor).

3. **Bloqueio de Acesso Temporário (Forçar Troca de Senha):**
   - Ao fazer login com a senha temporária, o usuário receberá o token JWT normalmente.
   - No entanto, qualquer requisição para outros endpoints do sistema (exceto `/api/v1/auth/change-password` e `/api/v1/auth/logout`) será bloqueada com `403 Forbidden` (`PASSWORD_CHANGE_REQUIRED`), forçando o administrador a alterar a senha antes de utilizar o sistema.

4. **Ciclo de Vida:**
   - Assim que o administrador chamar o endpoint de troca de senha com sucesso, definir `needs_password_change = false`.
   - Após esta alteração, o sistema nunca mais gerará senhas automáticas para este usuário no startup do servidor.

---

## Fluxo da Solução (Passo a Passo)

### Passo 1: Banco de Dados (Flyway)
Adicionar migração sequencial **`V11__adicionar_coluna_troca_senha_usuario.sql`**:
```sql
ALTER TABLE usuario ADD COLUMN needs_password_change BOOLEAN NOT NULL DEFAULT FALSE;
```

### Passo 2: Modelo de Domínio e Entidade
- Adicionar o campo `needsPasswordChange` em `User` (domínio), `UserEntity` (infraestrutura) e `UserResponseDTO`.
- O mapeamento MapStruct em `CoreMapper` e `WebMapper` será atualizado automaticamente por convenção de nome de campo.

### Passo 3: Serviço de Envio de E-mail
- Configurar dependência `spring-boot-starter-mail` no `pom.xml`.
- Configurar o SMTP do OCI em `application.yaml` apontando para as variáveis de ambiente `SMTP_HOST`, `SMTP_PORT`, `SMTP_USER`, `SMTP_PASSWORD` e `EMAIL_FROM`.
- Implementar `EmailNotificationService` para realizar o disparo do e-mail.

### Passo 4: Caso de Uso de Inicialização (`InitializeAdminUseCase`)
- Lógica de verificação, criação/atualização cíclica de senha, geração de senha forte aleatória, log em console e disparo de e-mail.
- Registrado como um `ApplicationListener<ContextRefreshedEvent>` na infraestrutura (`AdminInitializer.java`).

### Passo 5: Caso de Uso de Troca de Senha (`ChangePasswordUseCase`)
- Validar senha antiga criptografada, criptografar nova senha e atualizar `needs_password_change = false`.
- Adicionar endpoint `POST /api/v1/auth/change-password` no `AuthController`.

### Passo 6: Bloqueio de Segurança (`JwtAuthenticationFilter`)
- No filtro JWT, verificar se o usuário autenticado possui `needsPasswordChange == true`.
- Caso sim, bloquear chamadas a outros endpoints retornando `403 Forbidden` com código JSON padronizado.

---

## Plano de Testes

1. **Testes Unitários:**
   - Testar o `InitializeAdminUseCase` com mocks de repositório e serviço de e-mail.
   - Testar o `ChangePasswordUseCase` validando regras de senha antiga e nova senha.

2. **Testes de Integração:**
   - Iniciar o contexto em um teste que estende `BaseIntegrationTest` e simular o startup da aplicação com a feature habilitada.
   - Autenticar com a senha temporária e verificar o bloqueio de requisições em outras rotas.
   - Efetuar a troca de senha pelo endpoint `/api/v1/auth/change-password`.
   - Verificar que novas requisições são liberadas e que reinicializações do servidor não sobrescrevem mais a senha.
