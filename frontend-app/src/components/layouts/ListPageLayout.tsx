import React, { ReactNode } from 'react';
import { Alert, Container } from '@mui/material';
import { Header } from '../layout/Header';
import { BreadcrumbItem, PageBreadcrumbs } from '../common/PageBreadcrumbs';
import { PageHeader } from '../common/PageHeader';
import { SkeletonTable } from '../common/SkeletonTable';

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
          <Alert severity="error" sx={{ mb: 3 }} role="alert" aria-live="assertive">
            {error}
          </Alert>
        )}

        {isLoading ? <SkeletonTable rows={5} columns={4} /> : children}
      </Container>
    </>
  );
};
