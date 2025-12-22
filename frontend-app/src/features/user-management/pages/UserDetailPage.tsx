import { Box, Button, Stack, Tab, Tabs } from '@mui/material';
import { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useUser } from '../hooks/useUser';
import { useUpdateUserProfile } from '../hooks/useUpdateUserProfile';
import { UserDetail } from '../components/UserDetail';
import { UserProfileEditor } from '../components/UserProfileEditor';
import { UserRoleManager } from '../components/UserRoleManager';
import { UserActions } from '../components/UserActions';
import { UpdateUserProfileRequest } from '../types/user';
import { useAuth } from '../../../hooks/useAuth';
import { DetailPageLayout } from '../../../components/layouts';
import { getBreadcrumbs, Routes } from '../../../utils/navigationUtils';

type TabValue = 'details' | 'profile' | 'roles';

export const UserDetailPage = () => {
  const { userId } = useParams<{ userId: string }>();
  const navigate = useNavigate();
  const { user, isLoading, error, refetch } = useUser(userId);
  const { updateProfile, isLoading: isUpdatingProfile } = useUpdateUserProfile(userId || '');
  const { isSystemAdmin, isTenantAdmin } = useAuth();
  const [activeTab, setActiveTab] = useState<TabValue>('details');
  const [isEditingProfile, setIsEditingProfile] = useState(false);
  const [isManagingRoles, setIsManagingRoles] = useState(false);

  const canEdit = isSystemAdmin() || isTenantAdmin();

  const handleProfileSubmit = async (values: UpdateUserProfileRequest) => {
    try {
      await updateProfile(values);
      setIsEditingProfile(false);
      refetch();
    } catch {
      // Error is handled by hook
    }
  };

  if (!userId) {
    navigate(Routes.admin.users);
    return null;
  }

  const userDisplayName = user
    ? user.firstName && user.lastName
      ? `${user.firstName} ${user.lastName}`
      : user.username
    : userId || '...';

  return (
    <DetailPageLayout
      breadcrumbs={getBreadcrumbs.userDetail(userDisplayName)}
      title={userDisplayName}
      actions={
        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1}>
          {user && canEdit && (
            <UserActions userId={user.userId} status={user.status} onCompleted={refetch} />
          )}
          <Button variant="outlined" onClick={() => navigate(Routes.admin.users)}>
            Back to List
          </Button>
        </Stack>
      }
      isLoading={isLoading}
      error={error}
      maxWidth="lg"
    >
      {user && (
        <Stack spacing={3}>
          <Tabs value={activeTab} onChange={(_, newValue) => setActiveTab(newValue)}>
            <Tab label="Details" value="details" />
            {canEdit && <Tab label="Profile" value="profile" />}
            {canEdit && <Tab label="Roles" value="roles" />}
          </Tabs>

          {activeTab === 'details' && <UserDetail user={user} />}

          {activeTab === 'profile' && canEdit && (
            <Box>
              {!isEditingProfile ? (
                <Stack spacing={2}>
                  <UserDetail user={user} />
                  <Button variant="contained" onClick={() => setIsEditingProfile(true)}>
                    Edit Profile
                  </Button>
                </Stack>
              ) : (
                <UserProfileEditor
                  key={user.userId}
                  user={user}
                  onSubmit={handleProfileSubmit}
                  onCancel={() => setIsEditingProfile(false)}
                  isLoading={isUpdatingProfile}
                />
              )}
            </Box>
          )}

          {activeTab === 'roles' && canEdit && (
            <Box>
              {!isManagingRoles ? (
                <Stack spacing={2}>
                  <UserDetail user={user} />
                  <Button variant="contained" onClick={() => setIsManagingRoles(true)}>
                    Manage Roles
                  </Button>
                </Stack>
              ) : (
                <UserRoleManager
                  user={user}
                  onCancel={() => {
                    setIsManagingRoles(false);
                    refetch();
                  }}
                />
              )}
            </Box>
          )}
        </Stack>
      )}
    </DetailPageLayout>
  );
};
