import React, { ReactNode } from 'react';
import { Alert, Box, Container } from '@mui/material';
import { Header } from '../layout/Header';
import { BreadcrumbItem, PageBreadcrumbs } from '../common/PageBreadcrumbs';
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
      <Container maxWidth={maxWidth} sx={{ py: 4 }} component="main" role="main" aria-label={title}>
        <PageBreadcrumbs items={breadcrumbs} />
        <PageHeader title={title} description={description} />

        {error && (
          <Alert severity="error" sx={{ mb: 3 }} role="alert" aria-live="assertive">
            {error}
          </Alert>
        )}

        <Box role="region" aria-label="Form content">
          {children}
        </Box>
      </Container>
    </>
  );
};
