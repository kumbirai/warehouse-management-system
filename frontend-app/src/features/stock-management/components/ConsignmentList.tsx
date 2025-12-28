import { Typography } from '@mui/material';
import { useNavigate } from 'react-router-dom';
import { Column, getStatusVariant, ResponsiveTable, StatusBadge } from '../../../components/common';
import { Consignment } from '../types/stockManagement';
import { Routes } from '../../../utils/navigationUtils';
import { formatDateTime } from '../../../utils/dateUtils';
import { useLocationDescriptionMap } from '../hooks/useLocationDescription';

interface ConsignmentListProps {
  consignments: Consignment[];
  error: Error | null;
}

export const ConsignmentList = ({ consignments, error }: ConsignmentListProps) => {
  const navigate = useNavigate();
  const locationDescriptionMap = useLocationDescriptionMap();

  if (error) {
    return <Typography color="error">Error loading consignments: {error.message}</Typography>;
  }

  if (!Array.isArray(consignments)) {
    return <Typography color="error">Invalid data format: consignments is not an array</Typography>;
  }

  const columns: Column<Consignment>[] = [
    {
      key: 'consignmentReference',
      label: 'Reference',
      render: consignment => (
        <Typography variant="body2" fontWeight="medium">
          {consignment.consignmentReference}
        </Typography>
      ),
    },
    {
      key: 'status',
      label: 'Status',
      render: consignment => (
        <StatusBadge label={consignment.status} variant={getStatusVariant(consignment.status)} />
      ),
    },
    {
      key: 'warehouseId',
      label: 'Warehouse',
      render: consignment => (
        <Typography variant="body2">
          {locationDescriptionMap.get(consignment.warehouseId) || consignment.warehouseId}
        </Typography>
      ),
    },
    {
      key: 'lineItems',
      label: 'Items',
      render: consignment => (
        <Typography variant="body2">{consignment.lineItems.length} item(s)</Typography>
      ),
    },
    {
      key: 'receivedAt',
      label: 'Received At',
      render: consignment => (
        <Typography variant="body2">{formatDateTime(consignment.receivedAt)}</Typography>
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
      data={consignments}
      columns={columns}
      getRowKey={consignment => consignment.consignmentId}
      onRowClick={consignment => navigate(Routes.consignmentDetail(consignment.consignmentId))}
      emptyMessage="No consignments found"
    />
  );
};
