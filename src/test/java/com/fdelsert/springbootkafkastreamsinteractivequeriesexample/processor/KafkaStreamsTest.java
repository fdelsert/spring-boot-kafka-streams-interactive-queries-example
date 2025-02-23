package com.fdelsert.springbootkafkastreamsinteractivequeriesexample.processor;

import example.avro.User;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.streams.serdes.avro.GenericAvroSerde;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.TopologyTestDriver;
import org.apache.kafka.streams.test.TestRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.env.Environment;

import java.util.Map;
import java.util.Properties;

import static com.fdelsert.springbootkafkastreamsinteractivequeriesexample.config.TopicConfig.INPUT_TOPIC;
import static com.fdelsert.springbootkafkastreamsinteractivequeriesexample.config.TopicConfig.OUTPUT_TOPIC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class KafkaStreamsTest {

    private static final String MOCK_SCHEMA_REGISTRY_URL = "mock://schema-registry";
    private TopologyTestDriver testDriver;

    @BeforeEach
    public void setup() {
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "test");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:1234");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.StringSerde.class);
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, GenericAvroSerde.class);
        props.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, MOCK_SCHEMA_REGISTRY_URL);

        var env = Mockito.mock(Environment.class);
        when(env.getActiveProfiles()).thenReturn(new String[]{});

        StreamsBuilder streamsBuilder = new StreamsBuilder();
        StreamProcessor.buildStream(streamsBuilder);
        Topology topology = streamsBuilder.build();

        testDriver = new TopologyTestDriver(topology, props);
    }

    @AfterEach
    public void tearDown() {
        testDriver.close();
    }

    @Test
    void test_topology() {
        // given
        var avroUserSerde = new SpecificAvroSerde<User>();
        Map<String, String> config = Map.of(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, MOCK_SCHEMA_REGISTRY_URL);
        avroUserSerde.configure(config, false);
        var user = new User("John", 33, "blue");
        var record = new TestRecord<>("234", user);

        // when
        var inputTopic = testDriver.createInputTopic(INPUT_TOPIC, new StringSerializer(), avroUserSerde.serializer());
        var outputTopic = testDriver.createOutputTopic(OUTPUT_TOPIC, new StringDeserializer(), avroUserSerde.deserializer());
        inputTopic.pipeInput(record);

        // then
        TestRecord<String, User> outputRecord = outputTopic.readRecord();
        assertThat(outputRecord.getKey()).isEqualTo("234");
        assertThat(outputRecord.getValue()).isEqualTo(user);
    }

}