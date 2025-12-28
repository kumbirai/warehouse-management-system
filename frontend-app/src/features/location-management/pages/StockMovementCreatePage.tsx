import { useNavigate } from 'react-router-dom';
import { StockMovementForm } from '../components/StockMovementForm';
import { useCreateStockMovement } from '../hooks/useCreateStockMovement';
import { CreateStockMovementRequest } from '../services/stockMovementService';
import { useAuth } from '../../../hooks/useAuth';
import { FormPageLayout } from '../../../components/layouts';
import { getBreadcrumbs, Routes } from '../../../utils/navigationUtils';

export const StockMovementCreatePage = () => {
  const navigate = useNavigate();
  const { createStockMovement, isLoading, error } = useCreateStockMovement();
  const { user } = useAuth();

  const handleSubmit = async (values: CreateStockMovementRequest) => {
    if (!user?.tenantId) {
      throw new Error('Tenant ID is required');
    }
    await createStockMovement(values, user.tenantId);
  };

  const handleCancel = () => {
    navigate(Routes.stockMovements);
  };

  return (
    <FormPageLayout
      breadcrumbs={getBreadcrumbs.stockMovementCreate()}
      title="Create Stock Movement"
      description="Initiate a new stock movement between locations"
      error={error?.message || null}
      maxWidth="md"
    >
      <StockMovementForm onSubmit={handleSubmit} onCancel={handleCancel} isSubmitting={isLoading} />
    </FormPageLayout>
  );
};

