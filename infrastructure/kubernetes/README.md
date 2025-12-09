# Kubernetes Infrastructure

Kubernetes manifests for deploying the Warehouse Management System to a Kubernetes cluster.

## Structure

```
kubernetes/
├── namespace.yaml              # Namespace definition
├── configmaps/                # Configuration maps
├── secrets/                   # Secrets (encrypted)
├── deployments/               # Deployment manifests
├── services/                  # Service manifests
├── ingress/                   # Ingress configurations
└── monitoring/                # Monitoring configurations
```

## Prerequisites

- Kubernetes cluster (1.24+)
- kubectl configured
- Helm (optional, for advanced deployments)

## Deployment

```bash
# Apply namespace
kubectl apply -f namespace.yaml

# Apply configurations
kubectl apply -f configmaps/
kubectl apply -f secrets/

# Deploy services
kubectl apply -f deployments/
kubectl apply -f services/

# Apply ingress
kubectl apply -f ingress/
```

## Services

- stock-management-service
- location-management-service
- product-service
- picking-service
- returns-service
- reconciliation-service
- integration-service

