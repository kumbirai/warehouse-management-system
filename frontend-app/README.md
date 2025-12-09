# Frontend Application - Warehouse Management System

## Overview

Progressive Web App (PWA) frontend for the Warehouse Management System. Built with React, TypeScript, Material-UI, and Redux Toolkit.

## Features

- **Public Landing Page** - Marketing/informational page
- **Login Page** - Authentication via BFF
- **System Admin Dashboard** - For users with `SYSTEM_ADMIN` role
- **Tenant User Dashboard** - For users with `USER` role
- **HTTPS Support** - Configurable for development and production
- **Error Boundaries** - Graceful error handling
- **Route Protection** - Role-based access control

## Architecture

### Authentication Flow

1. User enters credentials on login page
2. Frontend calls BFF `/api/v1/bff/auth/login` endpoint
3. BFF authenticates with Keycloak and returns tokens + user context
4. Frontend stores tokens and user context
5. Frontend routes user to appropriate dashboard based on role

### State Management

- **Redux Toolkit** - Global state (auth, app settings)
- **React Context** - Theme, language preferences
- **Local Storage** - Tokens, user context (temporary)

### API Client

- **Axios** - HTTP client with interceptors
- **Automatic Token Injection** - Access token added to all requests
- **Automatic Token Refresh** - Refreshes expired tokens automatically
- **Error Handling** - Consistent error handling across all API calls

## Configuration

### Environment Variables

Create `.env` file (see `.env.example`):

```bash
# API Configuration
# Note: Gateway uses HTTPS, so use https:// for API connections
VITE_API_BASE_URL=https://localhost:8080/api/v1
VITE_API_TARGET=https://localhost:8080

# HTTPS Configuration (for development)
VITE_USE_HTTPS=false
VITE_HTTPS_KEY_PATH=./certs/localhost-key.pem
VITE_HTTPS_CERT_PATH=./certs/localhost.pem
```

### HTTPS Setup (Development)

1. Generate self-signed certificates:

```bash
mkdir -p certs
openssl req -x509 -newkey rsa:4096 -keyout certs/localhost-key.pem \
  -out certs/localhost.pem -days 365 -nodes \
  -subj "/CN=localhost"
```

2. Set `VITE_USE_HTTPS=true` in `.env`

3. Start dev server: `npm run dev`

## Development

### Prerequisites

- Node.js >= 18.0.0
- npm >= 9.0.0

### Setup

```bash
# Install dependencies
npm install

# Start development server
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview
```

### Project Structure

```
frontend-app/
├── src/
│   ├── components/          # Reusable UI components
│   │   ├── auth/           # Authentication components
│   │   └── layout/         # Layout components
│   ├── features/            # Feature modules
│   │   ├── admin/          # Admin dashboard
│   │   ├── auth/           # Authentication pages
│   │   ├── home/           # Landing page
│   │   └── user/           # User dashboard
│   ├── hooks/              # Custom React hooks
│   ├── services/           # API clients
│   ├── store/              # Redux store
│   └── types/              # TypeScript types
├── public/                 # Static assets
└── vite.config.ts          # Vite configuration
```

## Authentication

### Login Flow

1. User submits login form
2. `authService.login()` called with username/password
3. BFF returns tokens and user context
4. Tokens stored in localStorage
5. User context stored in Redux
6. User redirected to dashboard

### Token Management

- **Access Token**: Stored in localStorage, injected into API requests
- **Refresh Token**: Stored in localStorage, used for token refresh
- **Automatic Refresh**: API client automatically refreshes expired tokens
- **Logout**: Clears all tokens and user context

### Role-Based Routing

- `SYSTEM_ADMIN` → Admin Dashboard
- `USER` → User Dashboard
- No matching role → Redirected to landing page

## Security

### Best Practices

1. **HTTPS Only**: Always use HTTPS in production
2. **Token Storage**: Consider httpOnly cookies for production (requires backend changes)
3. **XSS Prevention**: All user input is sanitized
4. **CSP Headers**: Content Security Policy enforced by backend
5. **Error Messages**: Don't expose sensitive information in error messages

### Security Headers

Backend adds security headers:

- `X-Content-Type-Options: nosniff`
- `X-Frame-Options: DENY`
- `Strict-Transport-Security`
- `Content-Security-Policy`

## Error Handling

### Error Boundary

Global error boundary catches React errors and displays user-friendly error page.

### API Error Handling

- Network errors → User-friendly message
- Authentication errors → Redirect to login
- Validation errors → Display field-level errors
- Server errors → Generic error message

## Production Build

### Build Process

```bash
npm run build
```

Output: `dist/` directory with optimized production assets.

### Deployment

1. **Static Hosting**: Deploy `dist/` to static hosting (Netlify, Vercel, S3+CloudFront)
2. **Environment Variables**: Set production environment variables
3. **HTTPS**: Ensure HTTPS is enabled
4. **API Base URL**: Configure production API base URL

### Performance

- Code splitting by route
- Lazy loading for large components
- Optimized bundle size
- Service worker for offline support (PWA)

## Testing

```bash
# Run tests
npm test

# Run tests with coverage
npm run test:coverage

# Run tests in UI mode
npm run test:ui
```

## Troubleshooting

### Common Issues

1. **"Network Error"**
    - Check API base URL configuration
    - Verify backend services are running
    - Check CORS configuration

2. **"Invalid username or password"**
    - Verify user exists in Keycloak
    - Check BFF service logs
    - Verify Keycloak client secret configuration

3. **"Token refresh failed"**
    - Refresh token may be expired
    - User needs to login again
    - Check token expiration settings

4. **HTTPS Certificate Errors**
    - Accept self-signed certificate in browser
    - Or use valid certificate for development
