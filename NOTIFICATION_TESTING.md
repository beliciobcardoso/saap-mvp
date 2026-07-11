# Teste de Notificações por Email

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

Para usar canal específico:
```bash
# Apenas email
POST /api/v1/appointments/{id}/notify?channel=email

# Múltiplos canais
POST /api/v1/appointments/{id}/notify?channels=email,whatsapp,sms
```
