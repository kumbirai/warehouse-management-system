import { Button } from '@mui/material';
import { useNavigate, useParams } from 'react-router-dom';

import { DetailPageLayout } from '../../../components/layouts';
import { StockAllocationDetail } from '../components/StockAllocationDetail';
import { useStockAllocation } from '../hooks/useStockAllocation';
import { useReleaseStockAllocation } from '../hooks/useReleaseStockAllocation';
import { useAuth } from '../../../hooks/useAuth';
import { getBreadcrumbs, Routes } from '../../../utils/navigationUtils';
import { logger } from '../../../utils/logger';

export const StockAllocationDetailPage = () => {
  const { allocationId } = useParams<{ allocationId: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();
  const {
    data: queryResponse,
    isLoading,
    error,
    refetch,
  } = useStockAllocation(allocationId || null);
  const { releaseStockAllocation, isLoading: isReleasing } = useReleaseStockAllocation();

  const allocation = queryResponse?.data;

  const handleRelease = async () => {
    if (!allocationId || !user?.tenantId) {
      return;
    }

    try {
      await releaseStockAllocation(allocationId, user.tenantId);
      await refetch();
    } catch (err) {
      // Error is handled by the hook
      logger.error(
        'Failed to release allocation',
        err instanceof Error ? err : new Error(String(err)),
        {
          allocationId,
          tenantId: user?.tenantId,
        }
      );
    }
  };

  if (!allocationId) {
    navigate(Routes.stockAllocations);
    return null;
  }

  return (
    <DetailPageLayout
      breadcrumbs={getBreadcrumbs.stockAllocationDetail(allocationId)}
      title={`Allocation ${allocationId.substring(0, 8)}...`}
      actions={
        <Button variant="outlined" onClick={() => navigate(Routes.stockAllocations)}>
          Back to List
        </Button>
      }
      isLoading={isLoading}
      error={error?.message || null}
    >
      <StockAllocationDetail
        allocation={allocation || null}
        onRelease={handleRelease}
        isReleasing={isReleasing}
      />
    </DetailPageLayout>
  );
};
