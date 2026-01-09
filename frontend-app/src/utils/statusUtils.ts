import {StatusVariant} from '../components/common';

/**
 * Utility function to get the status variant for a given status string
 */
export const getStatusVariant = (status: string): StatusVariant => {
  const statusLower = status.toLowerCase();

  if (statusLower === 'active' || statusLower === 'available' || statusLower === 'confirmed') {
    return 'success';
  }

  if (statusLower === 'pending' || statusLower === 'reserved' || statusLower === 'in_progress') {
    return 'warning';
  }

  if (
    statusLower === 'inactive' ||
    statusLower === 'suspended' ||
    statusLower === 'unavailable' ||
    statusLower === 'blocked'
  ) {
    return 'error';
  }

  return 'default';
};
