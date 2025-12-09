import {Alert, Box, Breadcrumbs, Button, Container, Link, MenuItem, Pagination, Paper, Stack, TextField, Typography,} from '@mui/material';
import {ChangeEvent, useEffect, useState} from 'react';
import {Link as RouterLink, useLocation, useNavigate} from 'react-router-dom';
import {Header} from '../../../components/layout/Header';
import {useTenants} from '../hooks/useTenants';
import {Tenant} from '../types/tenant';
import {TenantList} from '../components/TenantList';

const statusOptions: (Tenant['status'] | 'ALL')[] = ['ALL', 'PENDING', 'ACTIVE', 'INACTIVE', 'SUSPENDED'];

export const TenantListPage = () => {
    const navigate = useNavigate();
    const location = useLocation();
    const {tenants, pagination, isLoading, error, updatePage, updateSearch, updateStatus, refetch} =
            useTenants({page: 1, size: 10});
    const [searchValue, setSearchValue] = useState('');
    const [statusValue, setStatusValue] = useState<'ALL' | Tenant['status']>('ALL');

    // Refresh the list when navigating to this page (e.g., from create/detail pages)
    useEffect(() => {
        // Only refetch if we're actually on the tenants list page
        if (location.pathname === '/admin/tenants') {
            refetch();
        }
    }, [location.pathname, refetch]);

    // Also refresh when the page becomes visible (user switches back to the tab)
    useEffect(() => {
        const handleVisibilityChange = () => {
            if (document.visibilityState === 'visible' && location.pathname === '/admin/tenants') {
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
        const next = event.target.value as 'ALL' | Tenant['status'];
        setStatusValue(next);
        updateStatus(next === 'ALL' ? undefined : next);
    };

    return (
            <>
                <Header/>
                <Container maxWidth="lg" sx={{py: 4}}>
                    <Breadcrumbs sx={{mb: 2}}>
                        <Link component={RouterLink} to="/dashboard">
                            Dashboard
                        </Link>
                        <Typography color="text.primary">Tenants</Typography>
                    </Breadcrumbs>
                    <Stack direction={{xs: 'column', md: 'row'}} justifyContent="space-between" alignItems="flex-start" mb={3}>
                        <Box>
                            <Typography variant="h4" gutterBottom>
                                Tenant Management
                            </Typography>
                            <Typography variant="body1" color="text.secondary">
                                Create, activate, and manage Local Distribution Partner tenants.
                            </Typography>
                        </Box>
                        <Button variant="contained" onClick={() => navigate('/admin/tenants/create')}>
                            Create Tenant
                        </Button>
                    </Stack>

                    <Paper sx={{p: 2, mb: 3}}>
                        <Stack direction={{xs: 'column', md: 'row'}} spacing={2}>
                            <TextField
                                    label="Search"
                                    placeholder="Search by ID or name"
                                    value={searchValue}
                                    onChange={handleSearchChange}
                                    fullWidth
                            />
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

                    <TenantList
                            tenants={tenants}
                            isLoading={isLoading}
                            onOpenTenant={(id) => navigate(`/admin/tenants/${id}`)}
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

