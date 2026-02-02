package com.example.demo;

import io.camunda.client.annotation.Deployment;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {
        "com.example.demo",
        "worker",
        "controller",
        "service",
        "model1",
        "exception"
})

@EnableJpaRepositories(basePackages = {"model1"})
@EntityScan(basePackages = {"model1"})
@Deployment(resources = "classpath*:/model/*.*")


public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
