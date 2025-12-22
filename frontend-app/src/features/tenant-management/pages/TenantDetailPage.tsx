import { Button, Stack } from '@mui/material';
import { useNavigate, useParams } from 'react-router-dom';
import { useTenant } from '../hooks/useTenant';
import { TenantDetail } from '../components/TenantDetail';
import { TenantActions } from '../components/TenantActions';
import { DetailPageLayout } from '../../../components/layouts';
import { getBreadcrumbs, Routes } from '../../../utils/navigationUtils';

export const TenantDetailPage = () => {
  const { tenantId } = useParams<{ tenantId: string }>();
  const navigate = useNavigate();
  const { tenant, isLoading, error, refetch } = useTenant(tenantId);

  if (!tenantId) {
    navigate(Routes.admin.tenants);
    return null;
  }

  return (
    <DetailPageLayout
      breadcrumbs={getBreadcrumbs.tenantDetail(tenant?.name || tenantId || '...')}
      title={tenant?.name || 'Loading...'}
      actions={
        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1}>
          {tenant && (
            <TenantActions
              tenantId={tenant.tenantId}
              status={tenant.status}
              onCompleted={refetch}
            />
          )}
          <Button variant="outlined" onClick={() => navigate(Routes.admin.tenants)}>
            Back to List
          </Button>
        </Stack>
      }
      isLoading={isLoading}
      error={error}
      maxWidth="lg"
    >
      {tenant && (
        <Stack spacing={3}>
          <TenantDetail tenant={tenant} />
        </Stack>
      )}
    </DetailPageLayout>
  );
};
