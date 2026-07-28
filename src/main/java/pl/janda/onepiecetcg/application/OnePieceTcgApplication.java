package pl.janda.onepiecetcg.application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ComponentScan(basePackages = "pl.janda.onepiecetcg")
@EnableJpaRepositories(basePackages = "pl.janda.onepiecetcg")
@EntityScan(basePackages = "pl.janda.onepiecetcg")
@EnableScheduling
public class OnePieceTcgApplication {

    public static void main(String[] args) {
        SpringApplication.run(OnePieceTcgApplication.class, args);
    }
}
