# Spring Boot Kafka Streams Interactive Queries Example

This project serves as an example showcasing the use of **Kafka Streams** **Interactive Query** in the context of a **Spring Boot** application.

> **This project is a learning example and is not intended for production use.** It is not production-ready and lacks error handling, security, scalability considerations, and other aspects required for a production deployment.

## Project Overview

The project is built using Java 21 and leverages the following technologies:

- [Spring Boot](https://spring.io/projects/spring-boot)
- [Spring Kafka](https://spring.io/projects/spring-kafka) : [Apache Kafka Streams Support](https://docs.spring.io/spring-kafka/reference/streams.html)
- [Apache Kafka Streams](https://kafka.apache.org/documentation/streams/)
- [Confluent SerDe Avro](https://docs.confluent.io/platform/current/schema-registry/fundamentals/serdes-develop/serdes-avro.html)

Inspired by [WordCountInteractiveQueriesExample.java](https://github.com/confluentinc/kafka-streams-examples/blob/7.5.2-post/src/main/java/io/confluent/examples/streams/interactivequeries/WordCountInteractiveQueriesExample.java) from [Confluent's Kafka Streams Examples](https://github.com/confluentinc/kafka-streams-examples).

## Architecture

The Kafka Streams application:
1. **Consumes** Avro messages from the `input-topic`
2. **Materialises** data into a state store named `user-table` (KTable)
3. **Replicates** messages to the `output-topic` (for validation)
4. **Exposes** a REST API to query the state store (Interactive Queries)

The `User` Avro schema is automatically serialized/deserialized using Confluent's Avro SerDe and Karapace Schema Registry.

## Quick Start

### Prerequisites

- Java 21
- Maven 3.6+
- Docker and Docker Compose

### 1. Start the Infrastructure

The `docker-compose.yaml` starts the following services:

| Service | Image | Port |
|---|---|---|
| **Kafka** | `confluentinc/cp-kafka:8.1.0` | `9092` |
| **Karapace Schema Registry** | `ghcr.io/aiven-open/karapace:6.1.3` | `8081` |

```bash
docker compose up -d

# Wait for all services to be healthy
docker compose ps
```

### 2. Build and Start the Application

```bash
# Compile the project (generates Avro classes)
mvn clean compile

# Start the Spring Boot application
export KAFKA_REGYSTRY_URL=http://localhost:8081
mvn spring-boot:run
```

The application starts on port `8080` by default.

### 3. Produce Sample Data

A script is provided to produce test data via `kafka-avro-console-producer` (from the `cp-schema-registry` image, pulled automatically):

```bash
chmod +x produce-sample-data.sh
./produce-sample-data.sh
```

This script produces 5 messages into `input-topic` with plain UTF-8 string keys and Avro-serialized values:
- **alice**: `{ name: "Alice", favorite_number: 42, favorite_color: "blue" }`
- **bob**: `{ name: "Bob", favorite_number: 7, favorite_color: "red" }`
- **charlie**: `{ name: "Charlie", favorite_number: null, favorite_color: "green" }`
- **david**: `{ name: "David", favorite_number: 99, favorite_color: null }`
- **eve**: `{ name: "Eve", favorite_number: 13, favorite_color: "purple" }`

You can also produce a message manually at any time:

```bash
echo 'frank:{"name":"Frank","favorite_number":{"int":100},"favorite_color":{"string":"yellow"}}' | \
  docker run --rm -i \
    --network spring-boot-kafka-streams-interactive-queries-example_kafka-network \
    confluentinc/cp-schema-registry:8.1.0 \
    kafka-avro-console-producer \
      --bootstrap-server kafka:29092 \
      --topic input-topic \
      --property schema.registry.url=http://karapace-registry:8081 \
      --property value.schema='{"type":"record","name":"User","namespace":"example.avro","fields":[{"name":"name","type":"string"},{"name":"favorite_number","type":["int","null"]},{"name":"favorite_color","type":["string","null"]}]}' \
      --property parse.key=true \
      --property key.separator=: \
      --property key.serializer=org.apache.kafka.common.serialization.StringSerializer
```

### 4. Query the State Store (Interactive Queries)

Once data is produced, the Kafka Streams application materialises it into the `user-table` state store. Query it via the REST API:

#### Get a specific user by key

```bash
curl -s http://localhost:8080/state/keyvalue/user-table/alice | jq
```

```json
{
  "key": "alice",
  "value": {
    "name": "Alice",
    "favorite_number": 42,
    "favorite_color": "blue"
  }
}
```

#### Get all users

```bash
curl -s http://localhost:8080/state/keyvalues/user-table/all | jq
```

```json
[
  {
    "key": "alice",
    "value": { "name": "Alice", "favorite_number": 42, "favorite_color": "blue" }
  },
  {
    "key": "bob",
    "value": { "name": "Bob", "favorite_number": 7, "favorite_color": "red" }
  },
  {
    "key": "charlie",
    "value": { "name": "Charlie", "favorite_number": null, "favorite_color": "green" }
  },
  {
    "key": "david",
    "value": { "name": "David", "favorite_number": 99, "favorite_color": null }
  },
  {
    "key": "eve",
    "value": { "name": "Eve", "favorite_number": 13, "favorite_color": "purple" }
  }
]
```

## Useful Commands

### Kafka

```bash
# List topics
docker exec -it kafka kafka-topics --bootstrap-server localhost:9092 --list

# Consume raw messages from input-topic
docker exec -it kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic input-topic \
  --from-beginning
```

### Karapace Schema Registry

```bash
# List registered subjects
curl -s http://localhost:8081/subjects | jq

# List versions of a subject
curl -s http://localhost:8081/subjects/input-topic-value/versions | jq

# Get a specific schema version
curl -s http://localhost:8081/subjects/input-topic-value/versions/1 | jq
```

### Application Health

```bash
curl -s http://localhost:8080/actuator/health | jq

# Kafka Streams state specifically
curl -s http://localhost:8080/actuator/health | jq '.components.kafkaStream'
```

## Cleanup

```bash
# Stop the Spring Boot application (Ctrl+C)

# Stop and remove containers
docker compose down

# Also remove persisted data
docker compose down -v
```

## Going Further: KStreamplify

If you are looking for a production-ready solution, consider [KStreamplify](https://github.com/michelin/kstreamplify), an open-source library by Michelin that integrates natively with Spring Boot and provides built-in support for Kafka Streams Interactive Queries (among other features like error handling, topology testing, health checks, and more).
