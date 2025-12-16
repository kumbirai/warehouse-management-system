import { Container, Typography } from '@mui/material';
import { ConsignmentCsvUploadForm } from '../components/ConsignmentCsvUploadForm';
import { useUploadConsignmentCsv } from '../hooks/useUploadConsignmentCsv';

export const ConsignmentCsvUploadPage = () => {
  const uploadCsv = useUploadConsignmentCsv();

  const handleUpload = async (file: File) => {
    return await uploadCsv.uploadCsv({ file });
  };

  return (
    <Container maxWidth="lg" sx={{ py: 4 }}>
      <Typography variant="h4" gutterBottom>
        Upload Consignment CSV
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        Upload a CSV file containing stock consignment data. The CSV should include columns for
        ConsignmentReference, ProductCode, Quantity, ReceivedDate, WarehouseId, and optionally
        ExpirationDate, BatchNumber, and ReceivedBy.
      </Typography>

      <ConsignmentCsvUploadForm onUpload={handleUpload} isLoading={uploadCsv.isLoading} />
    </Container>
  );
};
