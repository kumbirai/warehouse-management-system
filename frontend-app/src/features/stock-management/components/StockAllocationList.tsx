import { Typography } from '@mui/material';
import { useNavigate } from 'react-router-dom';
import { Column, getStatusVariant, ResponsiveTable, StatusBadge } from '../../../components/common';
import { StockAllocationResponse } from '../types/stockManagement';
import { Routes } from '../../../utils/navigationUtils';
import { formatDateTime } from '../../../utils/dateUtils';

interface StockAllocationListProps {
  allocations: StockAllocationResponse[];
  error: Error | null;
}

export const StockAllocationList = ({ allocations, error }: StockAllocationListProps) => {
  const navigate = useNavigate();

  if (error) {
    return <Typography color="error">Error loading stock allocations: {error.message}</Typography>;
  }

  if (!Array.isArray(allocations)) {
    return <Typography color="error">Invalid data format: allocations is not an array</Typography>;
  }

  const columns: Column<StockAllocationResponse>[] = [
    {
      key: 'allocationId',
      label: 'Allocation ID',
      render: allocation => (
        <Typography variant="body2" fontWeight="medium">
          {allocation.allocationId.substring(0, 8)}...
        </Typography>
      ),
    },
    {
      key: 'productId',
      label: 'Product ID',
      render: allocation => <Typography variant="body2">{allocation.productId}</Typography>,
      hideOnMobile: true,
    },
    {
      key: 'locationId',
      label: 'Location',
      render: allocation => (
        <Typography variant="body2">{allocation.locationId || '—'}</Typography>
      ),
      hideOnMobile: true,
    },
    {
      key: 'quantity',
      label: 'Quantity',
      render: allocation => <Typography variant="body2">{allocation.quantity}</Typography>,
    },
    {
      key: 'allocationType',
      label: 'Type',
      render: allocation => <Typography variant="body2">{allocation.allocationType}</Typography>,
      hideOnMobile: true,
    },
    {
      key: 'referenceId',
      label: 'Reference',
      render: allocation => (
        <Typography variant="body2">{allocation.referenceId || '—'}</Typography>
      ),
      hideOnMobile: true,
    },
    {
      key: 'status',
      label: 'Status',
      render: allocation => (
        <StatusBadge label={allocation.status} variant={getStatusVariant(allocation.status)} />
      ),
    },
    {
      key: 'allocatedAt',
      label: 'Allocated At',
      render: allocation => (
        <Typography variant="body2">{formatDateTime(allocation.allocatedAt)}</Typography>
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
      data={allocations}
      columns={columns}
      getRowKey={allocation => allocation.allocationId}
      onRowClick={allocation => navigate(Routes.stockAllocationDetail(allocation.allocationId))}
      emptyMessage="No stock allocations found"
    />
  );
};

