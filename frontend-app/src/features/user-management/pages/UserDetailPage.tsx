import {Alert, Box, Breadcrumbs, Button, CircularProgress, Container, Link, Stack, Tab, Tabs, Typography,} from '@mui/material';
import {useState} from 'react';
import {Link as RouterLink, useNavigate, useParams} from 'react-router-dom';
import {Header} from '../../../components/layout/Header';
import {useUser} from '../hooks/useUser';
import {useUpdateUserProfile} from '../hooks/useUpdateUserProfile';
import {UserDetail} from '../components/UserDetail';
import {UserProfileEditor} from '../components/UserProfileEditor';
import {UserRoleManager} from '../components/UserRoleManager';
import {UserActions} from '../components/UserActions';
import {UpdateUserProfileRequest} from '../types/user';
import {useAuth} from '../../../hooks/useAuth';

type TabValue = 'details' | 'profile' | 'roles';

export const UserDetailPage = () => {
    const {userId} = useParams<{ userId: string }>();
    const navigate = useNavigate();
    const {user, isLoading, error, refetch} = useUser(userId);
    const {updateProfile, isLoading: isUpdatingProfile} = useUpdateUserProfile(userId || '');
    const {isSystemAdmin, isTenantAdmin} = useAuth();
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

    return (
            <>
                <Header/>
                <Container maxWidth="lg" sx={{py: 4}}>
                    <Breadcrumbs sx={{mb: 2}}>
                        <Link component={RouterLink} to="/dashboard">
                            Dashboard
                        </Link>
                        <Link component={RouterLink} to="/admin/users">
                            Users
                        </Link>
                        <Typography color="text.primary">
                            {user ? (user.firstName && user.lastName ? `${user.firstName} ${user.lastName}` : user.username) : userId}
                        </Typography>
                    </Breadcrumbs>

                    {isLoading && (
                            <Box display="flex" justifyContent="center" alignItems="center" py={4}>
                                <CircularProgress/>
                            </Box>
                    )}

                    {error && (
                            <Alert severity="error" sx={{mb: 2}}>
                                {error}
                            </Alert>
                    )}

                    {user && (
                            <Stack spacing={3}>
                                <Stack direction={{xs: 'column', md: 'row'}} justifyContent="space-between" alignItems="flex-start">
                                    <Box>
                                        <Typography variant="h4">
                                            {user.firstName && user.lastName
                                                    ? `${user.firstName} ${user.lastName}`
                                                    : user.username}
                                        </Typography>
                                        <Typography variant="body1" color="text.secondary">
                                            {user.username} • {user.emailAddress}
                                        </Typography>
                                    </Box>
                                    <Stack direction={{xs: 'column', sm: 'row'}} spacing={1}>
                                        {canEdit && (
                                                <UserActions userId={user.userId} status={user.status} onCompleted={refetch}/>
                                        )}
                                        <Button variant="outlined" onClick={() => navigate('/admin/users')}>
                                            Back to list
                                        </Button>
                                    </Stack>
                                </Stack>

                                <Tabs value={activeTab} onChange={(_, newValue) => setActiveTab(newValue)}>
                                    <Tab label="Details" value="details"/>
                                    {canEdit && <Tab label="Profile" value="profile"/>}
                                    {canEdit && <Tab label="Roles" value="roles"/>}
                                </Tabs>

                                {activeTab === 'details' && <UserDetail user={user}/>}

                                {activeTab === 'profile' && canEdit && (
                                        <Box>
                                            {!isEditingProfile ? (
                                                    <Stack spacing={2}>
                                                        <UserDetail user={user}/>
                                                        <Button variant="contained" onClick={() => setIsEditingProfile(true)}>
                                                            Edit Profile
                                                        </Button>
                                                    </Stack>
                                            ) : (
                                                    <UserProfileEditor
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
                                                        <UserDetail user={user}/>
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
                </Container>
            </>
    );
};

