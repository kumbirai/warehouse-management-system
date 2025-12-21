import { Button } from '@mui/material';
import { Edit as EditIcon } from '@mui/icons-material';
import { useNavigate, useParams } from 'react-router-dom';
import { DetailPageLayout } from '../../../components/layouts';
import { getBreadcrumbs, Routes } from '../../../utils/navigationUtils';
import { ProductDetail } from '../components/ProductDetail';
import { useProduct } from '../hooks/useProduct';
import { useAuth } from '../../../hooks/useAuth';

export const ProductDetailPage = () => {
  const { productId } = useParams<{ productId: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();

  // Call hooks unconditionally before any early returns
  const { product, isLoading, error, refetch } = useProduct(productId || '', user?.tenantId || '');

  if (!productId) {
    navigate(Routes.products);
    return null;
  }

  if (!user?.tenantId) {
    return (
      <DetailPageLayout
        breadcrumbs={getBreadcrumbs.productList()}
        title="Product Details"
        isLoading={false}
        error="Tenant ID is required to view product details"
      >
        <div />
      </DetailPageLayout>
    );
  }

  const canEdit =
    user?.roles?.some(role =>
      ['SYSTEM_ADMIN', 'TENANT_ADMIN', 'WAREHOUSE_MANAGER'].includes(role)
    ) ?? false;

  return (
    <DetailPageLayout
      breadcrumbs={getBreadcrumbs.productDetail(product?.productCode || '...')}
      title={product?.productCode || 'Loading...'}
      actions={
        <>
          <Button variant="outlined" onClick={() => navigate(Routes.products)}>
            Back to List
          </Button>
          {canEdit && (
            <Button
              variant="outlined"
              startIcon={<EditIcon />}
              onClick={() => navigate(Routes.productEdit(productId))}
            >
              Edit
            </Button>
          )}
        </>
      }
      isLoading={isLoading}
      error={error?.message || null}
      maxWidth="lg"
    >
      <ProductDetail product={product} onUpdate={refetch} />
    </DetailPageLayout>
  );
};
