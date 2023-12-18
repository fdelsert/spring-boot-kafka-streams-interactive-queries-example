package com.fdelsert.springbootkafkastreamsinteractivequeriesexample;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafkaStreams;

@SpringBootApplication
@EnableKafkaStreams
public class SpringBootKafkaStreamsInteractiveQueriesExampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringBootKafkaStreamsInteractiveQueriesExampleApplication.class, args);
    }

}
