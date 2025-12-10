import { useState } from 'react';
import { tenantService } from '../services/tenantService';

type TenantAction = 'activate' | 'deactivate' | 'suspend';

interface ActionState {
  action?: TenantAction;
  tenantId?: string;
}

export const useTenantActions = () => {
  const [state, setState] = useState<ActionState>({});
  const [error, setError] = useState<string | null>(null);

  const runAction = async (tenantId: string, action: TenantAction) => {
    setState({ action, tenantId });
    setError(null);
    try {
      switch (action) {
        case 'activate':
          await tenantService.activateTenant(tenantId);
          break;
        case 'deactivate':
          await tenantService.deactivateTenant(tenantId);
          break;
        case 'suspend':
          await tenantService.suspendTenant(tenantId);
          break;
        default:
          break;
      }
      return true;
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Tenant action failed';
      setError(message);
      return false;
    } finally {
      setState({});
    }
  };

  return {
    isRunning: (tenantId: string, action: TenantAction) =>
      state.tenantId === tenantId && state.action === action,
    activateTenant: (tenantId: string) => runAction(tenantId, 'activate'),
    deactivateTenant: (tenantId: string) => runAction(tenantId, 'deactivate'),
    suspendTenant: (tenantId: string) => runAction(tenantId, 'suspend'),
    error,
  };
};
