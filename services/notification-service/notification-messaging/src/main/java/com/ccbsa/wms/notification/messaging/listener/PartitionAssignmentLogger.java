package com.ccbsa.wms.notification.messaging.listener;

import java.util.Collection;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.listener.ConsumerAwareRebalanceListener;

/**
 * Rebalance Listener: PartitionAssignmentLogger
 * <p>
 * Logs partition assignments and revocations for Kafka consumers to aid in debugging partition assignment issues.
 * <p>
 * This listener helps diagnose why certain listeners may not be receiving events due to partition assignment.
 */
public class PartitionAssignmentLogger implements ConsumerAwareRebalanceListener {
    private static final Logger logger = LoggerFactory.getLogger(PartitionAssignmentLogger.class);

    @Override
    public void onPartitionsAssigned(Consumer<?, ?> consumer, Collection<TopicPartition> partitions) {
        if (!partitions.isEmpty()) {
            logger.info("Partitions assigned to consumer {}: {} partitions - {}", consumer.groupMetadata().groupId(), partitions.size(), partitions);
        } else {
            logger.warn("No partitions assigned to consumer {} - this consumer will not receive any events", consumer.groupMetadata().groupId());
        }
    }

    @Override
    public void onPartitionsLost(Consumer<?, ?> consumer, Collection<TopicPartition> partitions) {
        if (!partitions.isEmpty()) {
            logger.warn("Partitions lost by consumer {}: {} partitions - {}", consumer.groupMetadata().groupId(), partitions.size(), partitions);
        }
    }
}

