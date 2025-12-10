import { useState } from 'react';
import {
  Button,
  ButtonGroup,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  Tooltip,
} from '@mui/material';
import { UserStatus } from '../types/user';
import { useUserActions } from '../hooks/useUserActions';

type ActionType = 'activate' | 'deactivate' | 'suspend';

interface UserActionsProps {
  userId: string;
  status: UserStatus;
  onCompleted?: () => void;
}

const actionLabels: Record<ActionType, string> = {
  activate: 'Activate',
  deactivate: 'Deactivate',
  suspend: 'Suspend',
};

const actionDescriptions: Record<ActionType, string> = {
  activate: 'Activating this user will enable access to the system.',
  deactivate: 'Deactivating this user revokes access until reactivated.',
  suspend: 'Suspending this user temporarily disables access without deactivating.',
};

export const UserActions = ({ userId, status, onCompleted }: UserActionsProps) => {
  const { activateUser, deactivateUser, suspendUser, isLoading } = useUserActions(onCompleted);
  const [dialogAction, setDialogAction] = useState<ActionType | null>(null);

  const openDialog = (action: ActionType) => setDialogAction(action);
  const closeDialog = () => setDialogAction(null);

  const handleConfirm = async () => {
    if (!dialogAction) {
      return;
    }
    try {
      switch (dialogAction) {
        case 'activate':
          await activateUser(userId);
          break;
        case 'deactivate':
          await deactivateUser(userId);
          break;
        case 'suspend':
          await suspendUser(userId);
          break;
        default:
          break;
      }
    } catch (error) {
      // Error is handled by the hook
    }
    closeDialog();
  };

  return (
    <>
      <ButtonGroup variant="outlined" size="small">
        <Tooltip title="Activate user" arrow>
          <span>
            <Button
              disabled={status === 'ACTIVE' || isLoading}
              onClick={() => openDialog('activate')}
            >
              Activate
            </Button>
          </span>
        </Tooltip>
        <Tooltip title="Suspend user" arrow>
          <span>
            <Button
              disabled={status !== 'ACTIVE' || isLoading}
              onClick={() => openDialog('suspend')}
            >
              Suspend
            </Button>
          </span>
        </Tooltip>
        <Tooltip title="Deactivate user" arrow>
          <span>
            <Button
              disabled={(status !== 'ACTIVE' && status !== 'SUSPENDED') || isLoading}
              onClick={() => openDialog('deactivate')}
            >
              Deactivate
            </Button>
          </span>
        </Tooltip>
      </ButtonGroup>

      <Dialog open={dialogAction !== null} onClose={closeDialog}>
        <DialogTitle>{dialogAction ? actionLabels[dialogAction] : ''} User</DialogTitle>
        <DialogContent>
          <DialogContentText>
            {dialogAction ? actionDescriptions[dialogAction] : ''}
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={closeDialog} disabled={isLoading}>
            Cancel
          </Button>
          <Button onClick={handleConfirm} autoFocus disabled={isLoading}>
            Confirm
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
};
