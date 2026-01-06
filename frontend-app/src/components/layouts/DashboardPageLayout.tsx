import React, { ReactNode } from 'react';
import { Container, Box } from '@mui/material';
import { Header } from '../layout/Header';

interface DashboardPageLayoutProps {
  title: string;
  subtitle?: string;
  children: ReactNode;
  maxWidth?: 'xs' | 'sm' | 'md' | 'lg' | 'xl';
}

export const DashboardPageLayout: React.FC<DashboardPageLayoutProps> = ({
  title,
  subtitle,
  children,
  maxWidth = 'lg',
}) => {
  return (
    <>
      <Header />
      <Container maxWidth={maxWidth} component="main" role="main" aria-label={title} sx={{ py: 4 }}>
        <Box component="header" sx={{ mb: 4 }}>
          <Box component="h1" sx={{ typography: 'h4', mb: 1 }}>
            {title}
          </Box>
          {subtitle && (
            <Box component="p" sx={{ typography: 'body1', color: 'text.secondary', m: 0 }}>
              {subtitle}
            </Box>
          )}
        </Box>
        <Box role="region" aria-label="Dashboard content">
          {children}
        </Box>
      </Container>
    </>
  );
};
