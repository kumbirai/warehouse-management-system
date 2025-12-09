import {Chip} from '@mui/material';
import {TenantStatus} from '../types/tenant';

const statusColorMap: Record<TenantStatus, 'default' | 'success' | 'warning' | 'error' | 'info'> = {
    PENDING: 'warning',
    ACTIVE: 'success',
    INACTIVE: 'default',
    SUSPENDED: 'error',
};

interface TenantStatusBadgeProps {
    status: TenantStatus;
}

export const TenantStatusBadge = ({status}: TenantStatusBadgeProps) => (
        <Chip label={status} color={statusColorMap[status]} size="small"/>
);

