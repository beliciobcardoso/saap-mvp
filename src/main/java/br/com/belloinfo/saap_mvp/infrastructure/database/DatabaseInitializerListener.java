package br.com.belloinfo.saap_mvp.infrastructure.database;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.ResultSet;

public class DatabaseInitializerListener implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        ConfigurableEnvironment environment = event.getEnvironment();
        
        String url = environment.getProperty("spring.datasource.url");
        String username = environment.getProperty("spring.datasource.username");
        String password = environment.getProperty("spring.datasource.password");
        
        if (url == null || !url.contains("postgresql")) {
            return;
        }
        
        try {
            int lastSlash = url.lastIndexOf('/');
            if (lastSlash == -1) return;
            
            String dbName = url.substring(lastSlash + 1);
            if (dbName.contains("?")) {
                dbName = dbName.substring(0, dbName.indexOf('?'));
            }
            
            // Connect to default 'postgres' database to check/create target db
            String baseUrl = url.substring(0, lastSlash + 1) + "postgres";
            if (url.contains("?")) {
                String params = url.substring(url.indexOf('?'));
                baseUrl = baseUrl + params;
            }
            
            try (Connection conn = DriverManager.getConnection(baseUrl, username, password)) {
                boolean dbExists = false;
                try (Statement stmt = conn.createStatement()) {
                    try (ResultSet rs = stmt.executeQuery("SELECT 1 FROM pg_database WHERE datname = '" + dbName + "'")) {
                        if (rs.next()) {
                            dbExists = true;
                        }
                    }
                }
                
                if (!dbExists) {
                    System.out.println("-----------------------------------------------------------------");
                    System.out.println("Banco de dados '" + dbName + "' nao encontrado.");
                    System.out.println("Criando banco de dados '" + dbName + "' automaticamente...");
                    try (Statement stmt = conn.createStatement()) {
                        stmt.executeUpdate("CREATE DATABASE " + dbName);
                        System.out.println("Banco de dados '" + dbName + "' criado com sucesso!");
                    }
                    System.out.println("-----------------------------------------------------------------");
                }
            }
        } catch (Exception e) {
            System.err.println("Aviso: Nao foi possivel verificar ou criar o banco de dados automaticamente: " + e.getMessage());
        }
    }
}
