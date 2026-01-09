import { useState } from 'react';
import { ListPageLayout } from '../../../components/layouts';
import { getBreadcrumbs } from '../../../utils/navigationUtils';
import { PickingListList } from '../components/PickingListList';
import { usePickingLists } from '../hooks/usePickingLists';
import { PickingListStatus } from '../types/pickingTypes';

export const PickingListListPage = () => {
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [status, setStatus] = useState<PickingListStatus | undefined>(undefined);

  const { pickingLists, isLoading, error, refetch } = usePickingLists({
    page,
    size,
    status,
  });

  return (
    <ListPageLayout
      breadcrumbs={getBreadcrumbs.pickingLists()}
      title="Picking Lists"
      description="View and manage picking lists"
      isLoading={isLoading}
      error={error?.message || null}
    >
      <PickingListList
        pickingLists={pickingLists}
        isLoading={isLoading}
        page={page}
        size={size}
        onPageChange={setPage}
        onSizeChange={setSize}
        status={status}
        onStatusChange={setStatus}
        onRefresh={refetch}
      />
    </ListPageLayout>
  );
};
