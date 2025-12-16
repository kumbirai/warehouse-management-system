import { Alert, Box, CircularProgress, Container, Typography } from '@mui/material';
import { useParams } from 'react-router-dom';
import { ConsignmentDetail } from '../components/ConsignmentDetail';
import { useConsignment } from '../hooks/useConsignment';
import { useValidateConsignment } from '../hooks/useValidateConsignment';
import { useAuth } from '../../../hooks/useAuth';

export const ConsignmentDetailPage = () => {
  const { consignmentId } = useParams<{ consignmentId: string }>();
  const { user } = useAuth();
  const { consignment, isLoading, error, refetch } = useConsignment(consignmentId);
  const validateConsignment = useValidateConsignment();

  const handleValidate = async () => {
    if (!consignmentId) {
      return;
    }

    try {
      await validateConsignment.validateConsignment({ consignmentId });
      // Refetch consignment data after validation
      await refetch();
    } catch (err) {
      // Error is handled by the hook
      console.error('Failed to validate consignment:', err);
    }
  };

  const canValidate =
    user?.roles?.some(role => ['ADMIN', 'MANAGER', 'OPERATOR'].includes(role)) ?? false;

  if (isLoading) {
    return (
      <Container maxWidth="lg" sx={{ py: 4 }}>
        <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
          <CircularProgress />
        </Box>
      </Container>
    );
  }

  if (error) {
    return (
      <Container maxWidth="lg" sx={{ py: 4 }}>
        <Alert severity="error">
          {error instanceof Error ? error.message : 'Failed to load consignment'}
        </Alert>
      </Container>
    );
  }

  if (!consignment) {
    return (
      <Container maxWidth="lg" sx={{ py: 4 }}>
        <Alert severity="warning">Consignment not found</Alert>
      </Container>
    );
  }

  return (
    <Container maxWidth="lg" sx={{ py: 4 }}>
      <Typography variant="h4" gutterBottom>
        Consignment Details
      </Typography>

      <ConsignmentDetail
        consignment={consignment}
        onValidate={handleValidate}
        isValidating={validateConsignment.isLoading}
        canValidate={canValidate}
      />
    </Container>
  );
};
