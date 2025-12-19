import {
  Box,
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
import { Product } from '../types/product';
import { useNavigate } from 'react-router-dom';

interface ProductListProps {
  products: Product[];
  isLoading: boolean;
  error: Error | null;
}

export const ProductList = ({ products, isLoading, error }: ProductListProps) => {
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
        <Typography color="error">Error loading products: {error.message}</Typography>
      </Paper>
    );
  }

  // Defensive check: ensure products is an array
  if (!Array.isArray(products)) {
    return (
      <Paper sx={{ p: 3 }}>
        <Typography color="error">Invalid data format: products is not an array</Typography>
      </Paper>
    );
  }

  if (products.length === 0) {
    return (
      <Paper sx={{ p: 3 }}>
        <Typography>No products found</Typography>
      </Paper>
    );
  }

  return (
    <TableContainer component={Paper}>
      <Table>
        <TableHead>
          <TableRow>
            <TableCell>Product Code</TableCell>
            <TableCell>Description</TableCell>
            <TableCell>Primary Barcode</TableCell>
            <TableCell>Unit of Measure</TableCell>
            <TableCell>Category</TableCell>
            <TableCell>Brand</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {products.map(product => (
            <TableRow
              key={product.productId}
              hover
              sx={{ cursor: 'pointer' }}
              onClick={() => navigate(`/products/${product.productId}`)}
            >
              <TableCell>{product.productCode}</TableCell>
              <TableCell>{product.description}</TableCell>
              <TableCell sx={{ fontFamily: 'monospace' }}>{product.primaryBarcode}</TableCell>
              <TableCell>{product.unitOfMeasure}</TableCell>
              <TableCell>{product.category || '-'}</TableCell>
              <TableCell>{product.brand || '-'}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  );
};
