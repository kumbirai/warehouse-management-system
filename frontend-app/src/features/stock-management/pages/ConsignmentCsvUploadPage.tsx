import { FormPageLayout } from '../../../components/layouts';
import { getBreadcrumbs } from '../../../utils/navigationUtils';
import { ConsignmentCsvUploadForm } from '../components/ConsignmentCsvUploadForm';
import { useUploadConsignmentCsv } from '../hooks/useUploadConsignmentCsv';

export const ConsignmentCsvUploadPage = () => {
  const { uploadCsv, isLoading, error } = useUploadConsignmentCsv();

  const handleUpload = async (file: File) => {
    return await uploadCsv({ file });
  };

  return (
    <FormPageLayout
      breadcrumbs={getBreadcrumbs.consignmentUploadCsv()}
      title="Upload Consignment CSV"
      description="Upload a CSV file containing stock consignment data. The CSV should include columns for ConsignmentReference, ProductCode, Quantity, ReceivedDate, WarehouseId, and optionally ExpirationDate, BatchNumber, and ReceivedBy."
      error={error?.message || null}
      maxWidth="lg"
    >
      <ConsignmentCsvUploadForm onUpload={handleUpload} isLoading={isLoading} />
    </FormPageLayout>
  );
};
