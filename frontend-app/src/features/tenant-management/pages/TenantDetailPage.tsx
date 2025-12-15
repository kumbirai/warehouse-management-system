import {
  Alert,
  Box,
  Breadcrumbs,
  Button,
  CircularProgress,
  Container,
  Link,
  Stack,
  Typography,
} from '@mui/material';
import { Link as RouterLink, useNavigate, useParams } from 'react-router-dom';
import { Header } from '../../../components/layout/Header';
import { useTenant } from '../hooks/useTenant';
import { TenantDetail } from '../components/TenantDetail';
import { TenantActions } from '../components/TenantActions';

export const TenantDetailPage = () => {
  const { tenantId } = useParams<{ tenantId: string }>();
  const navigate = useNavigate();
  const { tenant, isLoading, error, refetch } = useTenant(tenantId);

  return (
    <>
      <Header />
      <Container maxWidth="lg" sx={{ py: 4 }}>
        <Breadcrumbs sx={{ mb: 2 }}>
          <Link component={RouterLink} to="/dashboard">
            Dashboard
          </Link>
          <Link component={RouterLink} to="/admin/tenants">
            Tenants
          </Link>
          <Typography color="text.primary">{tenant?.name || tenantId}</Typography>
        </Breadcrumbs>

        {isLoading && (
          <Box display="flex" justifyContent="center" alignItems="center" py={4}>
            <CircularProgress />
          </Box>
        )}

        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}

        {tenant && (
          <Stack spacing={3}>
            <Stack
              direction={{ xs: 'column', md: 'row' }}
              justifyContent="space-between"
              alignItems="flex-start"
            >
              <Box>
                <Typography variant="h4">{tenant.name}</Typography>
                <Typography variant="body1" color="text.secondary">
                  Tenant ID: {tenant.tenantId}
                </Typography>
              </Box>
              <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1}>
                <TenantActions
                  tenantId={tenant.tenantId}
                  status={tenant.status}
                  onCompleted={refetch}
                />
                <Button variant="outlined" onClick={() => navigate('/admin/tenants')}>
                  Back to list
                </Button>
              </Stack>
            </Stack>

            <TenantDetail tenant={tenant} />
          </Stack>
        )}
      </Container>
    </>
  );
};
