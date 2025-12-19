import {
  Alert,
  Box,
  Button,
  Checkbox,
  Divider,
  FormControlLabel,
  Grid,
  Paper,
  Tooltip,
  Typography,
} from '@mui/material';
import { useEffect, useState } from 'react';
import { User } from '../types/user';
import { useUserRoles } from '../hooks/useUserRoles';
import { useAuth } from '../../../hooks/useAuth';
import {
  LOCATION_MANAGER,
  OPERATOR,
  PICKER,
  RECONCILIATION_CLERK,
  RECONCILIATION_MANAGER,
  RETURNS_CLERK,
  RETURNS_MANAGER,
  STOCK_CLERK,
  STOCK_MANAGER,
  SYSTEM_ADMIN,
  TENANT_ADMIN,
  USER,
  VIEWER,
  WAREHOUSE_MANAGER,
} from '../../../constants/roles';
import { canAssignRole, canRemoveRole, getRoleAssignmentReason } from '../utils/rolePermissions';

interface UserRoleManagerProps {
  user: User;
  onCancel: () => void;
}

interface RoleGroup {
  title: string;
  roles: string[];
}

export const UserRoleManager = ({ user, onCancel }: UserRoleManagerProps) => {
  const { roles, assignRole, removeRole, isLoading } = useUserRoles(user.userId);
  const { user: currentUser } = useAuth();
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

  const canAssign = (role: string): boolean => {
    if (!currentUser?.roles) return false;
    return canAssignRole(currentUser.roles, role, user.tenantId, currentUser.tenantId ?? undefined);
  };

  const canRemove = (role: string): boolean => {
    if (!currentUser?.roles) return false;
    return canRemoveRole(currentUser.roles, role, user.tenantId, currentUser.tenantId ?? undefined);
  };

  const isDisabled = (role: string, isSelected: boolean): boolean => {
    if (isLoading) return true;
    if (isSelected) {
      return !canRemove(role);
    }
    return !canAssign(role);
  };

  const getTooltipText = (role: string, isSelected: boolean): string => {
    if (isSelected && !canRemove(role)) {
      if (role === USER) {
        return 'USER is the base role and cannot be removed';
      }
      return (
        getRoleAssignmentReason(
          currentUser?.roles || [],
          role,
          user.tenantId,
          currentUser?.tenantId ?? undefined
        ) || 'Cannot remove this role'
      );
    }
    if (!isSelected && !canAssign(role)) {
      return (
        getRoleAssignmentReason(
          currentUser?.roles || [],
          role,
          user.tenantId,
          currentUser?.tenantId ?? undefined
        ) || 'Cannot assign this role'
      );
    }
    return '';
  };

  const roleGroups: RoleGroup[] = [
    {
      title: 'System-Level Roles',
      roles: [SYSTEM_ADMIN],
    },
    {
      title: 'Tenant-Level Administrative Roles',
      roles: [TENANT_ADMIN, WAREHOUSE_MANAGER],
    },
    {
      title: 'Specialized Manager Roles',
      roles: [STOCK_MANAGER, LOCATION_MANAGER, RECONCILIATION_MANAGER, RETURNS_MANAGER],
    },
    {
      title: 'Operational Roles',
      roles: [OPERATOR, PICKER, STOCK_CLERK, RECONCILIATION_CLERK, RETURNS_CLERK],
    },
    {
      title: 'Access Roles',
      roles: [VIEWER, USER],
    },
  ];

  // Check if current user can manage roles at all
  const canManageRoles = currentUser?.roles?.some(role =>
    [
      SYSTEM_ADMIN,
      TENANT_ADMIN,
      WAREHOUSE_MANAGER,
      ...Object.keys({
        [STOCK_MANAGER]: true,
        [LOCATION_MANAGER]: true,
        [RECONCILIATION_MANAGER]: true,
        [RETURNS_MANAGER]: true,
      }),
    ].includes(role)
  );

  if (!canManageRoles) {
    return (
      <Paper elevation={2} sx={{ p: 3 }}>
        <Alert severity="warning">You do not have permission to manage user roles.</Alert>
        <Box display="flex" justifyContent="flex-end" gap={2} mt={3}>
          <Button variant="outlined" onClick={onCancel}>
            Close
          </Button>
        </Box>
      </Paper>
    );
  }

  return (
    <Paper elevation={2} sx={{ p: 3 }}>
      <Typography variant="h6" gutterBottom>
        Role Management
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
        Select roles to assign to this user. Changes are applied immediately.
      </Typography>

      {user.tenantId && currentUser?.tenantId && user.tenantId !== currentUser.tenantId && (
        <Alert severity="info" sx={{ mb: 2 }}>
          You are managing roles for a user in a different tenant. Some roles may be restricted.
        </Alert>
      )}

      {roleGroups.map((group, groupIndex) => (
        <Box key={group.title} sx={{ mb: groupIndex < roleGroups.length - 1 ? 3 : 0 }}>
          <Typography variant="subtitle1" fontWeight="bold" sx={{ mb: 1.5 }}>
            {group.title}
          </Typography>
          <Grid container spacing={1}>
            {group.roles.map(role => {
              const isSelected = selectedRoles.includes(role);
              const disabled = isDisabled(role, isSelected);
              const tooltipText = getTooltipText(role, isSelected);

              const checkbox = (
                <FormControlLabel
                  control={
                    <Checkbox
                      checked={isSelected}
                      onChange={() => handleRoleToggle(role)}
                      disabled={disabled}
                      color={role === USER ? 'default' : 'primary'}
                    />
                  }
                  label={
                    <Box>
                      <Typography variant="body2">{role}</Typography>
                      {role === USER && (
                        <Typography variant="caption" color="text.secondary" display="block">
                          Base Role
                        </Typography>
                      )}
                    </Box>
                  }
                />
              );

              return (
                <Grid item xs={12} sm={6} md={4} key={role}>
                  {tooltipText ? (
                    <Tooltip title={tooltipText} arrow>
                      <span>{checkbox}</span>
                    </Tooltip>
                  ) : (
                    checkbox
                  )}
                </Grid>
              );
            })}
          </Grid>
          {groupIndex < roleGroups.length - 1 && <Divider sx={{ mt: 2 }} />}
        </Box>
      ))}

      <Box display="flex" justifyContent="flex-end" gap={2} mt={3}>
        <Button variant="outlined" onClick={onCancel} disabled={isLoading}>
          Close
        </Button>
      </Box>
    </Paper>
  );
};
