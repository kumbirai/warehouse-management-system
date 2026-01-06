import { useNavigate, useParams } from 'react-router-dom';
import { useMemo } from 'react';
import { LocationForm } from '../components/LocationForm';
import { useUpdateLocation } from '../hooks/useUpdateLocation';
import { useLocation } from '../hooks/useLocation';
import { CreateLocationRequest, UpdateLocationRequest } from '../types/location';
import { useAuth } from '../../../hooks/useAuth';
import { FormPageLayout } from '../../../components/layouts';
import { SkeletonForm } from '../../../components/common';
import { getBreadcrumbs, Routes } from '../../../utils/navigationUtils';

export const LocationEditPage = () => {
  const { locationId } = useParams<{ locationId: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();
  const { updateLocation, isLoading, error } = useUpdateLocation();

  // Fetch location for default values
  const { location, isLoading: isLoadingLocation } = useLocation(
    locationId || '',
    user?.tenantId || ''
  );

  // Convert Location to form default values - memoized to prevent unnecessary re-renders
  // Must be called before any early returns to follow React Hooks rules
  const defaultValues = useMemo(() => {
    if (!location) return undefined;
    return {
      zone: location.coordinates?.zone || '',
      aisle: location.coordinates?.aisle || '',
      rack: location.coordinates?.rack || '',
      level: location.coordinates?.level || '',
      barcode: location.barcode || '',
      description: location.description || '',
    };
  }, [location]);

  if (!locationId) {
    navigate(Routes.locations);
    return null;
  }

  if (!user?.tenantId) {
    return (
      <FormPageLayout
        breadcrumbs={getBreadcrumbs.locationList()}
        title="Edit Location"
        error="Tenant ID is required to edit locations"
      >
        <div />
      </FormPageLayout>
    );
  }

  // TypeScript narrowing: locationId and user.tenantId are guaranteed to be non-null after checks
  const validLocationId = locationId;
  const validTenantId = user.tenantId;

  const handleSubmit = async (values: CreateLocationRequest | UpdateLocationRequest) => {
    await updateLocation(validLocationId, values, validTenantId);
    navigate(Routes.locationDetail(validLocationId));
  };

  const handleCancel = () => {
    navigate(Routes.locationDetail(validLocationId));
  };

  return (
    <FormPageLayout
      breadcrumbs={getBreadcrumbs.locationEdit()}
      title="Edit Location"
      description="Update location information"
      error={error?.message || null}
      maxWidth="md"
    >
      {isLoadingLocation ? (
        <SkeletonForm fields={6} />
      ) : (
        <LocationForm
          key={location?.locationId}
          defaultValues={defaultValues}
          onSubmit={handleSubmit}
          onCancel={handleCancel}
          isSubmitting={isLoading || isLoadingLocation}
          isUpdate={true}
        />
      )}
    </FormPageLayout>
  );
};
