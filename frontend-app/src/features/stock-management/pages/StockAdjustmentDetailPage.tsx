import { Button } from '@mui/material';
import { useNavigate, useParams } from 'react-router-dom';

import { DetailPageLayout } from '../../../components/layouts';
import { StockAdjustmentDetail } from '../components/StockAdjustmentDetail';
import { useStockAdjustment } from '../hooks/useStockAdjustment';
import { getBreadcrumbs, Routes } from '../../../utils/navigationUtils';

export const StockAdjustmentDetailPage = () => {
  const { adjustmentId } = useParams<{ adjustmentId: string }>();
  const navigate = useNavigate();
  const { data: queryResponse, isLoading, error } = useStockAdjustment(adjustmentId || null);

  const adjustment = queryResponse?.data;

  if (!adjustmentId) {
    navigate(Routes.stockAdjustments);
    return null;
  }

  return (
    <DetailPageLayout
      breadcrumbs={getBreadcrumbs.stockAdjustmentDetail(adjustmentId)}
      title={`Adjustment ${adjustmentId.substring(0, 8)}...`}
      actions={
        <Button variant="outlined" onClick={() => navigate(Routes.stockAdjustments)}>
          Back to List
        </Button>
      }
      isLoading={isLoading}
      error={error?.message || null}
    >
      <StockAdjustmentDetail adjustment={adjustment || null} />
    </DetailPageLayout>
  );
};

