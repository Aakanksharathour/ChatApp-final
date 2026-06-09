package com.chatapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ════════════════════════════════════════════════════════════════
 *  ChatappApplication  —  THE ENTRY POINT of your entire app
 * ════════════════════════════════════════════════════════════════
 *
 *  This is the FIRST class Spring Boot looks for when you press "Run".
 *  Think of it like the front door of a building — everything starts here.
 *
 *  @SpringBootApplication is actually THREE annotations combined:
 *  1. @SpringBootConfiguration  — marks this as the main config class
 *  2. @EnableAutoConfiguration  — tells Spring to auto-setup everything
 *                                  (MongoDB, web server, security, etc.)
 *  3. @ComponentScan            — tells Spring to scan ALL classes in this
 *                                  package (com.chatapp) and sub-packages,
 *                                  and register them automatically
 *
 *  HOW TO RUN:
 *  - In IntelliJ IDEA: right-click this file → Run 'ChatappApplication'
 *  - In terminal: mvn spring-boot:run
 *
 *  After it starts, your API is available at http://localhost:8080
 */
@SpringBootApplication
public class ChatappApplication {

    public static void main(String[] args) {
        // This one line starts the entire Spring Boot application!
        // It boots up an embedded Tomcat web server inside your app —
        // no need to install a separate server like Apache.
        SpringApplication.run(ChatappApplication.class, args);
    }
}
