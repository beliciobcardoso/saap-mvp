package br.com.belloinfo.saap_mvp.infrastructure.database;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.ResultSet;
import java.util.regex.Pattern;

public class DatabaseInitializerListener implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    private static final Pattern VALID_DB_NAME = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*");

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

            if (!VALID_DB_NAME.matcher(dbName).matches()) {
                System.err.println("Aviso: nome de banco de dados invalido ('" + dbName + "'), criacao automatica ignorada.");
                return;
            }

            // Connect to default 'postgres' database to check/create target db
            String baseUrl = url.substring(0, lastSlash + 1) + "postgres";
            if (url.contains("?")) {
                String params = url.substring(url.indexOf('?'));
                baseUrl = baseUrl + params;
            }
            
            try (Connection conn = DriverManager.getConnection(baseUrl, username, password)) {
                boolean dbExists = false;
                try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM pg_database WHERE datname = ?")) {
                    ps.setString(1, dbName);
                    try (ResultSet rs = ps.executeQuery()) {
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
