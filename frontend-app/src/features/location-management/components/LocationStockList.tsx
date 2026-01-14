import {
  Box,
  Chip,
  Link,
  Paper,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material';
import { useNavigate } from 'react-router-dom';
import { StockItem } from '../../stock-management/types/stockManagement';
import { Routes } from '../../../utils/navigationUtils';
import { formatDate } from '../../../utils/dateUtils';

interface LocationStockListProps {
  stockItems: StockItem[];
  isLoading: boolean;
  error: Error | null;
}

export const LocationStockList = ({
  stockItems,
  isLoading,
  error,
}: LocationStockListProps) => {
  const navigate = useNavigate();

  if (isLoading) {
    return (
      <Paper elevation={1} sx={{ p: 3 }}>
        <Typography variant="h6" gutterBottom>
          Stock in Location
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Loading...
        </Typography>
      </Paper>
    );
  }

  if (error) {
    return (
      <Paper elevation={1} sx={{ p: 3 }}>
        <Typography variant="h6" gutterBottom>
          Stock in Location
        </Typography>
        <Typography variant="body2" color="error">
          Error loading stock items: {error.message}
        </Typography>
      </Paper>
    );
  }

  if (!stockItems || stockItems.length === 0) {
    return (
      <Paper elevation={1} sx={{ p: 3 }}>
        <Typography variant="h6" gutterBottom>
          Stock in Location
        </Typography>
        <Typography variant="body2" color="text.secondary">
          No stock items found at this location
        </Typography>
      </Paper>
    );
  }

  // Calculate totals
  const totalQuantity = stockItems.reduce((sum, item) => sum + item.quantity, 0);
  const uniqueProducts = new Set(stockItems.map((item) => item.productId)).size;

  return (
    <Paper elevation={1} sx={{ p: 3 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Typography variant="h6">
          Stock in Location
        </Typography>
        <Stack direction="row" spacing={2}>
          <Chip
            label={`${stockItems.length} item${stockItems.length !== 1 ? 's' : ''}`}
            size="small"
            variant="outlined"
          />
          <Chip
            label={`${uniqueProducts} product${uniqueProducts !== 1 ? 's' : ''}`}
            size="small"
            variant="outlined"
          />
          <Chip
            label={`Total: ${totalQuantity} units`}
            size="small"
            color="primary"
            variant="outlined"
          />
        </Stack>
      </Box>

      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell>Product Code</TableCell>
            <TableCell>Product Description</TableCell>
            <TableCell align="right">Quantity</TableCell>
            <TableCell>Expiration Date</TableCell>
            <TableCell>Classification</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {stockItems.map((item) => (
            <TableRow key={item.stockItemId} hover>
              <TableCell>
                <Link
                  component="button"
                  variant="body2"
                  onClick={() => navigate(Routes.stockItemDetail(item.stockItemId))}
                  sx={{
                    cursor: 'pointer',
                    textDecoration: 'none',
                    '&:hover': {
                      textDecoration: 'underline',
                    },
                  }}
                >
                  {item.productCode || 'N/A'}
                </Link>
              </TableCell>
              <TableCell>{item.productDescription || '—'}</TableCell>
              <TableCell align="right">{item.quantity}</TableCell>
              <TableCell>
                {item.expirationDate ? formatDate(item.expirationDate) : '—'}
              </TableCell>
              <TableCell>
                <Chip label={item.classification} size="small" variant="outlined" />
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </Paper>
  );
};
