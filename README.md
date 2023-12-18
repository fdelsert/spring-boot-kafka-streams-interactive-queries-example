# Spring Boot Kafka Streams Interactive Queries Example

This project serves as an example showcasing the use of **Kafka Streams** **Interactive Query** in the context of a **Spring Boot** application.

## Project Overview

The project is built using Java 21 and leverages the following technologies:

- [Spring Boot](https://spring.io/projects/spring-boot)
- [Spring Kafka](https://spring.io/projects/spring-kafka)
- [Apache Kafka Streams](https://kafka.apache.org/documentation/streams/)
- [Confluent SerDe](https://docs.confluent.io/platform/current/schema-registry/fundamentals/serdes-develop/serdes-avro.html)

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