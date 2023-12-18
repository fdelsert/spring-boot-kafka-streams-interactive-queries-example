package com.fdelsert.springbootkafkastreamsinteractivequeriesexample.controller;

import com.fasterxml.jackson.databind.JsonNode;

public record KeyValueRecord(String key, JsonNode value) {}
