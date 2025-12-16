import {
  Box,
  Card,
  CardContent,
  CircularProgress,
  Grid,
  Paper,
  Typography,
} from '@mui/material';
import { Product } from '../types/product';

interface ProductDetailProps {
  product: Product | null;
  isLoading: boolean;
  error: Error | null;
}

export const ProductDetail = ({ product, isLoading, error }: ProductDetailProps) => {
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
        <Typography color="error">Error loading product: {error.message}</Typography>
      </Paper>
    );
  }

  if (!product) {
    return (
      <Paper sx={{ p: 3 }}>
        <Typography>Product not found</Typography>
      </Paper>
    );
  }

  return (
    <Grid container spacing={3}>
      <Grid item xs={12}>
        <Card>
          <CardContent>
            <Typography variant="h5" gutterBottom>
              Product Details
            </Typography>
            <Grid container spacing={2} sx={{ mt: 1 }}>
              <Grid item xs={12} sm={6}>
                <Typography variant="body2" color="text.secondary">
                  Product ID
                </Typography>
                <Typography variant="body1">{product.productId}</Typography>
              </Grid>
              <Grid item xs={12} sm={6}>
                <Typography variant="body2" color="text.secondary">
                  Product Code
                </Typography>
                <Typography variant="body1" sx={{ fontFamily: 'monospace' }}>
                  {product.productCode}
                </Typography>
              </Grid>
              <Grid item xs={12}>
                <Typography variant="body2" color="text.secondary">
                  Description
                </Typography>
                <Typography variant="body1">{product.description}</Typography>
              </Grid>
              <Grid item xs={12} sm={6}>
                <Typography variant="body2" color="text.secondary">
                  Primary Barcode
                </Typography>
                <Typography variant="body1" sx={{ fontFamily: 'monospace' }}>
                  {product.primaryBarcode}
                </Typography>
              </Grid>
              <Grid item xs={12} sm={6}>
                <Typography variant="body2" color="text.secondary">
                  Unit of Measure
                </Typography>
                <Typography variant="body1">{product.unitOfMeasure}</Typography>
              </Grid>
              {product.category && (
                <Grid item xs={12} sm={6}>
                  <Typography variant="body2" color="text.secondary">
                    Category
                  </Typography>
                  <Typography variant="body1">{product.category}</Typography>
                </Grid>
              )}
              {product.brand && (
                <Grid item xs={12} sm={6}>
                  <Typography variant="body2" color="text.secondary">
                    Brand
                  </Typography>
                  <Typography variant="body1">{product.brand}</Typography>
                </Grid>
              )}
              {product.secondaryBarcodes && product.secondaryBarcodes.length > 0 && (
                <Grid item xs={12}>
                  <Typography variant="body2" color="text.secondary" gutterBottom>
                    Secondary Barcodes
                  </Typography>
                  <Box sx={{ display: 'flex', flexDirection: 'column', gap: 0.5 }}>
                    {product.secondaryBarcodes.map((barcode, index) => (
                      <Typography
                        key={index}
                        variant="body1"
                        sx={{ fontFamily: 'monospace' }}
                      >
                        {barcode}
                      </Typography>
                    ))}
                  </Box>
                </Grid>
              )}
              <Grid item xs={12} sm={6}>
                <Typography variant="body2" color="text.secondary">
                  Created At
                </Typography>
                <Typography variant="body1">
                  {new Date(product.createdAt).toLocaleString()}
                </Typography>
              </Grid>
              {product.lastModifiedAt && (
                <Grid item xs={12} sm={6}>
                  <Typography variant="body2" color="text.secondary">
                    Last Modified
                  </Typography>
                  <Typography variant="body1">
                    {new Date(product.lastModifiedAt).toLocaleString()}
                  </Typography>
                </Grid>
              )}
            </Grid>
          </CardContent>
        </Card>
      </Grid>
    </Grid>
  );
};

