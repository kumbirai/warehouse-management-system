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
import { User } from '../types/user';
import { UserStatusBadge } from './UserStatusBadge';
import { UserActions } from './UserActions';

interface UserListProps {
  users: User[];
  isLoading?: boolean;
  onOpenUser: (userId: string) => void;
  onActionCompleted: () => void;
}

export const UserList = ({
  users,
  isLoading = false,
  onOpenUser,
  onActionCompleted,
}: UserListProps) => {
  if (isLoading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" py={4}>
        <CircularProgress />
      </Box>
    );
  }

  if (!users.length) {
    return (
      <Box py={4}>
        <Typography variant="body2" color="text.secondary" align="center">
          No users found. Try adjusting filters or create a new user.
        </Typography>
      </Box>
    );
  }

  return (
    <TableContainer component={Paper} elevation={2}>
      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell>User</TableCell>
            <TableCell>Email</TableCell>
            <TableCell>Tenant</TableCell>
            <TableCell>Status</TableCell>
            <TableCell>Roles</TableCell>
            <TableCell>Created</TableCell>
            <TableCell align="right">Actions</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {users.map(user => (
            <TableRow hover key={user.userId}>
              <TableCell>
                <Typography variant="subtitle2">
                  {user.firstName && user.lastName
                    ? `${user.firstName} ${user.lastName}`
                    : user.username}
                </Typography>
                <Typography variant="caption" color="text.secondary">
                  {user.username}
                </Typography>
              </TableCell>
              <TableCell>{user.emailAddress || '—'}</TableCell>
              <TableCell>
                <Typography variant="body2">{user.tenantName || user.tenantId}</Typography>
              </TableCell>
              <TableCell>
                <UserStatusBadge status={user.status} />
              </TableCell>
              <TableCell>
                <Typography variant="body2">
                  {user.roles.length > 0 ? user.roles.join(', ') : '—'}
                </Typography>
              </TableCell>
              <TableCell>
                {new Date(user.createdAt).toLocaleString(undefined, {
                  year: 'numeric',
                  month: 'short',
                  day: 'numeric',
                  hour: '2-digit',
                  minute: '2-digit',
                })}
              </TableCell>
              <TableCell align="right">
                <Box display="flex" justifyContent="flex-end" gap={1}>
                  <UserActions
                    userId={user.userId}
                    status={user.status}
                    onCompleted={onActionCompleted}
                  />
                  <Tooltip title="Open detail view" arrow>
                    <IconButton size="small" onClick={() => onOpenUser(user.userId)}>
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
