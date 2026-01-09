import { useNavigate } from 'react-router-dom';
import { StockAdjustmentForm } from '../components/StockAdjustmentForm';
import { useAdjustStock } from '../hooks/useAdjustStock';
import { CreateStockAdjustmentRequest } from '../types/stockManagement';
import { useAuth } from '../../../hooks/useAuth';
import { FormPageLayout } from '../../../components/layouts';
import { getBreadcrumbs, Routes } from '../../../utils/navigationUtils';

export const StockAdjustmentCreatePage = () => {
  const navigate = useNavigate();
  const { adjustStock, isLoading, error } = useAdjustStock();
  const { user } = useAuth();

  const handleSubmit = async (values: CreateStockAdjustmentRequest) => {
    if (!user?.tenantId) {
      throw new Error('Tenant ID is required');
    }
    await adjustStock(values, user.tenantId);
  };

  const handleCancel = () => {
    navigate(Routes.stockAdjustments);
  };

  return (
    <FormPageLayout
      breadcrumbs={getBreadcrumbs.stockAdjustmentCreate()}
      title="Adjust Stock Level"
      description="Manually adjust stock levels with reason tracking"
      error={error?.message || null}
      maxWidth="md"
    >
      <StockAdjustmentForm
        onSubmit={handleSubmit}
        onCancel={handleCancel}
        isSubmitting={isLoading}
      />
    </FormPageLayout>
  );
};
