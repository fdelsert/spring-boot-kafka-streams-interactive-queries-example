package com.fdelsert.springbootkafkastreamsinteractivequeriesexample.processor;

import example.avro.User;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Materialized;
import org.springframework.stereotype.Component;

@Component
public class StreamProcessor {

    public StreamProcessor(StreamsBuilder streamsBuilder) {
        buildStream(streamsBuilder);
    }

    static void buildStream(StreamsBuilder streamsBuilder) {
        streamsBuilder.<String, User>stream("input-topic")
                .toTable(Materialized.as("user-table"))
                .toStream()
                .to("output-topic");
    }
}
