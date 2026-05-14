package ru.practicum.requestservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "ru.practicum.iteractionapi.feignapi")
@ComponentScan(basePackages = {"ru.practicum.requestservice", "ru.practicum.client"})
public class RequestApplication {
	public static void main(String[] args) {
		SpringApplication.run(RequestApplication.class, args);
	}
}
