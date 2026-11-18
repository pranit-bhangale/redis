package com.pranit.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class InventoryApplication {

    static void main(String[] args) {
        SpringApplication.run(InventoryApplication.class, args);
    }

}
