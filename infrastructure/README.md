# Infrastructure as Code

Infrastructure configurations for the Warehouse Management System.

## Structure

```
infrastructure/
├── docker/                    # Docker Compose for local development
│   └── docker-compose.dev.yml
├── kubernetes/               # Kubernetes manifests
│   ├── namespace.yaml
│   ├── configmaps/
│   ├── secrets/
│   ├── deployments/
│   ├── services/
│   ├── ingress/
│   └── monitoring/
└── terraform/                # Terraform IaC
    ├── main.tf
    ├── variables.tf
    ├── outputs.tf
    ├── environments/
    └── modules/
```

## Docker

Local development environment using Docker Compose.

**Usage:**

```bash
cd docker
docker-compose -f docker-compose.dev.yml up -d
```

## Kubernetes

Kubernetes manifests for deploying to a Kubernetes cluster.

**Prerequisites:**

- Kubernetes cluster 1.24+
- kubectl configured

**Deployment:**

```bash
kubectl apply -f kubernetes/
```

## Terraform

Infrastructure as Code for provisioning cloud resources.

**Prerequisites:**

- Terraform 1.5+
- Cloud provider credentials

**Usage:**

```bash
cd terraform/environments/dev
terraform init
terraform plan
terraform apply
```

## Cloud Providers

Supported cloud providers:

- AWS (EKS, RDS, MSK)
- Azure (AKS, Azure Database, Event Hubs)
- GCP (GKE, Cloud SQL, Pub/Sub)

