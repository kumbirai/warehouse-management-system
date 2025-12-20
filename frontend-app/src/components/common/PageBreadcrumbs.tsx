import React from 'react';
import { Breadcrumbs as MuiBreadcrumbs, Link, Typography } from '@mui/material';
import { Link as RouterLink } from 'react-router-dom';

export interface BreadcrumbItem {
  label: string;
  href?: string;
}

interface PageBreadcrumbsProps {
  items: BreadcrumbItem[];
}

export const PageBreadcrumbs: React.FC<PageBreadcrumbsProps> = ({ items }) => {
  return (
    <MuiBreadcrumbs aria-label="breadcrumb" sx={{ mb: 3 }}>
      {items.map((item, index) => {
        const isLast = index === items.length - 1;

        if (isLast || !item.href) {
          return (
            <Typography key={index} color="text.primary">
              {item.label}
            </Typography>
          );
        }

        return (
          <Link
            key={index}
            component={RouterLink}
            to={item.href}
            underline="hover"
            color="inherit"
          >
            {item.label}
          </Link>
        );
      })}
    </MuiBreadcrumbs>
  );
};
