import { Box, Typography } from '@mui/material';
import { Link, useNavigate } from 'react-router-dom';
import { Column, ResponsiveTable, StatusBadge } from '../../../components/common';
import { getStatusVariant } from '../../../utils/statusUtils';
import { Consignment } from '../types/stockManagement';
import { Routes } from '../../../utils/navigationUtils';
import { formatDateTime } from '../../../utils/dateUtils';
import { LocationOn as LocationIcon } from '@mui/icons-material';

interface ConsignmentListProps {
  consignments: Consignment[];
  error: Error | null;
}

export const ConsignmentList = ({ consignments, error }: ConsignmentListProps) => {
  const navigate = useNavigate();

  if (error) {
    return <Typography color="error">Error loading consignments: {error.message}</Typography>;
  }

  if (!Array.isArray(consignments)) {
    return <Typography color="error">Invalid data format: consignments is not an array</Typography>;
  }

  const getWarehouseDisplayName = (consignment: Consignment): string => {
    if (consignment.warehouseName && consignment.warehouseCode) {
      return `${consignment.warehouseCode} - ${consignment.warehouseName}`;
    }
    if (consignment.warehouseName) {
      return consignment.warehouseName;
    }
    if (consignment.warehouseCode) {
      return consignment.warehouseCode;
    }
    return consignment.warehouseId.substring(0, 8) + '...';
  };

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
        <Box>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
            <LocationIcon fontSize="small" color="action" />
            <Typography
              variant="body2"
              fontWeight="medium"
              component={Link}
              to={Routes.warehouseDetail(consignment.warehouseId)}
              onClick={e => {
                e.stopPropagation(); // Prevent row click
              }}
              sx={{
                color: 'primary.main',
                textDecoration: 'none',
                '&:hover': {
                  textDecoration: 'underline',
                },
              }}
            >
              {getWarehouseDisplayName(consignment)}
            </Typography>
          </Box>
        </Box>
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
