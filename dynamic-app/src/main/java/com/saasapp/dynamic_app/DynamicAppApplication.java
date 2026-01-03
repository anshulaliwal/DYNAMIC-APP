package com.saasapp.dynamic_app;

import com.saasapp.dynamic_app.config.DotenvConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DynamicAppApplication {

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(DynamicAppApplication.class);
		app.addInitializers(new DotenvConfig());
		app.run(args);
	}

}
