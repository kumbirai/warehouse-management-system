import { Box, Card, CardContent, Chip, Typography } from '@mui/material';
import { Link as RouterLink } from 'react-router-dom';
import { Notification } from '../types/notification';
import { Routes } from '../../../utils/navigationUtils';

interface NotificationItemProps {
  notification: Notification;
}

const getStatusColor = (
  status: Notification['status']
): 'default' | 'primary' | 'success' | 'error' => {
  switch (status) {
    case 'READ':
      return 'default';
    case 'DELIVERED':
      return 'primary';
    case 'SENT':
      return 'default';
    case 'FAILED':
      return 'error';
    default:
      return 'default';
  }
};

const getTypeLabel = (type: Notification['type']): string => {
  return type.replace(/_/g, ' ').replace(/\b\w/g, l => l.toUpperCase());
};

const formatTimeAgo = (dateString: string): string => {
  const date = new Date(dateString);
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffMins = Math.floor(diffMs / 60000);
  const diffHours = Math.floor(diffMs / 3600000);
  const diffDays = Math.floor(diffMs / 86400000);

  if (diffMins < 1) return 'just now';
  if (diffMins < 60) return `${diffMins} minute${diffMins > 1 ? 's' : ''} ago`;
  if (diffHours < 24) return `${diffHours} hour${diffHours > 1 ? 's' : ''} ago`;
  if (diffDays < 7) return `${diffDays} day${diffDays > 1 ? 's' : ''} ago`;
  return date.toLocaleDateString();
};

export const NotificationItem = ({ notification }: NotificationItemProps) => {
  const isUnread = notification.status === 'DELIVERED';
  const timeAgo = notification.createdAt ? formatTimeAgo(notification.createdAt) : '';

  return (
    <Card
      component={RouterLink}
      to={Routes.notificationDetail(notification.notificationId)}
      sx={{
        textDecoration: 'none',
        mb: 1,
        borderLeft: isUnread ? '4px solid' : 'none',
        borderColor: isUnread ? 'primary.main' : 'transparent',
        '&:hover': {
          bgcolor: 'action.hover',
        },
      }}
    >
      <CardContent>
        <Box
          sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 1 }}
        >
          <Typography variant="subtitle1" component="div" sx={{ fontWeight: isUnread ? 600 : 400 }}>
            {notification.title}
          </Typography>
          <Chip
            label={notification.status}
            size="small"
            color={getStatusColor(notification.status)}
            sx={{ ml: 1 }}
          />
        </Box>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
          {notification.message}
        </Typography>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Chip label={getTypeLabel(notification.type)} size="small" variant="outlined" />
          <Typography variant="caption" color="text.secondary">
            {timeAgo}
          </Typography>
        </Box>
      </CardContent>
    </Card>
  );
};
