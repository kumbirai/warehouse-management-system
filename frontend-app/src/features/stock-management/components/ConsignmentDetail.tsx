import {
  Alert,
  Box,
  Button,
  Divider,
  Grid,
  Paper,
  Stack,
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
import { getStatusVariant, StatusBadge } from '../../../components/common';
import { formatDateTime } from '../../../utils/dateUtils';

interface ConsignmentDetailProps {
  consignment: Consignment | null;
  onValidate?: () => void;
  isValidating?: boolean;
  canValidate?: boolean;
  onConfirm?: () => void;
  canConfirm?: boolean;
}

export const ConsignmentDetail = ({
  consignment,
  onValidate,
  isValidating = false,
  canValidate = false,
  onConfirm,
  canConfirm = false,
}: ConsignmentDetailProps) => {
  if (!consignment) {
    return (
      <Paper sx={{ p: 3 }}>
        <Typography>Consignment not found</Typography>
      </Paper>
    );
  }

  return (
    <Grid container spacing={3}>
      {/* Basic Information */}
      <Grid item xs={12} md={6}>
        <Paper elevation={1} sx={{ p: 3 }}>
          <Typography variant="h6" gutterBottom>
            Basic Information
          </Typography>
          <Divider sx={{ mb: 2 }} />

          <Stack spacing={2}>
            <Box>
              <Typography variant="caption" color="text.secondary">
                Consignment ID
              </Typography>
              <Typography variant="body1" sx={{ fontFamily: 'monospace' }}>
                {consignment.consignmentId}
              </Typography>
            </Box>

            <Box>
              <Typography variant="caption" color="text.secondary">
                Consignment Reference
              </Typography>
              <Typography variant="body1" sx={{ fontFamily: 'monospace' }}>
                {consignment.consignmentReference}
              </Typography>
            </Box>

            <Box>
              <Typography variant="caption" color="text.secondary">
                Warehouse ID
              </Typography>
              <Typography variant="body1">{consignment.warehouseId}</Typography>
            </Box>

            <Box>
              <Typography variant="caption" color="text.secondary">
                Status
              </Typography>
              <Box mt={0.5}>
                <StatusBadge
                  label={consignment.status}
                  variant={getStatusVariant(consignment.status)}
                />
              </Box>
            </Box>

            {consignment.receivedBy && (
              <Box>
                <Typography variant="caption" color="text.secondary">
                  Received By
                </Typography>
                <Typography variant="body1">{consignment.receivedBy}</Typography>
              </Box>
            )}
          </Stack>
        </Paper>
      </Grid>

      {/* Dates and Timeline */}
      <Grid item xs={12} md={6}>
        <Paper elevation={1} sx={{ p: 3 }}>
          <Typography variant="h6" gutterBottom>
            Timeline
          </Typography>
          <Divider sx={{ mb: 2 }} />

          <Stack spacing={2}>
            <Box>
              <Typography variant="caption" color="text.secondary">
                Received At
              </Typography>
              <Typography variant="body1">{formatDateTime(consignment.receivedAt)}</Typography>
            </Box>

            {consignment.confirmedAt && (
              <Box>
                <Typography variant="caption" color="text.secondary">
                  Confirmed At
                </Typography>
                <Typography variant="body1">{formatDateTime(consignment.confirmedAt)}</Typography>
              </Box>
            )}

            <Box>
              <Typography variant="caption" color="text.secondary">
                Created At
              </Typography>
              <Typography variant="body1">{formatDateTime(consignment.createdAt)}</Typography>
            </Box>

            {consignment.lastModifiedAt && (
              <Box>
                <Typography variant="caption" color="text.secondary">
                  Last Modified
                </Typography>
                <Typography variant="body1">
                  {formatDateTime(consignment.lastModifiedAt)}
                </Typography>
              </Box>
            )}
          </Stack>
        </Paper>
      </Grid>

      {/* Line Items */}
      <Grid item xs={12}>
        <Paper elevation={1} sx={{ p: 3 }}>
          <Typography variant="h6" gutterBottom>
            Line Items ({consignment.lineItems.length})
          </Typography>
          <Divider sx={{ mb: 2 }} />

          <TableContainer>
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
                {consignment.lineItems.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={4} align="center">
                      <Typography variant="body2" color="text.secondary">
                        No line items
                      </Typography>
                    </TableCell>
                  </TableRow>
                ) : (
                  consignment.lineItems.map((item, index) => (
                    <TableRow key={index}>
                      <TableCell>{item.productCode}</TableCell>
                      <TableCell align="right">{item.quantity}</TableCell>
                      <TableCell>
                        {item.expirationDate ? formatDateTime(item.expirationDate) : '—'}
                      </TableCell>
                      <TableCell>{item.batchNumber || '—'}</TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </TableContainer>
        </Paper>
      </Grid>

      {/* Actions Section */}
      {consignment.status === 'RECEIVED' && (canValidate || canConfirm) && (
        <Grid item xs={12}>
          <Paper elevation={1} sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>
              Actions
            </Typography>
            <Divider sx={{ mb: 2 }} />

            <Alert severity="info" sx={{ mb: 2 }}>
              This consignment is in RECEIVED status. Confirm it to create stock items and update
              inventory.
            </Alert>
            <Box sx={{ display: 'flex', gap: 2 }}>
              {canConfirm && onConfirm && (
                <Button
                  variant="contained"
                  color="success"
                  startIcon={<CheckCircleIcon />}
                  onClick={onConfirm}
                >
                  Confirm Consignment
                </Button>
              )}
              {canValidate && (
                <Button
                  variant="outlined"
                  color="primary"
                  onClick={onValidate}
                  disabled={isValidating}
                >
                  {isValidating ? 'Validating...' : 'Validate Consignment'}
                </Button>
              )}
            </Box>
          </Paper>
        </Grid>
      )}

      {consignment.status === 'CONFIRMED' && (
        <Grid item xs={12}>
          <Alert severity="success">
            This consignment has been confirmed and stock has been updated.
          </Alert>
        </Grid>
      )}
    </Grid>
  );
};
