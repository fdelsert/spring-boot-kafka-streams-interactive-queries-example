package com.fdelsert.springbootkafkastreamsinteractivequeriesexample.services;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsMetadata;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.stereotype.Service;

/**
 * Looks up StreamsMetadata from KafkaStreams and converts the results into Beans that can be JSON
 * serialized.
 */
@Service
public class MetadataService {

    private final StreamsBuilderFactoryBean streamsBuilderFactoryBean;

    public MetadataService(StreamsBuilderFactoryBean streamsBuilderFactoryBean) {
        this.streamsBuilderFactoryBean = streamsBuilderFactoryBean;
    }

    /**
     * Get the metadata for all of the instances of this Kafka Streams application
     *
     * @return Optional list of {@link HostStoreInfo}
     */
    public Optional<List<HostStoreInfo>> streamsMetadata() {
        // Get metadata for all of the instances of this Kafka Streams application
        return Optional.ofNullable(streamsBuilderFactoryBean.getKafkaStreams())
                .map(KafkaStreams::metadataForAllStreamsClients)
                .map(this::mapInstancesToHostStoreInfo);
    }

    /**
     * Get the metadata for all instances of this Kafka Streams application that currently has the
     * provided store.
     *
     * @param store The store to locate
     * @return Optional list of {@link HostStoreInfo}
     */
    public Optional<List<HostStoreInfo>> streamsMetadataForStore(final String store) {
        // Get metadata for all of the instances of this Kafka Streams application hosting the store
        return Optional.ofNullable(streamsBuilderFactoryBean.getKafkaStreams())
                .map(ks -> ks.streamsMetadataForStore(store))
                .map(this::mapInstancesToHostStoreInfo);
    }

    /**
     * Find the metadata for the instance of this Kafka Streams Application that has the given store
     * and would have the given key if it exists.
     *
     * @param store Store to find
     * @param key   The key to find
     * @return {@link HostStoreInfo}
     */
    public <K> Optional<HostStoreInfo> streamsMetadataForStoreAndKey(
            final String store, final K key, final Serializer<K> serializer) {
        // Get metadata for the instances of this Kafka Streams application hosting the store and
        // potentially the value for key
        return Optional.ofNullable(streamsBuilderFactoryBean.getKafkaStreams())
                .map(ks -> ks.queryMetadataForKey(store, key, serializer))
                .map(metadata -> new HostStoreInfo(
                        metadata.activeHost().host(),
                        metadata.activeHost().port(),
                        Collections.singleton(store)));
    }

    private List<HostStoreInfo> mapInstancesToHostStoreInfo(
            final Collection<StreamsMetadata> metadatas) {
        return metadatas.stream()
                .map(metadata -> new HostStoreInfo(metadata.host(), metadata.port(), metadata.stateStoreNames()))
                .toList();
    }
}
