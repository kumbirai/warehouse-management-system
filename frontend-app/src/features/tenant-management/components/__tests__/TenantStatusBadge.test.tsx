import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { TenantStatusBadge } from '../TenantStatusBadge';

describe('TenantStatusBadge', () => {
  it('renders provided status label', () => {
    render(<TenantStatusBadge status="PENDING" />);
    expect(screen.getByText('PENDING')).toBeInTheDocument();
  });
});
