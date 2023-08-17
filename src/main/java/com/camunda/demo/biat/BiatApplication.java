package com.camunda.demo.biat;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import com.camunda.demo.biat.storage.StorageProperties;
import com.camunda.demo.biat.storage.StorageService;

import io.camunda.zeebe.spring.client.annotation.Deployment;

@SpringBootApplication
@EnableConfigurationProperties(StorageProperties.class)
@Deployment(resources = "classpath:bpmn/*.bpmn")
public class BiatApplication {

	public static void main(String[] args) {
		SpringApplication.run(BiatApplication.class, args);
	}

	@Bean
	CommandLineRunner init(StorageService storageService) {
		return (args) -> {
			storageService.deleteAll();
			storageService.init();
		};
	}

}
