import React from 'react';
import { Button, Stack } from '@mui/material';

interface FormActionsProps {
  onCancel: () => void;
  onSubmit?: () => void;
  isSubmitting: boolean;
  submitLabel?: string;
  cancelLabel?: string;
  submitDisabled?: boolean;
}

export const FormActions: React.FC<FormActionsProps> = ({
  onCancel,
  onSubmit,
  isSubmitting,
  submitLabel = 'Submit',
  cancelLabel = 'Cancel',
  submitDisabled = false,
}) => {
  return (
    <Stack
      direction={{ xs: 'column', sm: 'row' }}
      spacing={2}
      justifyContent="flex-end"
      sx={{ mt: 3 }}
    >
      <Button
        variant="outlined"
        onClick={onCancel}
        disabled={isSubmitting}
        sx={{ width: { xs: '100%', sm: 'auto' } }}
        aria-label={cancelLabel}
      >
        {cancelLabel}
      </Button>
      <Button
        type={onSubmit ? 'button' : 'submit'}
        variant="contained"
        onClick={onSubmit}
        disabled={isSubmitting || submitDisabled}
        sx={{ width: { xs: '100%', sm: 'auto' } }}
        aria-label={submitLabel}
      >
        {isSubmitting ? `${submitLabel}...` : submitLabel}
      </Button>
    </Stack>
  );
};
