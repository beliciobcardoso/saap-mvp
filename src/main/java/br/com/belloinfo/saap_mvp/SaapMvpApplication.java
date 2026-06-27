package br.com.belloinfo.saap_mvp;

import br.com.belloinfo.saap_mvp.infrastructure.database.DatabaseInitializerListener;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SaapMvpApplication {

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(SaapMvpApplication.class);
		app.addListeners(new DatabaseInitializerListener());
		app.run(args);
	}

}
