package br.com.solarz.master;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.List;

@SpringBootApplication
public class MasterApplication {

	public static final List<String> WORKERS_ADDR = List.of(
//			"192.168.0.11",
			"localhost"
	);
	public static void main(String[] args) {
		SpringApplication.run(MasterApplication.class, args);
	}

}
