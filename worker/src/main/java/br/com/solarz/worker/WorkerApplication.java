package br.com.solarz.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class WorkerApplication {

	public static final String MASTER_ADDRESS = "192.168.0.10";
	public static void main(String[] args) {
		SpringApplication.run(WorkerApplication.class, args);
	}

}
