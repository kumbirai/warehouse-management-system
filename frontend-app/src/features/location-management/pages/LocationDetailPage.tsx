import { Button } from '@mui/material';
import { Edit as EditIcon } from '@mui/icons-material';
import { useNavigate, useParams } from 'react-router-dom';

import { DetailPageLayout } from '../../../components/layouts';
import { getBreadcrumbs, Routes } from '../../../utils/navigationUtils';
import { LocationDetail } from '../components/LocationDetail';
import { useLocation } from '../hooks/useLocation';
import { useAuth } from '../../../hooks/useAuth';

export const LocationDetailPage = () => {
  const { locationId } = useParams<{ locationId: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();

  // Call hooks unconditionally before any early returns
  const { location, isLoading, error, refetch } = useLocation(
    locationId || '',
    user?.tenantId || ''
  );

  // Handle missing location ID
  if (!locationId) {
    navigate(Routes.locations);
    return null;
  }

  // Handle missing tenant ID
  if (!user?.tenantId) {
    return (
      <DetailPageLayout
        breadcrumbs={getBreadcrumbs.locationList()}
        title="Location Details"
        isLoading={false}
        error="Tenant ID is required to view location details"
      >
        <div />
      </DetailPageLayout>
    );
  }

  const canEdit =
    user?.roles?.some(role =>
      ['SYSTEM_ADMIN', 'TENANT_ADMIN', 'WAREHOUSE_MANAGER'].includes(role)
    ) ?? false;

  return (
    <DetailPageLayout
      breadcrumbs={getBreadcrumbs.locationDetail(location?.code || '...')}
      title={location?.code || 'Loading...'}
      actions={
        <>
          <Button variant="outlined" onClick={() => navigate(Routes.locations)}>
            Back to List
          </Button>
          {canEdit && locationId && (
            <Button
              variant="outlined"
              startIcon={<EditIcon />}
              onClick={() => navigate(Routes.locationEdit(locationId))}
            >
              Edit
            </Button>
          )}
        </>
      }
      isLoading={isLoading}
      error={error?.message || null}
    >
      <LocationDetail location={location} onStatusUpdate={refetch} />
    </DetailPageLayout>
  );
};
