package pl.janda.onepiecetcg.infrastructure.web.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InternalSecurityConfig {

    @Bean
    public FilterRegistrationBean<InternalApiKeyFilter> internalApiKeyFilter(@Value("${internal.api-key}") String apiKey) {
        var registration = new FilterRegistrationBean<InternalApiKeyFilter>();
        registration.setFilter(new InternalApiKeyFilter(apiKey));
        registration.addUrlPatterns("/api/internal/*");
        return registration;
    }
}
