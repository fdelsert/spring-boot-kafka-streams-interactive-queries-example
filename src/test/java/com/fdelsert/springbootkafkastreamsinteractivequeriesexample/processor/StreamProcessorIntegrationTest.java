package com.fdelsert.springbootkafkastreamsinteractivequeriesexample.processor;

import static com.fdelsert.springbootkafkastreamsinteractivequeriesexample.processor.StreamProcessor.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import example.avro.User;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
        properties = {
                "spring.kafka.bootstrap-servers = ${spring.embedded.kafka.brokers}",
                "spring.kafka.streams.properties.schema.registry.url : mock://schema-registry"
        },
        webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@EmbeddedKafka(
        controlledShutdown = true,
        topics = {
                "input-topic",
                "output-topic"
        },
        partitions = 1,
        brokerProperties = {
                "listeners=PLAINTEXT://localhost:9092",
                "port=9092"
        })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc
class StreamProcessorIntegrationTest {

    private static final String MOCK_SCHEMA_REGISTRY_URL = "mock://schema-registry";

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private MockMvc mockMvc;

    private Consumer<String, User> createConsumer(String topic) {
        var avroUserSerde = new SpecificAvroSerde<User>();
        var serdeConfig = Map.of(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, MOCK_SCHEMA_REGISTRY_URL);
        avroUserSerde.configure(serdeConfig, false);

        Map<String, Object> consumerProps =
                KafkaTestUtils.consumerProps(
                        UUID.randomUUID().toString(), "true", this.embeddedKafkaBroker);

        DefaultKafkaConsumerFactory<String, User> kafkaConsumerFactory =
                new DefaultKafkaConsumerFactory<>(
                        consumerProps, new StringDeserializer(), avroUserSerde.deserializer(), false);

        var consumer = kafkaConsumerFactory.createConsumer();
        this.embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumer, topic);
        return consumer;
    }

    private Producer<String, User> createProducer() {
        var avroUserSerde = new SpecificAvroSerde<User>();
        var serdeConfig = Map.of(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, MOCK_SCHEMA_REGISTRY_URL);
        avroUserSerde.configure(serdeConfig, false);

        var producerProps = KafkaTestUtils.producerProps(this.embeddedKafkaBroker);

        var kafkaProducerFactory =
                new DefaultKafkaProducerFactory<>(
                        producerProps, new StringSerializer(), avroUserSerde.serializer(), false);

        return kafkaProducerFactory.createProducer();
    }

    @Test
    void test_kafka_stream() throws Exception {
        var user = new User("John", 33, "blue");

        var producer = createProducer();
        var consumer = createConsumer(OUTPUT_TOPIC);

        // when
        producer.send(new ProducerRecord<>(INPUT_TOPIC, "234", user)).get();

        // then
        var records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(60));
        assertThat(records).isNotEmpty();
        assertThat(records.iterator().next().value()).isEqualTo(user);

        this.mockMvc.perform(get("/state/keyvalue/" + USER_TABLE + "/234")).andDo(print()).andExpect(status().isOk())
                .andExpect(content().string(equalTo("{\"key\":\"234\",\"value\":{\"name\":\"John\",\"favorite_number\":33,\"favorite_color\":\"blue\"}}")));
    }

}