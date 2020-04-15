package com.easygql;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@Configuration
public class GraphqlSpringBootApplication {
  public static void main(String[] args) {
    SpringApplication springApplication = new SpringApplication(GraphqlSpringBootApplication.class);
    springApplication.run(args);
  }
}
