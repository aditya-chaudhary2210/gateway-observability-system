/*
 * Ownership: QA / Platform
 * Test harness application joining ingestion + detection bounded contexts inside a single Spring context so Kafka
 * hand-offs can be verified with Testcontainers.
 */
package com.gatewayobs.it.pipeline;

import com.gatewayobs.detection.IncidentDetectionApplication;
import com.gatewayobs.ingestion.LogIngestionApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@SpringBootApplication
@ComponentScan(
        basePackages = {"com.gatewayobs.ingestion", "com.gatewayobs.detection"},
        excludeFilters = {
            @ComponentScan.Filter(
                    type = FilterType.ASSIGNABLE_TYPE,
                    classes = {LogIngestionApplication.class, IncidentDetectionApplication.class})
        })
public class PipelineIntegrationHarnessApplication {}
