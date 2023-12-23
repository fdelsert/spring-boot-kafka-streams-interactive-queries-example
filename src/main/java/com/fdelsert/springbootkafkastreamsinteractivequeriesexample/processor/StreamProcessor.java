package com.fdelsert.springbootkafkastreamsinteractivequeriesexample.processor;

import example.avro.User;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Materialized;
import org.springframework.stereotype.Component;

@Component
public class StreamProcessor {

    static final String INPUT_TOPIC = "input-topic";
    static final String OUTPUT_TOPIC = "output-topic";
    static final String USER_TABLE = "user-table";

    public StreamProcessor(StreamsBuilder streamsBuilder) {
        buildStream(streamsBuilder);
    }

    static void buildStream(StreamsBuilder streamsBuilder) {
        var inputStream = streamsBuilder.<String, User>stream(INPUT_TOPIC);
        // only to validate Avro SerDe
        inputStream.to(OUTPUT_TOPIC);
        // the queryable state
        inputStream.toTable(Materialized.as(USER_TABLE));
    }
}
