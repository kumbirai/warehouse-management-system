import { Container, Typography } from '@mui/material';
import { ConsignmentForm } from '../components/ConsignmentForm';
import { useCreateConsignment } from '../hooks/useCreateConsignment';
import { CreateConsignmentRequest } from '../types/stockManagement';
import { useNavigate } from 'react-router-dom';

export const CreateConsignmentPage = () => {
  const navigate = useNavigate();
  const createConsignment = useCreateConsignment();

  const handleSubmit = async (request: CreateConsignmentRequest) => {
    await createConsignment.createConsignment(request);
  };

  const handleCancel = () => {
    navigate('/stock-management/consignments');
  };

  return (
    <Container maxWidth="lg" sx={{ py: 4 }}>
      <Typography variant="h4" gutterBottom>
        Create Stock Consignment
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        Create a new stock consignment by manually entering consignment details and line items. You
        can scan barcodes to automatically fill product codes.
      </Typography>

      <ConsignmentForm
        onSubmit={handleSubmit}
        onCancel={handleCancel}
        isSubmitting={createConsignment.isLoading}
      />
    </Container>
  );
};
