package com.skillswap.backend;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.config.annotation.*;
@Configuration
public class Config implements WebMvcConfigurer {
 @Value("${app.cors.allowed-origin:http://127.0.0.1:5500}") String origin;
 @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder();}
 @Override public void addCorsMappings(CorsRegistry r){r.addMapping("/api/**").allowedOrigins(origin,"http://localhost:5500").allowedMethods("GET","POST","PUT","PATCH","DELETE","OPTIONS").allowedHeaders("*");}
}
