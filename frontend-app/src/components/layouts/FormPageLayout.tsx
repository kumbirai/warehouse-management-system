import React, { ReactNode } from 'react';
import { Container, Alert } from '@mui/material';
import { Header } from '../layout/Header';
import { PageBreadcrumbs, BreadcrumbItem } from '../common/PageBreadcrumbs';
import { PageHeader } from '../common/PageHeader';

interface FormPageLayoutProps {
  breadcrumbs: BreadcrumbItem[];
  title: string;
  description?: string;
  error: string | null;
  children: ReactNode;
  maxWidth?: 'xs' | 'sm' | 'md' | 'lg' | 'xl';
}

export const FormPageLayout: React.FC<FormPageLayoutProps> = ({
  breadcrumbs,
  title,
  description,
  error,
  children,
  maxWidth = 'md',
}) => {
  return (
    <>
      <Header />
      <Container maxWidth={maxWidth} sx={{ py: 4 }}>
        <PageBreadcrumbs items={breadcrumbs} />
        <PageHeader title={title} description={description} />

        {error && (
          <Alert severity="error" sx={{ mb: 3 }}>
            {error}
          </Alert>
        )}

        {children}
      </Container>
    </>
  );
};
