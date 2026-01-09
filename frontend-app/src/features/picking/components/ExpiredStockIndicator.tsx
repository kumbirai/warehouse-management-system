import { Alert, AlertTitle } from '@mui/material';
import { StockClassificationBadge } from '../../stock-management/components/StockClassificationBadge';
import { StockClassification } from '../../stock-management/types/stockManagement';

interface ExpiredStockIndicatorProps {
  classification: StockClassification;
  message?: string;
}

/**
 * ExpiredStockIndicator Component
 * <p>
 * Displays an alert when stock is expired or expiring soon.
 * Used in picking task execution to warn users about expiration status.
 */
export const ExpiredStockIndicator = ({ classification, message }: ExpiredStockIndicatorProps) => {
  if (classification === 'NORMAL' || classification === 'EXTENDED_SHELF_LIFE') {
    return null; // Don't show indicator for normal stock
  }

  const getAlertSeverity = (): 'error' | 'warning' | 'info' => {
    switch (classification) {
      case 'EXPIRED':
        return 'error';
      case 'CRITICAL':
        return 'error';
      case 'NEAR_EXPIRY':
        return 'warning';
      default:
        return 'info';
    }
  };

  const getDefaultMessage = (): string => {
    switch (classification) {
      case 'EXPIRED':
        return 'This stock has expired and cannot be picked.';
      case 'CRITICAL':
        return 'This stock is expiring within 7 days. Please prioritize picking.';
      case 'NEAR_EXPIRY':
        return 'This stock is expiring within 30 days.';
      default:
        return 'Stock expiration status: ' + classification;
    }
  };

  return (
    <Alert severity={getAlertSeverity()} sx={{ mb: 2 }}>
      <AlertTitle>
        <StockClassificationBadge classification={classification} />
      </AlertTitle>
      {message || getDefaultMessage()}
    </Alert>
  );
};
