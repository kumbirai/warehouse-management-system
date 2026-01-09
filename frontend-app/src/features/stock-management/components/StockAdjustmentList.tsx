import { Typography } from '@mui/material';
import { useNavigate } from 'react-router-dom';
import { Column, ResponsiveTable } from '../../../components/common';
import { StockAdjustmentResponse } from '../types/stockManagement';
import { Routes } from '../../../utils/navigationUtils';
import { formatDateTime } from '../../../utils/dateUtils';

interface StockAdjustmentListProps {
  adjustments: StockAdjustmentResponse[];
  error: Error | null;
}

export const StockAdjustmentList = ({ adjustments, error }: StockAdjustmentListProps) => {
  const navigate = useNavigate();

  if (error) {
    return <Typography color="error">Error loading stock adjustments: {error.message}</Typography>;
  }

  if (!Array.isArray(adjustments)) {
    return <Typography color="error">Invalid data format: adjustments is not an array</Typography>;
  }

  const columns: Column<StockAdjustmentResponse>[] = [
    {
      key: 'adjustmentId',
      label: 'Adjustment ID',
      render: adjustment => (
        <Typography variant="body2" fontWeight="medium">
          {adjustment.adjustmentId.substring(0, 8)}...
        </Typography>
      ),
    },
    {
      key: 'productId',
      label: 'Product ID',
      render: adjustment => <Typography variant="body2">{adjustment.productId}</Typography>,
      hideOnMobile: true,
    },
    {
      key: 'locationId',
      label: 'Location',
      render: adjustment => <Typography variant="body2">{adjustment.locationId || '—'}</Typography>,
      hideOnMobile: true,
    },
    {
      key: 'adjustmentType',
      label: 'Type',
      render: adjustment => <Typography variant="body2">{adjustment.adjustmentType}</Typography>,
    },
    {
      key: 'quantity',
      label: 'Quantity',
      render: adjustment => <Typography variant="body2">{adjustment.quantity}</Typography>,
    },
    {
      key: 'quantityBefore',
      label: 'Before',
      render: adjustment => <Typography variant="body2">{adjustment.quantityBefore}</Typography>,
      hideOnMobile: true,
    },
    {
      key: 'quantityAfter',
      label: 'After',
      render: adjustment => <Typography variant="body2">{adjustment.quantityAfter}</Typography>,
      hideOnMobile: true,
    },
    {
      key: 'reason',
      label: 'Reason',
      render: adjustment => <Typography variant="body2">{adjustment.reason}</Typography>,
      hideOnMobile: true,
    },
    {
      key: 'adjustedAt',
      label: 'Adjusted At',
      render: adjustment => (
        <Typography variant="body2">{formatDateTime(adjustment.adjustedAt)}</Typography>
      ),
      hideOnMobile: true,
    },
    {
      key: 'actions',
      label: 'Actions',
      hideOnMobile: true,
      render: () => (
        <Typography variant="body2" color="primary" sx={{ cursor: 'pointer' }}>
          View
        </Typography>
      ),
    },
  ];

  return (
    <ResponsiveTable
      data={adjustments}
      columns={columns}
      getRowKey={adjustment => adjustment.adjustmentId}
      onRowClick={adjustment => navigate(Routes.stockAdjustmentDetail(adjustment.adjustmentId))}
      emptyMessage="No stock adjustments found"
    />
  );
};
