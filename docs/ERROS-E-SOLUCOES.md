# 📋 Registro de Erros e Soluções — SAAP-MVP

Histórico de problemas encontrados durante o desenvolvimento, suas causas raízes e as soluções aplicadas.
Mantido para evitar repetição dos mesmos erros em sessões futuras.

---

## ERR-001 · Lacuna na sequência de migrações Flyway (V1 → V3 sem V2)

**Data:** 2026-06-27
**Severidade:** 🔴 Alta — servidor não subia

### Sintoma
```
Failed to execute script V3__criar_index_unico_servico_ativo.sql
ERROR: relation "idx_servico_name_active" already exists
```

### Causa raiz
Dois erros combinados:
1. O arquivo foi criado com nome `V3__...` quando deveria ser `V2__...` (pulo de versão sem razão)
2. O índice `idx_servico_name_active` já havia sido criado no banco **antes** do Flyway registrá-lo — provavelmente pelo Hibernate em modo `create` em sessão anterior

### Primeira resposta (errada)
Usar `IF NOT EXISTS` no script para forçar a passagem do `V3`. Funcionou tecnicamente, mas:
- Deixou a lacuna `V1 → V3` no histórico
- Não corrigiu a causa raiz (numeração errada)
- Cria precedente ruim (gambiarra vira padrão)

### Solução correta aplicada
```bash
# 1. Renomear o arquivo
mv src/main/resources/db/migration/V3__criar_index_unico_servico_ativo.sql \
   src/main/resources/db/migration/V2__criar_index_unico_servico_ativo.sql

# 2. Corrigir o registro no banco
docker exec -i postgis psql -U postgres -d saap_db -c "
  UPDATE flyway_schema_history
  SET version = '2', script = 'V2__criar_index_unico_servico_ativo.sql'
  WHERE version = '3';
"

# 3. Sincronizar checksums
./mvnw flyway:repair -Dflyway.url="jdbc:postgresql://localhost:5432/saap_db" \
  -Dflyway.user=postgres -Dflyway.password=P@ssw0rds

# 4. Validar
./mvnw flyway:validate -Dflyway.url="jdbc:postgresql://localhost:5432/saap_db" \
  -Dflyway.user=postgres -Dflyway.password=P@ssw0rds
```

### Resultado
```
Successfully validated 2 migrations (execution time 00:00.038s)
BUILD SUCCESS
```

### Prevenção implementada
- **Regra no `GEMINI.md`:** agente obrigado a listar `db/migration/` antes de criar qualquer arquivo
- **Teste automático `FlywayMigrationSequenceTest`:** falha o build se houver lacuna na sequência ou nomenclatura inválida

---

## ERR-002 · PROFESSIONAL conseguia criar pacientes (falha de autorização)

**Data:** 2026-06-27
**Severidade:** 🔴 Alta — falha de segurança

### Sintoma
```http
POST /api/v1/patients
Authorization: Bearer {{professionalToken}}
→ HTTP 400 (esperado: 403 Forbidden)
```
O sistema retornava `400 Bad Request` em vez de `403 Forbidden`, porque a validação de CPF rodava antes da verificação de autorização, mascarando a real causa do bloqueio.

### Causa raiz
O endpoint `POST /patients` não estava protegido com `@PreAuthorize`. O `SecurityConfig` não listava explicitamente a rota como restrita a `ADMIN` e `RECEPTIONIST`.

### Solução aplicada
Adicionado `@PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")` no `PatientController` e ajustada a ordem de filtros no `SecurityConfig` para que a autenticação/autorização seja verificada antes da validação de payload.

### Verificação
```http
POST /api/v1/patients
Authorization: Bearer {{professionalToken}}
→ HTTP 403 Forbidden ✅
```

---

## ERR-003 · Serviços duplicados podiam ser cadastrados

**Data:** 2026-06-27
**Severidade:** 🟡 Média — integridade de dados

### Sintoma
Era possível cadastrar dois serviços com o mesmo nome, descrição, duração e preço sem nenhum erro.

### Causa raiz
Não havia constraint de unicidade na tabela `servico` nem validação na camada de aplicação.

### Solução aplicada
**Migration `V2__criar_index_unico_servico_ativo.sql`:**
```sql
-- Remove duplicatas existentes mantendo a mais antiga
DELETE FROM servico a USING servico b
WHERE a.id > b.id AND a.name = b.name AND a.is_active = true AND b.is_active = true;

-- Índice único parcial: unicidade apenas para serviços ativos
CREATE UNIQUE INDEX IF NOT EXISTS idx_servico_name_active ON servico(name) WHERE is_active = true;
```

**`CreateServiceUseCase`:** validação prévia retorna `409 Conflict` antes de tentar inserir:
```java
if (serviceRepository.existsByNameAndIsActiveTrue(service.getName())) {
    throw new DuplicateServiceException("Já existe um serviço ativo com este nome");
}
```

### Verificação
```http
POST /api/v1/services (nome já existente)
→ HTTP 409 Conflict ✅
  { "error": "Conflict", "message": "Já existe um serviço ativo com este nome" }
```

---

## ERR-004 · CPF formatado causava erro 400 ao cadastrar paciente

**Data:** 2026-06-27
**Severidade:** 🟡 Média — UX ruim / dados inconsistentes

### Sintoma
```http
POST /api/v1/patients
{ "cpf": "123.456.789-09" }
→ HTTP 400 — "O CPF informado é inválido"
```
CPF com formatação (`123.456.789-09`) era rejeitado, apesar de ser válido. Só funcionava sem formatação (`12345678909`).

### Causa raiz
A coluna `cpf` no banco tinha `length = 11`, mas o `WebMapper` passava o CPF bruto (com pontos e traço = 14 chars) diretamente para o domínio sem normalizar. O `CpfValidator` já removia os não-dígitos para validar, mas o valor original era persistido.

### Solução aplicada
Normalização na camada de aplicação (`CreatePatientUseCase` e `UpdatePatientUseCase`), que é o lugar correto no Clean Architecture:

```java
// Normaliza: aceita "123.456.789-09" ou "12345678909" → salva "12345678909"
String normalizedCpf = updated.getCpf().replaceAll("\\D", "");
```

### Verificação
```http
POST /api/v1/patients { "cpf": "529.982.247-25" } → HTTP 201 Created ✅
POST /api/v1/patients { "cpf": "71428793860" }     → HTTP 201 Created ✅
POST /api/v1/patients { "cpf": "529.982.247-25" }  → HTTP 409 Conflict ✅ (duplicata)
```

---

## ERR-005 · Warning `Unknown property 'api'` no VSCode Spring Boot

**Data:** 2026-06-27
**Severidade:** 🟢 Baixa — aviso de IDE, sem impacto em runtime

### Sintoma
VSCode exibia:
```
Unknown property 'api' vscode-spring-boot(YAML_UNKNOWN_PROPERTY)
```
Em `application.yaml`, a propriedade customizada `api.security.token.secret` não era reconhecida pelo Language Server do Spring Boot.

### Causa raiz
O Spring Boot Language Server valida propriedades contra metadados gerados pelo `spring-boot-configuration-processor`. Sem ele, qualquer propriedade fora do namespace `spring.*` é marcada como desconhecida.

### Solução aplicada
**`pom.xml`** — adicionada dependência opcional:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-configuration-processor</artifactId>
    <optional>true</optional>
</dependency>
```

**Nova classe `SecurityProperties.java`** com `@ConfigurationProperties(prefix = "api.security.token")`:
```java
@ConfigurationProperties(prefix = "api.security.token")
public record SecurityProperties(String secret, long expiration) {}
```

O processor gera `META-INF/spring-configuration-metadata.json` com os metadados das propriedades customizadas, eliminando o warning.

---

## 🔖 Referências rápidas

| Comando | Uso |
|---------|-----|
| `ls src/main/resources/db/migration/` | Verificar próxima versão antes de criar migration |
| `docker exec -i postgis psql -U postgres -d saap_db -c "SELECT version, script, success FROM flyway_schema_history ORDER BY installed_rank;"` | Ver histórico do Flyway |
| `./mvnw flyway:repair ...` | Sincronizar checksums após renomear arquivo |
| `./mvnw flyway:validate ...` | Confirmar consistência antes de subir servidor |
| `./mvnw test -Dtest="FlywayMigrationSequenceTest"` | Validar sequência das migrations |
| `./mvnw test` | Rodar todos os testes antes de commitar |
