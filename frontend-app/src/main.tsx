import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { Provider } from 'react-redux';
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

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <ErrorBoundary>
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
    </ErrorBoundary>
  </React.StrictMode>
);
