# Secrets

Kubernetes Secrets for sensitive configuration.

**Note:** Secrets should be managed using:

- Sealed Secrets
- External Secrets Operator
- Vault
- Cloud provider secret management

Do not commit actual secrets to version control.

## Template Files

- `database-secrets.yaml.template` - Database credentials template
- `d365-secrets.yaml.template` - D365 integration secrets template

