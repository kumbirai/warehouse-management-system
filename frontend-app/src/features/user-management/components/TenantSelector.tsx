import {FormControl, InputLabel, MenuItem, Select, SelectChangeEvent} from '@mui/material';
import {useTenants} from '../../tenant-management/hooks/useTenants';
import {Tenant} from '../../tenant-management/types/tenant';

interface TenantSelectorProps {
    value?: string;
    onChange: (tenantId?: string) => void;
    label?: string;
    required?: boolean;
}

export const TenantSelector = ({
                                   value,
                                   onChange,
                                   label = 'Tenant',
                                   required = false,
                               }: TenantSelectorProps) => {
    const {tenants, isLoading} = useTenants({page: 1, size: 100, status: 'ACTIVE'});

    const handleChange = (event: SelectChangeEvent<string>) => {
        const selectedValue = event.target.value;
        onChange(selectedValue === 'ALL' ? undefined : selectedValue);
    };

    return (
            <FormControl fullWidth required={required}>
                <InputLabel id="tenant-select-label">{label}</InputLabel>
                <Select
                        labelId="tenant-select-label"
                        id="tenant-select"
                        value={value || 'ALL'}
                        label={label}
                        onChange={handleChange}
                        disabled={isLoading}
                >
                    <MenuItem value="ALL">All Tenants</MenuItem>
                    {tenants.map((tenant: Tenant) => (
                            <MenuItem key={tenant.tenantId} value={tenant.tenantId}>
                                {tenant.name} ({tenant.tenantId})
                            </MenuItem>
                    ))}
                </Select>
            </FormControl>
    );
};

