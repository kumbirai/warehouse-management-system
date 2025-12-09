# Terraform Infrastructure

Infrastructure as Code (IaC) for provisioning cloud resources.

## Structure

```
terraform/
├── environments/              # Environment-specific configurations
│   ├── dev/
│   ├── staging/
│   └── production/
├── modules/                   # Reusable Terraform modules
│   ├── kubernetes/
│   ├── database/
│   ├── messaging/
│   └── networking/
└── main.tf                    # Root module
```

## Prerequisites

- Terraform 1.5+
- Cloud provider credentials configured
- Backend state storage configured (S3, GCS, Azure Storage)

## Usage

```bash
# Initialize Terraform
terraform init

# Plan changes
terraform plan

# Apply changes
terraform apply

# Destroy infrastructure
terraform destroy
```

## Supported Cloud Providers

- AWS
- Azure
- GCP

## Modules

- **Kubernetes Cluster** - EKS, AKS, GKE
- **Database** - RDS, Azure Database, Cloud SQL
- **Messaging** - MSK, Event Hubs, Pub/Sub
- **Networking** - VPC, Load Balancers, DNS

