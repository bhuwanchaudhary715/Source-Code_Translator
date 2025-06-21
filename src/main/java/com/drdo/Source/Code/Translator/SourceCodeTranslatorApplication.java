package com.drdo.Source.Code.Translator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

@SpringBootApplication

public class SourceCodeTranslatorApplication {

	public static void main(String[] args) {

		SpringApplication.run(SourceCodeTranslatorApplication.class, args);
	}

	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}

	@Bean
	public WebClient webClient() {
		return WebClient.builder().build();
	}

}




