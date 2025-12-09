# Frontend Architecture Document

## Warehouse Management System Integration - CCBSA LDP System

**Document Version:** 1.0  
**Date:** 2025-11  
**Status:** Draft  
**Related Documents:**

- [Business Requirements Document](../00-business-requiremants/business-requirements-document.md)
- [Service Architecture Document](Service_Architecture_Document.md)
- [Project Roadmap](../project-management/project-roadmap.md)

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture Principles](#architecture-principles)
3. [Frontend Technology Stack](#frontend-technology-stack)
4. [Progressive Web App (PWA) Architecture](#progressive-web-app-pwa-architecture)
5. [Offline-First Design and Data Synchronization](#offline-first-design-and-data-synchronization)
6. [Component Architecture](#component-architecture)
7. [State Management Strategy](#state-management-strategy)
8. [Barcode Scanning Integration](#barcode-scanning-integration)
9. [Responsive Design Guidelines](#responsive-design-guidelines)
10. [Accessibility Requirements](#accessibility-requirements)
11. [Multi-Language Support](#multi-language-support)
12. [Security Architecture](#security-architecture)
13. [Performance Optimization](#performance-optimization)
14. [Testing Strategy](#testing-strategy)
15. [Deployment Architecture](#deployment-architecture)

---

## Overview

### Purpose

This document defines the frontend architecture for the Warehouse Management System Integration. The frontend is implemented as a **Progressive Web App (PWA)** that provides an
app-like experience on mobile devices while maintaining full functionality on desktop and tablet devices.

### Key Requirements

- **Progressive Web App (PWA)** with offline support and installation capability
- **Offline-First Design** with automatic data synchronization
- **Barcode Scanning** support (handheld scanners and mobile device cameras)
- **Responsive Design** for desktop, tablet, and mobile
- **Accessibility Compliance** (WCAG 2.1 Level AA)
- **Multi-Language Support** (English, Afrikaans, Zulu)
- **Real-Time Updates** for critical operations
- **Electronic Stock Count Worksheets** (eliminating paper transcription)

### Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│                    Frontend PWA                          │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  ┌──────────────────────────────────────────────────┐   │
│  │           Presentation Layer                     │   │
│  │  - React Components                              │   │
│  │  - UI Components (Material-UI / Ant Design)     │   │
│  │  - Barcode Scanner Components                    │   │
│  └──────────────────────────────────────────────────┘   │
│                                                         │
│  ┌──────────────────────────────────────────────────┐   │
│  │           Application Layer                      │   │
│  │  - State Management (Redux Toolkit / Zustand)   │   │
│  │  - API Client (Axios / Fetch)                    │   │
│  │  - Business Logic                                │   │
│  └──────────────────────────────────────────────────┘   │
│                                                         │
│  ┌──────────────────────────────────────────────────┐   │
│  │           Data Layer                             │   │
│  │  - IndexedDB (Offline Storage)                  │   │
│  │  - LocalStorage (Settings/Cache)                │   │
│  │  - Service Worker (Offline Support)            │   │
│  │  - Sync Manager (Data Synchronization)          │   │
│  └──────────────────────────────────────────────────┘   │
│                                                         │
│  ┌──────────────────────────────────────────────────┐   │
│  │           Infrastructure Layer                   │   │
│  │  - Service Worker (Workbox)                     │   │
│  │  - Web Push Notifications                        │   │
│  │  - Background Sync                               │   │
│  │  - Network Detection                             │   │
│  └──────────────────────────────────────────────────┘   │
│                                                         │
└───────────────────────────┬─────────────────────────────┘
                            │
                            │ REST API (HTTPS)
                            │
┌───────────────────────────┴─────────────────────────────┐
│              API Gateway (Spring Cloud Gateway)         │
└─────────────────────────────────────────────────────────┘
```

---

## Architecture Principles

### Core Principles

1. **Offline-First** - Application works offline by default, syncs when online
2. **Progressive Enhancement** - Core functionality works without JavaScript, enhanced with JS
3. **Mobile-First** - Design and development start with mobile, scale up to desktop
4. **Component-Based** - Reusable, composable components
5. **Unidirectional Data Flow** - Predictable state management
6. **Accessibility First** - WCAG 2.1 Level AA compliance from the start
7. **Performance First** - Optimize for low-end devices and slow networks
8. **Security First** - Secure by default, defense in depth

### Design Patterns

- **Container/Presentational Components** - Separation of logic and presentation
- **Custom Hooks** - Reusable business logic
- **Higher-Order Components (HOCs)** - Cross-cutting concerns
- **Render Props** - Flexible component composition
- **Context API** - Global state (theme, language, user)

---

## Frontend Technology Stack

### Core Framework

**React 18+**

- **Rationale:** Industry-standard, excellent PWA support, large ecosystem
- **Key Features:** Concurrent rendering, Suspense, Server Components (future)
- **TypeScript Support:** Full TypeScript support for type safety

### Build Tools

**Vite**

- **Rationale:** Fast build tool, excellent development experience, native ES modules
- **Features:** Hot Module Replacement (HMR), optimized production builds
- **Alternative:** Create React App (CRA) or Next.js (if SSR needed)

**TypeScript 5+**

- **Rationale:** Type safety, better IDE support, reduced runtime errors
- **Strict Mode:** Enabled for maximum type safety

### UI Framework

**Material-UI (MUI) v5+**

- **Rationale:** Comprehensive component library, accessibility built-in, theming support
- **Features:** Material Design 3, dark mode, responsive grid system
- **Alternative:** Ant Design, Chakra UI

### State Management

**Redux Toolkit (RTK) + RTK Query**

- **Rationale:** Predictable state management, excellent DevTools, built-in async handling
- **Features:** Redux Toolkit for synchronous state, RTK Query for server state
- **Alternative:** Zustand, Jotai, Recoil

### Routing

**React Router v6+**

- **Rationale:** Industry standard, excellent code splitting support
- **Features:** Nested routing, route-based code splitting, protected routes

### Form Management

**React Hook Form**

- **Rationale:** Performance-focused, minimal re-renders, excellent validation
- **Validation:** Zod or Yup for schema validation
- **Alternative:** Formik

### HTTP Client

**Axios**

- **Rationale:** Interceptors, request/response transformation, cancel tokens
- **Features:** Request/response interceptors for auth, error handling
- **Alternative:** Fetch API with custom wrapper

### Offline & Storage

**IndexedDB**

- **Library:** Dexie.js or idb
- **Purpose:** Offline data storage, large datasets
- **Usage:** Stock counts, consignments, picking lists

**LocalStorage**

- **Purpose:** User preferences, settings, cache
- **Usage:** Language preference, theme, recent searches

**Service Worker**

- **Library:** Workbox
- **Purpose:** Offline support, background sync, push notifications
- **Features:** Caching strategies, background sync, push notifications

### Barcode Scanning

**ZXing (Zebra Crossing)**

- **Library:** @zxing/library (JavaScript port)
- **Purpose:** Barcode scanning from camera
- **Features:** Multiple barcode formats (EAN-13, Code 128, QR Code, etc.)

**Barcode Scanner API (if available)**

- **Purpose:** Native browser barcode scanning (Chrome/Edge)
- **Fallback:** ZXing camera-based scanning

### Internationalization (i18n)

**react-i18next**

- **Rationale:** Industry standard, excellent React integration
- **Features:** Namespace support, lazy loading translations, pluralization
- **Translation Management:** i18next-http-backend for loading translations

### Testing

**Unit Testing:**

- **Vitest** - Fast unit test runner (Vite-native)
- **React Testing Library** - Component testing
- **Jest** - Alternative (if not using Vite)

**Integration Testing:**

- **React Testing Library** - Integration tests
- **MSW (Mock Service Worker)** - API mocking

**End-to-End Testing:**

- **Playwright** - Cross-browser E2E testing
- **Cypress** - Alternative

**Visual Regression:**

- **Chromatic** - Component visual testing
- **Percy** - Visual regression testing

### Code Quality

**ESLint**

- **Config:** ESLint + TypeScript ESLint
- **Rules:** Airbnb or Standard + custom rules

**Prettier**

- **Purpose:** Code formatting
- **Integration:** ESLint integration

**Husky**

- **Purpose:** Git hooks
- **Hooks:** Pre-commit (lint, format, test), pre-push (test)

**lint-staged**

- **Purpose:** Run linters on staged files only

### Performance Monitoring

**Web Vitals**

- **Library:** web-vitals
- **Metrics:** LCP, FID, CLS, FCP, TTFB

**Error Tracking**

- **Sentry** - Error tracking and monitoring
- **Alternative:** LogRocket, Bugsnag

---

## Progressive Web App (PWA) Architecture

### PWA Requirements

#### 1. Web App Manifest

**File:** `manifest.json`

```json
{
  "name": "CCBSA Warehouse Management",
  "short_name": "CCBSA WMS",
  "description": "Warehouse Management System for Local Distribution Partners",
  "start_url": "/",
  "display": "standalone",
  "background_color": "#ffffff",
  "theme_color": "#1976d2",
  "orientation": "any",
  "icons": [
    {
      "src": "/icons/icon-72x72.png",
      "sizes": "72x72",
      "type": "image/png",
      "purpose": "any maskable"
    },
    {
      "src": "/icons/icon-96x96.png",
      "sizes": "96x96",
      "type": "image/png",
      "purpose": "any maskable"
    },
    {
      "src": "/icons/icon-128x128.png",
      "sizes": "128x128",
      "type": "image/png",
      "purpose": "any maskable"
    },
    {
      "src": "/icons/icon-144x144.png",
      "sizes": "144x144",
      "type": "image/png",
      "purpose": "any maskable"
    },
    {
      "src": "/icons/icon-152x152.png",
      "sizes": "152x152",
      "type": "image/png",
      "purpose": "any maskable"
    },
    {
      "src": "/icons/icon-192x192.png",
      "sizes": "192x192",
      "type": "image/png",
      "purpose": "any maskable"
    },
    {
      "src": "/icons/icon-384x384.png",
      "sizes": "384x384",
      "type": "image/png",
      "purpose": "any maskable"
    },
    {
      "src": "/icons/icon-512x512.png",
      "sizes": "512x512",
      "type": "image/png",
      "purpose": "any maskable"
    }
  ],
  "shortcuts": [
    {
      "name": "Stock Count",
      "short_name": "Count",
      "description": "Start stock count",
      "url": "/stock-count",
      "icons": [{ "src": "/icons/shortcut-stock-count.png", "sizes": "96x96" }]
    },
    {
      "name": "Picking",
      "short_name": "Pick",
      "description": "View picking tasks",
      "url": "/picking",
      "icons": [{ "src": "/icons/shortcut-picking.png", "sizes": "96x96" }]
    }
  ],
  "categories": ["business", "productivity"],
  "screenshots": []
}
```

#### 2. Service Worker

**File:** `sw.js` (generated by Workbox)

**Caching Strategies:**

- **Network First:** API calls (with fallback to cache)
- **Cache First:** Static assets (images, fonts, CSS, JS)
- **Stale While Revalidate:** App shell, HTML
- **Network Only:** Critical operations (consignment confirmation, reconciliation)

**Workbox Configuration:**

```javascript
// workbox-config.js
module.exports = {
  globDirectory: 'dist/',
  globPatterns: ['**/*.{js,css,html,png,svg,jpg,jpeg,woff,woff2}'],
  swDest: 'dist/sw.js',
  runtimeCaching: [
    {
      urlPattern: /^https:\/\/api\.example\.com\/api\/.*/i,
      handler: 'NetworkFirst',
      options: {
        cacheName: 'api-cache',
        networkTimeoutSeconds: 10,
        cacheableResponse: {
          statuses: [0, 200]
        }
      }
    },
    {
      urlPattern: /\.(?:png|jpg|jpeg|svg|gif)$/,
      handler: 'CacheFirst',
      options: {
        cacheName: 'image-cache',
        expiration: {
          maxEntries: 100,
          maxAgeSeconds: 30 * 24 * 60 * 60 // 30 days
        }
      }
    }
  ]
};
```

#### 3. Installability

**Install Prompt:**

- Custom install prompt for browsers that support it
- "Add to Home Screen" instructions for iOS Safari
- Install button in app header/navigation

**Install Criteria:**

- HTTPS (required)
- Valid manifest.json
- Service worker registered
- Icons provided (at least 192x192 and 512x512)

#### 4. App Shell Architecture

**App Shell:**

- Minimal HTML, CSS, JavaScript for initial render
- Navigation, header, footer
- Cached for instant loading

**Content:**

- Dynamic content loaded after shell
- Cached separately with appropriate strategy

---

## Offline-First Design and Data Synchronization

### Offline-First Principles

1. **Assume Offline** - Application works offline by default
2. **Cache Aggressively** - Cache all necessary data for offline use
3. **Queue Operations** - Queue write operations when offline
4. **Sync Automatically** - Automatically sync when online
5. **Conflict Resolution** - Handle conflicts gracefully

### Offline Data Storage

#### IndexedDB Schema

**Database:** `wms_offline_db`

**Object Stores:**

1. **stockCounts**
    - Key: `id` (UUID)
    - Indexes: `worksheetId`, `status`, `timestamp`
    - Data: Stock count entries

2. **consignments**
    - Key: `id` (UUID)
    - Indexes: `consignmentReference`, `status`, `timestamp`
    - Data: Consignment data

3. **pickingTasks**
    - Key: `id` (UUID)
    - Indexes: `loadId`, `status`, `timestamp`
    - Data: Picking task data

4. **stockMovements**
    - Key: `id` (UUID)
    - Indexes: `status`, `timestamp`
    - Data: Stock movement data

5. **syncQueue**
    - Key: `id` (UUID)
    - Indexes: `status`, `priority`, `timestamp`
    - Data: Queued operations for sync

6. **settings**
    - Key: `key` (string)
    - Data: User settings, preferences

**Dexie.js Schema:**

```typescript
import Dexie, { Table } from 'dexie';

interface StockCount {
  id: string;
  worksheetId: string;
  locationId: string;
  productId: string;
  quantity: number;
  status: 'pending' | 'synced' | 'error';
  timestamp: number;
  syncedAt?: number;
}

interface SyncQueueItem {
  id: string;
  operation: 'create' | 'update' | 'delete';
  entity: string;
  data: any;
  status: 'pending' | 'syncing' | 'synced' | 'error';
  priority: number;
  timestamp: number;
  retryCount: number;
  error?: string;
}

class WMSOfflineDB extends Dexie {
  stockCounts!: Table<StockCount>;
  consignments!: Table<any>;
  pickingTasks!: Table<any>;
  stockMovements!: Table<any>;
  syncQueue!: Table<SyncQueueItem>;
  settings!: Table<any>;

  constructor() {
    super('WMSOfflineDB');
    this.version(1).stores({
      stockCounts: 'id, worksheetId, status, timestamp',
      consignments: 'id, consignmentReference, status, timestamp',
      pickingTasks: 'id, loadId, status, timestamp',
      stockMovements: 'id, status, timestamp',
      syncQueue: 'id, status, priority, timestamp',
      settings: 'key'
    });
  }
}

export const db = new WMSOfflineDB();
```

### Data Synchronization Strategy

#### Sync Manager Architecture

```typescript
class SyncManager {
  private syncQueue: SyncQueueItem[] = [];
  private isOnline: boolean = navigator.onLine;
  private isSyncing: boolean = false;

  constructor() {
    this.setupNetworkListeners();
    this.setupBackgroundSync();
  }

  // Queue operation for sync
  async queueOperation(
    operation: 'create' | 'update' | 'delete',
    entity: string,
    data: any,
    priority: number = 0
  ): Promise<void> {
    const queueItem: SyncQueueItem = {
      id: uuid(),
      operation,
      entity,
      data,
      status: 'pending',
      priority,
      timestamp: Date.now(),
      retryCount: 0
    };

    await db.syncQueue.add(queueItem);

    if (this.isOnline && !this.isSyncing) {
      this.startSync();
    }
  }

  // Start sync process
  async startSync(): Promise<void> {
    if (this.isSyncing || !this.isOnline) return;

    this.isSyncing = true;

    try {
      const pendingItems = await db.syncQueue
        .where('status')
        .equals('pending')
        .sortBy('priority');

      for (const item of pendingItems) {
        await this.syncItem(item);
      }
    } finally {
      this.isSyncing = false;
    }
  }

  // Sync individual item
  private async syncItem(item: SyncQueueItem): Promise<void> {
    try {
      await db.syncQueue.update(item.id, { status: 'syncing' });

      // Perform API call based on operation type
      const response = await this.performSyncOperation(item);

      // Mark as synced
      await db.syncQueue.update(item.id, {
        status: 'synced',
        syncedAt: Date.now()
      });

      // Remove from queue after successful sync
      await db.syncQueue.delete(item.id);
    } catch (error) {
      // Handle error
      const retryCount = item.retryCount + 1;
      const maxRetries = 3;

      if (retryCount < maxRetries) {
        await db.syncQueue.update(item.id, {
          status: 'pending',
          retryCount,
          error: error.message
        });
      } else {
        await db.syncQueue.update(item.id, {
          status: 'error',
          error: error.message
        });
      }
    }
  }

  // Background sync registration
  private setupBackgroundSync(): void {
    if ('serviceWorker' in navigator && 'sync' in window.ServiceWorkerRegistration.prototype) {
      navigator.serviceWorker.ready.then(registration => {
        registration.sync.register('sync-data');
      });
    }
  }

  // Network status listeners
  private setupNetworkListeners(): void {
    window.addEventListener('online', () => {
      this.isOnline = true;
      this.startSync();
    });

    window.addEventListener('offline', () => {
      this.isOnline = false;
    });
  }
}
```

#### Conflict Resolution

**Strategies:**

1. **Last-Write-Wins (Default)**
    - Most recent timestamp wins
    - Simple, fast
    - May lose data

2. **Manual Resolution**
    - Present conflicts to user
    - User chooses resolution
    - Better data integrity

3. **Server Wins**
    - Server data always takes precedence
    - Simple, but may lose local changes

**Implementation:**

```typescript
interface ConflictResolution {
  strategy: 'last-write-wins' | 'manual' | 'server-wins';
  conflicts: Conflict[];
}

interface Conflict {
  id: string;
  entity: string;
  localData: any;
  serverData: any;
  timestamp: number;
}

class ConflictResolver {
  async resolveConflicts(conflicts: Conflict[]): Promise<void> {
    for (const conflict of conflicts) {
      const resolution = await this.resolveConflict(conflict);
      await this.applyResolution(resolution);
    }
  }

  private async resolveConflict(conflict: Conflict): Promise<Resolution> {
    // Implement resolution logic based on strategy
    // For manual resolution, show UI to user
  }
}
```

### Offline Indicators

**Visual Indicators:**

- Online/Offline badge in header
- Toast notification when going offline/online
- Disabled state for operations requiring online connection
- Sync status indicator

**Implementation:**

```typescript
const useOnlineStatus = () => {
  const [isOnline, setIsOnline] = useState(navigator.onLine);

  useEffect(() => {
    const handleOnline = () => setIsOnline(true);
    const handleOffline = () => setIsOnline(false);

    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);

    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
    };
  }, []);

  return isOnline;
};
```

---

## Component Architecture

### Component Structure

```
src/
├── components/           # Reusable UI components
│   ├── common/          # Common components (Button, Input, etc.)
│   ├── layout/          # Layout components (Header, Sidebar, etc.)
│   ├── forms/           # Form components
│   └── features/        # Feature-specific components
├── features/            # Feature modules
│   ├── stock-count/     # Stock count feature
│   │   ├── components/  # Feature components
│   │   ├── hooks/       # Feature hooks
│   │   ├── services/    # Feature services
│   │   └── types/       # Feature types
│   ├── picking/         # Picking feature
│   ├── consignment/     # Consignment feature
│   └── returns/         # Returns feature
├── hooks/               # Shared hooks
├── services/            # Shared services
├── store/               # Redux store
├── utils/               # Utility functions
└── types/               # Shared TypeScript types
```

### Component Patterns

#### 1. Container/Presentational Pattern

**Container Component (Smart):**

- Handles data fetching and state management
- Passes data and callbacks to presentational components

**Presentational Component (Dumb):**

- Receives props, renders UI
- No business logic, easily testable

**Example:**

```typescript
// Container
const StockCountContainer = () => {
  const { data, isLoading, error } = useStockCount();
  const handleSave = useSaveStockCount();

  return (
    <StockCountForm
      data={data}
      isLoading={isLoading}
      error={error}
      onSave={handleSave}
    />
  );
};

// Presentational
interface StockCountFormProps {
  data: StockCountData;
  isLoading: boolean;
  error: Error | null;
  onSave: (data: StockCountData) => void;
}

const StockCountForm: React.FC<StockCountFormProps> = ({
  data,
  isLoading,
  error,
  onSave
}) => {
  // Render form UI
};
```

#### 2. Custom Hooks Pattern

**Business Logic Hooks:**

```typescript
// hooks/useStockCount.ts
export const useStockCount = (worksheetId: string) => {
  const [data, setData] = useState<StockCountData | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  useEffect(() => {
    loadStockCount(worksheetId)
      .then(setData)
      .catch(setError)
      .finally(() => setIsLoading(false));
  }, [worksheetId]);

  const saveStockCount = async (data: StockCountData) => {
    try {
      await saveStockCountToDB(data);
      await syncManager.queueOperation('update', 'stockCount', data);
    } catch (err) {
      setError(err);
    }
  };

  return { data, isLoading, error, saveStockCount };
};
```

#### 3. Compound Components Pattern

**For Complex UI:**

```typescript
const StockCountForm = {
  Root: ({ children }) => <form>{children}</form>,
  LocationSection: ({ locationId }) => (
    <LocationScanner locationId={locationId} />
  ),
  ProductSection: ({ productId }) => (
    <ProductScanner productId={productId} />
  ),
  QuantityInput: ({ value, onChange }) => (
    <Input type="number" value={value} onChange={onChange} />
  ),
  SubmitButton: ({ onClick }) => (
    <Button onClick={onClick}>Save Count</Button>
  )
};

// Usage
<StockCountForm.Root>
  <StockCountForm.LocationSection locationId={locationId} />
  <StockCountForm.ProductSection productId={productId} />
  <StockCountForm.QuantityInput value={quantity} onChange={setQuantity} />
  <StockCountForm.SubmitButton onClick={handleSubmit} />
</StockCountForm.Root>
```

### Key Components

#### 1. Barcode Scanner Component

```typescript
interface BarcodeScannerProps {
  onScan: (barcode: string) => void;
  onError?: (error: Error) => void;
  formats?: BarcodeFormat[];
  continuous?: boolean;
}

const BarcodeScanner: React.FC<BarcodeScannerProps> = ({
  onScan,
  onError,
  formats = ['EAN_13', 'CODE_128', 'QR_CODE'],
  continuous = false
}) => {
  const videoRef = useRef<HTMLVideoElement>(null);
  const [isScanning, setIsScanning] = useState(false);

  useEffect(() => {
    if (isScanning && videoRef.current) {
      const codeReader = new BrowserMultiFormatReader();
      codeReader.decodeFromVideoDevice(undefined, videoRef.current, (result, err) => {
        if (result) {
          onScan(result.getText());
          if (!continuous) {
            setIsScanning(false);
          }
        }
        if (err) {
          onError?.(err);
        }
      });
    }

    return () => {
      // Cleanup
    };
  }, [isScanning, continuous, onScan, onError]);

  return (
    <div>
      <video ref={videoRef} style={{ width: '100%' }} />
      <Button onClick={() => setIsScanning(!isScanning)}>
        {isScanning ? 'Stop Scanning' : 'Start Scanning'}
      </Button>
    </div>
  );
};
```

#### 2. Offline Indicator Component

```typescript
const OfflineIndicator: React.FC = () => {
  const isOnline = useOnlineStatus();
  const syncStatus = useSyncStatus();

  if (isOnline) return null;

  return (
    <Alert severity="warning">
      <AlertTitle>Offline Mode</AlertTitle>
      You are currently offline. Changes will be synced when connection is restored.
      {syncStatus.pendingCount > 0 && (
        <div>{syncStatus.pendingCount} items pending sync</div>
      )}
    </Alert>
  );
};
```

#### 3. Stock Count Worksheet Component

```typescript
const StockCountWorksheet: React.FC<{ worksheetId: string }> = ({
  worksheetId
}) => {
  const { data, saveEntry } = useStockCountWorksheet(worksheetId);
  const [currentLocation, setCurrentLocation] = useState<string | null>(null);
  const [currentProduct, setCurrentProduct] = useState<string | null>(null);
  const [quantity, setQuantity] = useState<number>(0);

  const handleLocationScan = (barcode: string) => {
    const location = findLocationByBarcode(barcode);
    if (location) {
      setCurrentLocation(location.id);
    }
  };

  const handleProductScan = (barcode: string) => {
    const product = findProductByBarcode(barcode);
    if (product) {
      setCurrentProduct(product.id);
    }
  };

  const handleSave = () => {
    if (currentLocation && currentProduct && quantity > 0) {
      saveEntry({
        locationId: currentLocation,
        productId: currentProduct,
        quantity
      });
      // Reset
      setCurrentProduct(null);
      setQuantity(0);
    }
  };

  return (
    <Paper>
      <Typography variant="h5">Stock Count Worksheet</Typography>
      
      <BarcodeScanner
        label="Scan Location"
        onScan={handleLocationScan}
        continuous={false}
      />
      
      {currentLocation && (
        <>
          <BarcodeScanner
            label="Scan Product"
            onScan={handleProductScan}
            continuous={false}
          />
          
          {currentProduct && (
            <>
              <TextField
                label="Quantity"
                type="number"
                value={quantity}
                onChange={(e) => setQuantity(Number(e.target.value))}
              />
              <Button onClick={handleSave}>Save Entry</Button>
            </>
          )}
        </>
      )}
      
      <StockCountEntriesList entries={data.entries} />
    </Paper>
  );
};
```

---

## State Management Strategy

### State Management Architecture

**Redux Toolkit for Global State:**

- User authentication
- Application settings (theme, language)
- Offline/online status
- Sync queue status

**RTK Query for Server State:**

- API data caching
- Automatic refetching
- Optimistic updates
- Cache invalidation

**Local State (useState/useReducer):**

- Component-specific state
- Form state
- UI state (modals, dropdowns)

### Redux Store Structure

```typescript
interface RootState {
  auth: AuthState;
  app: AppState;
  sync: SyncState;
  // Feature slices
  stockCount: StockCountState;
  picking: PickingState;
  consignment: ConsignmentState;
}

// Store configuration
const store = configureStore({
  reducer: {
    auth: authSlice.reducer,
    app: appSlice.reducer,
    sync: syncSlice.reducer,
    stockCount: stockCountSlice.reducer,
    picking: pickingSlice.reducer,
    consignment: consignmentSlice.reducer,
    // RTK Query API
    [api.reducerPath]: api.reducer
  },
  middleware: (getDefaultMiddleware) =>
    getDefaultMiddleware().concat(api.middleware)
});
```

### RTK Query API Slice

```typescript
const api = createApi({
  reducerPath: 'api',
  baseQuery: fetchBaseQuery({
    baseUrl: '/api',
    prepareHeaders: (headers, { getState }) => {
      const token = (getState() as RootState).auth.token;
      if (token) {
        headers.set('authorization', `Bearer ${token}`);
      }
      return headers;
    }
  }),
  tagTypes: ['StockCount', 'Picking', 'Consignment', 'Location'],
  endpoints: (builder) => ({
    getStockCount: builder.query<StockCount, string>({
      query: (id) => `stock-count/${id}`,
      providesTags: ['StockCount']
    }),
    saveStockCount: builder.mutation<StockCount, StockCount>({
      query: (data) => ({
        url: `stock-count/${data.id}`,
        method: 'PUT',
        body: data
      }),
      invalidatesTags: ['StockCount']
    })
  })
});
```

### Offline State Management

**Sync Queue State:**

```typescript
interface SyncState {
  queue: SyncQueueItem[];
  isSyncing: boolean;
  lastSyncTime: number | null;
  error: Error | null;
}

const syncSlice = createSlice({
  name: 'sync',
  initialState: {
    queue: [],
    isSyncing: false,
    lastSyncTime: null,
    error: null
  } as SyncState,
  reducers: {
    addToQueue: (state, action: PayloadAction<SyncQueueItem>) => {
      state.queue.push(action.payload);
    },
    removeFromQueue: (state, action: PayloadAction<string>) => {
      state.queue = state.queue.filter(item => item.id !== action.payload);
    },
    setSyncing: (state, action: PayloadAction<boolean>) => {
      state.isSyncing = action.payload;
    },
    setLastSyncTime: (state, action: PayloadAction<number>) => {
      state.lastSyncTime = action.payload;
    }
  }
});
```

---

## Barcode Scanning Integration

### Barcode Scanning Architecture

**Multiple Input Methods:**

1. **Handheld Scanner (USB/Bluetooth)**
    - Acts as keyboard input
    - No special integration needed
    - Capture via input field focus

2. **Mobile Device Camera**
    - ZXing library for scanning
    - Camera API access
    - Real-time scanning

3. **Native Barcode Scanner API (Chrome/Edge)**
    - Browser-native API
    - Better performance
    - Fallback to ZXing

### Implementation

#### 1. Universal Barcode Input Component

```typescript
interface BarcodeInputProps {
  onScan: (barcode: string) => void;
  onError?: (error: Error) => void;
  placeholder?: string;
  autoFocus?: boolean;
  validate?: (barcode: string) => boolean;
}

const BarcodeInput: React.FC<BarcodeInputProps> = ({
  onScan,
  onError,
  placeholder = "Scan or enter barcode",
  autoFocus = true,
  validate
}) => {
  const inputRef = useRef<HTMLInputElement>(null);
  const [barcode, setBarcode] = useState('');
  const [isScanning, setIsScanning] = useState(false);

  // Handle keyboard input (handheld scanner)
  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      if (validate ? validate(barcode) : barcode.length > 0) {
        onScan(barcode);
        setBarcode('');
      }
    }
  };

  // Camera scanning
  const handleCameraScan = () => {
    setIsScanning(true);
    // Implement camera scanning
  };

  return (
    <Box>
      <TextField
        ref={inputRef}
        value={barcode}
        onChange={(e) => setBarcode(e.target.value)}
        onKeyPress={handleKeyPress}
        placeholder={placeholder}
        autoFocus={autoFocus}
        InputProps={{
          endAdornment: (
            <InputAdornment position="end">
              <IconButton onClick={handleCameraScan}>
                <CameraIcon />
              </IconButton>
            </InputAdornment>
          )
        }}
      />
      {isScanning && (
        <BarcodeScanner
          onScan={(scannedBarcode) => {
            if (validate ? validate(scannedBarcode) : true) {
              onScan(scannedBarcode);
              setIsScanning(false);
            }
          }}
          onError={onError}
        />
      )}
    </Box>
  );
};
```

#### 2. Barcode Validation Service

```typescript
class BarcodeValidationService {
  async validateProductBarcode(barcode: string): Promise<Product | null> {
    // Check local cache first
    const cached = await this.getCachedProduct(barcode);
    if (cached) return cached;

    // Validate format
    if (!this.isValidFormat(barcode)) {
      throw new Error('Invalid barcode format');
    }

    // Query product service
    try {
      const product = await productService.getByBarcode(barcode);
      await this.cacheProduct(barcode, product);
      return product;
    } catch (error) {
      // If offline, check IndexedDB
      if (!navigator.onLine) {
        return await db.products.where('barcode').equals(barcode).first();
      }
      throw error;
    }
  }

  async validateLocationBarcode(barcode: string): Promise<Location | null> {
    // Similar implementation for locations
  }

  private isValidFormat(barcode: string): boolean {
    // Validate barcode format (EAN-13, Code 128, etc.)
    const ean13Regex = /^\d{13}$/;
    const code128Regex = /^[A-Za-z0-9\-\.\s]+$/;
    return ean13Regex.test(barcode) || code128Regex.test(barcode);
  }
}
```

#### 3. Batch Scanning Support

```typescript
const BatchBarcodeScanner: React.FC = () => {
  const [scannedItems, setScannedItems] = useState<ScannedItem[]>([]);

  const handleScan = async (barcode: string) => {
    const product = await barcodeService.validateProductBarcode(barcode);
    if (product) {
      setScannedItems(prev => [
        ...prev,
        { barcode, product, timestamp: Date.now() }
      ]);
    }
  };

  const handleClear = () => {
    setScannedItems([]);
  };

  return (
    <Box>
      <BarcodeInput onScan={handleScan} />
      <List>
        {scannedItems.map((item, index) => (
          <ListItem key={index}>
            <ListItemText
              primary={item.product.name}
              secondary={`Barcode: ${item.barcode}`}
            />
          </ListItem>
        ))}
      </List>
      <Button onClick={handleClear}>Clear</Button>
    </Box>
  );
};
```

---

## Responsive Design Guidelines

### Breakpoints

**Material-UI Breakpoints:**

```typescript
const breakpoints = {
  xs: 0,      // Extra small devices (phones)
  sm: 600,    // Small devices (tablets)
  md: 900,    // Medium devices (small laptops)
  lg: 1200,   // Large devices (desktops)
  xl: 1536    // Extra large devices (large desktops)
};
```

### Mobile-First Approach

**Design Principles:**

1. **Start with Mobile** - Design for smallest screen first
2. **Progressive Enhancement** - Add features for larger screens
3. **Touch-Friendly** - Minimum 44x44px touch targets
4. **Readable Text** - Minimum 16px font size
5. **Adequate Spacing** - Generous padding and margins

### Responsive Layout Patterns

#### 1. Stack Layout (Mobile)

```typescript
const MobileLayout = () => (
  <Stack spacing={2}>
    <Header />
    <MainContent />
    <Footer />
  </Stack>
);
```

#### 2. Grid Layout (Desktop)

```typescript
const DesktopLayout = () => (
  <Grid container spacing={2}>
    <Grid item xs={12} md={3}>
      <Sidebar />
    </Grid>
    <Grid item xs={12} md={9}>
      <MainContent />
    </Grid>
  </Grid>
);
```

#### 3. Adaptive Components

```typescript
const ResponsiveComponent = () => {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('sm'));

  return (
    <Box>
      {isMobile ? (
        <MobileView />
      ) : (
        <DesktopView />
      )}
    </Box>
  );
};
```

### Touch Interactions

**Touch Gestures:**

- **Tap** - Primary action
- **Long Press** - Secondary action (context menu)
- **Swipe** - Navigation, delete
- **Pinch** - Zoom (if applicable)

**Implementation:**

```typescript
import { useSwipeable } from 'react-swipeable';

const SwipeableCard = ({ onSwipeLeft, onSwipeRight }) => {
  const handlers = useSwipeable({
    onSwipedLeft: onSwipeLeft,
    onSwipedRight: onSwipeRight,
    trackMouse: true
  });

  return <div {...handlers}>Card Content</div>;
};
```

---

## Accessibility Requirements

### WCAG 2.1 Level AA Compliance

#### 1. Perceivable

**Text Alternatives:**

- All images have alt text
- Icons have aria-labels
- Decorative images have empty alt text

**Color Contrast:**

- Text contrast ratio: 4.5:1 (normal text), 3:1 (large text)
- Interactive elements: 3:1 contrast ratio
- Use color AND other indicators (icons, patterns)

**Text Resizing:**

- Support up to 200% zoom without horizontal scrolling
- Responsive text sizing (rem/em units)

#### 2. Operable

**Keyboard Navigation:**

- All functionality available via keyboard
- Focus indicators visible
- Logical tab order
- Skip links for main content

**No Seizures:**

- No flashing content (max 3 flashes per second)

**Navigation:**

- Consistent navigation
- Multiple ways to find content
- Clear headings and labels

#### 3. Understandable

**Readable:**

- Language declared in HTML (`lang` attribute)
- Unusual words explained
- Abbreviations explained

**Predictable:**

- Consistent navigation
- Consistent identification
- Change on request (no unexpected changes)

**Input Assistance:**

- Error identification
- Labels and instructions
- Error suggestions
- Error prevention (reversible, confirmed, checked)

#### 4. Robust

**Compatible:**

- Valid HTML
- Proper use of ARIA attributes
- Screen reader compatibility

### Implementation

#### ARIA Attributes

```typescript
// Button with loading state
<Button
  aria-label="Save stock count"
  aria-busy={isLoading}
  disabled={isLoading}
>
  {isLoading ? 'Saving...' : 'Save'}
</Button>

// Form with error
<TextField
  label="Quantity"
  error={!!errors.quantity}
  helperText={errors.quantity?.message}
  aria-invalid={!!errors.quantity}
  aria-describedby={errors.quantity ? 'quantity-error' : undefined}
/>

// Modal
<Dialog
  open={isOpen}
  onClose={handleClose}
  aria-labelledby="dialog-title"
  aria-describedby="dialog-description"
>
  <DialogTitle id="dialog-title">Confirm Action</DialogTitle>
  <DialogContent>
    <Typography id="dialog-description">
      Are you sure you want to proceed?
    </Typography>
  </DialogContent>
</Dialog>
```

#### Keyboard Navigation

```typescript
// Custom keyboard handler
const useKeyboardNavigation = () => {
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      switch (e.key) {
        case 'Escape':
          handleClose();
          break;
        case 'Enter':
          if (e.ctrlKey || e.metaKey) {
            handleSubmit();
          }
          break;
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, []);
};
```

#### Screen Reader Support

```typescript
// Live region for announcements
<div role="status" aria-live="polite" aria-atomic="true">
  {announcement}
</div>

// Progress indicator
<div role="progressbar" aria-valuenow={progress} aria-valuemin={0} aria-valuemax={100}>
  {progress}% complete
</div>
```

### Testing Accessibility

**Tools:**

- **axe DevTools** - Browser extension for accessibility testing
- **Lighthouse** - Automated accessibility auditing
- **WAVE** - Web accessibility evaluation tool
- **Screen Readers** - NVDA (Windows), VoiceOver (macOS/iOS), TalkBack (Android)

**Manual Testing:**

- Keyboard-only navigation
- Screen reader testing
- Color contrast verification
- Focus management

---

## Multi-Language Support

### Internationalization (i18n) Architecture

**Supported Languages:**

- English (en) - Default
- Afrikaans (af)
- Zulu (zu)

### Implementation

#### 1. Translation File Structure

```
public/locales/
├── en/
│   ├── common.json
│   ├── stock-count.json
│   ├── picking.json
│   └── consignment.json
├── af/
│   ├── common.json
│   ├── stock-count.json
│   ├── picking.json
│   └── consignment.json
└── zu/
    ├── common.json
    ├── stock-count.json
    ├── picking.json
    └── consignment.json
```

#### 2. Translation Files

**common.json (English):**

```json
{
  "app": {
    "name": "CCBSA Warehouse Management",
    "welcome": "Welcome"
  },
  "common": {
    "save": "Save",
    "cancel": "Cancel",
    "delete": "Delete",
    "edit": "Edit",
    "search": "Search",
    "loading": "Loading...",
    "error": "An error occurred",
    "success": "Success"
  },
  "navigation": {
    "home": "Home",
    "stockCount": "Stock Count",
    "picking": "Picking",
    "consignment": "Consignment",
    "returns": "Returns",
    "settings": "Settings"
  }
}
```

**stock-count.json (English):**

```json
{
  "title": "Stock Count",
  "worksheet": "Stock Count Worksheet",
  "scanLocation": "Scan Location Barcode",
  "scanProduct": "Scan Product Barcode",
  "enterQuantity": "Enter Quantity",
  "saveEntry": "Save Entry",
  "completeCount": "Complete Count",
  "variance": "Variance",
  "systemQuantity": "System Quantity",
  "countedQuantity": "Counted Quantity"
}
```

#### 3. i18n Configuration

```typescript
import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import Backend from 'i18next-http-backend';
import LanguageDetector from 'i18next-browser-languagedetector';

i18n
  .use(Backend)
  .use(LanguageDetector)
  .use(initReactI18next)
  .init({
    fallbackLng: 'en',
    supportedLngs: ['en', 'af', 'zu'],
    defaultNS: 'common',
    ns: ['common', 'stock-count', 'picking', 'consignment'],
    
    backend: {
      loadPath: '/locales/{{lng}}/{{ns}}.json'
    },
    
    detection: {
      order: ['localStorage', 'navigator'],
      caches: ['localStorage']
    },
    
    interpolation: {
      escapeValue: false
    }
  });

export default i18n;
```

#### 4. Usage in Components

```typescript
import { useTranslation } from 'react-i18next';

const StockCountComponent = () => {
  const { t, i18n } = useTranslation(['common', 'stock-count']);

  const changeLanguage = (lng: string) => {
    i18n.changeLanguage(lng);
  };

  return (
    <Box>
      <Typography variant="h5">{t('stock-count:title')}</Typography>
      <Button onClick={() => changeLanguage('en')}>English</Button>
      <Button onClick={() => changeLanguage('af')}>Afrikaans</Button>
      <Button onClick={() => changeLanguage('zu')}>Zulu</Button>
    </Box>
  );
};
```

#### 5. Language Switcher Component

```typescript
const LanguageSwitcher: React.FC = () => {
  const { i18n } = useTranslation();

  const languages = [
    { code: 'en', name: 'English' },
    { code: 'af', name: 'Afrikaans' },
    { code: 'zu', name: 'isiZulu' }
  ];

  return (
    <Select
      value={i18n.language}
      onChange={(e) => i18n.changeLanguage(e.target.value)}
    >
      {languages.map((lang) => (
        <MenuItem key={lang.code} value={lang.code}>
          {lang.name}
        </MenuItem>
      ))}
    </Select>
  );
};
```

### Right-to-Left (RTL) Support

**Note:** Not required for current languages, but architecture supports future RTL languages.

---

## Security Architecture

### Authentication

**OAuth 2.0 / JWT:**

- Token-based authentication
- **Access token stored in memory** (JavaScript variable) - Industry best practice ✅
- **Refresh token stored in httpOnly cookies** - Industry best practice ✅
- Token refresh on expiry (reactive) and proactive refresh support
- Client-side JWT expiration checking for better UX

**Token Storage Strategy (Industry Best Practices - Fully Implemented):**

1. **Access Tokens - In-Memory Storage** ✅
    - Stored in JavaScript variable (module-level)
    - Automatically cleared on page unload
    - XSS-resistant (not accessible via localStorage)
    - Industry best practice for access tokens

2. **Refresh Tokens - httpOnly Cookies** ✅
    - Stored in httpOnly cookies by backend
    - Not accessible to JavaScript (XSS protection)
    - Automatically sent by browser with requests
    - Secure, SameSite=Strict, HttpOnly attributes
    - Industry best practice for refresh tokens

**Security Measures:**

1. **Content Security Policy (CSP)** - Configured at gateway/CDN level to prevent XSS
2. **Input Sanitization** - All user input sanitized, React's built-in XSS protection
3. **HTTPS Only** - All communications over HTTPS with HSTS headers
4. **Token Expiration Checking** - Client-side JWT decode to check expiration before requests
5. **Proactive Token Refresh** - Tokens refreshed before expiration (80% of lifetime)
6. **Regular Security Audits** - Dependency scanning and penetration testing
7. **httpOnly Cookies** - Refresh tokens protected from XSS attacks
8. **SameSite=Strict** - CSRF protection for cookies

**Implementation Status:**

- ✅ **Phase 1 Complete**: In-memory access token storage implemented
- ✅ **Phase 2 Complete**: Backend httpOnly cookie support for refresh tokens
- ✅ **Phase 3 Complete**: Frontend refresh token cookie handling

**Note:** Full implementation details are documented in `Authentication_Alignment_Analysis.md`.

### Authorization

**Role-Based Access Control (RBAC):**

- Roles: Admin, Manager, Operator, Viewer
- Permissions checked on frontend and backend
- Route protection based on roles

### Data Security

**Encryption:**

- HTTPS only (TLS 1.2+)
- Sensitive data encrypted at rest (IndexedDB)
- No sensitive data in localStorage

**Input Validation:**

- Client-side validation (UX)
- Server-side validation (security)
- Sanitize user input

### Security Best Practices

1. **Content Security Policy (CSP)**
2. **XSS Prevention** - Sanitize user input
3. **CSRF Protection** - Token-based
4. **Secure Headers** - HSTS, X-Frame-Options, etc.
5. **Dependency Scanning** - Regular security audits

---

## Performance Optimization

### Code Splitting

**Route-Based Splitting:**

```typescript
const StockCount = lazy(() => import('./features/stock-count'));
const Picking = lazy(() => import('./features/picking'));
```

**Component-Based Splitting:**

- Large components loaded on demand
- Heavy libraries loaded dynamically

### Image Optimization

- **WebP Format** - Modern image format
- **Lazy Loading** - Load images on scroll
- **Responsive Images** - srcset for different sizes
- **Image Compression** - Optimize before deployment

### Bundle Optimization

- **Tree Shaking** - Remove unused code
- **Minification** - Minify JavaScript and CSS
- **Compression** - Gzip/Brotli compression
- **CDN** - Serve static assets from CDN

### Caching Strategy

- **Static Assets** - Long-term caching (1 year)
- **API Responses** - RTK Query caching
- **Service Worker** - Offline caching

### Performance Metrics

**Target Metrics:**

- **First Contentful Paint (FCP)**: < 1.8s
- **Largest Contentful Paint (LCP)**: < 2.5s
- **First Input Delay (FID)**: < 100ms
- **Cumulative Layout Shift (CLS)**: < 0.1
- **Time to Interactive (TTI)**: < 3.8s

---

## Testing Strategy

### Unit Testing

**Components:**

- React Testing Library
- Test component behavior, not implementation
- Mock external dependencies

**Hooks:**

- Custom hook testing utilities
- Test hook behavior

**Utilities:**

- Pure function testing
- Edge cases

### Integration Testing

**API Integration:**

- MSW for API mocking
- Test data flow
- Test error handling

**Component Integration:**

- Test component interactions
- Test user flows

### End-to-End Testing

**Playwright:**

- Critical user flows
- Cross-browser testing
- Visual regression testing

**Test Scenarios:**

- Stock count workflow
- Picking workflow
- Offline/online transitions
- Barcode scanning

### Visual Regression Testing

**Chromatic/Percy:**

- Component visual testing
- Screenshot comparison
- UI consistency

---

## Deployment Architecture

### Build Process

**Production Build:**

```bash
npm run build
```

**Output:**

- Static files (HTML, CSS, JS)
- Service worker
- Manifest.json
- Assets (images, fonts)

### Deployment Options

**Static Hosting:**

- **Netlify** - Recommended for PWA
- **Vercel** - Alternative
- **AWS S3 + CloudFront** - Enterprise option
- **Azure Static Web Apps** - Microsoft ecosystem

### CI/CD Pipeline

**GitHub Actions / GitLab CI:**

1. **Lint & Test** - Run linters and tests
2. **Build** - Create production build
3. **Deploy** - Deploy to hosting platform
4. **Smoke Tests** - Verify deployment

### Environment Configuration

**Environment Variables:**

- API endpoint
- Feature flags
- Analytics keys
- Sentry DSN

**Configuration Files:**

- `.env.development`
- `.env.production`
- `.env.staging`

---

## Appendix

### Glossary

| Term               | Definition                            |
|--------------------|---------------------------------------|
| **PWA**            | Progressive Web App                   |
| **Service Worker** | Background script for offline support |
| **IndexedDB**      | Browser database for offline storage  |
| **Workbox**        | Service worker library                |
| **RTK**            | Redux Toolkit                         |
| **i18n**           | Internationalization                  |
| **WCAG**           | Web Content Accessibility Guidelines  |
| **LCP**            | Largest Contentful Paint              |
| **FID**            | First Input Delay                     |
| **CLS**            | Cumulative Layout Shift               |

### References

- [PWA Documentation](https://web.dev/progressive-web-apps/)
- [React Documentation](https://react.dev/)
- [Material-UI Documentation](https://mui.com/)
- [WCAG 2.1 Guidelines](https://www.w3.org/WAI/WCAG21/quickref/)
- [Workbox Documentation](https://developers.google.com/web/tools/workbox)

---

**Document Control**

- **Version History:** This document will be version controlled with change tracking
- **Review Cycle:** This document will be reviewed weekly during architecture phase
- **Distribution:** This document will be distributed to all frontend team members

