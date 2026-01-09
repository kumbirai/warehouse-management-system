import { Button } from '@mui/material';
import { useNavigate, useParams } from 'react-router-dom';

import { DetailPageLayout } from '../../../components/layouts';
import { getBreadcrumbs, Routes } from '../../../utils/navigationUtils';
import { ConsignmentDetail } from '../components/ConsignmentDetail';
import { useConsignment } from '../hooks/useConsignment';
import { useValidateConsignment } from '../hooks/useValidateConsignment';
import { ConfirmConsignmentDialog } from '../components/ConfirmConsignmentDialog';
import { useAuth } from '../../../hooks/useAuth';
import { useState } from 'react';
import { logger } from '../../../utils/logger';
import {
  OPERATOR,
  STOCK_CLERK,
  STOCK_MANAGER,
  SYSTEM_ADMIN,
  TENANT_ADMIN,
  WAREHOUSE_MANAGER,
} from '../../../constants/roles';

export const ConsignmentDetailPage = () => {
  const { consignmentId } = useParams<{ consignmentId: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();
  const { consignment, isLoading, error, refetch } = useConsignment(consignmentId);
  const validateConsignment = useValidateConsignment();
  const [confirmDialogOpen, setConfirmDialogOpen] = useState(false);

  // Handle missing consignment ID
  if (!consignmentId) {
    navigate(Routes.consignments);
    return null;
  }

  const handleValidate = async () => {
    if (!consignmentId) {
      return;
    }

    try {
      await validateConsignment.validateConsignment({ consignmentId });
      // Refetch consignment data after validation
      await refetch();
    } catch (err) {
      // Error is handled by the hook
      logger.error(
        'Failed to validate consignment',
        err instanceof Error ? err : new Error(String(err)),
        {
          consignmentId,
        }
      );
    }
  };

  const handleConfirm = () => {
    setConfirmDialogOpen(true);
  };

  const handleConfirmSuccess = async () => {
    setConfirmDialogOpen(false);
    await refetch();
  };

  const canValidate =
    user?.roles?.some(role =>
      [
        SYSTEM_ADMIN,
        TENANT_ADMIN,
        WAREHOUSE_MANAGER,
        STOCK_MANAGER,
        OPERATOR,
        STOCK_CLERK,
      ].includes(role)
    ) ?? false;

  const canConfirm =
    user?.roles?.some(role =>
      [
        SYSTEM_ADMIN,
        TENANT_ADMIN,
        WAREHOUSE_MANAGER,
        STOCK_MANAGER,
        OPERATOR,
        STOCK_CLERK,
      ].includes(role)
    ) ?? false;

  return (
    <DetailPageLayout
      breadcrumbs={getBreadcrumbs.consignmentDetail(consignment?.consignmentReference || '...')}
      title={consignment?.consignmentReference || 'Loading...'}
      actions={
        <Button variant="outlined" onClick={() => navigate(Routes.consignments)}>
          Back to List
        </Button>
      }
      isLoading={isLoading}
      error={error?.message || null}
    >
      <ConsignmentDetail
        consignment={consignment}
        onValidate={handleValidate}
        isValidating={validateConsignment.isLoading}
        canValidate={canValidate}
        onConfirm={handleConfirm}
        canConfirm={canConfirm && consignment?.status === 'RECEIVED'}
      />
      <ConfirmConsignmentDialog
        open={confirmDialogOpen}
        onClose={() => setConfirmDialogOpen(false)}
        consignment={consignment}
        onSuccess={handleConfirmSuccess}
      />
    </DetailPageLayout>
  );
};
