import { Button } from '@mui/material';
import { useNavigate, useParams } from 'react-router-dom';

import { DetailPageLayout } from '../../../components/layouts';
import { getBreadcrumbs, Routes } from '../../../utils/navigationUtils';
import { WarehouseDetail } from '../components/WarehouseDetail';
import { useWarehouse } from '../hooks/useWarehouse';
import { useAuth } from '../../../hooks/useAuth';

export const WarehouseDetailPage = () => {
  const { warehouseId } = useParams<{ warehouseId: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();

  // Call hooks unconditionally before any early returns
  const { warehouse, isLoading, error } = useWarehouse(warehouseId || '', user?.tenantId || '');

  // Handle missing warehouse ID
  if (!warehouseId) {
    navigate(Routes.locations);
    return null;
  }

  // Handle missing tenant ID
  if (!user?.tenantId) {
    return (
      <DetailPageLayout
        breadcrumbs={getBreadcrumbs.locationList()}
        title="Warehouse Details"
        isLoading={false}
        error="Tenant ID is required to view warehouse details"
      >
        <div />
      </DetailPageLayout>
    );
  }

  const displayName = warehouse?.name || warehouse?.code || 'Loading...';

  return (
    <DetailPageLayout
      breadcrumbs={getBreadcrumbs.warehouseDetail(displayName)}
      title={displayName}
      actions={
        <Button variant="outlined" onClick={() => navigate(Routes.locations)}>
          Back to List
        </Button>
      }
      isLoading={isLoading}
      error={error?.message || null}
    >
      <WarehouseDetail warehouse={warehouse} />
    </DetailPageLayout>
  );
};
