package br.com.belloinfo.saap_mvp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Garante que os scripts de migração Flyway seguem a numeração sequencial correta.
 *
 * Motivação: em 2026-06-27, um script foi nomeado V3 pulando o V2, causando falha
 * no startup do servidor. Esta validação impede que o mesmo erro ocorra novamente.
 */
@DisplayName("Flyway Migration Sequence Guard")
class FlywayMigrationSequenceTest {

    private static final String MIGRATION_DIR = "src/main/resources/db/migration";
    private static final Pattern VERSION_PATTERN = Pattern.compile("^V(\\d+)__.*\\.sql$");

    @Test
    @DisplayName("Scripts de migração devem ter numeração sequencial sem lacunas")
    void migrationVersionsShouldBeSequentialWithoutGaps() {
        File migrationDir = new File(MIGRATION_DIR);
        assertThat(migrationDir).exists().isDirectory();

        File[] files = migrationDir.listFiles((dir, name) -> name.matches("V\\d+__.*\\.sql"));
        assertThat(files).as("Deve haver ao menos uma migration").isNotNull().isNotEmpty();

        List<Integer> versions = Arrays.stream(files)
                .map(File::getName)
                .map(name -> {
                    Matcher m = VERSION_PATTERN.matcher(name);
                    return m.matches() ? Integer.parseInt(m.group(1)) : null;
                })
                .filter(v -> v != null)
                .sorted()
                .collect(Collectors.toList());

        // Verifica que começa em 1
        assertThat(versions.get(0))
                .as("A primeira migration deve ser V1")
                .isEqualTo(1);

        // Verifica sequência contínua: V1, V2, V3, ...
        for (int i = 0; i < versions.size() - 1; i++) {
            int current = versions.get(i);
            int next = versions.get(i + 1);
            if (next != current + 1) {
                fail(String.format(
                        "Lacuna na sequência de migrations: V%d existe mas V%d está faltando. " +
                        "Versões encontradas: %s. " +
                        "NUNCA pule versões — crie V%d antes de V%d.",
                        current, current + 1, versions, current + 1, next));
            }
        }
    }

    @Test
    @DisplayName("Scripts de migração devem seguir o padrão de nomenclatura V{N}__descricao.sql")
    void migrationFilesShouldFollowNamingConvention() {
        File migrationDir = new File(MIGRATION_DIR);
        File[] allSqlFiles = migrationDir.listFiles((dir, name) -> name.endsWith(".sql"));
        assertThat(allSqlFiles).isNotNull();

        List<String> invalidNames = Arrays.stream(allSqlFiles)
                .map(File::getName)
                .filter(name -> !name.matches("V\\d+__[a-z0-9_]+\\.sql"))
                .collect(Collectors.toList());

        assertThat(invalidNames)
                .as("Arquivos com nomenclatura inválida (esperado: V{N}__descricao_minuscula.sql)")
                .isEmpty();
    }
}
