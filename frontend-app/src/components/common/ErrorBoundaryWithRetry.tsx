import { Component, ErrorInfo, ReactNode } from 'react';
import { Box, Button, Container, Typography } from '@mui/material';
import { Home as HomeIcon } from '@mui/icons-material';
import { Link as RouterLink } from 'react-router-dom';
import { Routes } from '../../utils/navigationUtils';
import { logger } from '../../utils/logger';

interface Props {
  children: ReactNode;
}

interface State {
  hasError: boolean;
  error: Error | null;
}

export class ErrorBoundaryWithRetry extends Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    logger.error('ErrorBoundaryWithRetry caught an error:', error, {
      componentStack: errorInfo.componentStack,
    });
  }

  handleRetry = () => {
    this.setState({ hasError: false, error: null });
  };

  render() {
    if (this.state.hasError) {
      return (
        <Container maxWidth="sm" sx={{ mt: 8, textAlign: 'center' }}>
          <Typography variant="h4" component="h1" gutterBottom>
            Something went wrong
          </Typography>
          <Typography variant="body1" color="text.secondary" sx={{ mt: 2, mb: 4 }}>
            {this.state.error?.message || 'An unexpected error occurred. Please try again.'}
          </Typography>
          <Box sx={{ display: 'flex', gap: 2, justifyContent: 'center', flexWrap: 'wrap' }}>
            <Button variant="contained" onClick={this.handleRetry} aria-label="Retry loading page">
              Retry
            </Button>
            <Button
              variant="outlined"
              component={RouterLink}
              to={Routes.dashboard}
              startIcon={<HomeIcon />}
              aria-label="Go to dashboard"
            >
              Go Home
            </Button>
          </Box>
        </Container>
      );
    }

    return this.props.children;
  }
}
