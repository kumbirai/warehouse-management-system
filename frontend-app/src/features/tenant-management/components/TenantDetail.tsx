import {Divider, Grid, Paper, Typography} from '@mui/material';
import {Tenant} from '../types/tenant';
import {TenantStatusBadge} from './TenantStatusBadge';

interface TenantDetailProps {
    tenant: Tenant;
}

export const TenantDetail = ({tenant}: TenantDetailProps) => (
        <Grid container spacing={3}>
            <Grid item xs={12} md={6}>
                <Paper elevation={1} sx={{p: 3}}>
                    <Typography variant="h6" gutterBottom>
                        Profile
                    </Typography>
                    <Divider sx={{mb: 2}}/>
                    <Typography variant="body2" color="text.secondary">
                        Tenant ID
                    </Typography>
                    <Typography variant="body1" gutterBottom>
                        {tenant.tenantId}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                        Name
                    </Typography>
                    <Typography variant="body1" gutterBottom>
                        {tenant.name}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                        Status
                    </Typography>
                    <TenantStatusBadge status={tenant.status}/>
                </Paper>
            </Grid>
            <Grid item xs={12} md={6}>
                <Paper elevation={1} sx={{p: 3}}>
                    <Typography variant="h6" gutterBottom>
                        Contact
                    </Typography>
                    <Divider sx={{mb: 2}}/>
                    <Typography variant="body2" color="text.secondary">
                        Email
                    </Typography>
                    <Typography variant="body1" gutterBottom>
                        {tenant.emailAddress || '—'}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                        Phone
                    </Typography>
                    <Typography variant="body1" gutterBottom>
                        {tenant.phone || '—'}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                        Address
                    </Typography>
                    <Typography variant="body1">{tenant.address || '—'}</Typography>
                </Paper>
            </Grid>
            <Grid item xs={12}>
                <Paper elevation={1} sx={{p: 3}}>
                    <Typography variant="h6" gutterBottom>
                        Configuration
                    </Typography>
                    <Divider sx={{mb: 2}}/>
                    <Typography variant="body2" color="text.secondary">
                        Keycloak Realm Strategy
                    </Typography>
                    <Typography variant="body1" gutterBottom>
                        {tenant.usePerTenantRealm ? 'Dedicated per-tenant realm' : 'Single shared realm'}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                        Keycloak Realm
                    </Typography>
                    <Typography variant="body1">
                        {tenant.keycloakRealmName || (tenant.usePerTenantRealm ? 'Pending provisioning' : 'wms-realm')}
                    </Typography>
                </Paper>
            </Grid>
        </Grid>
);

