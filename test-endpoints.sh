#!/bin/bash

# Script para testar endpoints de notificação em tempo real
# Uso: ./test-endpoints.sh

set -e

BASE_URL="http://localhost:8080/api/v1"

echo "=== TESTE DE ENDPOINTS DE NOTIFICAÇÃO ==="
echo ""

# Cores para output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

log_success() {
  echo -e "${GREEN}✅ $1${NC}"
}

log_error() {
  echo -e "${RED}❌ $1${NC}"
}

log_info() {
  echo -e "${YELLOW}ℹ️  $1${NC}"
}

# Verificar se servidor está rodando
log_info "Verificando se servidor está rodando..."
if ! curl -s http://localhost:8080/health >/dev/null 2>&1; then
  log_error "Servidor não está respondendo em localhost:8080"
  exit 1
fi
log_success "Servidor está rodando"
echo ""

# Tentar login com diferentes usuários de teste
log_info "Tentando autenticação..."

for EMAIL in "admin@saap.com" "receptionist@saap.com" "user@test.com"; do
  for PASSWORD in "adminPass123" "password123" "test"; do
    response=$(curl -s -X POST "$BASE_URL/auth/login" \
      -H "Content-Type: application/json" \
      -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}" 2>/dev/null || echo "{}")

    token=$(echo "$response" | grep -o '"token":"[^"]*' | cut -d'"' -f4 || true)

    if [ -n "$token" ] && [ "$token" != "null" ]; then
      log_success "Login bem-sucedido com $EMAIL"
      AUTH_TOKEN="$token"
      AUTH_EMAIL="$EMAIL"
      break 2
    fi
  done
done

if [ -z "$AUTH_TOKEN" ]; then
  log_error "Não foi possível autenticar com nenhum usuário de teste"
  echo ""
  log_info "Para criar usuário de teste, execute:"
  echo "  ./mvnw test -Dtest=SecurityIntegrationTest"
  exit 1
fi

echo ""
log_info "Autenticado como: $AUTH_EMAIL"
echo ""

# Listar pacientes
log_info "Buscando pacientes..."
patients_response=$(curl -s -X GET "$BASE_URL/patients" \
  -H "Authorization: Bearer $AUTH_TOKEN" \
  -H "Content-Type: application/json")

# Extrair primeiro paciente com email
patient_count=$(echo "$patients_response" | grep -o '"id":"[^"]*' | wc -l)

if [ "$patient_count" -eq 0 ]; then
  log_error "Nenhum paciente encontrado"
  exit 1
fi

log_success "Encontrados $patient_count pacientes"
echo ""

# Listar profissionais
log_info "Buscando profissionais..."
professionals=$(curl -s -X GET "$BASE_URL/professionals" \
  -H "Authorization: Bearer $AUTH_TOKEN" \
  -H "Content-Type: application/json")

prof_count=$(echo "$professionals" | grep -o '"id":"[^"]*' | wc -l)

if [ "$prof_count" -eq 0 ]; then
  log_error "Nenhum profissional encontrado"
  exit 1
fi

log_success "Encontrados $prof_count profissionais"
echo ""

# Listar serviços
log_info "Buscando serviços..."
services=$(curl -s -X GET "$BASE_URL/services" \
  -H "Authorization: Bearer $AUTH_TOKEN" \
  -H "Content-Type: application/json")

service_count=$(echo "$services" | grep -o '"id":"[^"]*' | wc -l)

if [ "$service_count" -eq 0 ]; then
  log_error "Nenhum serviço encontrado"
  exit 1
fi

log_success "Encontrados $service_count serviços"
echo ""

# Extrair IDs para teste
PATIENT_ID=$(echo "$patients_response" | grep -o '"id":"[^"]*' | head -1 | cut -d'"' -f4)
PROFESSIONAL_ID=$(echo "$professionals" | grep -o '"id":"[^"]*' | head -1 | cut -d'"' -f4)
SERVICE_ID=$(echo "$services" | grep -o '"id":"[^"]*' | head -1 | cut -d'"' -f4)

echo "IDs para teste:"
echo "  Paciente: ${PATIENT_ID:0:8}..."
echo "  Profissional: ${PROFESSIONAL_ID:0:8}..."
echo "  Serviço: ${SERVICE_ID:0:8}..."
echo ""

# Criar agendamento
log_info "Criando agendamento..."
datetime=$(date -u -d '+1 day' +%Y-%m-%dT14:00:00 2>/dev/null || date -u -v+1d +%Y-%m-%dT14:00:00 2>/dev/null || echo "2026-07-12T14:00:00")

apt_response=$(curl -s -X POST "$BASE_URL/appointments" \
  -H "Authorization: Bearer $AUTH_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"patientId\": \"$PATIENT_ID\",
    \"professionalId\": \"$PROFESSIONAL_ID\",
    \"serviceId\": \"$SERVICE_ID\",
    \"dateTime\": \"$datetime\"
  }")

APPOINTMENT_ID=$(echo "$apt_response" | grep -o '"id":"[^"]*' | head -1 | cut -d'"' -f4)

if [ -z "$APPOINTMENT_ID" ]; then
  log_error "Falha ao criar agendamento"
  echo "Resposta: $apt_response"
  exit 1
fi

log_success "Agendamento criado: ${APPOINTMENT_ID:0:8}..."
echo ""

# Confirmar agendamento (deve disparar notificação)
log_info "Confirmando agendamento (disparando notificação por email)..."
confirm_response=$(curl -s -X PUT "$BASE_URL/appointments/$APPOINTMENT_ID/confirm" \
  -H "Authorization: Bearer $AUTH_TOKEN" \
  -H "Content-Type: application/json")

log_success "Agendamento confirmado"
echo ""

# Instruções finais
echo "=== ✅ TESTES COMPLETADOS ==="
echo ""
log_info "Próximas etapas:"
echo "1. Aguarde ~5 segundos para o email ser processado"
echo "2. Verifique sua caixa de entrada por email de:"
echo "   - Assunto: 'SAAP Notificação'"
echo "   - Remitente: Configurado em MAIL_FROM no .env"
echo ""
log_info "Se o email não chegar:"
echo "1. Verifique os logs:"
echo "   tail -f /tmp/saap.log | grep -i email"
echo "2. Confirme credenciais SMTP no .env"
echo "3. Verifique conectividade: nc -zv smtp.email.sa-saopaulo-1.oci.oraclecloud.com 587"
echo ""
log_info "Endpoint do agendamento: /api/v1/appointments/$APPOINTMENT_ID"
