import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Alert, Box, Button, Container, Link, Paper, TextField, Typography } from '@mui/material';
import axios from 'axios';
import { useAuth } from '../../hooks/useAuth';

/**
 * Login page component.
 * Handles user authentication via BFF.
 */
export const LoginPage = () => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const { login, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const loginInProgressRef = useRef(false);
  const loginSuccessRef = useRef(false);

  // Redirect if already authenticated
  useEffect(() => {
    if (isAuthenticated && !loginSuccessRef.current) {
      console.log('[LoginPage] User already authenticated, redirecting to dashboard');
      navigate('/dashboard');
    }
  }, [isAuthenticated, navigate]);

  // Navigate to dashboard when authentication state becomes true after successful login
  useEffect(() => {
    if (loginSuccessRef.current && isAuthenticated) {
      console.log('[LoginPage] Login successful, navigating to dashboard', {
        isAuthenticated,
        loginSuccess: loginSuccessRef.current,
      });
      loginSuccessRef.current = false;
      navigate('/dashboard');
    }
  }, [isAuthenticated, navigate]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    // Prevent concurrent login attempts
    if (loginInProgressRef.current || isLoading) {
      return;
    }

    setError(null);
    setIsLoading(true);
    loginInProgressRef.current = true;

    try {
      console.log('[LoginPage] Calling login function');
      await login(username, password);
      console.log('[LoginPage] Login function completed successfully, setting loginSuccessRef');
      // Mark login as successful - navigation will happen via useEffect when isAuthenticated becomes true
      loginSuccessRef.current = true;
      console.log(
        '[LoginPage] loginSuccessRef set to true, waiting for isAuthenticated to become true'
      );
    } catch (err) {
      let errorMessage = 'Invalid username or password. Please try again.';

      if (axios.isAxiosError(err)) {
        // Check for standardized ApiResponse error format
        if (err.response?.data?.error?.message) {
          errorMessage = err.response.data.error.message;
        }
        // Fallback: check for direct error message
        else if (err.response?.data?.message) {
          errorMessage = err.response.data.message;
        }
        // Fallback: check for error string
        else if (typeof err.response?.data === 'string') {
          errorMessage = err.response.data;
        }
        // Network or connection errors
        else if (err.code === 'ECONNABORTED' || err.message?.includes('timeout')) {
          errorMessage = 'Request timed out. Please check your connection and try again.';
        }
        // No response (network error)
        else if (!err.response) {
          errorMessage =
            'Unable to connect to the server. Please check your connection and try again.';
        }
      } else if (err instanceof Error) {
        errorMessage = err.message;
      }

      setError(errorMessage);
      loginSuccessRef.current = false; // Reset on error
    } finally {
      setIsLoading(false);
      loginInProgressRef.current = false;
    }
  };

  return (
    <Container component="main" maxWidth="xs">
      <Box
        sx={{
          marginTop: 8,
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
        }}
      >
        <Paper elevation={3} sx={{ padding: 4, width: '100%' }}>
          <Typography component="h1" variant="h4" align="center" gutterBottom>
            Warehouse Management System
          </Typography>
          <Typography
            component="h2"
            variant="h6"
            align="center"
            color="text.secondary"
            gutterBottom
          >
            Sign In
          </Typography>

          {error && (
            <Alert severity="error" sx={{ mt: 2, mb: 2 }}>
              {error}
            </Alert>
          )}

          <Box component="form" onSubmit={handleSubmit} sx={{ mt: 3 }}>
            <TextField
              margin="normal"
              required
              fullWidth
              id="username"
              label="Username"
              name="username"
              autoComplete="username"
              autoFocus
              value={username}
              onChange={e => setUsername(e.target.value)}
              disabled={isLoading}
            />
            <TextField
              margin="normal"
              required
              fullWidth
              name="password"
              label="Password"
              type="password"
              id="password"
              autoComplete="current-password"
              value={password}
              onChange={e => setPassword(e.target.value)}
              disabled={isLoading}
            />
            <Button
              type="submit"
              fullWidth
              variant="contained"
              sx={{ mt: 3, mb: 2 }}
              disabled={isLoading}
            >
              {isLoading ? 'Signing in...' : 'Sign In'}
            </Button>
            <Box textAlign="center">
              <Link href="/" variant="body2">
                Back to Home
              </Link>
            </Box>
          </Box>
        </Paper>
      </Box>
    </Container>
  );
};
