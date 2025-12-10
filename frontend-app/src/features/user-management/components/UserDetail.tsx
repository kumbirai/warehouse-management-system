import { Divider, Grid, Paper, Typography } from '@mui/material';
import { User } from '../types/user';
import { UserStatusBadge } from './UserStatusBadge';

interface UserDetailProps {
  user: User;
}

export const UserDetail = ({ user }: UserDetailProps) => (
  <Grid container spacing={3}>
    <Grid item xs={12} md={6}>
      <Paper elevation={1} sx={{ p: 3 }}>
        <Typography variant="h6" gutterBottom>
          Basic Information
        </Typography>
        <Divider sx={{ mb: 2 }} />
        <Typography variant="body2" color="text.secondary">
          Username
        </Typography>
        <Typography variant="body1" gutterBottom>
          {user.username}
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Email
        </Typography>
        <Typography variant="body1" gutterBottom>
          {user.emailAddress || '—'}
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Full Name
        </Typography>
        <Typography variant="body1" gutterBottom>
          {user.firstName && user.lastName
            ? `${user.firstName} ${user.lastName}`
            : user.firstName || user.lastName || '—'}
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Status
        </Typography>
        <UserStatusBadge status={user.status} />
      </Paper>
    </Grid>
    <Grid item xs={12} md={6}>
      <Paper elevation={1} sx={{ p: 3 }}>
        <Typography variant="h6" gutterBottom>
          Tenant Information
        </Typography>
        <Divider sx={{ mb: 2 }} />
        <Typography variant="body2" color="text.secondary">
          Tenant
        </Typography>
        <Typography variant="body1" gutterBottom>
          {user.tenantName || user.tenantId}
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Tenant ID
        </Typography>
        <Typography variant="body1" gutterBottom>
          {user.tenantId}
        </Typography>
      </Paper>
    </Grid>
    <Grid item xs={12}>
      <Paper elevation={1} sx={{ p: 3 }}>
        <Typography variant="h6" gutterBottom>
          Roles & Permissions
        </Typography>
        <Divider sx={{ mb: 2 }} />
        <Typography variant="body2" color="text.secondary">
          Assigned Roles
        </Typography>
        <Typography variant="body1" gutterBottom>
          {user.roles.length > 0 ? user.roles.join(', ') : 'No roles assigned'}
        </Typography>
      </Paper>
    </Grid>
    <Grid item xs={12}>
      <Paper elevation={1} sx={{ p: 3 }}>
        <Typography variant="h6" gutterBottom>
          Account Information
        </Typography>
        <Divider sx={{ mb: 2 }} />
        <Typography variant="body2" color="text.secondary">
          User ID
        </Typography>
        <Typography variant="body1" gutterBottom>
          {user.userId}
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Created At
        </Typography>
        <Typography variant="body1" gutterBottom>
          {new Date(user.createdAt).toLocaleString(undefined, {
            year: 'numeric',
            month: 'long',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit',
          })}
        </Typography>
        {user.lastModifiedAt && (
          <>
            <Typography variant="body2" color="text.secondary">
              Last Modified
            </Typography>
            <Typography variant="body1">
              {new Date(user.lastModifiedAt).toLocaleString(undefined, {
                year: 'numeric',
                month: 'long',
                day: 'numeric',
                hour: '2-digit',
                minute: '2-digit',
              })}
            </Typography>
          </>
        )}
      </Paper>
    </Grid>
  </Grid>
);
