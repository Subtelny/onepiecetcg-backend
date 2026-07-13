package pl.janda.onepiecetcg.application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "pl.janda.onepiecetcg")
public class OnePieceTcgApplication {

    public static void main(String[] args) {
        SpringApplication.run(OnePieceTcgApplication.class, args);
    }
}
