package com.example.productcatalogue;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ProductCatalogueApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductCatalogueApplication.class, args);
    }

}