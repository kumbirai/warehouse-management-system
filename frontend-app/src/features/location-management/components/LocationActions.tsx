import { useState } from 'react';
import {
  Box,
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
import { logger } from '../../../utils/logger';

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

    // Validate reason is required for blocking
    if (dialogAction === 'block' && !reason.trim()) {
      return; // Don't proceed if reason is empty
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
      logger.error('Failed to update location status:', error);
    }
  };

  const availableActions = statusToAction[status] || [];

  if (availableActions.length === 0) {
    return null;
  }

  return (
    <>
      <Box
        sx={{
          display: 'flex',
          flexDirection: { xs: 'column', sm: 'row' },
          gap: 1,
          flexWrap: 'wrap',
        }}
      >
        <ButtonGroup
          variant="outlined"
          size="small"
          orientation="horizontal"
          sx={{ flexDirection: { xs: 'column', sm: 'row' } }}
        >
          {availableActions.includes('block') && (
            <Tooltip title="Block location" arrow>
              <Button disabled={isLoading} onClick={() => openDialog('block')} aria-label="Block location">
                Block
              </Button>
            </Tooltip>
          )}
          {availableActions.includes('unblock') && (
            <Tooltip title="Unblock location" arrow>
              <Button disabled={isLoading} onClick={() => openDialog('unblock')} aria-label="Unblock location">
                Unblock
              </Button>
            </Tooltip>
          )}
          {availableActions.includes('reserve') && (
            <Tooltip title="Reserve location" arrow>
              <Button disabled={isLoading} onClick={() => openDialog('reserve')} aria-label="Reserve location">
                Reserve
              </Button>
            </Tooltip>
          )}
          {availableActions.includes('release') && (
            <Tooltip title="Release location" arrow>
              <Button disabled={isLoading} onClick={() => openDialog('release')} aria-label="Release location">
                Release
              </Button>
            </Tooltip>
          )}
        </ButtonGroup>
      </Box>

      <Dialog
        open={dialogAction !== null}
        onClose={closeDialog}
        maxWidth="sm"
        fullWidth
        aria-labelledby="location-action-dialog-title"
        aria-describedby="location-action-dialog-description"
        aria-modal="true"
      >
        <DialogTitle id="location-action-dialog-title">
          {dialogAction ? actionLabels[dialogAction] : ''} Location
        </DialogTitle>
        <DialogContent>
          <DialogContentText id="location-action-dialog-description" sx={{ mb: 2 }}>
            {dialogAction ? actionDescriptions[dialogAction] : ''}
          </DialogContentText>
          {dialogAction === 'block' && (
            <TextField
              autoFocus
              margin="dense"
              label="Reason"
              fullWidth
              variant="outlined"
              multiline
              rows={3}
              value={reason}
              onChange={e => setReason(e.target.value)}
              placeholder="Enter reason for blocking this location..."
              required
              error={!reason.trim()}
              helperText={!reason.trim() ? 'Reason is required for blocking a location' : ''}
              aria-label="Reason for blocking location input field"
              aria-required="true"
            />
          )}
          {dialogAction === 'reserve' && (
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
              aria-label="Reason for reserving location input field (optional)"
            />
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={closeDialog} disabled={isLoading} aria-label="Cancel location action">
            Cancel
          </Button>
          <Button
            onClick={handleConfirm}
            variant="contained"
            disabled={isLoading || (dialogAction === 'block' && !reason.trim())}
            aria-label={`Confirm ${dialogAction} location`}
          >
            {isLoading ? 'Updating...' : 'Confirm'}
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
};
