import {
  Box,
  Chip,
  CircularProgress,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material';
import { StockMovement } from '../services/stockMovementService';
import { useNavigate } from 'react-router-dom';
import { getStatusVariant } from '../../../utils/statusUtils';

interface StockMovementListProps {
  movements: StockMovement[];
  isLoading: boolean;
  error: Error | null;
}

export const StockMovementList = ({ movements, isLoading, error }: StockMovementListProps) => {
  const navigate = useNavigate();

  if (isLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', p: 3 }}>
        <CircularProgress />
      </Box>
    );
  }

  if (error) {
    return (
      <Paper sx={{ p: 3 }}>
        <Typography color="error">Error loading stock movements: {error.message}</Typography>
      </Paper>
    );
  }

  if (!Array.isArray(movements)) {
    return (
      <Paper sx={{ p: 3 }}>
        <Typography color="error">Invalid data format: movements is not an array</Typography>
      </Paper>
    );
  }

  if (movements.length === 0) {
    return (
      <Paper sx={{ p: 3 }}>
        <Typography>No stock movements found</Typography>
      </Paper>
    );
  }

  return (
    <TableContainer component={Paper}>
      <Table>
        <TableHead>
          <TableRow>
            <TableCell>Movement ID</TableCell>
            <TableCell>Product ID</TableCell>
            <TableCell>Source Location</TableCell>
            <TableCell>Destination Location</TableCell>
            <TableCell>Quantity</TableCell>
            <TableCell>Type</TableCell>
            <TableCell>Reason</TableCell>
            <TableCell>Status</TableCell>
            <TableCell>Initiated At</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {movements.map(movement => (
            <TableRow
              key={movement.stockMovementId}
              hover
              sx={{ cursor: 'pointer' }}
              onClick={() => navigate(`/stock-movements/${movement.stockMovementId}`)}
            >
              <TableCell>{movement.stockMovementId}</TableCell>
              <TableCell>{movement.productId}</TableCell>
              <TableCell>{movement.sourceLocationId}</TableCell>
              <TableCell>{movement.destinationLocationId}</TableCell>
              <TableCell>{movement.quantity}</TableCell>
              <TableCell>{movement.movementType}</TableCell>
              <TableCell>{movement.reason}</TableCell>
              <TableCell>
                <Chip
                  label={movement.status}
                  color={
                    getStatusVariant(movement.status) === 'success'
                      ? 'success'
                      : getStatusVariant(movement.status) === 'error'
                        ? 'error'
                        : getStatusVariant(movement.status) === 'warning'
                          ? 'warning'
                          : 'default'
                  }
                  size="small"
                />
              </TableCell>
              <TableCell>{new Date(movement.initiatedAt).toLocaleString()}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  );
};
