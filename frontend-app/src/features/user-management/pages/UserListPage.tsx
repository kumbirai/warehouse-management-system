import {Alert, Box, Breadcrumbs, Button, Container, Link, MenuItem, Pagination, Paper, Stack, TextField, Typography,} from '@mui/material';
import {ChangeEvent, useEffect, useState} from 'react';
import {Link as RouterLink, useLocation, useNavigate} from 'react-router-dom';
import {Header} from '../../../components/layout/Header';
import {useUsers} from '../hooks/useUsers';
import {UserStatus} from '../types/user';
import {UserList} from '../components/UserList';
import {TenantSelector} from '../components/TenantSelector';
import {useAuth} from '../../../hooks/useAuth';

const statusOptions: (UserStatus | 'ALL')[] = ['ALL', 'ACTIVE', 'INACTIVE', 'SUSPENDED'];

export const UserListPage = () => {
    const navigate = useNavigate();
    const location = useLocation();
    const {isSystemAdmin, user: currentUser} = useAuth();
    const currentTenantId = currentUser?.tenantId || undefined;
    const {users, pagination, isLoading, error, updatePage, updateSearch, updateStatus, updateTenantId, refetch} =
            useUsers({page: 1, size: 10, tenantId: isSystemAdmin() ? undefined : currentTenantId});
    const [searchValue, setSearchValue] = useState('');
    const [statusValue, setStatusValue] = useState<'ALL' | UserStatus>('ALL');
    const [tenantValue, setTenantValue] = useState<string | undefined>(
            isSystemAdmin() ? undefined : currentTenantId
    );

    // Refresh the list when navigating to this page
    useEffect(() => {
        if (location.pathname === '/admin/users') {
            refetch();
        }
    }, [location.pathname, refetch]);

    // Also refresh when the page becomes visible
    useEffect(() => {
        const handleVisibilityChange = () => {
            if (document.visibilityState === 'visible' && location.pathname === '/admin/users') {
                refetch();
            }
        };

        document.addEventListener('visibilitychange', handleVisibilityChange);
        return () => {
            document.removeEventListener('visibilitychange', handleVisibilityChange);
        };
    }, [location.pathname, refetch]);

    const handleSearchChange = (event: ChangeEvent<HTMLInputElement>) => {
        setSearchValue(event.target.value);
        updateSearch(event.target.value || undefined);
    };

    const handleStatusChange = (event: ChangeEvent<HTMLInputElement>) => {
        const next = event.target.value as 'ALL' | UserStatus;
        setStatusValue(next);
        updateStatus(next === 'ALL' ? undefined : next);
    };

    const handleTenantChange = (tenantId?: string) => {
        const normalizedTenantId = tenantId || undefined;
        setTenantValue(normalizedTenantId);
        updateTenantId(normalizedTenantId);
    };

    return (
            <>
                <Header/>
                <Container maxWidth="lg" sx={{mt: 4, mb: 4}}>
                    <Breadcrumbs aria-label="breadcrumb" sx={{mb: 3}}>
                        <Link component={RouterLink} to="/dashboard" color="inherit">
                            Dashboard
                        </Link>
                        <Typography color="text.primary">User Management</Typography>
                    </Breadcrumbs>

                    <Stack direction={{xs: 'column', md: 'row'}} justifyContent="space-between" alignItems="flex-start" mb={3}>
                        <Box>
                            <Typography variant="h4" component="h1" gutterBottom>
                                User Management
                            </Typography>
                            <Typography variant="body1" color="text.secondary">
                                Manage user accounts, roles, and permissions across tenants.
                            </Typography>
                        </Box>
                        <Button
                                variant="contained"
                                onClick={() => navigate('/admin/users/create')}
                                sx={{mt: {xs: 2, md: 0}}}
                        >
                            Create User
                        </Button>
                    </Stack>

                    <Paper sx={{p: 2, mb: 3}}>
                        <Stack direction={{xs: 'column', md: 'row'}} spacing={2}>
                            <TextField
                                    label="Search"
                                    placeholder="Search by username or email"
                                    value={searchValue}
                                    onChange={handleSearchChange}
                                    fullWidth
                            />
                            {isSystemAdmin() && (
                                    <TenantSelector
                                            value={tenantValue}
                                            onChange={handleTenantChange}
                                            label="Filter by Tenant"
                                    />
                            )}
                            <TextField select label="Status" value={statusValue} onChange={handleStatusChange} sx={{minWidth: 180}}>
                                {statusOptions.map((status) => (
                                        <MenuItem key={status} value={status}>
                                            {status}
                                        </MenuItem>
                                ))}
                            </TextField>
                        </Stack>
                    </Paper>

                    {error && (
                            <Alert severity="error" sx={{mb: 2}}>
                                {error}
                            </Alert>
                    )}

                    <UserList
                            users={users}
                            isLoading={isLoading}
                            onOpenUser={(id) => navigate(`/admin/users/${id}`)}
                            onActionCompleted={refetch}
                    />

                    <Box display="flex" justifyContent="center" mt={3}>
                        <Pagination
                                count={pagination?.totalPages || 1}
                                page={pagination?.page || 1}
                                onChange={(_, page) => updatePage(page)}
                                color="primary"
                        />
                    </Box>
                </Container>
            </>
    );
};
