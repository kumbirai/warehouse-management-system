import { Box, Button, Checkbox, FormControlLabel, Grid, Paper, Typography } from '@mui/material';
import { useEffect, useState } from 'react';
import { User } from '../types/user';
import { useUserRoles } from '../hooks/useUserRoles';

interface UserRoleManagerProps {
  user: User;
  onCancel: () => void;
}

const availableRoles = ['USER', 'PICKER', 'WAREHOUSE_MANAGER', 'TENANT_ADMIN', 'SYSTEM_ADMIN'];

export const UserRoleManager = ({ user, onCancel }: UserRoleManagerProps) => {
  const { roles, assignRole, removeRole, isLoading } = useUserRoles(user.userId);
  const [selectedRoles, setSelectedRoles] = useState<string[]>([]);

  useEffect(() => {
    setSelectedRoles(roles);
  }, [roles]);

  const handleRoleToggle = async (role: string) => {
    const isSelected = selectedRoles.includes(role);
    const newRoles = isSelected ? selectedRoles.filter(r => r !== role) : [...selectedRoles, role];

    setSelectedRoles(newRoles);

    // Immediately apply the change
    try {
      if (isSelected) {
        await removeRole(role);
      } else {
        await assignRole(role);
      }
    } catch (error) {
      // Revert on error
      setSelectedRoles(roles);
    }
  };

  return (
    <Paper elevation={2} sx={{ p: 3 }}>
      <Typography variant="h6" gutterBottom>
        Role Management
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
        Select roles to assign to this user. Changes are applied immediately.
      </Typography>

      <Grid container spacing={1}>
        {availableRoles.map(role => (
          <Grid item xs={12} sm={6} md={4} key={role}>
            <FormControlLabel
              control={
                <Checkbox
                  checked={selectedRoles.includes(role)}
                  onChange={() => handleRoleToggle(role)}
                  disabled={isLoading}
                />
              }
              label={role}
            />
          </Grid>
        ))}
      </Grid>

      <Box display="flex" justifyContent="flex-end" gap={2} mt={3}>
        <Button variant="outlined" onClick={onCancel} disabled={isLoading}>
          Close
        </Button>
      </Box>
    </Paper>
  );
};
