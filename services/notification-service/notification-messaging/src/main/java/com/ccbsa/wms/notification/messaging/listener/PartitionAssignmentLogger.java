package com.ccbsa.wms.notification.messaging.listener;

import java.util.Collection;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.TopicPartition;
import org.springframework.kafka.listener.ConsumerAwareRebalanceListener;

import lombok.extern.slf4j.Slf4j;

/**
 * Rebalance Listener: PartitionAssignmentLogger
 * <p>
 * Logs partition assignments and revocations for Kafka consumers to aid in debugging partition assignment issues.
 * <p>
 * This listener helps diagnose why certain listeners may not be receiving events due to partition assignment.
 */
@Slf4j
public class PartitionAssignmentLogger implements ConsumerAwareRebalanceListener {

    @Override
    public void onPartitionsAssigned(Consumer<?, ?> consumer, Collection<TopicPartition> partitions) {
        if (!partitions.isEmpty()) {
            log.info("Partitions assigned to consumer {}: {} partitions - {}", consumer.groupMetadata().groupId(), partitions.size(), partitions);
        } else {
            log.warn("No partitions assigned to consumer {} - this consumer will not receive any events", consumer.groupMetadata().groupId());
        }
    }

    @Override
    public void onPartitionsLost(Consumer<?, ?> consumer, Collection<TopicPartition> partitions) {
        if (!partitions.isEmpty()) {
            log.warn("Partitions lost by consumer {}: {} partitions - {}", consumer.groupMetadata().groupId(), partitions.size(), partitions);
        }
    }
}

