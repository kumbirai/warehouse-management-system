import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { Provider } from 'react-redux';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ThemeProvider } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';
import App from './App';
import { store } from './store';
import { theme } from './utils/theme';
import { ErrorBoundary } from './components/ErrorBoundary';
import { logger } from './utils/logger';
import './index.css';

// Validate environment variables
const requiredEnvVars = ['VITE_API_BASE_URL'];
const missingVars = requiredEnvVars.filter(
  varName => !import.meta.env[varName] && !import.meta.env.DEV
);

if (missingVars.length > 0 && !import.meta.env.DEV) {
  logger.warn('Missing environment variables', {
    missingVars,
    message: 'Using default values in development mode.',
  });
}

/**
 * QueryClient configuration for React Query.
 * Production-grade settings:
 * - Default stale time: 5 minutes (reduces unnecessary refetches)
 * - Default cache time: 10 minutes (keeps data in cache)
 * - Retry: 3 times with exponential backoff
 * - Refetch on window focus: disabled (prevents unnecessary refetches)
 * - Refetch on reconnect: enabled (syncs data when connection restored)
 */
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 5 * 60 * 1000, // 5 minutes
      gcTime: 10 * 60 * 1000, // 10 minutes (formerly cacheTime)
      retry: 3,
      refetchOnWindowFocus: false,
      refetchOnReconnect: true,
      refetchOnMount: true,
    },
    mutations: {
      retry: 1,
    },
  },
});

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <ErrorBoundary>
      <QueryClientProvider client={queryClient}>
        <Provider store={store}>
          <BrowserRouter
            future={{
              v7_startTransition: true,
              v7_relativeSplatPath: true,
            }}
          >
            <ThemeProvider theme={theme}>
              <CssBaseline />
              <App />
            </ThemeProvider>
          </BrowserRouter>
        </Provider>
      </QueryClientProvider>
    </ErrorBoundary>
  </React.StrictMode>
);
