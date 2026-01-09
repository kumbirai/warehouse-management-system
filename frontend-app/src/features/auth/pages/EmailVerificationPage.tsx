import {useEffect, useState} from 'react';
import {useNavigate, useSearchParams} from 'react-router-dom';
import {Alert, Box, Button, CircularProgress, Container, Paper, Typography} from '@mui/material';
import {verifyEmail} from '../services/verificationService';
import { logger } from '../../../utils/logger';

/**
 * Email verification page.
 * Handles Keycloak email verification callback.
 *
 * Keycloak redirects to this page after processing the verification link.
 * URL parameters:
 * - execution: Keycloak execution ID
 * - client_id: Keycloak client ID
 * - tab_id: Keycloak tab ID
 * - session_code: Keycloak session code (for verification)
 */
export const EmailVerificationPage = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [status, setStatus] = useState<'loading' | 'success' | 'error'>('loading');
  const [message, setMessage] = useState<string>('');

  useEffect(() => {
    const verify = async () => {
      try {
        // Extract token from URL parameters
        // Keycloak may pass different parameters depending on the flow
        const token = searchParams.get('token') || searchParams.get('session_code');
        const key = searchParams.get('key');

        if (!token) {
          // Check if this is a Keycloak redirect after successful verification
          const execution = searchParams.get('execution');
          const clientId = searchParams.get('client_id');

          if (execution && clientId) {
            // This is a Keycloak redirect - verification was likely successful
            setStatus('success');
            setMessage('Your email has been verified successfully! You can now log in.');
            return;
          }

          // No token and no Keycloak parameters - invalid link
          setStatus('error');
          setMessage(
            'Invalid verification link. Please check your email for a valid verification link.'
          );
          return;
        }

        // Verify email with token
        await verifyEmail(token, key || undefined);
        setStatus('success');
        setMessage('Your email has been verified successfully! You can now log in.');
      } catch (error: unknown) {
        logger.error(
          'Email verification error',
          error instanceof Error ? error : new Error(String(error))
        );
        setStatus('error');
        const errorMessage =
          error instanceof Error
            ? error.message
            : 'Email verification failed. The link may have expired. Please request a new verification email.';
        setMessage(errorMessage);
      }
    };

    verify();
  }, [searchParams]);

  return (
    <Container component="main" maxWidth="sm">
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
            Email Verification
          </Typography>

          {status === 'loading' && (
            <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', mt: 4 }}>
              <CircularProgress />
              <Typography variant="body1" sx={{ mt: 2 }}>
                Verifying your email address...
              </Typography>
            </Box>
          )}

          {status === 'success' && (
            <Box sx={{ mt: 3 }}>
              <Alert severity="success" sx={{ mb: 3 }}>
                {message}
              </Alert>
              <Box sx={{ display: 'flex', justifyContent: 'center', gap: 2 }}>
                <Button variant="contained" onClick={() => navigate('/login')}>
                  Go to Login
                </Button>
              </Box>
            </Box>
          )}

          {status === 'error' && (
            <Box sx={{ mt: 3 }}>
              <Alert severity="error" sx={{ mb: 3 }}>
                {message}
              </Alert>
              <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 2 }}>
                <Typography variant="body2" color="text.secondary">
                  If you need a new verification email, please contact your system administrator.
                </Typography>
                <Button variant="outlined" onClick={() => navigate('/login')}>
                  Back to Login
                </Button>
              </Box>
            </Box>
          )}
        </Paper>
      </Box>
    </Container>
  );
};
