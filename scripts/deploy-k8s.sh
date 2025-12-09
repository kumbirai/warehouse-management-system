#!/bin/bash

set -e

NAMESPACE="wms-system"
K8S_DIR="infrastructure/kubernetes"

echo "Deploying to Kubernetes..."

# Check if kubectl is available
if ! command -v kubectl &> /dev/null; then
    echo "Error: kubectl is not installed or not in PATH"
    exit 1
fi

# Create namespace
echo "Creating namespace..."
kubectl apply -f ${K8S_DIR}/namespace.yaml

# Apply ConfigMaps
echo "Applying ConfigMaps..."
kubectl apply -f ${K8S_DIR}/configmaps/

# Apply Secrets (user must create these first)
echo "Warning: Ensure secrets are created before proceeding"
read -p "Have you created the secrets? (y/n) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Please create secrets first:"
    echo "  kubectl create secret generic wms-database-secrets --from-literal=POSTGRES_USER=postgres --from-literal=POSTGRES_PASSWORD=your-password -n ${NAMESPACE}"
    echo "  kubectl create secret generic wms-d365-secrets --from-literal=D365_CLIENT_ID=xxx --from-literal=D365_CLIENT_SECRET=xxx -n ${NAMESPACE}"
    exit 1
fi

# Apply Deployments
echo "Applying Deployments..."
kubectl apply -f ${K8S_DIR}/deployments/

# Check for missing deployments
echo ""
echo "Note: The following services exist in the project but may not have K8s deployments yet:"
echo "  - gateway-service"
echo "  - user-service"
echo "  - tenant-service"
echo "  - notification-service"
echo "  If these are needed, ensure their deployment manifests exist in ${K8S_DIR}/deployments/"

# Apply Services
echo ""
echo "Applying Services..."
kubectl apply -f ${K8S_DIR}/services/

# Apply Ingress
echo "Applying Ingress..."
kubectl apply -f ${K8S_DIR}/ingress/

echo "Deployment complete!"
echo ""
echo "Check status with:"
echo "  kubectl get pods -n ${NAMESPACE}"
echo "  kubectl get services -n ${NAMESPACE}"
echo "  kubectl get ingress -n ${NAMESPACE}"

