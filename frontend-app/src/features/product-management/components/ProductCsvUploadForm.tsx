import {
  Alert,
  Box,
  Button,
  CircularProgress,
  LinearProgress,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material';
import { useState, useRef } from 'react';
import { UploadProductCsvResponse, ProductCsvError } from '../types/product';
import CloudUploadIcon from '@mui/icons-material/CloudUpload';

interface ProductCsvUploadFormProps {
  onUpload: (file: File) => Promise<UploadProductCsvResponse>;
  isLoading: boolean;
}

export const ProductCsvUploadForm = ({ onUpload, isLoading }: ProductCsvUploadFormProps) => {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [uploadResult, setUploadResult] = useState<UploadProductCsvResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleFileSelect = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (file) {
      // Validate file type
      if (!file.name.endsWith('.csv')) {
        setError('File must be a CSV file');
        return;
      }

      // Validate file size (10MB limit)
      const maxSize = 10 * 1024 * 1024; // 10MB
      if (file.size > maxSize) {
        setError('File size exceeds 10MB limit');
        return;
      }

      setSelectedFile(file);
      setError(null);
      setUploadResult(null);
    }
  };

  const handleUpload = async () => {
    if (!selectedFile) {
      setError('Please select a file');
      return;
    }

    setError(null);
    setUploadResult(null);

    try {
      const result = await onUpload(selectedFile);
      setUploadResult(result);
      setSelectedFile(null);
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to upload CSV file');
    }
  };

  const handleClear = () => {
    setSelectedFile(null);
    setUploadResult(null);
    setError(null);
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  return (
    <Paper sx={{ p: 3 }}>
      <Typography variant="h6" gutterBottom>
        Upload Product CSV
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        Upload a CSV file containing product master data. Maximum file size: 10MB.
      </Typography>

      <Box sx={{ mb: 3 }}>
        <input
          ref={fileInputRef}
          type="file"
          accept=".csv"
          onChange={handleFileSelect}
          style={{ display: 'none' }}
          id="csv-file-input"
        />
        <label htmlFor="csv-file-input">
          <Button
            variant="outlined"
            component="span"
            startIcon={<CloudUploadIcon />}
            disabled={isLoading}
          >
            Select CSV File
          </Button>
        </label>
        {selectedFile && (
          <Typography variant="body2" sx={{ mt: 1 }}>
            Selected: {selectedFile.name} ({(selectedFile.size / 1024).toFixed(2)} KB)
          </Typography>
        )}
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}

      {isLoading && (
        <Box sx={{ mb: 2 }}>
          <LinearProgress />
          <Typography variant="body2" sx={{ mt: 1 }}>
            Uploading and processing CSV file...
          </Typography>
        </Box>
      )}

      {uploadResult && (
        <Box sx={{ mb: 2 }}>
          <Alert severity={uploadResult.errorCount > 0 ? 'warning' : 'success'} sx={{ mb: 2 }}>
            <Typography variant="subtitle2" gutterBottom>
              Upload Complete
            </Typography>
            <Typography variant="body2">
              Total Rows: {uploadResult.totalRows} | Created: {uploadResult.createdCount} |
              Updated: {uploadResult.updatedCount} | Errors: {uploadResult.errorCount}
            </Typography>
          </Alert>

          {uploadResult.errors && uploadResult.errors.length > 0 && (
            <TableContainer component={Paper} sx={{ mt: 2 }}>
              <Typography variant="subtitle2" sx={{ p: 2 }}>
                Errors ({uploadResult.errors.length})
              </Typography>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Row</TableCell>
                    <TableCell>Product Code</TableCell>
                    <TableCell>Error Message</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {uploadResult.errors.map((error: ProductCsvError, index: number) => (
                    <TableRow key={index}>
                      <TableCell>{error.rowNumber}</TableCell>
                      <TableCell>{error.productCode || '-'}</TableCell>
                      <TableCell>{error.errorMessage}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </Box>
      )}

      <Box sx={{ display: 'flex', gap: 2 }}>
        <Button
          variant="contained"
          onClick={handleUpload}
          disabled={!selectedFile || isLoading}
        >
          {isLoading ? 'Uploading...' : 'Upload CSV'}
        </Button>
        <Button variant="outlined" onClick={handleClear} disabled={isLoading}>
          Clear
        </Button>
      </Box>
    </Paper>
  );
};

