# Regras do Projeto SAAP-MVP

Estas regras são obrigatórias. O agente DEVE segui-las em toda sessão, sem exceções.

---

## 🗄️ REGRA: Migrações Flyway

### Numeração sequencial obrigatória

**ANTES de criar qualquer arquivo de migração**, o agente DEVE:

1. Listar os arquivos existentes em `src/main/resources/db/migration/`
2. Identificar o maior número de versão atual (ex: `V2` → próximo é `V3`)
3. Nomear o novo arquivo como `V{N+1}__descricao_curta.sql`

```bash
# SEMPRE executar antes de criar uma migration:
ls src/main/resources/db/migration/
```

**Proibido:**
- ❌ Pular versões (V1 → V3 sem V2)
- ❌ Criar migration sem verificar a sequência atual
- ❌ Usar `IF NOT EXISTS` como workaround para contornar falha de migração
- ❌ Criar o índice/tabela manualmente no banco antes de rodar o Flyway

**Obrigatório:**
- ✅ Sequência sempre contínua: V1, V2, V3, V4...
- ✅ Nome descritivo: `V3__adicionar_coluna_email_paciente.sql`
- ✅ Scripts idempotentes apenas quando explicitamente justificado no comentário do SQL

### Se uma migração falhar

**NÃO** use `IF NOT EXISTS` como primeira solução. O procedimento correto é:

1. Identificar a causa raiz (objeto já existe? versão errada? checksum divergente?)
2. Corrigir a causa raiz (renomear arquivo, ajustar SQL, reparar histórico)
3. Executar `./mvnw flyway:repair` para sincronizar checksums
4. Executar `./mvnw flyway:validate` para confirmar consistência
5. Só então subir o servidor

```bash
# Verificar histórico antes de qualquer ação:
docker exec -i postgis psql -U postgres -d saap_db \
  -c "SELECT version, script, success FROM flyway_schema_history ORDER BY installed_rank;"

# Reparar após correção:
./mvnw flyway:repair -Dflyway.url="jdbc:postgresql://localhost:5432/saap_db" \
  -Dflyway.user=postgres -Dflyway.password=P@ssw0rds

# Validar sempre antes de subir:
./mvnw flyway:validate -Dflyway.url="jdbc:postgresql://localhost:5432/saap_db" \
  -Dflyway.user=postgres -Dflyway.password=P@ssw0rds
```

### Histórico de incidentes

| Data | Problema | Causa | Lição |
|------|----------|-------|-------|
| 2026-06-27 | V1 → V3 sem V2; servidor não subia | Arquivo nomeado `V3` quando deveria ser `V2`; índice criado antes do Flyway | Sempre verificar a sequência antes de criar migration |

---

## 🏗️ Convenções gerais

- **Banco de dados:** PostgreSQL via container `postgis` na porta `5432`
- **Banco de desenvolvimento:** `saap_db` / usuário `postgres`
- **ORM:** Hibernate com `ddl-auto: validate` — o schema é gerenciado **exclusivamente** pelo Flyway
- **Nunca** usar `ddl-auto: create` ou `ddl-auto: update` em qualquer ambiente

---

## 🧪 Testes

- Sempre rodar `./mvnw test` antes de commitar
- Arquivos REST Client para testes manuais estão em `docs/REST Client/`
- Seed de dados de teste: ver `docs/REST Client/FullTest.http`

---

## 📋 Registro de erros

Todos os erros encontrados durante o desenvolvimento, suas causas raízes e soluções estão documentados em:

**[`docs/ERROS-E-SOLUCOES.md`](docs/ERROS-E-SOLUCOES.md)**

**Quando encontrar um novo erro:**
1. Resolver seguindo os procedimentos documentados
2. Registrar o novo incidente no arquivo acima com: sintoma, causa raiz, primeira resposta (se errada), solução correta e verificação
