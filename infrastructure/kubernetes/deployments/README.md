# Deployments

Kubernetes Deployment manifests for each service.

## Service Deployments

- `stock-management-deployment.yaml`
- `location-management-deployment.yaml`
- `product-deployment.yaml`
- `picking-deployment.yaml`
- `returns-deployment.yaml`
- `reconciliation-deployment.yaml`
- `integration-deployment.yaml`

Each deployment includes:

- Container image configuration
- Resource limits and requests
- Health checks (liveness and readiness probes)
- Environment variables
- Volume mounts
- Replica configuration

