output "namespace" {
  description = "Kubernetes namespace name"
  value       = kubernetes_namespace.wms_system.metadata[0].name
}

