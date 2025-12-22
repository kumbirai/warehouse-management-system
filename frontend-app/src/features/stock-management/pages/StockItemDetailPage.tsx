import {useNavigate, useParams} from 'react-router-dom';
import {Button} from '@mui/material';

import {DetailPageLayout} from '../../../components/layouts';
import {getBreadcrumbs, Routes} from '../../../utils/navigationUtils';
import {StockItemDetail} from '../components/StockItemDetail';
import {useStockItem} from '../hooks/useStockItem';

/**
 * StockItemDetailPage
 * <p>
 * Page for viewing a single stock item with classification and location information.
 * Follows mandated frontend templates and CQRS principles.
 */
export const StockItemDetailPage = () => {
  const { stockItemId } = useParams<{ stockItemId: string }>();
  const navigate = useNavigate();
  const { data: queryResponse, isLoading, error, refetch } = useStockItem(stockItemId || null);

  const stockItem = queryResponse?.data;

  const handleLocationAssigned = async () => {
    // Refetch stock item data after location assignment
    await refetch();
  };

  return (
    <DetailPageLayout
      breadcrumbs={getBreadcrumbs.stockItemDetail(stockItemId || '...')}
      title={`Stock Item ${stockItemId?.substring(0, 8) || '...'}`}
      actions={
        <Button variant="outlined" onClick={() => navigate(Routes.stockItems)}>
          Back to List
        </Button>
      }
      isLoading={isLoading}
      error={error?.message || null}
    >
      <StockItemDetail stockItem={stockItem || null} onLocationAssigned={handleLocationAssigned} />
    </DetailPageLayout>
  );
};
