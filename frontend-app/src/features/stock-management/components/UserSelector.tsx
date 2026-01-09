import { FormControl, InputLabel, MenuItem, Select, SelectChangeEvent } from '@mui/material';
import { useUsers } from '../../user-management/hooks/useUsers';
import { useAppSelector } from '../../../store/hooks';
import { selectUser } from '../../../store/authSlice';
import { User } from '../../user-management/types/user';

interface UserSelectorProps {
  value?: string;
  onChange: (userId?: string) => void;
  label?: string;
  required?: boolean;
  error?: boolean;
  helperText?: string;
}

export const UserSelector = ({
  value,
  onChange,
  label = 'Received By',
  required = false,
  error = false,
  helperText,
}: UserSelectorProps) => {
  const currentUser = useAppSelector(selectUser);
  const tenantId = currentUser?.tenantId;

  // Fetch users for the current tenant
  const { users, isLoading } = useUsers({
    page: 1,
    size: 100, // Fetch enough users
    tenantId: tenantId || undefined,
    status: 'ACTIVE',
  });

  const handleChange = (event: SelectChangeEvent<string>) => {
    const selectedValue = event.target.value;
    onChange(selectedValue || undefined);
  };

  const getUserDisplayName = (user: User): string => {
    if (user.firstName || user.lastName) {
      return `${user.firstName || ''} ${user.lastName || ''}`.trim();
    }
    return user.username || user.userId;
  };

  // Only use the value if it exists in the available options to prevent MUI Select errors
  const validValue = value && users.some(user => user.userId === value) ? value : '';

  return (
    <FormControl fullWidth required={required} error={error}>
      <InputLabel id="user-select-label">{label}</InputLabel>
      <Select
        labelId="user-select-label"
        id="user-select"
        value={validValue}
        label={label}
        onChange={handleChange}
        disabled={isLoading || !tenantId}
      >
        {users.length === 0 && !isLoading ? (
          <MenuItem value="" disabled>
            No users available
          </MenuItem>
        ) : (
          users.map(user => (
            <MenuItem key={user.userId} value={user.userId}>
              {getUserDisplayName(user)} ({user.username})
            </MenuItem>
          ))
        )}
      </Select>
      {helperText && (
        <span
          style={{
            fontSize: '0.75rem',
            marginTop: '3px',
            marginLeft: '14px',
            color: error ? '#d32f2f' : 'rgba(0, 0, 0, 0.6)',
          }}
        >
          {helperText}
        </span>
      )}
    </FormControl>
  );
};
