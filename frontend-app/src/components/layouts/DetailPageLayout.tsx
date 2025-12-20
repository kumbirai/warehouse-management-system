import React, { ReactNode } from 'react';
import { Container, Alert } from '@mui/material';
import { Header } from '../layout/Header';
import { PageBreadcrumbs, BreadcrumbItem } from '../common/PageBreadcrumbs';
import { PageHeader } from '../common/PageHeader';
import { LoadingSpinner } from '../common/LoadingSpinner';

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

        {isLoading && <LoadingSpinner />}

        {error && (
          <Alert severity="error" sx={{ mb: 3 }}>
            {error}
          </Alert>
        )}

        {!isLoading && !error && (
          <>
            <PageHeader title={title} actions={actions} />
            {children}
          </>
        )}
      </Container>
    </>
  );
};
