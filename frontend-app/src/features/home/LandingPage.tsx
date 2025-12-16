import {Box, Button, Container, Grid, Paper, Typography} from '@mui/material';
import {useNavigate} from 'react-router-dom';
import {useAuth} from '../../hooks/useAuth';

/**
 * Public landing page component.
 */
export const LandingPage = () => {
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();

  const handleGetStarted = () => {
    if (isAuthenticated) {
      navigate('/dashboard');
    } else {
      navigate('/login');
    }
  };

  return (
    <Container maxWidth="lg">
      <Box
        sx={{
          minHeight: '100vh',
          display: 'flex',
          flexDirection: 'column',
          justifyContent: 'center',
          alignItems: 'center',
          textAlign: 'center',
          py: 8,
        }}
      >
        <Typography variant="h2" component="h1" gutterBottom fontWeight="bold">
          Warehouse Management System
        </Typography>
        <Typography variant="h5" component="h2" color="text.secondary" gutterBottom>
          CCBSA Local Distribution Partner System
        </Typography>
        <Typography variant="body1" color="text.secondary" sx={{ mt: 3, mb: 4, maxWidth: 600 }}>
          Streamline your warehouse operations with our comprehensive management system. Manage
          stock, track locations, optimize picking, and ensure seamless reconciliation.
        </Typography>

        <Button
          variant="contained"
          size="large"
          onClick={handleGetStarted}
          sx={{ mt: 2, px: 4, py: 1.5 }}
        >
          {isAuthenticated ? 'Go to Dashboard' : 'Get Started'}
        </Button>

        <Grid container spacing={4} sx={{ mt: 8 }}>
          <Grid item xs={12} md={4}>
            <Paper elevation={2} sx={{ p: 3, height: '100%' }}>
              <Typography variant="h6" gutterBottom>
                Stock Management
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Track consignments, monitor stock levels, and manage expiration dates with FEFO
                compliance.
              </Typography>
            </Paper>
          </Grid>
          <Grid item xs={12} md={4}>
            <Paper elevation={2} sx={{ p: 3, height: '100%' }}>
              <Typography variant="h6" gutterBottom>
                Location Tracking
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Manage warehouse locations, track movements, and optimize storage capacity.
              </Typography>
            </Paper>
          </Grid>
          <Grid item xs={12} md={4}>
            <Paper elevation={2} sx={{ p: 3, height: '100%' }}>
              <Typography variant="h6" gutterBottom>
                Picking & Returns
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Optimize picking operations and manage returns efficiently.
              </Typography>
            </Paper>
          </Grid>
        </Grid>
      </Box>
    </Container>
  );
};
