import { Button, Typography } from '@mui/material';
import { useNavigate } from 'react-router-dom';
import { Product } from '../types/product';
import { Column, ResponsiveTable } from '../../../components/common';
import { Routes } from '../../../utils/navigationUtils';

interface ProductListProps {
  products: Product[];
  error: Error | null;
}

export const ProductList = ({ products, error }: ProductListProps) => {
  const navigate = useNavigate();

  if (error) {
    return <Typography color="error">Error loading products: {error.message}</Typography>;
  }

  // Defensive check: ensure products is an array
  if (!Array.isArray(products)) {
    return <Typography color="error">Invalid data format: products is not an array</Typography>;
  }

  const columns: Column<Product>[] = [
    {
      key: 'productCode',
      label: 'Product Code',
      render: product => (
        <Typography sx={{ fontFamily: 'monospace', fontWeight: 'medium' }}>
          {product.productCode}
        </Typography>
      ),
    },
    {
      key: 'description',
      label: 'Description',
      render: product => <Typography>{product.description}</Typography>,
    },
    {
      key: 'primaryBarcode',
      label: 'Primary Barcode',
      hideOnMobile: true,
      render: product => (
        <Typography sx={{ fontFamily: 'monospace' }}>{product.primaryBarcode}</Typography>
      ),
    },
    {
      key: 'unitOfMeasure',
      label: 'Unit of Measure',
      hideOnMobile: true,
      render: product => <Typography>{product.unitOfMeasure}</Typography>,
    },
    {
      key: 'category',
      label: 'Category',
      hideOnMobile: true,
      render: product => <Typography>{product.category || '-'}</Typography>,
    },
    {
      key: 'brand',
      label: 'Brand',
      hideOnMobile: true,
      render: product => <Typography>{product.brand || '-'}</Typography>,
    },
    {
      key: 'actions',
      label: 'Actions',
      hideOnMobile: true,
      render: product => (
        <Button size="small" onClick={() => navigate(Routes.productDetail(product.productId))}>
          View
        </Button>
      ),
    },
  ];

  return (
    <ResponsiveTable
      data={products}
      columns={columns}
      getRowKey={product => product.productId}
      onRowClick={product => navigate(Routes.productDetail(product.productId))}
      emptyMessage="No products found"
    />
  );
};
