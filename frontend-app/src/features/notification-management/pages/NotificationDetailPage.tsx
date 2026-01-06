import { useParams, useNavigate } from 'react-router-dom';
import { Box, Button, Card, CardContent, Chip, Typography } from '@mui/material';
import { ArrowBack as ArrowBackIcon } from '@mui/icons-material';
import { useNotification } from '../hooks/useNotification';
import { useMarkAsRead } from '../hooks/useMarkAsRead';
import { DetailPageLayout } from '../../../components/layouts/DetailPageLayout';
import { getBreadcrumbs, Routes } from '../../../utils/navigationUtils';
import { LoadingSpinner } from '../../../components/common/LoadingSpinner';

const getStatusColor = (status: string): 'default' | 'primary' | 'success' | 'error' => {
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

const getTypeLabel = (type: string): string => {
  return type.replace(/_/g, ' ').replace(/\b\w/g, (l) => l.toUpperCase());
};

const formatDateTime = (dateString?: string): string => {
  if (!dateString) return 'N/A';
  return new Date(dateString).toLocaleString();
};

export const NotificationDetailPage = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { data: notificationResponse, isLoading, error } = useNotification(id || null);
  const markAsReadMutation = useMarkAsRead();

  const notification = notificationResponse?.data;

  const handleMarkAsRead = async () => {
    if (id && notification?.status === 'DELIVERED') {
      await markAsReadMutation.mutateAsync(id);
    }
  };

  if (isLoading) {
    return (
      <DetailPageLayout
        breadcrumbs={getBreadcrumbs.notificationDetail('')}
        title="Notification"
        isLoading={true}
        error={null}
      >
        <LoadingSpinner />
      </DetailPageLayout>
    );
  }

  if (error || !notification) {
    return (
      <DetailPageLayout
        breadcrumbs={getBreadcrumbs.notificationDetail('')}
        title="Notification"
        isLoading={false}
        error={error?.message || 'Notification not found'}
      >
        <Button startIcon={<ArrowBackIcon />} onClick={() => navigate(Routes.notifications)}>
          Back to Notifications
        </Button>
      </DetailPageLayout>
    );
  }

  const isUnread = notification.status === 'DELIVERED';

  return (
    <DetailPageLayout
      breadcrumbs={getBreadcrumbs.notificationDetail(notification.title)}
      title="Notification Details"
      isLoading={false}
      error={null}
      actions={
        <Box
          sx={{
            display: 'flex',
            flexDirection: { xs: 'column', sm: 'row' },
            gap: 1,
            width: { xs: '100%', sm: 'auto' },
          }}
        >
          {isUnread && (
            <Button
              variant="contained"
              onClick={handleMarkAsRead}
              disabled={markAsReadMutation.isPending}
              sx={{ width: { xs: '100%', sm: 'auto' } }}
              aria-label="Mark notification as read"
            >
              Mark as Read
            </Button>
          )}
          <Button
            startIcon={<ArrowBackIcon />}
            onClick={() => navigate(Routes.notifications)}
            sx={{ width: { xs: '100%', sm: 'auto' } }}
            aria-label="Back to notifications"
          >
            Back
          </Button>
        </Box>
      }
    >
      <Card>
        <CardContent>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 2 }}>
            <Typography variant="h5" component="h2" sx={{ fontWeight: isUnread ? 600 : 400 }}>
              {notification.title}
            </Typography>
            <Chip
              label={notification.status}
              color={getStatusColor(notification.status)}
              sx={{ ml: 2 }}
            />
          </Box>

          <Typography variant="body1" sx={{ mb: 3, whiteSpace: 'pre-wrap' }}>
            {notification.message}
          </Typography>

          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
            <Box sx={{ display: 'flex', gap: 2 }}>
              <Typography variant="body2" color="text.secondary" sx={{ minWidth: 150 }}>
                Type:
              </Typography>
              <Chip label={getTypeLabel(notification.type)} size="small" variant="outlined" />
            </Box>

            <Box sx={{ display: 'flex', gap: 2 }}>
              <Typography variant="body2" color="text.secondary" sx={{ minWidth: 150 }}>
                Created:
              </Typography>
              <Typography variant="body2">{formatDateTime(notification.createdAt)}</Typography>
            </Box>

            {notification.sentAt && (
              <Box sx={{ display: 'flex', gap: 2 }}>
                <Typography variant="body2" color="text.secondary" sx={{ minWidth: 150 }}>
                  Sent:
                </Typography>
                <Typography variant="body2">{formatDateTime(notification.sentAt)}</Typography>
              </Box>
            )}

            {notification.readAt && (
              <Box sx={{ display: 'flex', gap: 2 }}>
                <Typography variant="body2" color="text.secondary" sx={{ minWidth: 150 }}>
                  Read:
                </Typography>
                <Typography variant="body2">{formatDateTime(notification.readAt)}</Typography>
              </Box>
            )}
          </Box>
        </CardContent>
      </Card>
    </DetailPageLayout>
  );
};
