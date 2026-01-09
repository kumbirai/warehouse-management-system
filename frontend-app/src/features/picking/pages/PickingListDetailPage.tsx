import { useNavigate, useParams } from 'react-router-dom';
import { Button } from '@mui/material';
import { DetailPageLayout } from '../../../components/layouts';
import { getBreadcrumbs, Routes } from '../../../utils/navigationUtils';
import { PickingListDetail } from '../components/PickingListDetail';
import { usePickingList } from '../hooks/usePickingList';

export const PickingListDetailPage = () => {
  const { pickingListId } = useParams<{ pickingListId: string }>();
  const navigate = useNavigate();
  const { pickingList, isLoading, error } = usePickingList(pickingListId || '');

  const displayReference =
    pickingList?.pickingListReference || pickingListId?.substring(0, 8) + '...' || '...';

  return (
    <DetailPageLayout
      breadcrumbs={getBreadcrumbs.pickingListDetail(displayReference)}
      title={
        pickingList?.pickingListReference ||
        `Picking List ${pickingListId?.substring(0, 8) || '...'}`
      }
      actions={
        <Button variant="outlined" onClick={() => navigate(Routes.pickingLists)}>
          Back to List
        </Button>
      }
      isLoading={isLoading}
      error={error?.message || null}
    >
      <PickingListDetail pickingList={pickingList} />
    </DetailPageLayout>
  );
};
