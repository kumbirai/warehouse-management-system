output "kafka_bootstrap_brokers" {
  description = "Kafka bootstrap brokers"
  value       = aws_msk_cluster.wms_kafka.bootstrap_brokers
  sensitive   = true
}

output "kafka_bootstrap_brokers_tls" {
  description = "Kafka bootstrap brokers TLS"
  value       = aws_msk_cluster.wms_kafka.bootstrap_brokers_tls
  sensitive   = true
}

output "kafka_zookeeper_connect_string" {
  description = "Zookeeper connect string"
  value       = aws_msk_cluster.wms_kafka.zookeeper_connect_string
  sensitive   = true
}

output "kafka_security_group_id" {
  description = "Kafka security group ID"
  value       = aws_security_group.wms_kafka_sg.id
}

