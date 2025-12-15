import {
  Box,
  CircularProgress,
  IconButton,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Tooltip,
  Typography,
} from '@mui/material';
import OpenInNewIcon from '@mui/icons-material/OpenInNew';
import { Tenant } from '../types/tenant';
import { TenantStatusBadge } from './TenantStatusBadge';
import { TenantActions } from './TenantActions';
import { formatDateTime } from '../../../utils/dateUtils';

interface TenantListProps {
  tenants: Tenant[];
  isLoading?: boolean;
  onOpenTenant: (tenantId: string) => void;
  onActionCompleted: () => void;
}

export const TenantList = ({
  tenants,
  isLoading = false,
  onOpenTenant,
  onActionCompleted,
}: TenantListProps) => {
  if (isLoading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" py={4}>
        <CircularProgress />
      </Box>
    );
  }

  if (!tenants.length) {
    return (
      <Box py={4}>
        <Typography variant="body2" color="text.secondary" align="center">
          No tenants found. Try adjusting filters or create a new tenant.
        </Typography>
      </Box>
    );
  }

  return (
    <TableContainer component={Paper} elevation={2}>
      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell>Tenant</TableCell>
            <TableCell>Status</TableCell>
            <TableCell>Email</TableCell>
            <TableCell>Created</TableCell>
            <TableCell align="right">Actions</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {tenants.map(tenant => (
            <TableRow hover key={tenant.tenantId}>
              <TableCell>
                <Typography variant="subtitle2">{tenant.name}</Typography>
                <Typography variant="caption" color="text.secondary">
                  {tenant.tenantId}
                </Typography>
              </TableCell>
              <TableCell>
                <TenantStatusBadge status={tenant.status} />
              </TableCell>
              <TableCell>{tenant.emailAddress || '—'}</TableCell>
              <TableCell>{formatDateTime(tenant.createdAt)}</TableCell>
              <TableCell align="right">
                <Box display="flex" justifyContent="flex-end" gap={1}>
                  <TenantActions
                    tenantId={tenant.tenantId}
                    status={tenant.status}
                    onCompleted={onActionCompleted}
                  />
                  <Tooltip title="Open detail view" arrow>
                    <IconButton size="small" onClick={() => onOpenTenant(tenant.tenantId)}>
                      <OpenInNewIcon fontSize="small" />
                    </IconButton>
                  </Tooltip>
                </Box>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  );
};
