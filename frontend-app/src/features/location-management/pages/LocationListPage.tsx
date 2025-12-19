import { Box, Breadcrumbs, Button, Container, Link, Typography } from '@mui/material';
import { Link as RouterLink, useNavigate } from 'react-router-dom';
import { LocationList } from '../components/LocationList';
import { useLocations } from '../hooks/useLocations';
import { useAuth } from '../../../hooks/useAuth';
import { Header } from '../../../components/layout/Header';
import AddIcon from '@mui/icons-material/Add';

export const LocationListPage = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const { locations, isLoading, error } = useLocations({
    tenantId: user?.tenantId ?? undefined,
    page: 0,
    size: 100,
  });

  return (
    <>
      <Header />
      <Container maxWidth="lg" sx={{ mt: 4, mb: 4 }}>
        <Breadcrumbs sx={{ mb: 2 }}>
          <Link component={RouterLink} to="/dashboard" color="inherit">
            Dashboard
          </Link>
          <Typography color="text.primary">Locations</Typography>
        </Breadcrumbs>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
          <Typography variant="h4" component="h1">
            Locations
          </Typography>
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
    </>
  );
};
