package br.com.belloinfo.saap_mvp;

import br.com.belloinfo.saap_mvp.infrastructure.config.ClinicSettings;
import br.com.belloinfo.saap_mvp.infrastructure.config.SaapProperties;
import br.com.belloinfo.saap_mvp.infrastructure.database.DatabaseInitializerListener;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@SpringBootApplication
@org.springframework.scheduling.annotation.EnableScheduling
@EnableConfigurationProperties({ClinicSettings.class, SaapProperties.class})
public class SaapMvpApplication {

	public static void main(String[] args) {
		loadEnv();
		SpringApplication app = new SpringApplication(SaapMvpApplication.class);
		app.addListeners(new DatabaseInitializerListener());
		app.run(args);
	}

	private static void loadEnv() {
		try {
			if (Files.exists(Paths.get(".env"))) {
				List<String> lines = Files.readAllLines(Paths.get(".env"));
				for (String line : lines) {
					line = line.trim();
					if (line.isEmpty() || line.startsWith("#")) {
						continue;
					}
					int eqIdx = line.indexOf('=');
					if (eqIdx != -1) {
						String key = line.substring(0, eqIdx).trim();
						String val = line.substring(eqIdx + 1).trim();
						// Strip quotes if present
						if (val.startsWith("\"") && val.endsWith("\"")) {
							val = val.substring(1, val.length() - 1);
						} else if (val.startsWith("'") && val.endsWith("'")) {
							val = val.substring(1, val.length() - 1);
						}
						System.setProperty(key, val);
					}
				}
				System.out.println("Variáveis de ambiente do arquivo .env carregadas com sucesso!");
			}
		} catch (IOException e) {
			System.err.println("Aviso: Não foi possível carregar o arquivo .env: " + e.getMessage());
		}
	}

}
