package de.telekom.bot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
public class BotApplication {

    public static void main(String[] args) {
        // Completely disable JMX to avoid RMI warnings
        System.setProperty("spring.jmx.enabled", "false");
        System.setProperty("com.sun.management.jmxremote", "false");
        System.setProperty("com.sun.management.jmxremote.port", "-1");
        System.setProperty("com.sun.management.jmxremote.registry.port", "-1");
        System.setProperty("com.sun.management.jmxremote.authenticate", "false");
        System.setProperty("com.sun.management.jmxremote.ssl", "false");
        System.setProperty("java.rmi.server.hostname", "localhost");
        
        // Create SpringApplication and disable JMX
        SpringApplication app = new SpringApplication(BotApplication.class);
        app.setRegisterShutdownHook(false);
        
        // Run the application
        app.run(args);
    }

}
