package br.com.solarz.master;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.List;

@SpringBootApplication(scanBasePackages = {
		"br.com.solarz.master",
		"repository",
		"model",
		"config"
})
@EnableJpaRepositories(basePackages = "repository")
@EntityScan(basePackages = "model")
public class MasterApplication {

	public static final List<String> WORKERS_ADDR = List.of(
			"192.168.0.7",
			"192.168.0.21",
			"192.168.0.11"
	);
	public static void main(String[] args) {
		SpringApplication.run(MasterApplication.class, args);
	}

}
