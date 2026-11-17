package com.bookdhara;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class BookDharaApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookDharaApplication.class, args);
    }
}
