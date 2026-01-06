import React, { ReactNode } from 'react';
import {
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
} from '@mui/material';

interface ActionDialogProps {
  open: boolean;
  title: string;
  description: string | ReactNode;
  confirmLabel?: string;
  cancelLabel?: string;
  onConfirm: () => void | Promise<void>;
  onCancel: () => void;
  isLoading?: boolean;
  variant?: 'default' | 'danger';
}

export const ActionDialog: React.FC<ActionDialogProps> = ({
  open,
  title,
  description,
  confirmLabel = 'Confirm',
  cancelLabel = 'Cancel',
  onConfirm,
  onCancel,
  isLoading = false,
  variant = 'default',
}) => {
  const handleConfirm = async () => {
    await onConfirm();
  };

  return (
    <Dialog
      open={open}
      onClose={isLoading ? undefined : onCancel}
      maxWidth="sm"
      fullWidth
      aria-labelledby="dialog-title"
      aria-describedby="dialog-description"
      aria-modal="true"
    >
      <DialogTitle id="dialog-title">{title}</DialogTitle>
      <DialogContent>
        {typeof description === 'string' ? (
          <DialogContentText id="dialog-description">{description}</DialogContentText>
        ) : (
          <div id="dialog-description">{description}</div>
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={onCancel} disabled={isLoading} aria-label={cancelLabel}>
          {cancelLabel}
        </Button>
        <Button
          onClick={handleConfirm}
          variant="contained"
          color={variant === 'danger' ? 'error' : 'primary'}
          disabled={isLoading}
          aria-label={confirmLabel}
        >
          {isLoading ? 'Processing...' : confirmLabel}
        </Button>
      </DialogActions>
    </Dialog>
  );
};
