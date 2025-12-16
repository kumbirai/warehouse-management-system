import { Box, Button, Container } from '@mui/material';
import { useNavigate } from 'react-router-dom';
import { LocationList } from '../components/LocationList';
import { useLocations } from '../hooks/useLocations';
import { useAuth } from '../../../hooks/useAuth';
import AddIcon from '@mui/icons-material/Add';

export const LocationListPage = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const { locations, isLoading, error } = useLocations({
    tenantId: user?.tenantId,
    page: 0,
    size: 100,
  });

  return (
    <Container maxWidth="lg" sx={{ mt: 4, mb: 4 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <h1>Locations</h1>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => navigate('/locations/create')}
        >
          Create Location
        </Button>
      </Box>
      <LocationList locations={locations} isLoading={isLoading} error={error} />
    </Container>
  );
};

