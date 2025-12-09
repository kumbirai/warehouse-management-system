output "kubernetes_cluster_endpoint" {
  description = "Kubernetes cluster endpoint"
  value       = ""
}

output "database_endpoint" {
  description = "Database endpoint"
  value       = ""
  sensitive   = true
}

output "kafka_bootstrap_servers" {
  description = "Kafka bootstrap servers"
  value       = ""
  sensitive   = true
}

