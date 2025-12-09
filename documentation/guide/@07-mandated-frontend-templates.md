# Mandated Frontend Templates

## Warehouse Management System Integration - CCBSA LDP System

**Document Version:** 1.0  
**Date:** 2025-01  
**Status:** Approved

---

## Overview

Templates for the **Frontend** application (`frontend-app`). Provides user interface with CQRS compliance.

---

## Package Structure

The Frontend application (`frontend-app`) follows a strict package structure to enforce CQRS and feature-based organization:

```
frontend-app/
├── src/
│   ├── components/                   # Reusable UI components
│   │   ├── common/                   # Common components (buttons, inputs, etc.)
│   │   └── {DomainObject}/           # Domain-specific components
│   │       └── {DomainObject}Form.tsx
│   ├── features/                     # Feature modules (CQRS separation)
│   │   └── {domain-object}/          # Feature for domain object
│   │       ├── commands/             # Command components
│   │       │   └── {Action}{DomainObject}Form.tsx
│   │       ├── queries/              # Query components
│   │       │   └── {DomainObject}List.tsx
│   │       └── hooks/               # Feature-specific hooks
│   │           └── use{DomainObject}.ts
│   ├── services/                     # API clients
│   │   ├── {service}ApiClient.ts     # Service API client
│   │   └── apiClient.ts              # Base API client
│   ├── store/                        # State management (Redux/Zustand)
│   │   ├── {domain-object}/          # Domain object store slice
│   │   │   ├── {domainObject}Slice.ts
│   │   │   └── {domainObject}Thunks.ts
│   │   └── store.ts                  # Store configuration
│   ├── types/                        # TypeScript types
│   │   ├── {domain-object}.ts        # Domain object types
│   │   └── api.ts                    # API types
│   ├── hooks/                        # Shared hooks
│   │   ├── useTenant.ts              # Tenant context hook
│   │   └── useApi.ts                 # API hook
│   ├── utils/                        # Utility functions
│   │   └── validation.ts             # Validation utilities
│   └── App.tsx                       # Root component
└── public/                           # Static assets
    └── locales/                      # i18n translations
```

**Package Naming Convention:**

- Base directory: `frontend-app/src`
- Feature directories: `features/{domain-object}` (kebab-case)
- Component files: `{ComponentName}.tsx` (PascalCase)
- Service files: `{service}ApiClient.ts` (camelCase)
- Type files: `{domain-object}.ts` (kebab-case)

**Package Responsibilities:**

| Package                             | Responsibility         | Contains                                               |
|-------------------------------------|------------------------|--------------------------------------------------------|
| `components`                        | Reusable UI components | Presentational components, shared across features      |
| `features`                          | Feature modules        | Feature-specific components organized by domain object |
| `features/{domain-object}/commands` | Command components     | UI components for write operations                     |
| `features/{domain-object}/queries`  | Query components       | UI components for read operations                      |
| `services`                          | API clients            | Service clients for backend API communication          |
| `store`                             | State management       | Redux/Zustand store slices and thunks                  |
| `types`                             | TypeScript types       | Type definitions for domain objects and API            |
| `hooks`                             | React hooks            | Custom hooks for shared logic                          |
| `utils`                             | Utility functions      | Helper functions and validators                        |

**Important Package Rules:**

- **CQRS separation**: Commands and queries in separate directories
- **Feature-based**: Organize by domain object/feature
- **Type safety**: All API calls typed with TypeScript
- **State management**: Centralized state in store
- **Reusability**: Common components in `components/common`

---

## Important Domain Driven Design, Clean Hexagonal Architecture principles, CQRS and Event Driven Design Notes

**Domain-Driven Design (DDD) Principles:**

- Frontend organized by domain features
- Components reflect domain concepts
- Domain types match backend domain model
- Ubiquitous language in component names

**Clean Hexagonal Architecture Principles:**

- API clients form the adapter layer
- Components are presentation layer
- Store manages application state
- Types define contracts with backend

**CQRS Principles:**

- **Command components**: Handle write operations, call command endpoints
- **Query components**: Handle read operations, call query endpoints
- **Separation**: Commands and queries use different components and API clients
- **Optimization**: Query components optimized for read performance

**Event-Driven Design Principles:**

- WebSocket connections for real-time event updates
- Event listeners update UI reactively
- Event correlation tracked through request headers
- Correlation ID generated per session and sent in X-Correlation-Id header
- Optimistic UI updates with event confirmation

---

## Correlation ID Service Template

```typescript
// services/correlationIdService.ts

import {logger} from '../utils/logger';

const CORRELATION_ID_STORAGE_KEY = 'wms_correlation_id';
const CORRELATION_ID_HEADER = 'X-Correlation-Id';

const generateCorrelationId = (): string => {
    return crypto.randomUUID();
};

export const getCorrelationId = (): string => {
    if (typeof window !== 'undefined') {
        const stored = sessionStorage.getItem(CORRELATION_ID_STORAGE_KEY);
        if (stored) {
            return stored;
        }
    }

    const correlationId = generateCorrelationId();
    
    if (typeof window !== 'undefined') {
        sessionStorage.setItem(CORRELATION_ID_STORAGE_KEY, correlationId);
        logger.debug('Generated new correlation ID', {correlationId});
    }

    return correlationId;
};

export const clearCorrelationId = (): void => {
    if (typeof window !== 'undefined') {
        sessionStorage.removeItem(CORRELATION_ID_STORAGE_KEY);
        logger.debug('Cleared correlation ID');
    }
};

export const getCorrelationIdHeader = (): string => {
    return CORRELATION_ID_HEADER;
};

export const correlationIdService = {
    getCorrelationId,
    clearCorrelationId,
    getCorrelationIdHeader,
};
```

## Base API Client Template (TypeScript)

```typescript
// services/apiClient.ts

import axios, {InternalAxiosRequestConfig} from 'axios';
import {correlationIdService} from './correlationIdService';

const apiClient = axios.create({
    baseURL: getBaseURL(),
    headers: {
        'Content-Type': 'application/json',
    },
    timeout: 30000,
    withCredentials: true,
});

apiClient.interceptors.request.use(
    (config: InternalAxiosRequestConfig) => {
        // Inject access token
        const token = tokenStorage.getAccessToken();
        if (token && config.headers) {
            config.headers.Authorization = `Bearer ${token}`;
        }

        // Inject correlation ID for traceability
        const correlationId = correlationIdService.getCorrelationId();
        if (config.headers) {
            config.headers[correlationIdService.getCorrelationIdHeader()] = correlationId;
        }

        return config;
    }
);

export default apiClient;
```

## Service API Client Template (TypeScript)

```typescript
// services/{service}ApiClient.ts

import apiClient from './apiClient';

export class {Service}ApiClient {
  private baseUrl: string;
  
  constructor(baseUrl: string) {
    this.baseUrl = baseUrl;
  }
  
  // Command operations
  async {action}{DomainObject}(
    id: string,
    tenantId: string,
    command: {Action}{DomainObject}CommandDTO
  ): Promise<{Action}{DomainObject}ResultDTO> {
    const response = await apiClient.post<{Action}{DomainObject}ResultDTO>(
      `/${domain-objects}/${id}/${action}`,
      command,
      {
        headers: {
          'X-Tenant-Id': tenantId,
        },
      }
    );
    return response.data;
  }
  
  // Query operations
  async get{DomainObject}(id: string, tenantId: string): Promise<{DomainObject}QueryResultDTO> {
    const response = await apiClient.get<{DomainObject}QueryResultDTO>(
      `/${domain-objects}/${id}`,
      {
        headers: {
          'X-Tenant-Id': tenantId,
        },
      }
    );
    return response.data;
  }
}
```

## React Component Template

```typescript
// components/{DomainObject}Form.tsx

import React from 'react';
import { useForm } from 'react-hook-form';

interface {DomainObject}FormProps {
  onSubmit: (data: Create{DomainObject}CommandDTO) => void;
  isLoading: boolean;
}

export const {DomainObject}Form: React.FC<{DomainObject}FormProps> = ({
  onSubmit,
  isLoading,
}) => {
  const { register, handleSubmit, formState: { errors } } = useForm<Create{DomainObject}CommandDTO>();
  
  return (
    <form onSubmit={handleSubmit(onSubmit)}>
      {/* Form fields */}
      <button type="submit" disabled={isLoading}>
        {isLoading ? 'Submitting...' : 'Submit'}
      </button>
    </form>
  );
};
```

## Traceability Requirements

**Correlation ID Flow:**

1. **Session Management**: Correlation ID generated per user session, stored in sessionStorage
2. **Request Injection**: All API requests automatically include X-Correlation-Id header via API client interceptor
3. **Logging**: All log entries automatically include correlation ID for log correlation
4. **Session Lifecycle**: Correlation ID cleared on logout, new ID generated on new session

**Implementation Checklist:**

- [ ] Correlation ID service created and configured
- [ ] API client interceptor injects X-Correlation-Id header
- [ ] Logger includes correlation ID in all log entries
- [ ] Correlation ID cleared on logout
- [ ] Correlation ID persists across page reloads (sessionStorage)

---

**Document Control**

- **Version History:** v1.0 (2025-01) - Initial template creation
- **Review Cycle:** Review when frontend patterns change

