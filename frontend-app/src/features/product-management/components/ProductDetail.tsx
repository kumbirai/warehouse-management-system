import { Box, Chip, Divider, Grid, Paper, Stack, Typography } from '@mui/material';
import { Product } from '../types/product';
import { formatDateTime } from '../../../utils/dateUtils';
import { useStockLevels } from '../../stock-management/hooks/useStockLevels';
import { StockLevelList } from '../../stock-management/components/StockLevelList';

interface ProductDetailProps {
  product: Product | null;
  onUpdate?: () => void;
}

export const ProductDetail = ({ product }: ProductDetailProps) => {
  const { data: stockLevelsResponse, isLoading: isLoadingStockLevels } = useStockLevels({
    productId: product?.productId,
  });

  const stockLevels = stockLevelsResponse?.data || [];

  if (!product) {
    return null;
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
                Product Code
              </Typography>
              <Typography variant="body1" sx={{ fontFamily: 'monospace', fontWeight: 'medium' }}>
                {product.productCode}
              </Typography>
            </Box>

            <Box>
              <Typography variant="caption" color="text.secondary">
                Description
              </Typography>
              <Typography variant="body1">{product.description}</Typography>
            </Box>

            <Box>
              <Typography variant="caption" color="text.secondary">
                Primary Barcode
              </Typography>
              <Typography variant="body1" sx={{ fontFamily: 'monospace' }}>
                {product.primaryBarcode}
              </Typography>
            </Box>

            <Box>
              <Typography variant="caption" color="text.secondary">
                Unit of Measure
              </Typography>
              <Typography variant="body1">{product.unitOfMeasure}</Typography>
            </Box>
          </Stack>
        </Paper>
      </Grid>

      {/* Additional Information */}
      <Grid item xs={12} md={6}>
        <Paper elevation={1} sx={{ p: 3 }}>
          <Typography variant="h6" gutterBottom>
            Additional Information
          </Typography>
          <Divider sx={{ mb: 2 }} />

          <Stack spacing={2}>
            {product.category && (
              <Box>
                <Typography variant="caption" color="text.secondary">
                  Category
                </Typography>
                <Typography variant="body1">{product.category}</Typography>
              </Box>
            )}

            {product.brand && (
              <Box>
                <Typography variant="caption" color="text.secondary">
                  Brand
                </Typography>
                <Typography variant="body1">{product.brand}</Typography>
              </Box>
            )}

            {product.secondaryBarcodes && product.secondaryBarcodes.length > 0 && (
              <Box>
                <Typography variant="caption" color="text.secondary" gutterBottom>
                  Secondary Barcodes
                </Typography>
                <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
                  {product.secondaryBarcodes.map((barcode, index) => (
                    <Chip
                      key={index}
                      label={barcode}
                      size="small"
                      sx={{ fontFamily: 'monospace' }}
                    />
                  ))}
                </Stack>
              </Box>
            )}

            <Box>
              <Typography variant="caption" color="text.secondary">
                Created At
              </Typography>
              <Typography variant="body1">{formatDateTime(product.createdAt)}</Typography>
            </Box>

            {product.lastModifiedAt && (
              <Box>
                <Typography variant="caption" color="text.secondary">
                  Last Modified
                </Typography>
                <Typography variant="body1">{formatDateTime(product.lastModifiedAt)}</Typography>
              </Box>
            )}
          </Stack>
        </Paper>
      </Grid>

      {/* Stock by Location */}
      <Grid item xs={12}>
        <Paper elevation={1} sx={{ p: 3 }}>
          <Typography variant="h6" gutterBottom>
            Stock by Location
          </Typography>
          <Divider sx={{ mb: 2 }} />
          {isLoadingStockLevels ? (
            <Typography variant="body2" color="text.secondary">
              Loading stock levels...
            </Typography>
          ) : stockLevels.length > 0 ? (
            <StockLevelList stockLevels={stockLevels} />
          ) : (
            <Typography variant="body2" color="text.secondary">
              No stock available for this product
            </Typography>
          )}
        </Paper>
      </Grid>
    </Grid>
  );
};
