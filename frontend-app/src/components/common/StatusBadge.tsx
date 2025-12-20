import React from 'react';
import { Chip, ChipProps } from '@mui/material';

export type StatusVariant = 'success' | 'warning' | 'error' | 'info' | 'default';

interface StatusBadgeProps {
  label: string;
  variant?: StatusVariant;
  size?: ChipProps['size'];
}

const variantColorMap: Record<StatusVariant, ChipProps['color']> = {
  success: 'success',
  warning: 'warning',
  error: 'error',
  info: 'info',
  default: 'default',
};

export const StatusBadge: React.FC<StatusBadgeProps> = ({
  label,
  variant = 'default',
  size = 'small',
}) => {
  return (
    <Chip
      label={label}
      color={variantColorMap[variant]}
      size={size}
      sx={{ fontWeight: 500 }}
    />
  );
};

export const getStatusVariant = (status: string): StatusVariant => {
  const statusLower = status.toLowerCase();

  if (statusLower === 'active' || statusLower === 'available' || statusLower === 'confirmed') {
    return 'success';
  }

  if (statusLower === 'pending' || statusLower === 'reserved' || statusLower === 'in_progress') {
    return 'warning';
  }

  if (statusLower === 'inactive' || statusLower === 'suspended' || statusLower === 'unavailable' || statusLower === 'blocked') {
    return 'error';
  }

  return 'default';
};
