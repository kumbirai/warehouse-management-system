import { Chip, ChipProps } from '@mui/material';
import { StockClassification } from '../types/stockManagement';

interface StockClassificationBadgeProps {
  classification: StockClassification;
  size?: ChipProps['size'];
}

/**
 * StockClassificationBadge Component
 * <p>
 * Displays a color-coded badge for stock classification based on expiration dates.
 * <p>
 * Color scheme:
 * - EXPIRED: Red (error)
 * - CRITICAL: Orange (warning) - Expiring within 7 days
 * - NEAR_EXPIRY: Yellow (warning) - Expiring within 30 days
 * - NORMAL: Green (success)
 * - EXTENDED_SHELF_LIFE: Blue (info) - Expiring after 365 days
 */
export const StockClassificationBadge = ({
  classification,
  size = 'small',
}: StockClassificationBadgeProps) => {
  const getClassificationConfig = (): {
    label: string;
    color: ChipProps['color'];
    variant: ChipProps['variant'];
  } => {
    switch (classification) {
      case 'EXPIRED':
        return {
          label: 'Expired',
          color: 'error',
          variant: 'filled' as const,
        };
      case 'CRITICAL':
        return {
          label: 'Critical - Expiring within 7 days',
          color: 'warning',
          variant: 'filled' as const,
        };
      case 'NEAR_EXPIRY':
        return {
          label: 'Near Expiry - Expiring within 30 days',
          color: 'warning',
          variant: 'outlined' as const,
        };
      case 'NORMAL':
        return {
          label: 'Normal',
          color: 'success',
          variant: 'filled' as const,
        };
      case 'EXTENDED_SHELF_LIFE':
        return {
          label: 'Extended Shelf Life',
          color: 'info',
          variant: 'filled' as const,
        };
      default:
        return {
          label: classification,
          color: 'default',
          variant: 'outlined' as const,
        };
    }
  };

  const config = getClassificationConfig();

  return <Chip label={config.label} color={config.color} variant={config.variant} size={size} />;
};
