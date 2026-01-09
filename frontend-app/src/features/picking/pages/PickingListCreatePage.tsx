import { FormPageLayout } from '../../../components/layouts';
import { getBreadcrumbs } from '../../../utils/navigationUtils';
import { PickingListForm } from '../components/PickingListForm';
import { useCreatePickingList } from '../hooks/useCreatePickingList';
import { useNavigate } from 'react-router-dom';
import { CreatePickingListRequest } from '../types/pickingTypes';
import { logger } from '../../../utils/logger';

export const PickingListCreatePage = () => {
  const navigate = useNavigate();
  const { createPickingList, isLoading, error } = useCreatePickingList();

  const handleSubmit = async (values: CreatePickingListRequest) => {
    try {
      const result = await createPickingList(values);
      logger.info('Picking list created successfully', { pickingListId: result.pickingListId });
      // Navigate to picking list detail or list page
      navigate(`/picking/picking-lists/${result.pickingListId}`);
    } catch (err) {
      logger.error('Failed to create picking list:', err);
      // Error is handled by the hook and displayed in the form
    }
  };

  return (
    <FormPageLayout
      breadcrumbs={getBreadcrumbs.pickingListCreate()}
      title="Create Picking List"
      description="Manually create a picking list by entering load and order information."
      error={error?.message || null}
      maxWidth="lg"
    >
      <PickingListForm onSubmit={handleSubmit} isSubmitting={isLoading} />
    </FormPageLayout>
  );
};
