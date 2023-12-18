package com.fdelsert.springbootkafkastreamsinteractivequeriesexample.actuator;

import java.util.Optional;

import org.apache.kafka.streams.KafkaStreams;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.stereotype.Component;

@Component
public class KafkaStreamHealthIndicator implements HealthIndicator {

    private final StreamsBuilderFactoryBean factoryBean;

    public KafkaStreamHealthIndicator(StreamsBuilderFactoryBean factoryBean) {
        this.factoryBean = factoryBean;
    }

    @Override
    public Health health() {
        KafkaStreams.State kafkaStreamsState =
                Optional.ofNullable(factoryBean.getKafkaStreams())
                        .map(KafkaStreams::state)
                        .orElse(KafkaStreams.State.NOT_RUNNING);

        // CREATED, RUNNING or REBALANCING
        if (kafkaStreamsState == KafkaStreams.State.CREATED
                || kafkaStreamsState.isRunningOrRebalancing()) {
            return Health.up().build();
        }

        // ERROR, NOT_RUNNING, PENDING_SHUTDOWN,
        return Health.down().withDetail("state", kafkaStreamsState.name()).build();
    }
}
