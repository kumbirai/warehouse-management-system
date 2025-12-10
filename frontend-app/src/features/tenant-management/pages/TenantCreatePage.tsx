import { Alert, Breadcrumbs, Container, Link, Snackbar, Stack, Typography } from '@mui/material';
import { useState } from 'react';
import { Link as RouterLink, useNavigate } from 'react-router-dom';
import { Header } from '../../../components/layout/Header';
import { TenantForm, TenantFormValues } from '../components/TenantForm';
import { useCreateTenant } from '../hooks/useCreateTenant';
import { CreateTenantRequest } from '../types/tenant';

export const TenantCreatePage = () => {
  const navigate = useNavigate();
  const { createTenant, isSubmitting, error } = useCreateTenant();
  const [snackbarMessage, setSnackbarMessage] = useState<string | null>(null);

  const handleSubmit = async (values: TenantFormValues) => {
    const payload: CreateTenantRequest = {
      tenantId: values.tenantId,
      name: values.name,
      emailAddress: values.emailAddress || undefined,
      phone: values.phone || undefined,
      address: values.address || undefined,
      keycloakRealmName: values.keycloakRealmName || undefined,
      usePerTenantRealm: values.usePerTenantRealm,
    };
    try {
      const result = await createTenant(payload);
      setSnackbarMessage('Tenant created successfully');
      const tenantId = result?.tenantId ?? values.tenantId;
      setTimeout(() => navigate(`/admin/tenants/${tenantId}`), 1500);
    } catch {
      // Error is handled by hook state
    }
  };

  return (
    <>
      <Header />
      <Container maxWidth="md" sx={{ py: 4 }}>
        <Breadcrumbs sx={{ mb: 2 }}>
          <Link component={RouterLink} to="/dashboard">
            Dashboard
          </Link>
          <Link component={RouterLink} to="/admin/tenants">
            Tenants
          </Link>
          <Typography color="text.primary">Create Tenant</Typography>
        </Breadcrumbs>
        <Stack spacing={2} mb={3}>
          <Typography variant="h4">Create Tenant</Typography>
          <Typography variant="body1" color="text.secondary">
            Provide tenant onboarding information. Validation is applied automatically.
          </Typography>
        </Stack>

        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}

        <TenantForm
          onSubmit={handleSubmit}
          onCancel={() => navigate('/admin/tenants')}
          isSubmitting={isSubmitting}
        />
      </Container>

      <Snackbar
        open={!!snackbarMessage}
        autoHideDuration={4000}
        onClose={() => setSnackbarMessage(null)}
        message={snackbarMessage}
      />
    </>
  );
};
