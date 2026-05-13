package com.gatewayobs.detection.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "detection")
public record DetectionKafkaTopics(String enrichedTopic, String incidentsTopic, long latencyThresholdMs) {}
