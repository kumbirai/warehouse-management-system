import { Container } from '@mui/material';
import { useNavigate } from 'react-router-dom';
import { LocationForm } from '../components/LocationForm';
import { useCreateLocation } from '../hooks/useCreateLocation';
import { CreateLocationRequest } from '../types/location';
import { useAuth } from '../../../hooks/useAuth';

export const LocationCreatePage = () => {
  const navigate = useNavigate();
  const { createLocation, isLoading } = useCreateLocation();
  const { user } = useAuth();

  const handleSubmit = async (values: CreateLocationRequest) => {
    if (!user?.tenantId) {
      throw new Error('Tenant ID is required');
    }
    await createLocation(values, user.tenantId);
  };

  const handleCancel = () => {
    navigate('/locations');
  };

  return (
    <Container maxWidth="md" sx={{ mt: 4, mb: 4 }}>
      <LocationForm onSubmit={handleSubmit} onCancel={handleCancel} isSubmitting={isLoading} />
    </Container>
  );
};
