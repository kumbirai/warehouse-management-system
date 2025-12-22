import {
  Alert,
  Button,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Typography,
} from '@mui/material';
import { Consignment } from '../types/stockManagement';
import { useConfirmConsignment } from '../hooks/useConfirmConsignment';

interface ConfirmConsignmentDialogProps {
  open: boolean;
  onClose: () => void;
  consignment: Consignment | null;
  onSuccess?: () => void;
}

/**
 * ConfirmConsignmentDialog Component
 * <p>
 * Dialog for confirming consignment receipt. This triggers stock item creation
 * and automatic classification.
 */
export const ConfirmConsignmentDialog = ({
  open,
  onClose,
  consignment,
  onSuccess,
}: ConfirmConsignmentDialogProps) => {
  const confirmConsignment = useConfirmConsignment();

  const handleConfirm = async () => {
    if (!consignment) return;

    try {
      await confirmConsignment.mutateAsync(consignment.consignmentId);
      onSuccess?.();
      onClose();
    } catch (error) {
      // Error is handled by the mutation
      console.error('Failed to confirm consignment:', error);
    }
  };

  if (!consignment) {
    return null;
  }

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>Confirm Consignment Receipt</DialogTitle>
      <DialogContent>
        <Typography variant="body1" gutterBottom>
          Are you sure you want to confirm receipt of consignment{' '}
          <strong>{consignment.consignmentReference}</strong>?
        </Typography>
        <Alert severity="info" sx={{ mt: 2 }}>
          Confirming this consignment will:
          <ul style={{ marginTop: 8, marginBottom: 0 }}>
            <li>Create stock items for each line item</li>
            <li>Automatically classify stock based on expiration dates</li>
            <li>Trigger FEFO location assignment (if configured)</li>
          </ul>
        </Alert>
        {confirmConsignment.isError && (
          <Alert severity="error" sx={{ mt: 2 }}>
            {confirmConsignment.error instanceof Error
              ? confirmConsignment.error.message
              : 'Failed to confirm consignment'}
          </Alert>
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} disabled={confirmConsignment.isPending}>
          Cancel
        </Button>
        <Button
          onClick={handleConfirm}
          variant="contained"
          color="primary"
          disabled={confirmConsignment.isPending || consignment.status !== 'RECEIVED'}
          startIcon={confirmConsignment.isPending ? <CircularProgress size={16} /> : null}
        >
          {confirmConsignment.isPending ? 'Confirming...' : 'Confirm Receipt'}
        </Button>
      </DialogActions>
    </Dialog>
  );
};
