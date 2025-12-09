import {Chip} from '@mui/material';
import {UserStatus} from '../types/user';

const statusColorMap: Record<UserStatus, 'default' | 'success' | 'warning' | 'error' | 'info'> = {
    ACTIVE: 'success',
    INACTIVE: 'default',
    SUSPENDED: 'error',
};

interface UserStatusBadgeProps {
    status: UserStatus;
}

export const UserStatusBadge = ({status}: UserStatusBadgeProps) => (
        <Chip label={status} color={statusColorMap[status]} size="small"/>
);

