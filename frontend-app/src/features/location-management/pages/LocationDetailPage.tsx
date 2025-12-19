import { Container } from '@mui/material';
import { useNavigate, useParams } from 'react-router-dom';
import { LocationDetail } from '../components/LocationDetail';
import { useLocation } from '../hooks/useLocation';
import { useAuth } from '../../../hooks/useAuth';

export const LocationDetailPage = () => {
  const { locationId } = useParams<{ locationId: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();

  // Call hooks unconditionally before any early returns
  const { location, isLoading, error } = useLocation(locationId || '', user?.tenantId || '');

  if (!locationId) {
    navigate('/locations');
    return null;
  }

  if (!user?.tenantId) {
    return <div>Tenant ID is required</div>;
  }

  return (
    <Container maxWidth="lg" sx={{ mt: 4, mb: 4 }}>
      <LocationDetail location={location} isLoading={isLoading} error={error} />
    </Container>
  );
};
