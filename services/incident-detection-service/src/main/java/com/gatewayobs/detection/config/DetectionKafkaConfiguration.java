package com.gatewayobs.detection.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;

@Configuration
@EnableKafka
@EnableConfigurationProperties(DetectionKafkaTopics.class)
class DetectionKafkaConfiguration {}
