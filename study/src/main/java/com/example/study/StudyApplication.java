package com.example.study;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main entry point for the Study Spring Boot application.
 * 
 * @SpringBootApplication marks this as a configuration class that declares one or more @Bean methods 
 * and also triggers auto-configuration and component scanning.
 * @EnableAsync enables Spring's ability to run methods with the @Async annotation in a background thread pool.
 * @EnableScheduling enables Spring's scheduled task execution capability for methods annotated with @Scheduled.
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class StudyApplication {

	/**
	 * The main method serves as the entry point that launches the Spring application.
	 * 
	 * @param args command line arguments
	 */
	public static void main(String[] args) {
		SpringApplication.run(StudyApplication.class, args);
	}
}
