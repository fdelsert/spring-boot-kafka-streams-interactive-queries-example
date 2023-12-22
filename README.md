# Spring Boot Kafka Streams Interactive Queries Example

This project serves as an example showcasing the use of **Kafka Streams** **Interactive Query** in the context of a **Spring Boot** application.

Inspired by [WordCountInteractiveQueriesExample.java](https://github.com/confluentinc/kafka-streams-examples/blob/7.5.2-post/src/main/java/io/confluent/examples/streams/interactivequeries/WordCountInteractiveQueriesExample.java) from [Confluent's Kafka Streams Examples](https://github.com/confluentinc/kafka-streams-examples) for the Interactive Queries part.

## Project Overview

The project is built using Java 21 and leverages the following technologies:

- [Spring Boot](https://spring.io/projects/spring-boot)
- [Spring Kafka](https://spring.io/projects/spring-kafka) : [Apache Kafka Streams Support](https://docs.spring.io/spring-kafka/reference/streams.html)
- [Apache Kafka Streams](https://kafka.apache.org/documentation/streams/)
- [Confluent SerDe Avro](https://docs.confluent.io/platform/current/schema-registry/fundamentals/serdes-develop/serdes-avro.html)

## Maven Configuration

The project's Maven configuration is specified in the `pom.xml` file, with notable dependencies and plugins including:

- `spring-boot-starter-web`
- `spring-boot-starter-actuator`
- `spring-kafka`
- `kafka-streams`
- `kafka-streams-avro-serde`
- `avro-maven-plugin`
- `spring-boot-starter-test`
- `spring-kafka-test`

## Additional Notes

Feel free to explore and adapt this project as needed for your own use cases. For more information on Kafka Streams Interactive Queries, refer to the [Confluent documentation](https://docs.confluent.io/platform/current/streams/developer-guide/interactive-queries.html).