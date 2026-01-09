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

/**
 * Maps a status string to a StatusVariant for color coding.
 * @param status - The status string to map
 * @returns The corresponding StatusVariant
 */
export const getStatusVariant = (status: string): StatusVariant => {
  const statusLower = status.toLowerCase();
  
  // Active/available statuses
  if (['active', 'available', 'confirmed', 'completed', 'open'].includes(statusLower)) {
    return 'success';
  }
  
  // Pending/in-progress statuses
  if (['pending', 'reserved', 'in_progress', 'processing', 'in-progress'].includes(statusLower)) {
    return 'warning';
  }
  
  // Inactive/unavailable statuses
  if (['inactive', 'suspended', 'unavailable', 'closed', 'cancelled', 'canceled'].includes(statusLower)) {
    return 'error';
  }
  
  // Informational statuses
  if (['info', 'information', 'notice'].includes(statusLower)) {
    return 'info';
  }
  
  // Default for unknown statuses
  return 'default';
};

export const StatusBadge: React.FC<StatusBadgeProps> = ({
  label,
  variant = 'default',
  size = 'small',
}) => {
  return (
    <Chip label={label} color={variantColorMap[variant]} size={size} sx={{ fontWeight: 500 }} />
  );
};
