import {useState} from 'react';
import {Button, ButtonGroup, Dialog, DialogActions, DialogContent, DialogContentText, DialogTitle, Tooltip,} from '@mui/material';
import {TenantStatus} from '../types/tenant';
import {useTenantActions} from '../hooks/useTenantActions';

type ActionType = 'activate' | 'deactivate' | 'suspend';

interface TenantActionsProps {
    tenantId: string;
    status: TenantStatus;
    onCompleted?: () => void;
}

const actionLabels: Record<ActionType, string> = {
    activate: 'Activate',
    deactivate: 'Deactivate',
    suspend: 'Suspend',
};

const actionDescriptions: Record<ActionType, string> = {
    activate: 'Activating this tenant will enable access for assigned users.',
    deactivate: 'Deactivating this tenant revokes access until reactivated.',
    suspend: 'Suspending this tenant temporarily disables access without deactivating.',
};

export const TenantActions = ({tenantId, status, onCompleted}: TenantActionsProps) => {
    const {activateTenant, deactivateTenant, suspendTenant, isRunning} = useTenantActions();
    const [dialogAction, setDialogAction] = useState<ActionType | null>(null);

    const openDialog = (action: ActionType) => setDialogAction(action);
    const closeDialog = () => setDialogAction(null);

    const handleConfirm = async () => {
        if (!dialogAction) {
            return;
        }
        let success = false;
        switch (dialogAction) {
            case 'activate':
                success = await activateTenant(tenantId);
                break;
            case 'deactivate':
                success = await deactivateTenant(tenantId);
                break;
            case 'suspend':
                success = await suspendTenant(tenantId);
                break;
            default:
                break;
        }
        if (success && onCompleted) {
            onCompleted();
        }
        closeDialog();
    };

    return (
            <>
                <ButtonGroup variant="outlined" size="small">
                    <Tooltip title="Activate tenant" arrow>
          <span>
            <Button
                    disabled={status !== 'PENDING' || isRunning(tenantId, 'activate')}
                    onClick={() => openDialog('activate')}
            >
              Activate
            </Button>
          </span>
                    </Tooltip>
                    <Tooltip title="Suspend tenant" arrow>
          <span>
            <Button
                    disabled={status !== 'ACTIVE' || isRunning(tenantId, 'suspend')}
                    onClick={() => openDialog('suspend')}
            >
              Suspend
            </Button>
          </span>
                    </Tooltip>
                    <Tooltip title="Deactivate tenant" arrow>
          <span>
            <Button
                    disabled={(status !== 'ACTIVE' && status !== 'SUSPENDED') || isRunning(tenantId, 'deactivate')}
                    onClick={() => openDialog('deactivate')}
            >
              Deactivate
            </Button>
          </span>
                    </Tooltip>
                </ButtonGroup>

                <Dialog open={dialogAction !== null} onClose={closeDialog}>
                    <DialogTitle>{dialogAction ? actionLabels[dialogAction] : ''} Tenant</DialogTitle>
                    <DialogContent>
                        <DialogContentText>
                            {dialogAction ? actionDescriptions[dialogAction] : ''}
                        </DialogContentText>
                    </DialogContent>
                    <DialogActions>
                        <Button onClick={closeDialog}>Cancel</Button>
                        <Button onClick={handleConfirm} autoFocus>
                            Confirm
                        </Button>
                    </DialogActions>
                </Dialog>
            </>
    );
};

