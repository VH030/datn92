package vn.hoang.datn92demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins(
                                "http://localhost:9000",
                                "http://34.87.169.80"
                        )
                        .allowedMethods("*")
                        .allowedHeaders("*")
                        .exposedHeaders(
                                "Authorization",
                                "X-Total-Count",
                                "X-Refresh-Token",
                                "Content-Disposition"
                        )
                        .allowCredentials(true)
                        .maxAge(3600);
            }
        };
    }
}
