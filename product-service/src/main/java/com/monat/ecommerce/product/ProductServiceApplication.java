package com.monat.ecommerce.product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication(scanBasePackages = {
        "com.monat.ecommerce.product",
        "com.monat.ecommerce.common"
})
@EnableMongoAuditing
@EnableMongoRepositories(basePackages = "com.monat.ecommerce.product.domain.repository")
@EnableElasticsearchRepositories(basePackages = "com.monat.ecommerce.product.infrastructure.search")
public class ProductServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductServiceApplication.class, args);
    }
}
