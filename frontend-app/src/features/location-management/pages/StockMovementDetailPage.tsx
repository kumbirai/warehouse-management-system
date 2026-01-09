import { Button } from '@mui/material';
import { useNavigate, useParams } from 'react-router-dom';
import { useState } from 'react';

import { DetailPageLayout } from '../../../components/layouts';
import { ActionDialog } from '../../../components/common';
import { StockMovementDetail } from '../components/StockMovementDetail';
import { useStockMovement } from '../hooks/useStockMovement';
import { useCompleteStockMovement } from '../hooks/useCompleteStockMovement';
import { useCancelStockMovement } from '../hooks/useCancelStockMovement';
import { useAuth } from '../../../hooks/useAuth';
import { getBreadcrumbs, Routes } from '../../../utils/navigationUtils';

export const StockMovementDetailPage = () => {
  const { movementId } = useParams<{ movementId: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();

  const [completeDialogOpen, setCompleteDialogOpen] = useState(false);
  const [cancelDialogOpen, setCancelDialogOpen] = useState(false);
  const [cancellationReason, setCancellationReason] = useState('');

  const { movement, isLoading, error, refetch } = useStockMovement(
    movementId || '',
    user?.tenantId || ''
  );

  const { completeStockMovement, isLoading: isCompleting } = useCompleteStockMovement();
  const { cancelStockMovement, isLoading: isCancelling } = useCancelStockMovement();

  if (!movementId) {
    navigate(Routes.stockMovements);
    return null;
  }

  if (!user?.tenantId) {
    return (
      <DetailPageLayout
        breadcrumbs={getBreadcrumbs.stockMovementDetail(movementId)}
        title="Stock Movement Details"
        isLoading={false}
        error="Tenant ID is required to view stock movement details"
      >
        <div />
      </DetailPageLayout>
    );
  }

  const handleComplete = async () => {
    if (!movementId || !user?.tenantId) {
      return;
    }
    try {
      await completeStockMovement(movementId, user.tenantId);
      setCompleteDialogOpen(false);
      refetch();
    } catch (err) {
      // Error is handled by the hook
    }
  };

  const handleCancel = async () => {
    if (!cancellationReason.trim() || !movementId || !user?.tenantId) {
      return;
    }
    try {
      await cancelStockMovement(movementId, { cancellationReason }, user.tenantId);
      setCancelDialogOpen(false);
      setCancellationReason('');
      refetch();
    } catch (err) {
      // Error is handled by the hook
    }
  };

  return (
    <>
      <DetailPageLayout
        breadcrumbs={getBreadcrumbs.stockMovementDetail(movement?.stockMovementId || movementId)}
        title={movement?.stockMovementId || 'Loading...'}
        actions={
          <Button variant="outlined" onClick={() => navigate(Routes.stockMovements)}>
            Back to List
          </Button>
        }
        isLoading={isLoading}
        error={error?.message || null}
      >
        <StockMovementDetail
          movement={movement}
          onComplete={() => setCompleteDialogOpen(true)}
          onCancel={() => setCancelDialogOpen(true)}
          isCompleting={isCompleting}
          isCancelling={isCancelling}
        />
      </DetailPageLayout>

      <ActionDialog
        open={completeDialogOpen}
        title="Complete Stock Movement"
        description="Are you sure you want to complete this stock movement? This action cannot be undone."
        confirmLabel="Complete"
        cancelLabel="Cancel"
        onConfirm={handleComplete}
        onCancel={() => setCompleteDialogOpen(false)}
        isLoading={isCompleting}
        variant="default"
      />

      <ActionDialog
        open={cancelDialogOpen}
        title="Cancel Stock Movement"
        description="Are you sure you want to cancel this stock movement? Please provide a cancellation reason."
        confirmLabel="Cancel Movement"
        cancelLabel="Keep Movement"
        onConfirm={handleCancel}
        onCancel={() => {
          setCancelDialogOpen(false);
          setCancellationReason('');
        }}
        isLoading={isCancelling}
        variant="danger"
      />
    </>
  );
};
