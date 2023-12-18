package com.fdelsert.springbootkafkastreamsinteractivequeriesexample.services;

import java.util.Set;

public record HostStoreInfo(String host, int port, Set<String> storeNames) {}
