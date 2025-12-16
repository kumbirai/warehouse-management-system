import { Container } from '@mui/material';
import { useUploadProductCsv } from '../hooks/useUploadProductCsv';
import { ProductCsvUploadForm } from '../components/ProductCsvUploadForm';
import { useAuth } from '../../../hooks/useAuth';

export const ProductCsvUploadPage = () => {
  const { uploadCsv, isLoading } = useUploadProductCsv();
  const { user } = useAuth();

  const handleUpload = async (file: File) => {
    if (!user?.tenantId) {
      throw new Error('Tenant ID is required');
    }
    return await uploadCsv(file, user.tenantId);
  };

  return (
    <Container maxWidth="lg" sx={{ mt: 4, mb: 4 }}>
      <ProductCsvUploadForm onUpload={handleUpload} isLoading={isLoading} />
    </Container>
  );
};
