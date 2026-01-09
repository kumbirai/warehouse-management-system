import { FormPageLayout } from '../../../components/layouts';
import { getBreadcrumbs } from '../../../utils/navigationUtils';
import { PickingListCsvUploadForm } from '../components/PickingListCsvUploadForm';
import { useUploadPickingListCsv } from '../hooks/useUploadPickingListCsv';

export const PickingListCsvUploadPage = () => {
  const { uploadCsv, isLoading, error } = useUploadPickingListCsv();

  const handleUpload = async (file: File) => {
    return await uploadCsv({ file });
  };

  return (
    <FormPageLayout
      breadcrumbs={getBreadcrumbs.pickingListUploadCsv()}
      title="Upload Picking List CSV"
      description="Upload a CSV file containing picking list data. The CSV should include columns for LoadNumber, OrderNumber, CustomerCode, CustomerName, Priority, ProductCode, Quantity, and optionally Notes."
      error={error?.message || null}
      maxWidth="lg"
    >
      <PickingListCsvUploadForm onUpload={handleUpload} isLoading={isLoading} />
    </FormPageLayout>
  );
};
