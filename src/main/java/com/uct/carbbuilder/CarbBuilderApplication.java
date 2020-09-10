package com.uct.carbbuilder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CarbBuilderApplication {

    public static void main(String[] args)
    {
        SpringApplication.run(CarbBuilderApplication.class, args);
    }

    /*public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry)
            {
                registry.addMapping("/**")
                        .allowedOrigins("https://glycarbo.cs.uct.ac.za/")
                        .allowedOrigins("http://127.0.0.1:8080/")
                        .allowedOrigins("http://sugarstore-1.cs.uct.ac.za/");
            }
        };
    }*/



}
