# Ingress

Kubernetes Ingress configurations for external access.

## Ingress Controller

- NGINX Ingress Controller (recommended)
- Traefik
- Istio Gateway

## Configuration

- `api-ingress.yaml` - API Gateway ingress
- `frontend-ingress.yaml` - Frontend PWA ingress

## TLS

TLS certificates should be managed using:

- cert-manager with Let's Encrypt
- Cloud provider certificate management

