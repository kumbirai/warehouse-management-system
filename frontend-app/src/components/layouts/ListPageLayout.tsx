import React, { ReactNode } from 'react';
import { Container, Alert } from '@mui/material';
import { Header } from '../layout/Header';
import { PageBreadcrumbs, BreadcrumbItem } from '../common/PageBreadcrumbs';
import { PageHeader } from '../common/PageHeader';
import { LoadingSpinner } from '../common/LoadingSpinner';

interface ListPageLayoutProps {
  breadcrumbs: BreadcrumbItem[];
  title: string;
  description?: string;
  actions?: ReactNode;
  isLoading: boolean;
  error: string | null;
  children: ReactNode;
  maxWidth?: 'xs' | 'sm' | 'md' | 'lg' | 'xl';
}

export const ListPageLayout: React.FC<ListPageLayoutProps> = ({
  breadcrumbs,
  title,
  description,
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
        <PageHeader title={title} description={description} actions={actions} />

        {error && (
          <Alert severity="error" sx={{ mb: 3 }}>
            {error}
          </Alert>
        )}

        {isLoading && <LoadingSpinner />}

        {!isLoading && children}
      </Container>
    </>
  );
};
