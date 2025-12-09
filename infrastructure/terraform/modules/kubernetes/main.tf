terraform {
  required_providers {
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.23"
    }
  }
}

variable "cluster_name" {
  description = "Name of the Kubernetes cluster"
  type        = string
}

variable "cluster_endpoint" {
  description = "Kubernetes cluster endpoint"
  type        = string
}

variable "cluster_ca_certificate" {
  description = "Base64 encoded CA certificate"
  type        = string
  sensitive   = true
}

variable "cluster_token" {
  description = "Kubernetes cluster token"
  type        = string
  sensitive   = true
}

provider "kubernetes" {
  host                   = var.cluster_endpoint
  cluster_ca_certificate = base64decode(var.cluster_ca_certificate)
  token                  = var.cluster_token
}

resource "kubernetes_namespace" "wms_system" {
  metadata {
    name = "wms-system"
    labels = {
      name        = "wms-system"
      environment = var.environment
    }
  }
}

output "namespace" {
  value = kubernetes_namespace.wms_system.metadata[0].name
}

