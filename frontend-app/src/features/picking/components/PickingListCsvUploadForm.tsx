import {
  Alert,
  Box,
  Button,
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
import { useRef, useState } from 'react';
import { CsvValidationError, UploadPickingListCsvResponse } from '../types/pickingTypes';
import CloudUploadIcon from '@mui/icons-material/CloudUpload';
import DownloadIcon from '@mui/icons-material/Download';

interface PickingListCsvUploadFormProps {
  onUpload: (file: File) => Promise<UploadPickingListCsvResponse>;
  isLoading: boolean;
}

export const PickingListCsvUploadForm = ({
  onUpload,
  isLoading,
}: PickingListCsvUploadFormProps) => {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [uploadResult, setUploadResult] = useState<UploadPickingListCsvResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleFileSelect = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (file) {
      if (!file.name.endsWith('.csv')) {
        setError('File must be a CSV file');
        return;
      }

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

  const handleDownloadTemplate = () => {
    // CSV template content
    const template = `LoadNumber,OrderNumber,CustomerCode,CustomerName,Priority,ProductCode,Quantity,Notes
LOAD-001,ORD-001,CUST-001,Acme Corp,HIGH,PROD-001,10,Urgent order
LOAD-001,ORD-001,CUST-001,Acme Corp,HIGH,PROD-002,5,
LOAD-002,ORD-002,CUST-002,Widget Inc,NORMAL,PROD-003,20,`;

    const blob = new Blob([template], { type: 'text/csv' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'picking-list-template.csv';
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    window.URL.revokeObjectURL(url);
  };

  const handleDownloadErrorReport = () => {
    if (!uploadResult || !uploadResult.errors || uploadResult.errors.length === 0) {
      return;
    }

    const errorReport = [
      ['Row Number', 'Field Name', 'Error Message', 'Invalid Value'],
      ...uploadResult.errors.map((err: CsvValidationError) => [
        err.rowNumber.toString(),
        err.fieldName,
        err.errorMessage,
        err.invalidValue || '',
      ]),
    ]
      .map(row => row.join(','))
      .join('\n');

    const blob = new Blob([errorReport], { type: 'text/csv' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `picking-list-errors-${new Date().toISOString()}.csv`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    window.URL.revokeObjectURL(url);
  };

  return (
    <Paper sx={{ p: 3 }}>
      <Typography variant="h6" gutterBottom>
        Upload Picking List CSV
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        Upload a CSV file containing picking list data. Maximum file size: 10MB.
      </Typography>

      <Box sx={{ mb: 3, display: 'flex', gap: 2, flexDirection: { xs: 'column', sm: 'row' } }}>
        <Button
          variant="outlined"
          startIcon={<DownloadIcon />}
          onClick={handleDownloadTemplate}
          aria-label="Download CSV template"
        >
          Download Template
        </Button>
      </Box>

      <Box sx={{ mb: 3 }}>
        <input
          ref={fileInputRef}
          type="file"
          accept=".csv"
          onChange={handleFileSelect}
          style={{ display: 'none' }}
          id="picking-list-csv-file-input"
        />
        <label htmlFor="picking-list-csv-file-input">
          <Button
            variant="outlined"
            component="span"
            startIcon={<CloudUploadIcon />}
            disabled={isLoading}
            aria-label="Select CSV file"
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
          <Alert severity={uploadResult.errorRows > 0 ? 'warning' : 'success'} sx={{ mb: 2 }}>
            <Typography variant="subtitle2" gutterBottom>
              Upload Complete
            </Typography>
            <Typography variant="body2">
              Total Rows: {uploadResult.totalRows} | Successful: {uploadResult.successfulRows} |
              Errors: {uploadResult.errorRows}
            </Typography>
            {uploadResult.createdPickingListIds.length > 0 && (
              <Typography variant="body2" sx={{ mt: 1 }}>
                Created Picking Lists: {uploadResult.createdPickingListIds.length}
              </Typography>
            )}
          </Alert>

          {uploadResult.errors && uploadResult.errors.length > 0 && (
            <Box sx={{ mt: 2 }}>
              <Button
                variant="outlined"
                startIcon={<DownloadIcon />}
                onClick={handleDownloadErrorReport}
                sx={{ mb: 2 }}
                aria-label="Download error report"
              >
                Download Error Report
              </Button>
              <TableContainer component={Paper}>
                <Typography variant="subtitle2" sx={{ p: 2 }}>
                  Errors ({uploadResult.errors.length})
                </Typography>
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>Row</TableCell>
                      <TableCell>Field</TableCell>
                      <TableCell>Error Message</TableCell>
                      <TableCell>Invalid Value</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {uploadResult.errors.map((error: CsvValidationError, index: number) => (
                      <TableRow key={index}>
                        <TableCell>{error.rowNumber}</TableCell>
                        <TableCell>{error.fieldName}</TableCell>
                        <TableCell>{error.errorMessage}</TableCell>
                        <TableCell>{error.invalidValue || '-'}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            </Box>
          )}
        </Box>
      )}

      <Box
        sx={{
          display: 'flex',
          flexDirection: { xs: 'column', sm: 'row' },
          gap: 2,
        }}
      >
        <Button
          variant="contained"
          onClick={handleUpload}
          disabled={!selectedFile || isLoading}
          sx={{ width: { xs: '100%', sm: 'auto' } }}
          aria-label="Upload CSV file"
        >
          {isLoading ? 'Uploading...' : 'Upload CSV'}
        </Button>
        <Button
          variant="outlined"
          onClick={handleClear}
          disabled={isLoading}
          sx={{ width: { xs: '100%', sm: 'auto' } }}
          aria-label="Clear form"
        >
          Clear
        </Button>
      </Box>
    </Paper>
  );
};
