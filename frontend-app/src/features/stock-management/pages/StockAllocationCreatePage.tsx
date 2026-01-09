import { useNavigate } from 'react-router-dom';
import { StockAllocationForm } from '../components/StockAllocationForm';
import { useAllocateStock } from '../hooks/useAllocateStock';
import { CreateStockAllocationRequest } from '../types/stockManagement';
import { useAuth } from '../../../hooks/useAuth';
import { FormPageLayout } from '../../../components/layouts';
import { getBreadcrumbs, Routes } from '../../../utils/navigationUtils';

export const StockAllocationCreatePage = () => {
  const navigate = useNavigate();
  const { allocateStock, isLoading, error } = useAllocateStock();
  const { user } = useAuth();

  const handleSubmit = async (values: CreateStockAllocationRequest) => {
    if (!user?.tenantId) {
      throw new Error('Tenant ID is required');
    }
    await allocateStock(values, user.tenantId);
  };

  const handleCancel = () => {
    navigate(Routes.stockAllocations);
  };

  return (
    <FormPageLayout
      breadcrumbs={getBreadcrumbs.stockAllocationCreate()}
      title="Allocate Stock"
      description="Allocate stock for picking orders or reservations using FEFO"
      error={error?.message || null}
      maxWidth="md"
    >
      <StockAllocationForm
        onSubmit={handleSubmit}
        onCancel={handleCancel}
        isSubmitting={isLoading}
      />
    </FormPageLayout>
  );
};
