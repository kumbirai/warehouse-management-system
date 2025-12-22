import { useNavigate } from 'react-router-dom';
import { FormPageLayout } from '../../../components/layouts';
import { getBreadcrumbs, Routes } from '../../../utils/navigationUtils';
import { ConsignmentForm } from '../components/ConsignmentForm';
import { useCreateConsignment } from '../hooks/useCreateConsignment';
import { CreateConsignmentRequest } from '../types/stockManagement';

export const CreateConsignmentPage = () => {
  const navigate = useNavigate();
  const { createConsignment, isLoading, error } = useCreateConsignment();

  const handleSubmit = async (request: CreateConsignmentRequest) => {
    await createConsignment(request);
  };

  const handleCancel = () => {
    navigate(Routes.consignments);
  };

  return (
    <FormPageLayout
      breadcrumbs={getBreadcrumbs.consignmentCreate()}
      title="Create Consignment"
      description="Create a new stock consignment by manually entering consignment details and line items. You can scan barcodes to automatically fill product codes."
      error={error?.message || null}
      maxWidth="lg"
    >
      <ConsignmentForm onSubmit={handleSubmit} onCancel={handleCancel} isSubmitting={isLoading} />
    </FormPageLayout>
  );
};
