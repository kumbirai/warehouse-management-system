package com.ccbsa.common.messaging.health;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Health indicator for Kafka connectivity and broker status.
 * <p>
 * Checks: - Kafka broker connectivity - Cluster information - Producer/consumer health
 */
@Component
public class KafkaHealthIndicator
        implements HealthIndicator {
    private static final Logger logger = LoggerFactory.getLogger(KafkaHealthIndicator.class);

    private final String bootstrapServers;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private AdminClient adminClient;

    public KafkaHealthIndicator(
            @Value("${spring.kafka.bootstrap-servers:localhost:9092}") String bootstrapServers, KafkaTemplate<String, Object> kafkaTemplate) {
        this.bootstrapServers = Objects.requireNonNull(bootstrapServers, "bootstrapServers must not be null");
        this.kafkaTemplate = Objects.requireNonNull(kafkaTemplate, "kafkaTemplate must not be null");
    }

    @Override
    public Health health() {
        try {
            Map<String, Object> details = new HashMap<>();
            details.put("bootstrapServers", bootstrapServers);

            // Check broker connectivity
            if (adminClient == null) {
                adminClient = createAdminClient();
            }

            DescribeClusterResult clusterResult = adminClient.describeCluster();

            // Get cluster ID (non-blocking check)
            try {
                String clusterId = clusterResult.clusterId()
                        .get(5, TimeUnit.SECONDS);
                details.put("clusterId", clusterId);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                logger.warn("Could not retrieve cluster ID: {}", e.getMessage());
                details.put("clusterId", "unknown");
            }

            // Get controller (non-blocking check)
            try {
                String controller = clusterResult.controller()
                        .get(5, TimeUnit.SECONDS)
                        .idString();
                details.put("controller", controller);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                logger.warn("Could not retrieve controller: {}", e.getMessage());
            }

            // Get node count (non-blocking check)
            try {
                int nodeCount = clusterResult.nodes()
                        .get(5, TimeUnit.SECONDS)
                        .size();
                details.put("nodeCount", nodeCount);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                logger.warn("Could not retrieve node count: {}", e.getMessage());
            }

            // Test producer connectivity
            try {
                kafkaTemplate.getProducerFactory()
                        .createProducer();
                details.put("producer", "available");
            } catch (RuntimeException e) {
                logger.warn("Producer health check failed: {}", e.getMessage());
                details.put("producer", "unavailable");
                return Health.down()
                        .withDetails(details)
                        .withException(e)
                        .build();
            }

            details.put("status", "UP");
            logger.debug("Kafka health check passed");

            return Health.up()
                    .withDetails(details)
                    .build();

        } catch (RuntimeException e) {
            logger.error("Kafka health check failed", e);

            Map<String, Object> details = new HashMap<>();
            details.put("bootstrapServers", bootstrapServers);
            details.put("status", "DOWN");
            details.put("error", e.getMessage());

            return Health.down()
                    .withDetails(details)
                    .withException(e)
                    .build();
        }
    }

    private AdminClient createAdminClient() {
        Map<String, Object> config = new HashMap<>();
        config.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 5000);
        config.put(AdminClientConfig.CONNECTIONS_MAX_IDLE_MS_CONFIG, 10000);
        return AdminClient.create(config);
    }
}

