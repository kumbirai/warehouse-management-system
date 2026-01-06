import { Alert, Box, Button } from '@mui/material';
import { useNavigate } from 'react-router-dom';
import { ArrowBack as ArrowBackIcon } from '@mui/icons-material';
import { useAssignLocationsFEFO } from '../hooks/useAssignLocationsFEFO';
import { FEFOAssignmentForm } from '../components/FEFOAssignmentForm';
import { FormPageLayout } from '../../../components/layouts/FormPageLayout';
import { getBreadcrumbs, Routes } from '../../../utils/navigationUtils';
import { useStockItemsByClassification } from '../../stock-management/hooks/useStockItemsByClassification';
import { StockItem } from '../../stock-management/types/stockManagement';
import { AssignLocationsFEFORequest } from '../types/location';
import { useToast } from '../../../hooks/useToast';

export const FEFOAssignmentPage = () => {
  const navigate = useNavigate();
  const assignLocationsFEFO = useAssignLocationsFEFO();
  const { success, error: showError } = useToast();

  // Fetch all unassigned stock items (no classification filter to get all)
  const { data: stockItemsResponse, isLoading: isLoadingStockItems, error: stockItemsError } =
    useStockItemsByClassification(null);

  const stockItems: StockItem[] = stockItemsResponse?.data?.stockItems || [];

  const handleSubmit = async (request: AssignLocationsFEFORequest) => {
    try {
      await assignLocationsFEFO.mutateAsync(request);
      success(
        `Successfully assigned locations to ${request.stockItems.length} stock item(s) using FEFO algorithm.`
      );
    } catch (error) {
      const errorMessage =
        error instanceof Error
          ? error.message
          : 'Failed to assign locations. Please try again.';
      showError(errorMessage);
      throw error;
    }
  };

  const error = stockItemsError?.message || (assignLocationsFEFO.error as Error)?.message || null;

  return (
    <FormPageLayout
      breadcrumbs={getBreadcrumbs.fefoAssignment()}
      title="FEFO Assignment"
      description="Assign locations to stock items using the First Expired, First Out (FEFO) algorithm based on expiration dates."
      error={error}
      maxWidth="lg"
    >
      <Box
        sx={{
          mb: 2,
          display: 'flex',
          flexDirection: { xs: 'column', sm: 'row' },
        }}
      >
        <Button
          startIcon={<ArrowBackIcon />}
          onClick={() => navigate(Routes.stockItems)}
          aria-label="Back to stock items"
          sx={{ width: { xs: '100%', sm: 'auto' } }}
        >
          Back to Stock Items
        </Button>
      </Box>

      {isLoadingStockItems ? (
        <Alert severity="info">Loading stock items...</Alert>
      ) : (
        <FEFOAssignmentForm
          stockItems={stockItems}
          onSubmit={handleSubmit}
          isLoading={assignLocationsFEFO.isPending}
        />
      )}
    </FormPageLayout>
  );
};
