# Teste de Notificações (Email e WhatsApp)

## Setup Necessário

### 1. Credenciais SMTP configuradas em `.env`
```bash
MAIL_HOST=smtp.email.sa-saopaulo-1.oci.oraclecloud.com
MAIL_PORT=587
MAIL_USERNAME=seu_usuario@oci
MAIL_PASSWORD=sua_senha
MAIL_FROM=noreply@clinica.com
```

### 2. Servidor rodando
```bash
./mvnw spring-boot:run
```

## Fluxo de Teste

### Passo 1: Autenticar
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "receptionist@clinic.com",
    "password": "password123"
  }'
```

Guarde o `token` retornado.

### Passo 2: Listar Pacientes
```bash
curl -X GET http://localhost:8080/api/v1/patients \
  -H "Authorization: Bearer <TOKEN>"
```

Pegue um `id` de paciente com email válido.

### Passo 3: Criar Agendamento
```bash
curl -X POST http://localhost:8080/api/v1/appointments \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "patientId": "<PATIENT_ID>",
    "professionalId": "<PROFESSIONAL_ID>",
    "serviceId": "<SERVICE_ID>",
    "dateTime": "2026-07-12T14:00:00"
  }'
```

Guarde o `id` do agendamento.

### Passo 4: Confirmar Agendamento (dispara follow-up notification)
```bash
curl -X PUT http://localhost:8080/api/v1/appointments/<APPOINTMENT_ID>/confirm \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json"
```

## O que testar

1. **Verificar logs da aplicação**
   ```bash
   grep -i "email sent successfully\|error sending email" logs
   ```

2. **Verificar caixa de entrada**
   - Procure por email do `MAIL_FROM` configurado
   - Assunto: "SAAP Notificação"
   - Contém: dados do agendamento

3. **Testar casos extremos**
   - Paciente sem email configurado → não deve crashar
   - Email inválido → aplicação trata gracefully
   - Servidor SMTP indisponível → fallback para logging

## Endpoints de Notificação

### Follow-up de Agendamento
- **Disparado por**: Confirmação de agendamento
- **Destinatário**: Email do paciente
- **Conteúdo**: Confirmação, data/hora, profissional, links de confirmação/cancelamento

### Oferta de Fila de Espera
- **Disparado por**: Disponibilidade de vaga para paciente em fila
- **Destinatário**: Email do paciente
- **Conteúdo**: Serviço, data/hora, links de aceitar/recusar

## Troubleshooting

### Email não chega
1. Verificar credenciais SMTP em `.env`
2. Verificar se porta 587 ou 25 está aberta (firewall)
3. Verificar logs da aplicação:
   ```bash
   ./mvnw spring-boot:run | grep -i "email\|smtp\|mail"
   ```

### Teste localmente com Greenmail
```bash
./mvnw test -Dtest=EmailNotificationServiceIntegrationTest
```

## Configuração de Produção

Para habilitar/desabilitar notificações:
```bash
APP_NOTIFICATIONS_ENABLED=true  # ou false
```

---

# Teste de Notificações por WhatsApp (Botões Interativos)

Ver `docs/twilio.md` para setup completo de credenciais, Content Templates e túnel local. Esta seção cobre o **protocolo de teste real** usado para validar a feature ponta a ponta (sem mocks — sempre contra a API real do Twilio).

## Pré-requisitos

1. Servidor rodando localmente com `.env` configurado (`TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, `TWILIO_FROM_NUMBER`, `TWILIO_WAITLIST_CONTENT_SID`).
2. Túnel Cloudflare ativo (`cloudflared tunnel --url http://localhost:8080`) com `TWILIO_WEBHOOK_BASE_URL` no `.env` apontando para a URL do túnel **atual** (reiniciar o servidor após qualquer troca).
3. Endpoint de webhook configurado no Console Twilio (Sandbox settings → "When a message comes in") apontando para `<url-do-tunel>/api/v1/notifications/whatsapp/webhook`, método `POST`.
4. Paciente de teste com número já conectado ao Sandbox Twilio (join code do Sandbox).

## Protocolo de Teste: Gerar uma Oferta de Vaga com Botões

Os botões do WhatsApp são de uso único — depois de clicados, a mensagem não pode ser reaproveitada para um novo teste. Para gerar uma oferta nova sempre que necessário:

1. **Resetar a entrada da fila de espera para `WAITING`**, marcando `is_active = true` (via SQL direto ou endpoint administrativo), garantindo que ela volte a ser candidata a receber oferta.
2. **Criar um agendamento descartável** que ocupe exatamente o horário/profissional/serviço da entrada da fila:
   ```bash
   curl -X POST http://localhost:8080/api/v1/appointments \
     -H "Authorization: Bearer <TOKEN>" \
     -H "Content-Type: application/json" \
     -d '{
       "patientId": "<PATIENT_ID>",
       "professionalId": "<PROFESSIONAL_ID>",
       "serviceId": "<SERVICE_ID>",
       "dateTime": "<DATA_HORA_DO_SLOT>",
       "paymentMethod": "CASH"
     }'
   ```
3. **Cancelar esse agendamento**, o que libera o slot e dispara o preenchimento automático da fila de espera (auto-fill), gerando a notificação de oferta com botões:
   ```bash
   curl -X PUT http://localhost:8080/api/v1/appointments/<APPOINTMENT_ID>/cancel \
     -H "Authorization: Bearer <TOKEN>"
   ```
4. **Confirmar o envio via API real do Twilio** (nunca assumir sucesso só pelo log local):
   ```bash
   curl -s -u "<TWILIO_ACCOUNT_SID>:<TWILIO_AUTH_TOKEN>" \
     "https://api.twilio.com/2010-04-01/Accounts/<TWILIO_ACCOUNT_SID>/Messages.json?To=whatsapp:<NUMERO_PACIENTE>&PageSize=1"
   ```
   Verificar `status` (`delivered`/`read`) e o horário de envio da mensagem mais recente.

## Teste do Clique Real nos Botões

1. No celular conectado ao Sandbox, abrir a conversa de WhatsApp com o número Twilio e clicar em **Aceitar** ou **Recusar**.
2. O Twilio faz `POST` ao webhook; a aplicação responde em TwiML, que aparece como mensagem de volta no WhatsApp.
3. **Mensagens esperadas** (idênticas às usadas no fluxo de e-mail/link público, por paridade):
   - Aceitar: `"Vaga da fila de espera aceita e agendamento confirmado com sucesso!"`
   - Recusar: `"Vaga da fila de espera recusada com sucesso."`
4. **Verificar efeito no banco**: a entrada da fila de espera deve mudar para `status=ACCEPTED` (com novo agendamento `CONFIRMED` criado) ou `status=DECLINED`, e `is_active=false` em ambos os casos.

## Troubleshooting

- **Nenhuma resposta chega / erro genérico "não foi possível processar"**: verificar se há uma oferta `OFFERED` + `is_active=true` para o telefone que clicou — o webhook busca a oferta mais recente por telefone (`findMostRecentOfferedByPatientPhone`); sem oferta ativa, não há o que responder.
- **403 no webhook / Twilio reporta erro 11200**: mismatch entre `TWILIO_WEBHOOK_BASE_URL` e a URL real do túnel usada pelo Twilio para assinar a requisição — ver `docs/twilio.md` (seção de troubleshooting 11210 vs 11200).
- **Erro 11210 no Twilio**: túnel Cloudflare caiu/expirou — subir um novo e atualizar `.env`.

## Encerrando o Ambiente de Teste

Após validar todos os fluxos, encerrar o túnel Cloudflare (ele não deve ficar rodando sem necessidade):
```bash
pkill -f "cloudflared tunnel"
ps aux | grep cloudflared   # confirmar que não sobrou processo
```

Não há endpoint manual para disparo avulso por canal — email e WhatsApp são disparados automaticamente por `NotificationServiceImpl` nos fluxos de follow-up de agendamento (`sendFollowUpNotification`) e oferta de fila de espera (`sendWaitlistOfferNotification`), sempre para ambos os canais quando o paciente tiver e-mail e telefone cadastrados.

O canal SMS (`SmsNotificationService`) foi removido do projeto — o WhatsApp com Content Templates/botões interativos cobre o mesmo caso de uso com uma experiência mais rica (botões clicáveis em vez de responder um SMS em texto livre).
