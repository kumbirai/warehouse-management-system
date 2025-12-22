import { useState } from 'react';
import {
  Button,
  ButtonGroup,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  TextField,
  Tooltip,
} from '@mui/material';
import { LocationStatus } from '../types/location';
import { useUpdateLocationStatus } from '../hooks/useUpdateLocationStatus';

type ActionType = 'block' | 'unblock' | 'reserve' | 'release';

interface LocationActionsProps {
  locationId: string;
  status: LocationStatus;
  tenantId: string;
  onCompleted?: () => void;
}

const actionLabels: Record<ActionType, string> = {
  block: 'Block',
  unblock: 'Unblock',
  reserve: 'Reserve',
  release: 'Release',
};

const actionDescriptions: Record<ActionType, string> = {
  block: 'Blocking this location prevents stock assignment. Provide a reason for blocking.',
  unblock: 'Unblocking this location makes it available for stock assignment.',
  reserve: 'Reserving this location sets it aside for upcoming stock assignment.',
  release: 'Releasing this location makes it available for stock assignment.',
};

const statusToAction: Record<LocationStatus, ActionType[]> = {
  AVAILABLE: ['block', 'reserve'],
  OCCUPIED: ['block'],
  RESERVED: ['release', 'block'],
  BLOCKED: ['unblock'],
};

export const LocationActions = ({
  locationId,
  status,
  tenantId,
  onCompleted,
}: LocationActionsProps) => {
  const { updateStatus, isLoading } = useUpdateLocationStatus();
  const [dialogAction, setDialogAction] = useState<ActionType | null>(null);
  const [reason, setReason] = useState('');

  const openDialog = (action: ActionType) => {
    setDialogAction(action);
    setReason('');
  };

  const closeDialog = () => {
    setDialogAction(null);
    setReason('');
  };

  const handleConfirm = async () => {
    if (!dialogAction) {
      return;
    }

    let newStatus: LocationStatus;
    switch (dialogAction) {
      case 'block':
        newStatus = 'BLOCKED';
        break;
      case 'unblock':
        newStatus = 'AVAILABLE';
        break;
      case 'reserve':
        newStatus = 'RESERVED';
        break;
      case 'release':
        newStatus = 'AVAILABLE';
        break;
      default:
        return;
    }

    try {
      await updateStatus(locationId, newStatus, reason || undefined, tenantId);
      if (onCompleted) {
        onCompleted();
      }
      closeDialog();
    } catch (error) {
      // Error is handled by the hook and can be accessed via error state if needed
      // For now, we'll just close the dialog on error
      console.error('Failed to update location status:', error);
    }
  };

  const availableActions = statusToAction[status] || [];

  if (availableActions.length === 0) {
    return null;
  }

  return (
    <>
      <ButtonGroup variant="outlined" size="small">
        {availableActions.includes('block') && (
          <Tooltip title="Block location" arrow>
            <Button disabled={isLoading} onClick={() => openDialog('block')}>
              Block
            </Button>
          </Tooltip>
        )}
        {availableActions.includes('unblock') && (
          <Tooltip title="Unblock location" arrow>
            <Button disabled={isLoading} onClick={() => openDialog('unblock')}>
              Unblock
            </Button>
          </Tooltip>
        )}
        {availableActions.includes('reserve') && (
          <Tooltip title="Reserve location" arrow>
            <Button disabled={isLoading} onClick={() => openDialog('reserve')}>
              Reserve
            </Button>
          </Tooltip>
        )}
        {availableActions.includes('release') && (
          <Tooltip title="Release location" arrow>
            <Button disabled={isLoading} onClick={() => openDialog('release')}>
              Release
            </Button>
          </Tooltip>
        )}
      </ButtonGroup>

      <Dialog open={dialogAction !== null} onClose={closeDialog} maxWidth="sm" fullWidth>
        <DialogTitle>{dialogAction ? actionLabels[dialogAction] : ''} Location</DialogTitle>
        <DialogContent>
          <DialogContentText sx={{ mb: 2 }}>
            {dialogAction ? actionDescriptions[dialogAction] : ''}
          </DialogContentText>
          {(dialogAction === 'block' || dialogAction === 'reserve') && (
            <TextField
              autoFocus
              margin="dense"
              label="Reason (Optional)"
              fullWidth
              variant="outlined"
              multiline
              rows={3}
              value={reason}
              onChange={e => setReason(e.target.value)}
              placeholder="Enter reason for this action..."
            />
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={closeDialog} disabled={isLoading}>
            Cancel
          </Button>
          <Button onClick={handleConfirm} variant="contained" disabled={isLoading} autoFocus>
            {isLoading ? 'Updating...' : 'Confirm'}
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
};
