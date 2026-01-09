import React, { ReactNode } from 'react';
import { Alert, Container } from '@mui/material';
import { Header } from '../layout/Header';
import { BreadcrumbItem, PageBreadcrumbs } from '../common/PageBreadcrumbs';
import { PageHeader } from '../common/PageHeader';
import { SkeletonCard } from '../common/SkeletonCard';

interface DetailPageLayoutProps {
  breadcrumbs: BreadcrumbItem[];
  title: string;
  actions?: ReactNode;
  isLoading: boolean;
  error: string | null;
  children: ReactNode;
  maxWidth?: 'xs' | 'sm' | 'md' | 'lg' | 'xl';
}

export const DetailPageLayout: React.FC<DetailPageLayoutProps> = ({
  breadcrumbs,
  title,
  actions,
  isLoading,
  error,
  children,
  maxWidth = 'lg',
}) => {
  return (
    <>
      <Header />
      <Container maxWidth={maxWidth} sx={{ py: 4 }}>
        <PageBreadcrumbs items={breadcrumbs} />

        {isLoading ? (
          <SkeletonCard lines={6} />
        ) : (
          <>
            {error && (
              <Alert severity="error" sx={{ mb: 3 }} role="alert" aria-live="assertive">
                {error}
              </Alert>
            )}

            {!error && (
              <>
                <PageHeader title={title} actions={actions} />
                {children}
              </>
            )}
          </>
        )}
      </Container>
    </>
  );
};
