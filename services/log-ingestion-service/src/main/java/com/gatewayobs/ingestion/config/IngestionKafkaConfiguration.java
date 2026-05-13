package com.gatewayobs.ingestion.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@Configuration
@EnableKafka
@EnableConfigurationProperties(IngestionProperties.class)
class IngestionKafkaConfiguration {}
