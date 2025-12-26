package com.cyan.dataman;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * @author cy.Y
 * @since  1.0.0
 */
@SpringBootApplication
@ComponentScan(basePackages = "com.cyan")
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}