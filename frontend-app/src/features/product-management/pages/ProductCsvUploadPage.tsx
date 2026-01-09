import { useState } from 'react';
import { useUploadProductCsv } from '../hooks/useUploadProductCsv';
import { ProductCsvUploadForm } from '../components/ProductCsvUploadForm';
import { useAuth } from '../../../hooks/useAuth';
import { FormPageLayout } from '../../../components/layouts';
import { getBreadcrumbs } from '../../../utils/navigationUtils';

export const ProductCsvUploadPage = () => {
  const { uploadCsv, isLoading } = useUploadProductCsv();
  const { user } = useAuth();
  const [error, setError] = useState<string | null>(null);

  const handleUpload = async (file: File) => {
    if (!user?.tenantId) {
      const errorMsg = 'Tenant ID is required';
      setError(errorMsg);
      throw new Error(errorMsg);
    }
    try {
      setError(null);
      return await uploadCsv(file, user.tenantId);
    } catch (err) {
      const errorMsg = err instanceof Error ? err.message : 'Failed to upload CSV file';
      setError(errorMsg);
      throw err;
    }
  };

  return (
    <FormPageLayout
      breadcrumbs={getBreadcrumbs.productUploadCsv()}
      title="Upload Products CSV"
      description="Bulk import products from a CSV file"
      error={error}
    >
      <ProductCsvUploadForm onUpload={handleUpload} isLoading={isLoading} />
    </FormPageLayout>
  );
};
