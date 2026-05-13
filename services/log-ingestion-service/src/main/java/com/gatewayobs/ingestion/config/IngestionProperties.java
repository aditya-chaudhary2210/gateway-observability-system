package com.gatewayobs.ingestion.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ingestion")
public record IngestionProperties(String gatewayTopic, String enrichedTopic, String esIndex) {}
