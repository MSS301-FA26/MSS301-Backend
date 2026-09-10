package com.sba301.cinemaai;

import com.sba301.cinemaai.config.AIProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@EnableScheduling
@SpringBootApplication
@EnableConfigurationProperties(AIProperties.class)
public class CinemaAiApplication {

	public static void main(String[] args) {
		SpringApplication.run(CinemaAiApplication.class, args);
	}

}
