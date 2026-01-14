import { Box, Button, Container, Paper, Typography } from '@mui/material';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';

/**
 * Unauthorized page component.
 * Shown when user doesn't have required role/permission.
 */
export const UnauthorizedPage = () => {
  const navigate = useNavigate();
  const { logout } = useAuth();

  const handleGoHome = () => {
    navigate('/');
  };

  const handleLogout = () => {
    logout();
  };

  return (
    <Container maxWidth="sm">
      <Box
        sx={{
          minHeight: '100vh',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
        }}
      >
        <Paper elevation={3} sx={{ p: 4, textAlign: 'center' }}>
          <Typography variant="h4" component="h1" gutterBottom color="error">
            Access Denied
          </Typography>
          <Typography variant="body1" color="text.secondary" sx={{ mb: 3 }}>
            You don't have permission to access this resource.
          </Typography>
          <Box sx={{ display: 'flex', gap: 2, justifyContent: 'center' }}>
            <Button variant="contained" onClick={handleGoHome}>
              Go to Home
            </Button>
            <Button variant="outlined" onClick={handleLogout}>
              Logout
            </Button>
          </Box>
        </Paper>
      </Box>
    </Container>
  );
};
