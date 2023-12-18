package com.fdelsert.springbootkafkastreamsinteractivequeriesexample.controller;

import static org.apache.kafka.streams.StoreQueryParameters.fromNameAndType;
import static org.springframework.http.HttpStatus.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.avro.AvroFactory;
import com.fasterxml.jackson.dataformat.avro.AvroSchema;
import com.fdelsert.springbootkafkastreamsinteractivequeriesexample.services.HostStoreInfo;
import com.fdelsert.springbootkafkastreamsinteractivequeriesexample.services.MetadataService;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.state.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@org.springframework.web.bind.annotation.RestController
@RequestMapping("state")
public class RestController {

    private final StreamsBuilderFactoryBean streamsBuilderFactoryBean;
    private final MetadataService metadataService;
    private final HostInfo hostInfo;
    private final RestClient restClient = RestClient.create();
    private final ObjectMapper mapper = new ObjectMapper(new AvroFactory());

    public RestController(StreamsBuilderFactoryBean streamsBuilderFactoryBean, MetadataService metadataService, @Value("${spring.kafka.streams.properties.application.server}") String applicationServer) {
        this.streamsBuilderFactoryBean = streamsBuilderFactoryBean;
        this.metadataService = metadataService;
        hostInfo = HostInfo.buildFromEndpoint(applicationServer);
    }

    public JsonNode convertAvroToJsonNode(GenericRecord genericRecord) {
        try(ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            DatumWriter<GenericRecord> writer = new GenericDatumWriter<>(genericRecord.getSchema());
            BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(outputStream, null);
            writer.write(genericRecord, encoder);
            encoder.flush();

            byte[] bytes = outputStream.toByteArray();

            return mapper.readerFor(ObjectNode.class)
                    .with(new AvroSchema(genericRecord.getSchema()))
                    .readValue(bytes);
        } catch (IOException e) {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Avro to Json conversion error");
        }
    }

    /**
     * Get a key-value pair from a KeyValue Store
     *
     * @param storeName the store to look in
     * @param key       the key to get
     * @return {@link KeyValueRecord} representing the key-value pair
     */
    @GetMapping("/keyvalue/{storeName}/{key}")
    public KeyValueRecord byKey(
            @PathVariable("storeName") final String storeName, @PathVariable("key") final String key) {

        final HostStoreInfo hostStoreInfo = streamsMetadataForStoreAndKey(storeName, key);

        if (!thisHost(hostStoreInfo)) {
            return fetchByKey(hostStoreInfo, "state/keyvalue/" + storeName + "/" + key);
        }

        // Lookup the KeyValueStore with the provided storeName
        final ReadOnlyKeyValueStore<String, GenericRecord> store =
                getKafkaStreams()
                        .store(fromNameAndType(storeName, QueryableStoreTypes.keyValueStore()));
        if (store == null) {
            throw new ResponseStatusException(NOT_FOUND, "Store not found.");
        }

        // Get the value from the store
        var value = store.get(key);

        if (value == null) {
            throw new ResponseStatusException(NOT_FOUND, "Key not found.");
        }
        return new KeyValueRecord(key, convertAvroToJsonNode(value));
    }

    private KeyValueRecord fetchByKey(final HostStoreInfo host, final String path) {
        return restClient
                .get()
                .uri(String.format("http://%s:%d/%s", host.host(), host.port(), path))
                .retrieve()
                .body(KeyValueRecord.class);
    }

    /**
     * Get all of the key-value pairs available in a store
     *
     * @param storeName store to query
     * @return A List of {@link KeyValueRecord}s representing all of the key-values in the provided
     * store
     */
    @GetMapping("/keyvalues/{storeName}/all")
    public List<KeyValueRecord> allForStore(@PathVariable("storeName") final String storeName) {
        return rangeForKeyValueStore(storeName, ReadOnlyKeyValueStore::all);
    }

    /**
     * Get all of the key-value pairs that have keys within the range from...to
     *
     * @param storeName store to query
     * @param from      start of the range (inclusive)
     * @param to        end of the range (inclusive)
     * @return A List of {@link KeyValueRecord}s representing all of the key-values in the provided
     * store that fall withing the given range.
     */
    @GetMapping("/keyvalues/{storeName}/range/{from}/{to}")
    public List<KeyValueRecord> keyRangeForStore(
            @PathVariable("storeName") final String storeName,
            @PathVariable("from") final String from,
            @PathVariable("to") final String to) {
        return rangeForKeyValueStore(storeName, store -> store.range(from, to));
    }

    /**
     * Get the metadata for all of the instances of this Kafka Streams application
     *
     * @return List of {@link HostStoreInfo}
     */
    @GetMapping("/instances")
    public List<HostStoreInfo> streamsMetadata() {
        return metadataService.streamsMetadata()
                .orElseThrow(() -> new ResponseStatusException(SERVICE_UNAVAILABLE, "StreamsBuilderFactoryBean hasn't been started."));
    }

    /**
     * Get the metadata for all instances of this Kafka Streams application that currently has the
     * provided store.
     *
     * @param store The store to locate
     * @return List of {@link HostStoreInfo}
     */
    @GetMapping("/instances/{storeName}")
    public List<HostStoreInfo> streamsMetadataForStore(
            @PathVariable("storeName") final String store) {
        return metadataService.streamsMetadataForStore(store)
                .orElseThrow(() -> new ResponseStatusException(SERVICE_UNAVAILABLE, "StreamsBuilderFactoryBean hasn't been started."));
    }

    /**
     * Find the metadata for the instance of this Kafka Streams Application that has the given store
     * and would have the given key if it exists.
     *
     * @param store Store to find
     * @param key   The key to find
     * @return {@link HostStoreInfo}
     */
    @GetMapping("/instance/{storeName}/{key}")
    public HostStoreInfo streamsMetadataForStoreAndKey(
            @PathVariable("storeName") final String store, @PathVariable("key") final String key) {
        return metadataService.streamsMetadataForStoreAndKey(store, key, new StringSerializer())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Host store not found."));
    }

    /**
     * Performs a range query on a KeyValue Store and converts the results into a List of {@link
     * KeyValueRecord}
     *
     * @param storeName     The store to query
     * @param rangeFunction The range query to run, i.e., all, from(start, end)
     * @return List of {@link KeyValueRecord}
     */
    private List<KeyValueRecord> rangeForKeyValueStore(
            final String storeName,
            final Function<
                    ReadOnlyKeyValueStore<String, GenericRecord>,
                    KeyValueIterator<String, GenericRecord>>
                    rangeFunction) {

        // Get the KeyValue Store
        final ReadOnlyKeyValueStore<String, GenericRecord> store =
                getKafkaStreams()
                        .store(fromNameAndType(storeName, QueryableStoreTypes.keyValueStore()));
        final List<KeyValueRecord> results = new ArrayList<>();
        // Apply the function, i.e., query the store
        try(KeyValueIterator<String, GenericRecord> range = rangeFunction.apply(store)) {
            // Convert the results
            while (range.hasNext()) {
                final KeyValue<String, GenericRecord> next = range.next();
                results.add(new KeyValueRecord(next.key, convertAvroToJsonNode(next.value)));
            }
        }

        return results;
    }

    private KafkaStreams getKafkaStreams() {
        return Optional.ofNullable(streamsBuilderFactoryBean.getKafkaStreams())
                .orElseThrow(() -> new ResponseStatusException(SERVICE_UNAVAILABLE, "StreamsBuilderFactoryBean hasn't been started."));
    }

    private boolean thisHost(final HostStoreInfo host) {
        return host.host().equals(hostInfo.host()) && host.port() == hostInfo.port();
    }
}
