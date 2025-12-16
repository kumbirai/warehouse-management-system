import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  Grid,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material';
import { Consignment } from '../types/stockManagement';
import { CheckCircle as CheckCircleIcon } from '@mui/icons-material';

interface ConsignmentDetailProps {
  consignment: Consignment;
  onValidate?: () => void;
  isValidating?: boolean;
  canValidate?: boolean;
}

export const ConsignmentDetail = ({
  consignment,
  onValidate,
  isValidating = false,
  canValidate = false,
}: ConsignmentDetailProps) => {
  const getStatusColor = (status: string): 'default' | 'primary' | 'success' | 'error' => {
    switch (status) {
      case 'CONFIRMED':
        return 'success';
      case 'RECEIVED':
        return 'primary';
      case 'CANCELLED':
      case 'REJECTED':
        return 'error';
      default:
        return 'default';
    }
  };

  return (
    <Box>
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
            <Typography variant="h5">Consignment Details</Typography>
            <Chip
              label={consignment.status}
              color={getStatusColor(consignment.status)}
              size="small"
            />
          </Box>

          <Grid container spacing={2}>
            <Grid item xs={12} sm={6}>
              <Typography variant="body2" color="text.secondary">
                Consignment ID
              </Typography>
              <Typography variant="body1">{consignment.consignmentId}</Typography>
            </Grid>

            <Grid item xs={12} sm={6}>
              <Typography variant="body2" color="text.secondary">
                Consignment Reference
              </Typography>
              <Typography variant="body1">{consignment.consignmentReference}</Typography>
            </Grid>

            <Grid item xs={12} sm={6}>
              <Typography variant="body2" color="text.secondary">
                Warehouse ID
              </Typography>
              <Typography variant="body1">{consignment.warehouseId}</Typography>
            </Grid>

            <Grid item xs={12} sm={6}>
              <Typography variant="body2" color="text.secondary">
                Received At
              </Typography>
              <Typography variant="body1">
                {new Date(consignment.receivedAt).toLocaleString()}
              </Typography>
            </Grid>

            {consignment.confirmedAt && (
              <Grid item xs={12} sm={6}>
                <Typography variant="body2" color="text.secondary">
                  Confirmed At
                </Typography>
                <Typography variant="body1">
                  {new Date(consignment.confirmedAt).toLocaleString()}
                </Typography>
              </Grid>
            )}

            {consignment.receivedBy && (
              <Grid item xs={12} sm={6}>
                <Typography variant="body2" color="text.secondary">
                  Received By
                </Typography>
                <Typography variant="body1">{consignment.receivedBy}</Typography>
              </Grid>
            )}

            <Grid item xs={12} sm={6}>
              <Typography variant="body2" color="text.secondary">
                Created At
              </Typography>
              <Typography variant="body1">
                {new Date(consignment.createdAt).toLocaleString()}
              </Typography>
            </Grid>
          </Grid>
        </CardContent>
      </Card>

      <Card>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            Line Items ({consignment.lineItems.length})
          </Typography>

          <TableContainer component={Paper} variant="outlined">
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Product Code</TableCell>
                  <TableCell align="right">Quantity</TableCell>
                  <TableCell>Expiration Date</TableCell>
                  <TableCell>Batch Number</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {consignment.lineItems.map((item, index) => (
                  <TableRow key={index}>
                    <TableCell>{item.productCode}</TableCell>
                    <TableCell align="right">{item.quantity}</TableCell>
                    <TableCell>
                      {item.expirationDate
                        ? new Date(item.expirationDate).toLocaleDateString()
                        : '-'}
                    </TableCell>
                    <TableCell>{item.batchNumber || '-'}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        </CardContent>
      </Card>

      {canValidate && consignment.status === 'RECEIVED' && (
        <Box sx={{ mt: 3 }}>
          <Alert severity="info" sx={{ mb: 2 }}>
            This consignment is in RECEIVED status and can be validated (confirmed).
          </Alert>
          <Button
            variant="contained"
            color="success"
            startIcon={<CheckCircleIcon />}
            onClick={onValidate}
            disabled={isValidating}
          >
            {isValidating ? 'Validating...' : 'Validate Consignment'}
          </Button>
        </Box>
      )}

      {consignment.status === 'CONFIRMED' && (
        <Box sx={{ mt: 3 }}>
          <Alert severity="success">
            This consignment has been confirmed and stock has been updated.
          </Alert>
        </Box>
      )}
    </Box>
  );
};
