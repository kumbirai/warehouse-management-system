import { useNavigate } from 'react-router-dom';
import { LocationForm } from '../components/LocationForm';
import { useCreateLocation } from '../hooks/useCreateLocation';
import { CreateLocationRequest } from '../types/location';
import { useAuth } from '../../../hooks/useAuth';
import { FormPageLayout } from '../../../components/layouts';
import { getBreadcrumbs, Routes } from '../../../utils/navigationUtils';

export const LocationCreatePage = () => {
  const navigate = useNavigate();
  const { createLocation, isLoading, error } = useCreateLocation();
  const { user } = useAuth();

  const handleSubmit = async (values: CreateLocationRequest) => {
    if (!user?.tenantId) {
      throw new Error('Tenant ID is required');
    }
    await createLocation(values, user.tenantId);
  };

  const handleCancel = () => {
    navigate(Routes.locations);
  };

  return (
    <FormPageLayout
      breadcrumbs={getBreadcrumbs.locationCreate()}
      title="Create Location"
      description="Create a new warehouse location with barcode and coordinates"
      error={error?.message || null}
      maxWidth="md"
    >
      <LocationForm onSubmit={handleSubmit} onCancel={handleCancel} isSubmitting={isLoading} />
    </FormPageLayout>
  );
};
