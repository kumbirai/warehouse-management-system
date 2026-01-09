import { useNavigate, useParams } from 'react-router-dom';
import { Button } from '@mui/material';
import { DetailPageLayout } from '../../../components/layouts';
import { getBreadcrumbs, Routes } from '../../../utils/navigationUtils';
import { LoadDetail } from '../components/LoadDetail';
import { useLoad } from '../hooks/useLoad';

export const LoadDetailPage = () => {
  const { loadId } = useParams<{ loadId: string }>();
  const navigate = useNavigate();
  const { load, isLoading, error } = useLoad(loadId || null);

  return (
    <DetailPageLayout
      breadcrumbs={getBreadcrumbs.loadDetail(loadId || '...')}
      title={`Load ${load?.loadNumber || loadId?.substring(0, 8) || '...'}`}
      actions={
        <Button variant="outlined" onClick={() => navigate(Routes.pickingLists)}>
          Back to Picking Lists
        </Button>
      }
      isLoading={isLoading}
      error={error?.message || null}
    >
      <LoadDetail load={load} />
    </DetailPageLayout>
  );
};
