import { Snackbar } from '@mui/material';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { TenantForm, TenantFormValues } from '../components/TenantForm';
import { useCreateTenant } from '../hooks/useCreateTenant';
import { CreateTenantRequest } from '../types/tenant';
import { FormPageLayout } from '../../../components/layouts';
import { getBreadcrumbs, Routes } from '../../../utils/navigationUtils';

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
      <FormPageLayout
        breadcrumbs={getBreadcrumbs.tenantCreate()}
        title="Create Tenant"
        description="Provide tenant onboarding information. Validation is applied automatically."
        error={error}
        maxWidth="md"
      >
        <TenantForm
          onSubmit={handleSubmit}
          onCancel={() => navigate(Routes.admin.tenants)}
          isSubmitting={isSubmitting}
        />
      </FormPageLayout>

      <Snackbar
        open={!!snackbarMessage}
        autoHideDuration={4000}
        onClose={() => setSnackbarMessage(null)}
        message={snackbarMessage}
      />
    </>
  );
};
