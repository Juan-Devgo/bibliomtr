package co.edu.uniquindio.bibliomtr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class BibliomtrApplication {

	public static void main(String[] args) {
		SpringApplication.run(BibliomtrApplication.class, args);
	}

}
