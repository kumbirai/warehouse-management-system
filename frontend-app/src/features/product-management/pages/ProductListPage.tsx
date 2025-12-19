import { Box, Breadcrumbs, Button, Container, Link, Tab, Tabs, Typography } from '@mui/material';
import { useState } from 'react';
import { Link as RouterLink, useNavigate } from 'react-router-dom';
import { ProductList } from '../components/ProductList';
import { useProducts } from '../hooks/useProducts';
import { useAuth } from '../../../hooks/useAuth';
import { Header } from '../../../components/layout/Header';
import AddIcon from '@mui/icons-material/Add';
import UploadFileIcon from '@mui/icons-material/UploadFile';
import { ProductCsvUploadForm } from '../components/ProductCsvUploadForm';
import { useUploadProductCsv } from '../hooks/useUploadProductCsv';

export const ProductListPage = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [activeTab, setActiveTab] = useState(0);
  const { products, isLoading, error } = useProducts({
    tenantId: user?.tenantId ?? undefined,
    page: 0,
    size: 100,
  });
  const { uploadCsv, isLoading: isUploading } = useUploadProductCsv();

  const handleUpload = async (file: File) => {
    if (!user?.tenantId) {
      throw new Error('Tenant ID is required');
    }
    return await uploadCsv(file, user.tenantId);
  };

  return (
    <>
      <Header />
      <Container maxWidth="lg" sx={{ mt: 4, mb: 4 }}>
        <Breadcrumbs sx={{ mb: 2 }}>
          <Link component={RouterLink} to="/dashboard" color="inherit">
            Dashboard
          </Link>
          <Typography color="text.primary">Products</Typography>
        </Breadcrumbs>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
          <Typography variant="h4" component="h1">
            Products
          </Typography>
          <Box sx={{ display: 'flex', gap: 2 }}>
            <Button
              variant="outlined"
              startIcon={<UploadFileIcon />}
              onClick={() => setActiveTab(1)}
            >
              Upload CSV
            </Button>
            <Button
              variant="contained"
              startIcon={<AddIcon />}
              onClick={() => navigate('/products/create')}
            >
              Create Product
            </Button>
          </Box>
        </Box>

        <Tabs value={activeTab} onChange={(_, newValue) => setActiveTab(newValue)} sx={{ mb: 3 }}>
          <Tab label="Product List" />
          <Tab label="CSV Upload" />
        </Tabs>

        {activeTab === 0 && <ProductList products={products} isLoading={isLoading} error={error} />}

        {activeTab === 1 && (
          <ProductCsvUploadForm onUpload={handleUpload} isLoading={isUploading} />
        )}
      </Container>
    </>
  );
};
