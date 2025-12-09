# Environment Configurations

Environment-specific Terraform configurations.

## Environments

- **dev** - Development environment
- **staging** - Staging environment
- **production** - Production environment

Each environment directory contains:

- `terraform.tfvars` - Environment-specific variables
- `backend.tf` - Backend state configuration
- `provider.tf` - Provider configuration

