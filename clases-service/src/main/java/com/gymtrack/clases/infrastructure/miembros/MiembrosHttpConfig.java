package com.gymtrack.clases.infrastructure.miembros;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import java.time.Duration;

@Configuration
public class MiembrosHttpConfig {
    @Bean
    public RestClient miembrosHttpClient(RestClient.Builder builder,
            @Value("${miembros.base-url:http://localhost:8081}") String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(3));
        return builder.baseUrl(baseUrl).requestFactory(factory).build();
    }
}
