# Perform Stock Count Entry Implementation Plan

## US-8.1.2: Perform Stock Count Entry

**Service:** Reconciliation Service
**Priority:** Must Have
**Story Points:** 8
**Sprint:** Sprint 8

---

## Table of Contents

1. [Overview](#overview)
2. [UI Design](#ui-design)
3. [Offline-First Architecture](#offline-first-architecture)
4. [Domain Model Design](#domain-model-design)
5. [Backend Implementation](#backend-implementation)
6. [Frontend Implementation](#frontend-implementation)
7. [Data Flow](#data-flow)
8. [Testing Strategy](#testing-strategy)
9. [Acceptance Criteria Validation](#acceptance-criteria-validation)
10. [Implementation Checklist](#implementation-checklist)

---

## Overview

### User Story

**As a** warehouse operator
**I want** to record stock count entries on a mobile device with offline support
**So that** I can count inventory efficiently without relying on constant network connectivity

### Business Requirements

- **Barcode scanning** for location and product identification (handheld scanner or mobile camera)
- **Digital entry** into electronic worksheet (no paper transcription)
- **Offline capability** with IndexedDB storage for network-independent operation
- **Background sync** when connection is restored
- **Auto-save functionality** to prevent data loss
- **Duplicate prevention** - no duplicate location/product combinations within a count
- **Real-time validation** of barcodes, quantities, and business rules
- **Resume incomplete counts** from any device
- **Progress tracking** with running totals and completion percentage
- **Audit trail** with timestamp and operator identification for each entry

### Technical Requirements

- Follow DDD, Clean Hexagonal Architecture, CQRS, Event-Driven Choreography
- Pure Java domain core (no framework dependencies)
- Multi-tenant support with schema isolation
- Move common value objects to `common-domain` (DRY principle)
- **Offline-first PWA architecture** with service workers
- **IndexedDB** for persistent offline storage
- **Background Sync API** for automatic synchronization
- **Network status detection** with UI feedback
- **Conflict resolution** strategy for concurrent offline edits
- Mobile-optimized responsive design
- Accessibility compliance (WCAG 2.1 Level AA)

### Acceptance Criteria

1. **AC-1**: System supports barcode scanning for location and product identification
2. **AC-2**: System displays expected quantity for reference
3. **AC-3**: System allows manual quantity entry
4. **AC-4**: System validates entries (no duplicates, valid products/locations)
5. **AC-5**: System supports offline entry with IndexedDB storage
6. **AC-6**: System publishes `StockCountEntryRecordedEvent` for each entry
7. **AC-7**: System provides running total and progress indicator

### Key Features

#### Offline-First Capabilities

- **IndexedDB Storage**: Persistent client-side database for offline entries
- **Background Sync**: Automatic synchronization when network is restored
- **Conflict Resolution**: Timestamp-based resolution with user notification
- **Sync Status Indicators**: Visual feedback for pending, syncing, and synced states
- **Batch Sync**: Efficient bulk synchronization of multiple entries
- **Error Recovery**: Retry mechanisms for failed sync attempts
- **Data Integrity**: Client-side validation matching server-side rules

#### Mobile Optimization

- **Touch-first Interface**: Large touch targets, swipe gestures
- **Camera Integration**: Mobile device camera for barcode scanning
- **Handheld Scanner Support**: USB/Bluetooth scanner compatibility
- **Voice Input**: Voice-to-text for notes (accessibility)
- **Haptic Feedback**: Vibration confirmation for scans
- **Auto-focus**: Automatic field focus after scan
- **Keyboard Shortcuts**: Quick navigation for power users

---

## UI Design

### Stock Count Entry Page

**Component:** `StockCountEntryPage.tsx`
**Route:** `/reconciliation/stock-count/:worksheetId/entry`

**Features:**

- **Worksheet Header** - Reference number, count type, status, progress
- **Network Status Indicator** - Online/offline badge with sync status
- **Barcode Scanner** - Camera or handheld scanner input
- **Entry Form** - Location, product, quantity fields with validation
- **Expected Quantity Display** - Reference for operator comparison
- **Variance Indicator** - Real-time calculation and color coding
- **Progress Bar** - Visual representation of count completion
- **Entry List** - Recent entries with edit/delete capabilities
- **Sync Queue** - Pending offline entries awaiting synchronization
- **Confirmation Feedback** - Visual and haptic confirmation on successful entry

### UI Flow

#### 1. Initial Load

1. User navigates to stock count entry page
2. System loads worksheet details from server (if online) or IndexedDB (if offline)
3. System displays:
   - Worksheet reference number
   - Count type and scope
   - Total expected entries
   - Progress: X of Y entries recorded (Z% complete)
   - Network status indicator (online/offline)
   - Sync status (if offline entries pending)

#### 2. Recording an Entry (Online)

1. **Scan Location Barcode**
   - User clicks "Scan Location" or focuses location field
   - Camera/scanner activates
   - User scans location barcode
   - System validates location:
     - ✅ Valid: Display location code and description
     - ❌ Invalid: Display error and allow retry
   - Auto-focus shifts to product field

2. **Scan Product Barcode**
   - User clicks "Scan Product" or focuses product field
   - Camera/scanner activates
   - User scans product barcode
   - System validates product and retrieves expected quantity:
     - ✅ Valid: Display product details and expected quantity
     - ❌ Invalid: Display error and allow retry
     - ⚠️ Duplicate: Display warning (already counted at this location)
   - Auto-focus shifts to quantity field

3. **Enter Counted Quantity**
   - User enters actual counted quantity
   - System calculates variance in real-time:
     - `variance = counted_quantity - expected_quantity`
     - Display variance with color coding:
       - 🟢 Green: No variance (exact match)
       - 🟡 Yellow: Minor variance (< 5%)
       - 🟠 Orange: Moderate variance (5-10%)
       - 🔴 Red: Significant variance (> 10%)
   - User optionally adds notes (for variances or damaged stock)

4. **Submit Entry**
   - User clicks "Submit" or presses Enter
   - System validates:
     - ✅ No duplicate location-product combination
     - ✅ Valid quantity (non-negative decimal)
     - ✅ Location and product exist in worksheet scope
   - System submits to server via API
   - Server responds with entry confirmation
   - System displays success feedback:
     - ✅ Check icon animation
     - Haptic vibration (mobile)
     - Audio beep (optional)
   - System updates progress bar
   - System clears form for next entry
   - System adds entry to recent entries list

5. **Continue Counting**
   - Repeat steps 1-4 for each location-product combination
   - System auto-saves progress after each entry

#### 3. Recording an Entry (Offline)

1. **Network Loss Detection**
   - System detects network disconnection
   - Display offline indicator (banner or badge)
   - Notify user: "Working offline - entries will sync automatically"

2. **Offline Entry Process** (Same UX as online)
   - User scans location barcode → Validated against cached data
   - User scans product barcode → Validated against cached data
   - User enters counted quantity → Variance calculated locally
   - User submits entry

3. **IndexedDB Storage**
   - System stores entry in IndexedDB with:
     - `syncStatus: 'PENDING'`
     - `createdAt: timestamp`
     - `attemptCount: 0`
   - System displays entry in "Pending Sync" queue
   - System updates local progress counter

4. **Visual Feedback**
   - Entry card shows orange "Pending Sync" badge
   - Sync queue displays count: "3 entries pending sync"
   - Progress bar updates (local count)

#### 4. Background Sync (Network Restored)

1. **Network Restoration**
   - System detects network connection
   - Display online indicator
   - Notify user: "Online - syncing X entries..."

2. **Batch Sync Process**
   - System retrieves all `PENDING` entries from IndexedDB (max 50 per batch)
   - For each entry:
     - Submit to server via POST `/api/v1/reconciliation/stock-counts/{id}/entries`
     - If success:
       - Update entry: `syncStatus: 'SYNCED'`, `syncedAt: timestamp`
       - Remove from IndexedDB (or mark as synced)
       - Update UI: Change badge to green "Synced"
     - If failure (4xx/5xx):
       - Increment `attemptCount`
       - If `attemptCount < 5`:
         - Retry with exponential backoff
       - If `attemptCount >= 5`:
         - Update entry: `syncStatus: 'FAILED'`
         - Display error notification
         - Move to "Failed Sync" queue for manual review

3. **Conflict Resolution**
   - If server responds with 409 Conflict (duplicate entry):
     - Display conflict dialog:
       - "Entry already exists for this location-product combination"
       - Show server entry details (timestamp, user, quantity)
       - Show local entry details
       - Options:
         - "Keep Server Entry" (discard local)
         - "Keep Local Entry" (update server)
         - "Review Both" (manual decision)
   - User selects resolution
   - System applies resolution and updates sync status

4. **Sync Complete**
   - Display success notification: "All entries synced successfully"
   - Clear sync queue
   - Update progress with server confirmation

#### 5. Viewing Recent Entries

1. User scrolls to "Recent Entries" section
2. System displays paginated list of entries:
   - Location code and description
   - Product code and description
   - Expected quantity
   - Counted quantity
   - Variance (with color coding)
   - Entry timestamp
   - Operator name
   - Sync status badge (online entries always "Synced")
   - Actions: Edit (if within 5 minutes), Delete (if within 5 minutes)

#### 6. Editing an Entry

1. User clicks "Edit" on a recent entry
2. System validates:
   - ✅ Entry recorded < 5 minutes ago: Allow edit
   - ❌ Entry recorded > 5 minutes ago: Disallow edit (show message)
3. System pre-fills form with entry data
4. User modifies quantity or notes
5. User clicks "Update"
6. System submits updated entry
7. System updates entry in list

#### 7. Deleting an Entry

1. User clicks "Delete" on a recent entry
2. System displays confirmation dialog
3. User confirms deletion
4. System submits delete request
5. System removes entry from list
6. System updates progress bar

#### 8. Completing the Count

1. User clicks "Complete Count" (available once all expected entries recorded)
2. System validates:
   - ✅ All worksheet entries recorded: Allow completion
   - ⚠️ Some entries missing: Display warning with missing count
   - ❌ Pending offline entries: Block completion, require sync first
3. User confirms completion (if warnings present)
4. System navigates to "Complete Stock Count" page (US-8.1.3)

### Page Layout (TypeScript/React)

```typescript
import React, { useState, useEffect, useCallback } from 'react';
import {
  Box,
  Paper,
  Typography,
  Grid,
  TextField,
  Button,
  IconButton,
  Alert,
  AlertTitle,
  Card,
  CardContent,
  CardActions,
  Chip,
  LinearProgress,
  List,
  ListItem,
  ListItemText,
  ListItemSecondaryAction,
  Divider,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Badge,
  Tooltip,
  CircularProgress,
  Stack,
  Fab
} from '@mui/material';
import {
  QrCodeScanner as ScanIcon,
  CheckCircle as CheckCircleIcon,
  CloudOff as OfflineIcon,
  CloudDone as OnlineIcon,
  Sync as SyncIcon,
  Error as ErrorIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  NavigateNext as NextIcon,
  Inventory as InventoryIcon,
  Warning as WarningIcon
} from '@mui/icons-material';
import { useParams, useNavigate } from 'react-router-dom';
import { useStockCountEntry } from '../hooks/useStockCountEntry';
import { useOfflineSync } from '../hooks/useOfflineSync';
import { useBarcodeScanner } from '../hooks/useBarcodeScanner';
import { useNetworkStatus } from '../../common/hooks/useNetworkStatus';
import { BarcodeScanner } from '../components/BarcodeScanner';
import { PageBreadcrumbs } from '../../common/PageBreadcrumbs';
import { StatusBadge } from '../../common/StatusBadge';
import { VarianceIndicator } from '../components/VarianceIndicator';
import { SyncQueuePanel } from '../components/SyncQueuePanel';
import type {
  StockCountEntry,
  EntryFormData,
  SyncStatus
} from '../types/stockCount';

export const StockCountEntryPage: React.FC = () => {
  const { worksheetId } = useParams<{ worksheetId: string }>();
  const navigate = useNavigate();

  // Network status
  const { isOnline } = useNetworkStatus();

  // Worksheet data
  const {
    worksheet,
    entries,
    progress,
    isLoading,
    error: worksheetError,
    refetch: refetchWorksheet
  } = useStockCountWorksheet(worksheetId!);

  // Entry recording
  const {
    recordEntry,
    updateEntry,
    deleteEntry,
    isRecording,
    recordingError,
    lastRecordedEntry,
    resetError
  } = useStockCountEntry(worksheetId!);

  // Offline sync
  const {
    pendingEntries,
    failedEntries,
    syncPending,
    syncStatus,
    retryFailedEntry,
    clearSyncQueue
  } = useOfflineSync(worksheetId!);

  // Barcode scanner
  const {
    scanLocation,
    scanProduct,
    isScanningLocation,
    isScanningProduct,
    scanError,
    resetScanError
  } = useBarcodeScanner();

  // Form state
  const [locationBarcode, setLocationBarcode] = useState<string>('');
  const [locationId, setLocationId] = useState<string>('');
  const [locationDescription, setLocationDescription] = useState<string>('');
  const [productBarcode, setProductBarcode] = useState<string>('');
  const [productId, setProductId] = useState<string>('');
  const [productDescription, setProductDescription] = useState<string>('');
  const [expectedQuantity, setExpectedQuantity] = useState<number | null>(null);
  const [countedQuantity, setCountedQuantity] = useState<string>('');
  const [variance, setVariance] = useState<number>(0);
  const [notes, setNotes] = useState<string>('');
  const [showBarcodeScanner, setShowBarcodeScanner] = useState<boolean>(false);
  const [scannerMode, setScannerMode] = useState<'location' | 'product'>('location');
  const [showSuccessFeedback, setShowSuccessFeedback] = useState<boolean>(false);

  // Validation state
  const [duplicateWarning, setDuplicateWarning] = useState<boolean>(false);
  const [validationErrors, setValidationErrors] = useState<{
    location?: string;
    product?: string;
    quantity?: string;
  }>({});

  // Calculate variance in real-time
  useEffect(() => {
    if (expectedQuantity !== null && countedQuantity !== '') {
      const counted = parseFloat(countedQuantity);
      if (!isNaN(counted)) {
        setVariance(counted - expectedQuantity);
      }
    } else {
      setVariance(0);
    }
  }, [expectedQuantity, countedQuantity]);

  // Validation
  const validateForm = (): boolean => {
    const errors: typeof validationErrors = {};

    if (!locationId) {
      errors.location = 'Location is required';
    }

    if (!productId) {
      errors.product = 'Product is required';
    }

    if (countedQuantity === '') {
      errors.quantity = 'Counted quantity is required';
    } else {
      const counted = parseFloat(countedQuantity);
      if (isNaN(counted) || counted < 0) {
        errors.quantity = 'Quantity must be a non-negative number';
      }
    }

    setValidationErrors(errors);
    return Object.keys(errors).length === 0;
  };

  // Check for duplicate entry
  const checkDuplicate = useCallback(() => {
    if (locationId && productId && entries) {
      const duplicate = entries.some(
        entry => entry.locationId === locationId && entry.productId === productId
      );
      setDuplicateWarning(duplicate);
      return duplicate;
    }
    return false;
  }, [locationId, productId, entries]);

  useEffect(() => {
    checkDuplicate();
  }, [checkDuplicate]);

  // Handlers
  const handleScanLocation = async () => {
    setScannerMode('location');
    setShowBarcodeScanner(true);
  };

  const handleScanProduct = async () => {
    setScannerMode('product');
    setShowBarcodeScanner(true);
  };

  const handleBarcodeScanned = async (barcode: string) => {
    setShowBarcodeScanner(false);

    if (scannerMode === 'location') {
      const result = await scanLocation(barcode, worksheetId!);
      if (result.success) {
        setLocationBarcode(barcode);
        setLocationId(result.locationId!);
        setLocationDescription(result.locationDescription!);
        resetScanError();
      }
    } else if (scannerMode === 'product') {
      const result = await scanProduct(barcode, worksheetId!, locationId);
      if (result.success) {
        setProductBarcode(barcode);
        setProductId(result.productId!);
        setProductDescription(result.productDescription!);
        setExpectedQuantity(result.expectedQuantity!);
        resetScanError();

        // Auto-focus quantity field
        document.getElementById('counted-quantity')?.focus();
      }
    }
  };

  const handleSubmitEntry = async () => {
    if (!validateForm()) {
      return;
    }

    if (duplicateWarning) {
      // Show confirmation dialog for duplicate
      if (!confirm('This location-product combination has already been counted. Continue anyway?')) {
        return;
      }
    }

    const entryData: EntryFormData = {
      locationId,
      locationBarcode,
      productId,
      productBarcode,
      systemQuantity: expectedQuantity!,
      countedQuantity: parseFloat(countedQuantity),
      notes: notes.trim() || undefined
    };

    const result = await recordEntry(entryData);

    if (result.success) {
      // Success feedback
      setShowSuccessFeedback(true);
      setTimeout(() => setShowSuccessFeedback(false), 2000);

      // Haptic feedback (mobile)
      if (navigator.vibrate) {
        navigator.vibrate(100);
      }

      // Reset form
      resetForm();

      // Refetch worksheet to update progress
      refetchWorksheet();
    }
  };

  const resetForm = () => {
    setLocationBarcode('');
    setLocationId('');
    setLocationDescription('');
    setProductBarcode('');
    setProductId('');
    setProductDescription('');
    setExpectedQuantity(null);
    setCountedQuantity('');
    setVariance(0);
    setNotes('');
    setDuplicateWarning(false);
    setValidationErrors({});
  };

  const handleCompleteCount = () => {
    if (pendingEntries.length > 0) {
      alert('Please sync all pending entries before completing the count.');
      return;
    }

    navigate(`/reconciliation/stock-count/${worksheetId}/complete`);
  };

  // Loading state
  if (isLoading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    );
  }

  // Error state
  if (worksheetError) {
    return (
      <Alert severity="error">
        <AlertTitle>Error Loading Worksheet</AlertTitle>
        {worksheetError}
      </Alert>
    );
  }

  return (
    <Box>
      <PageBreadcrumbs
        items={[
          { label: 'Reconciliation', href: '/reconciliation' },
          { label: 'Stock Counts', href: '/reconciliation/stock-count' },
          { label: worksheet?.countReference || 'Entry' }
        ]}
      />

      {/* Network Status Banner */}
      {!isOnline && (
        <Alert severity="warning" icon={<OfflineIcon />} sx={{ mb: 2 }}>
          <AlertTitle>Working Offline</AlertTitle>
          You are currently offline. Entries will be saved locally and synced automatically when connection is restored.
        </Alert>
      )}

      {/* Sync Status Banner */}
      {pendingEntries.length > 0 && (
        <Alert severity="info" icon={<SyncIcon />} sx={{ mb: 2 }}>
          <AlertTitle>Pending Sync</AlertTitle>
          {pendingEntries.length} {pendingEntries.length === 1 ? 'entry' : 'entries'} waiting to sync.
          {isOnline && syncStatus === 'SYNCING' && ' Syncing now...'}
        </Alert>
      )}

      {/* Failed Sync Alert */}
      {failedEntries.length > 0 && (
        <Alert severity="error" icon={<ErrorIcon />} sx={{ mb: 2 }}>
          <AlertTitle>Sync Failed</AlertTitle>
          {failedEntries.length} {failedEntries.length === 1 ? 'entry' : 'entries'} failed to sync.
          Please review and retry.
        </Alert>
      )}

      {/* Worksheet Header */}
      <Paper sx={{ p: 3, mb: 3 }}>
        <Grid container spacing={2} alignItems="center">
          <Grid item xs={12} md={6}>
            <Typography variant="h4" gutterBottom>
              Stock Count Entry
            </Typography>
            <Typography variant="body2" color="textSecondary">
              Reference: <strong>{worksheet?.countReference}</strong>
            </Typography>
            <Typography variant="body2" color="textSecondary">
              Type: <strong>{worksheet?.countType}</strong>
            </Typography>
          </Grid>
          <Grid item xs={12} md={6} textAlign="right">
            <Stack direction="row" spacing={1} justifyContent="flex-end" alignItems="center">
              <StatusBadge status={worksheet?.status || 'DRAFT'} />
              <Chip
                icon={isOnline ? <OnlineIcon /> : <OfflineIcon />}
                label={isOnline ? 'Online' : 'Offline'}
                color={isOnline ? 'success' : 'warning'}
                size="small"
              />
              {pendingEntries.length > 0 && (
                <Badge badgeContent={pendingEntries.length} color="warning">
                  <SyncIcon />
                </Badge>
              )}
            </Stack>
          </Grid>
        </Grid>

        {/* Progress Bar */}
        <Box sx={{ mt: 3 }}>
          <Box display="flex" justifyContent="space-between" alignItems="center" mb={1}>
            <Typography variant="body2" color="textSecondary">
              Progress
            </Typography>
            <Typography variant="body2" fontWeight="bold">
              {progress?.recordedEntries || 0} of {progress?.totalExpectedEntries || 0} entries
              ({progress?.percentageComplete || 0}%)
            </Typography>
          </Box>
          <LinearProgress
            variant="determinate"
            value={progress?.percentageComplete || 0}
            sx={{ height: 8, borderRadius: 1 }}
          />
        </Box>
      </Paper>

      {/* Entry Form */}
      <Paper sx={{ p: 3, mb: 3 }}>
        <Typography variant="h6" gutterBottom>
          Record Entry
        </Typography>

        {recordingError && (
          <Alert severity="error" sx={{ mb: 2 }} onClose={resetError}>
            {recordingError}
          </Alert>
        )}

        {duplicateWarning && (
          <Alert severity="warning" sx={{ mb: 2 }}>
            <AlertTitle>Duplicate Entry</AlertTitle>
            This location-product combination has already been counted in this worksheet.
          </Alert>
        )}

        <Grid container spacing={3}>
          {/* Location Scan */}
          <Grid item xs={12} md={6}>
            <Typography variant="subtitle2" gutterBottom>
              1. Scan Location
            </Typography>
            <Stack direction="row" spacing={2}>
              <TextField
                fullWidth
                label="Location Barcode"
                value={locationBarcode}
                onChange={(e) => setLocationBarcode(e.target.value)}
                error={!!validationErrors.location}
                helperText={validationErrors.location || locationDescription}
                InputProps={{
                  readOnly: true
                }}
              />
              <Button
                variant="contained"
                onClick={handleScanLocation}
                disabled={isScanningLocation}
                startIcon={<ScanIcon />}
                sx={{ minWidth: 120 }}
              >
                Scan
              </Button>
            </Stack>
          </Grid>

          {/* Product Scan */}
          <Grid item xs={12} md={6}>
            <Typography variant="subtitle2" gutterBottom>
              2. Scan Product
            </Typography>
            <Stack direction="row" spacing={2}>
              <TextField
                fullWidth
                label="Product Barcode"
                value={productBarcode}
                onChange={(e) => setProductBarcode(e.target.value)}
                error={!!validationErrors.product}
                helperText={validationErrors.product || productDescription}
                disabled={!locationId}
                InputProps={{
                  readOnly: true
                }}
              />
              <Button
                variant="contained"
                onClick={handleScanProduct}
                disabled={!locationId || isScanningProduct}
                startIcon={<ScanIcon />}
                sx={{ minWidth: 120 }}
              >
                Scan
              </Button>
            </Stack>
          </Grid>

          {/* Expected Quantity */}
          {expectedQuantity !== null && (
            <Grid item xs={12} md={4}>
              <TextField
                fullWidth
                label="Expected Quantity"
                value={expectedQuantity}
                InputProps={{
                  readOnly: true
                }}
                helperText="System quantity"
              />
            </Grid>
          )}

          {/* Counted Quantity */}
          <Grid item xs={12} md={expectedQuantity !== null ? 4 : 6}>
            <Typography variant="subtitle2" gutterBottom>
              3. Enter Counted Quantity
            </Typography>
            <TextField
              id="counted-quantity"
              fullWidth
              label="Counted Quantity"
              type="number"
              value={countedQuantity}
              onChange={(e) => setCountedQuantity(e.target.value)}
              disabled={!productId}
              error={!!validationErrors.quantity}
              helperText={validationErrors.quantity}
              inputProps={{
                min: 0,
                step: 0.01
              }}
            />
          </Grid>

          {/* Variance */}
          {expectedQuantity !== null && countedQuantity !== '' && (
            <Grid item xs={12} md={4}>
              <Typography variant="subtitle2" gutterBottom>
                Variance
              </Typography>
              <VarianceIndicator
                variance={variance}
                expectedQuantity={expectedQuantity}
              />
            </Grid>
          )}

          {/* Notes */}
          <Grid item xs={12}>
            <TextField
              fullWidth
              label="Notes (Optional)"
              multiline
              rows={2}
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              disabled={!productId}
              placeholder="Add notes about this entry (e.g., damaged items, partial cases)"
            />
          </Grid>

          {/* Submit Button */}
          <Grid item xs={12}>
            <Button
              variant="contained"
              size="large"
              fullWidth
              onClick={handleSubmitEntry}
              disabled={isRecording || !productId || countedQuantity === ''}
              startIcon={isRecording ? <CircularProgress size={20} /> : <CheckCircleIcon />}
            >
              {isRecording ? 'Recording Entry...' : 'Submit Entry'}
            </Button>
          </Grid>
        </Grid>

        {/* Success Feedback */}
        {showSuccessFeedback && (
          <Alert severity="success" sx={{ mt: 2 }}>
            <AlertTitle>Entry Recorded Successfully</AlertTitle>
            Entry saved{!isOnline && ' locally (will sync when online)'}.
          </Alert>
        )}
      </Paper>

      {/* Sync Queue Panel (if offline entries pending) */}
      {(pendingEntries.length > 0 || failedEntries.length > 0) && (
        <SyncQueuePanel
          pendingEntries={pendingEntries}
          failedEntries={failedEntries}
          onRetry={retryFailedEntry}
          onClearQueue={clearSyncQueue}
        />
      )}

      {/* Recent Entries */}
      <Paper sx={{ p: 3, mb: 3 }}>
        <Typography variant="h6" gutterBottom>
          Recent Entries
        </Typography>

        {entries && entries.length > 0 ? (
          <List>
            {entries.slice(0, 10).map((entry, index) => (
              <React.Fragment key={entry.id}>
                {index > 0 && <Divider />}
                <ListItem>
                  <ListItemText
                    primary={
                      <Stack direction="row" spacing={1} alignItems="center">
                        <Typography variant="body1">
                          {entry.locationCode} - {entry.productCode}
                        </Typography>
                        <Chip
                          label={entry.syncStatus || 'SYNCED'}
                          color={
                            entry.syncStatus === 'PENDING' ? 'warning' :
                            entry.syncStatus === 'FAILED' ? 'error' :
                            'success'
                          }
                          size="small"
                        />
                      </Stack>
                    }
                    secondary={
                      <>
                        <Typography variant="body2" component="span">
                          Expected: {entry.systemQuantity} | Counted: {entry.countedQuantity} |
                          Variance: {entry.varianceQuantity > 0 ? '+' : ''}{entry.varianceQuantity}
                        </Typography>
                        <br />
                        <Typography variant="caption" color="textSecondary">
                          {new Date(entry.recordedAt).toLocaleString()} by {entry.recordedBy}
                        </Typography>
                      </>
                    }
                  />
                  <ListItemSecondaryAction>
                    {/* Edit/Delete actions if within 5 minutes */}
                  </ListItemSecondaryAction>
                </ListItem>
              </React.Fragment>
            ))}
          </List>
        ) : (
          <Typography variant="body2" color="textSecondary">
            No entries recorded yet.
          </Typography>
        )}
      </Paper>

      {/* Complete Count Button */}
      {progress?.percentageComplete === 100 && pendingEntries.length === 0 && (
        <Fab
          variant="extended"
          color="primary"
          onClick={handleCompleteCount}
          sx={{
            position: 'fixed',
            bottom: 16,
            right: 16
          }}
        >
          <CheckCircleIcon sx={{ mr: 1 }} />
          Complete Count
        </Fab>
      )}

      {/* Barcode Scanner Dialog */}
      <BarcodeScanner
        open={showBarcodeScanner}
        onClose={() => setShowBarcodeScanner(false)}
        onScan={handleBarcodeScanned}
        mode={scannerMode}
      />
    </Box>
  );
};
```

### Mobile-Optimized Components

#### 1. BarcodeScanner Component

```typescript
// frontend-app/src/features/reconciliation/components/BarcodeScanner.tsx

import React, { useRef, useEffect, useState } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Box,
  Typography,
  Alert,
  IconButton
} from '@mui/material';
import {
  Close as CloseIcon,
  FlashlightOn as FlashOnIcon,
  FlashlightOff as FlashOffIcon
} from '@mui/icons-material';
import { Html5Qrcode } from 'html5-qrcode';

interface BarcodeScannerProps {
  open: boolean;
  onClose: () => void;
  onScan: (barcode: string) => void;
  mode: 'location' | 'product';
}

export const BarcodeScanner: React.FC<BarcodeScannerProps> = ({
  open,
  onClose,
  onScan,
  mode
}) => {
  const scannerRef = useRef<Html5Qrcode | null>(null);
  const [isScanning, setIsScanning] = useState(false);
  const [error, setError] = useState<string>('');
  const [torchEnabled, setTorchEnabled] = useState(false);

  useEffect(() => {
    if (open) {
      startScanner();
    } else {
      stopScanner();
    }

    return () => {
      stopScanner();
    };
  }, [open]);

  const startScanner = async () => {
    try {
      const scanner = new Html5Qrcode('barcode-scanner-region');
      scannerRef.current = scanner;

      await scanner.start(
        { facingMode: 'environment' }, // Use rear camera
        {
          fps: 10,
          qrbox: { width: 250, height: 250 }
        },
        (decodedText) => {
          // Barcode successfully scanned
          onScan(decodedText);
          stopScanner();
        },
        (errorMessage) => {
          // Scan error (ignore, happens continuously)
        }
      );

      setIsScanning(true);
    } catch (err) {
      setError('Failed to start camera. Please check permissions.');
      console.error('Scanner error:', err);
    }
  };

  const stopScanner = async () => {
    if (scannerRef.current && isScanning) {
      try {
        await scannerRef.current.stop();
        scannerRef.current = null;
        setIsScanning(false);
      } catch (err) {
        console.error('Failed to stop scanner:', err);
      }
    }
  };

  const toggleTorch = async () => {
    if (scannerRef.current) {
      try {
        const track = await scannerRef.current.getRunningTrack();
        if (track && 'applyConstraints' in track) {
          await (track as any).applyConstraints({
            advanced: [{ torch: !torchEnabled }]
          });
          setTorchEnabled(!torchEnabled);
        }
      } catch (err) {
        console.error('Torch not supported:', err);
      }
    }
  };

  const handleManualEntry = () => {
    const barcode = prompt(`Enter ${mode} barcode manually:`);
    if (barcode) {
      onScan(barcode);
    }
  };

  return (
    <Dialog
      open={open}
      onClose={onClose}
      maxWidth="sm"
      fullWidth
      fullScreen={window.innerWidth < 600} // Full screen on mobile
    >
      <DialogTitle>
        <Box display="flex" justifyContent="space-between" alignItems="center">
          <Typography variant="h6">
            Scan {mode === 'location' ? 'Location' : 'Product'} Barcode
          </Typography>
          <IconButton onClick={onClose}>
            <CloseIcon />
          </IconButton>
        </Box>
      </DialogTitle>

      <DialogContent>
        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}

        <Box
          id="barcode-scanner-region"
          sx={{
            width: '100%',
            minHeight: 300,
            backgroundColor: 'black',
            borderRadius: 1,
            overflow: 'hidden'
          }}
        />

        <Typography variant="body2" color="textSecondary" textAlign="center" mt={2}>
          Position the barcode within the frame. It will scan automatically.
        </Typography>

        {/* Torch Toggle (if supported) */}
        <Box textAlign="center" mt={2}>
          <IconButton onClick={toggleTorch} disabled={!isScanning}>
            {torchEnabled ? <FlashOffIcon /> : <FlashOnIcon />}
          </IconButton>
        </Box>
      </DialogContent>

      <DialogActions>
        <Button onClick={handleManualEntry}>
          Manual Entry
        </Button>
        <Button onClick={onClose}>
          Cancel
        </Button>
      </DialogActions>
    </Dialog>
  );
};
```

#### 2. VarianceIndicator Component

```typescript
// frontend-app/src/features/reconciliation/components/VarianceIndicator.tsx

import React from 'react';
import { Box, Typography, Chip } from '@mui/material';
import {
  CheckCircle as CheckIcon,
  Warning as WarningIcon,
  Error as ErrorIcon
} from '@mui/icons-material';

interface VarianceIndicatorProps {
  variance: number;
  expectedQuantity: number;
}

export const VarianceIndicator: React.FC<VarianceIndicatorProps> = ({
  variance,
  expectedQuantity
}) => {
  const variancePercentage = expectedQuantity > 0
    ? Math.abs((variance / expectedQuantity) * 100)
    : 0;

  const getSeverity = (): {
    color: 'success' | 'warning' | 'error';
    icon: React.ReactElement;
    label: string;
  } => {
    if (variance === 0) {
      return {
        color: 'success',
        icon: <CheckIcon />,
        label: 'Exact Match'
      };
    }

    if (variancePercentage < 5) {
      return {
        color: 'success',
        icon: <CheckIcon />,
        label: 'Minor Variance'
      };
    }

    if (variancePercentage < 10) {
      return {
        color: 'warning',
        icon: <WarningIcon />,
        label: 'Moderate Variance'
      };
    }

    return {
      color: 'error',
      icon: <ErrorIcon />,
      label: 'Significant Variance'
    };
  };

  const severity = getSeverity();

  return (
    <Box>
      <Box display="flex" alignItems="center" gap={1} mb={1}>
        <Typography
          variant="h5"
          color={severity.color}
          fontWeight="bold"
        >
          {variance > 0 ? '+' : ''}{variance.toFixed(2)}
        </Typography>
        <Chip
          icon={severity.icon}
          label={severity.label}
          color={severity.color}
          size="small"
        />
      </Box>
      <Typography variant="caption" color="textSecondary">
        {variancePercentage.toFixed(1)}% variance
      </Typography>
    </Box>
  );
};
```

#### 3. SyncQueuePanel Component

```typescript
// frontend-app/src/features/reconciliation/components/SyncQueuePanel.tsx

import React from 'react';
import {
  Paper,
  Typography,
  List,
  ListItem,
  ListItemText,
  ListItemSecondaryAction,
  IconButton,
  Button,
  Box,
  Chip,
  Alert,
  AlertTitle
} from '@mui/material';
import {
  Refresh as RetryIcon,
  Delete as DeleteIcon,
  CloudUpload as UploadIcon
} from '@mui/icons-material';
import type { PendingStockCountEntry } from '../types/offline';

interface SyncQueuePanelProps {
  pendingEntries: PendingStockCountEntry[];
  failedEntries: PendingStockCountEntry[];
  onRetry: (entryId: string) => void;
  onClearQueue: () => void;
}

export const SyncQueuePanel: React.FC<SyncQueuePanelProps> = ({
  pendingEntries,
  failedEntries,
  onRetry,
  onClearQueue
}) => {
  return (
    <Paper sx={{ p: 3, mb: 3 }}>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
        <Typography variant="h6">
          Sync Queue
        </Typography>
        {(pendingEntries.length > 0 || failedEntries.length > 0) && (
          <Button
            size="small"
            onClick={onClearQueue}
            startIcon={<DeleteIcon />}
          >
            Clear Synced
          </Button>
        )}
      </Box>

      {/* Pending Entries */}
      {pendingEntries.length > 0 && (
        <>
          <Alert severity="info" icon={<UploadIcon />} sx={{ mb: 2 }}>
            <AlertTitle>Pending Sync</AlertTitle>
            {pendingEntries.length} {pendingEntries.length === 1 ? 'entry' : 'entries'} waiting to sync
          </Alert>

          <List>
            {pendingEntries.map((entry) => (
              <ListItem key={entry.id}>
                <ListItemText
                  primary={`${entry.locationCode} - ${entry.productCode}`}
                  secondary={`Quantity: ${entry.countedQuantity} | Created: ${new Date(entry.createdAt).toLocaleString()}`}
                />
                <ListItemSecondaryAction>
                  <Chip
                    label="PENDING"
                    color="warning"
                    size="small"
                  />
                </ListItemSecondaryAction>
              </ListItem>
            ))}
          </List>
        </>
      )}

      {/* Failed Entries */}
      {failedEntries.length > 0 && (
        <>
          <Alert severity="error" sx={{ mb: 2 }}>
            <AlertTitle>Failed Sync</AlertTitle>
            {failedEntries.length} {failedEntries.length === 1 ? 'entry' : 'entries'} failed to sync
          </Alert>

          <List>
            {failedEntries.map((entry) => (
              <ListItem key={entry.id}>
                <ListItemText
                  primary={`${entry.locationCode} - ${entry.productCode}`}
                  secondary={`Error: ${entry.error || 'Unknown error'} | Attempts: ${entry.attemptCount}`}
                />
                <ListItemSecondaryAction>
                  <IconButton
                    edge="end"
                    onClick={() => onRetry(entry.id)}
                    size="small"
                  >
                    <RetryIcon />
                  </IconButton>
                </ListItemSecondaryAction>
              </ListItem>
            ))}
          </List>
        </>
      )}

      {pendingEntries.length === 0 && failedEntries.length === 0 && (
        <Typography variant="body2" color="textSecondary">
          No entries pending sync.
        </Typography>
      )}
    </Paper>
  );
};
```

---


## Offline-First Architecture

### Overview

The stock count entry feature implements a comprehensive offline-first architecture to ensure warehouse operators can record inventory counts without interruption, even in areas with poor network connectivity. This section details the client-side storage strategy, synchronization mechanisms, conflict resolution, and error handling.

### Architecture Principles

1. **Offline by Default**: Application works fully offline from first load
2. **Progressive Enhancement**: Online features enhance but don't block offline usage
3. **Eventual Consistency**: Data syncs when network available, conflicts resolved gracefully
4. **Transparent Sync**: Users informed of sync status but not blocked by it
5. **Data Integrity**: Client-side validation matches server-side rules
6. **Resilience**: Graceful degradation when services unavailable

### IndexedDB Storage Strategy

#### Database Schema

```typescript
// frontend-app/src/features/reconciliation/services/offlineStorageService.ts

interface StockCountDB extends DBSchema {
  worksheets: {
    key: string; // worksheetId
    value: {
      id: string;
      countReference: string;
      countType: string;
      status: string;
      locationFilter: any;
      productFilter: any;
      expectedEntries: number;
      recordedEntries: number;
      percentageComplete: number;
      initiatedBy: string;
      initiatedAt: string;
      lastSyncedAt: string;
      version: number;
    };
    indexes: {
      'by-status': string;
      'by-initiated-at': string;
    };
  };
  entries: {
    key: string; // entryId
    value: {
      id: string;
      worksheetId: string;
      locationId: string;
      locationBarcode: string;
      locationCode: string;
      locationDescription: string;
      productId: string;
      productBarcode: string;
      productCode: string;
      productDescription: string;
      systemQuantity: number;
      countedQuantity: number;
      varianceQuantity: number;
      variancePercentage: number;
      notes?: string;
      recordedBy: string;
      recordedAt: string;
      syncStatus: 'PENDING' | 'SYNCING' | 'SYNCED' | 'FAILED';
      syncedAt?: string;
      attemptCount: number;
      lastAttemptAt?: string;
      error?: string;
      version: number;
    };
    indexes: {
      'by-worksheet': string;
      'by-sync-status': string;
      'by-recorded-at': string;
      'by-location-product': [string, string, string]; // [worksheetId, locationId, productId]
    };
  };
  products: {
    key: string; // productId
    value: {
      id: string;
      code: string;
      barcode: string;
      description: string;
      category: string;
      unit: string;
      cachedAt: string;
    };
    indexes: {
      'by-barcode': string;
      'by-code': string;
    };
  };
  locations: {
    key: string; // locationId
    value: {
      id: string;
      code: string;
      barcode: string;
      description: string;
      zone: string;
      aisle: string;
      cachedAt: string;
    };
    indexes: {
      'by-barcode': string;
      'by-code': string;
    };
  };
  syncQueue: {
    key: string; // queueId
    value: {
      id: string;
      entryId: string;
      worksheetId: string;
      operation: 'CREATE' | 'UPDATE' | 'DELETE';
      payload: any;
      createdAt: string;
      attemptCount: number;
      lastAttemptAt?: string;
      nextRetryAt?: string;
      error?: string;
    };
    indexes: {
      'by-worksheet': string;
      'by-next-retry': string;
    };
  };
}
```

#### IndexedDB Operations

```typescript
import { openDB, DBSchema, IDBPDatabase } from 'idb';

const DB_NAME = 'stock-count-db';
const DB_VERSION = 1;

class OfflineStorageService {
  private db: IDBPDatabase<StockCountDB> | null = null;

  async initialize(): Promise<void> {
    this.db = await openDB<StockCountDB>(DB_NAME, DB_VERSION, {
      upgrade(db, oldVersion, newVersion, transaction) {
        // Create worksheets store
        if (!db.objectStoreNames.contains('worksheets')) {
          const worksheetStore = db.createObjectStore('worksheets', {
            keyPath: 'id'
          });
          worksheetStore.createIndex('by-status', 'status');
          worksheetStore.createIndex('by-initiated-at', 'initiatedAt');
        }

        // Create entries store
        if (!db.objectStoreNames.contains('entries')) {
          const entryStore = db.createObjectStore('entries', {
            keyPath: 'id'
          });
          entryStore.createIndex('by-worksheet', 'worksheetId');
          entryStore.createIndex('by-sync-status', 'syncStatus');
          entryStore.createIndex('by-recorded-at', 'recordedAt');
          entryStore.createIndex('by-location-product', [
            'worksheetId',
            'locationId',
            'productId'
          ]);
        }

        // Create products store
        if (!db.objectStoreNames.contains('products')) {
          const productStore = db.createObjectStore('products', {
            keyPath: 'id'
          });
          productStore.createIndex('by-barcode', 'barcode');
          productStore.createIndex('by-code', 'code');
        }

        // Create locations store
        if (!db.objectStoreNames.contains('locations')) {
          const locationStore = db.createObjectStore('locations', {
            keyPath: 'id'
          });
          locationStore.createIndex('by-barcode', 'barcode');
          locationStore.createIndex('by-code', 'code');
        }

        // Create sync queue store
        if (!db.objectStoreNames.contains('syncQueue')) {
          const queueStore = db.createObjectStore('syncQueue', {
            keyPath: 'id'
          });
          queueStore.createIndex('by-worksheet', 'worksheetId');
          queueStore.createIndex('by-next-retry', 'nextRetryAt');
        }
      }
    });
  }

  // Worksheet operations
  async saveWorksheet(worksheet: any): Promise<void> {
    if (!this.db) await this.initialize();
    await this.db!.put('worksheets', worksheet);
  }

  async getWorksheet(worksheetId: string): Promise<any | undefined> {
    if (!this.db) await this.initialize();
    return await this.db!.get('worksheets', worksheetId);
  }

  // Entry operations
  async saveEntry(entry: any): Promise<void> {
    if (!this.db) await this.initialize();
    await this.db!.put('entries', entry);
  }

  async getEntry(entryId: string): Promise<any | undefined> {
    if (!this.db) await this.initialize();
    return await this.db!.get('entries', entryId);
  }

  async getEntriesByWorksheet(worksheetId: string): Promise<any[]> {
    if (!this.db) await this.initialize();
    return await this.db!.getAllFromIndex('entries', 'by-worksheet', worksheetId);
  }

  async getEntriesBySyncStatus(status: string): Promise<any[]> {
    if (!this.db) await this.initialize();
    return await this.db!.getAllFromIndex('entries', 'by-sync-status', status);
  }

  async checkDuplicateEntry(
    worksheetId: string,
    locationId: string,
    productId: string
  ): Promise<boolean> {
    if (!this.db) await this.initialize();
    const entries = await this.db!.getAllFromIndex(
      'entries',
      'by-location-product',
      [worksheetId, locationId, productId]
    );
    return entries.length > 0;
  }

  async deleteEntry(entryId: string): Promise<void> {
    if (!this.db) await this.initialize();
    await this.db!.delete('entries', entryId);
  }

  // Product operations (cached for offline lookup)
  async cacheProduct(product: any): Promise<void> {
    if (!this.db) await this.initialize();
    await this.db!.put('products', {
      ...product,
      cachedAt: new Date().toISOString()
    });
  }

  async getProductByBarcode(barcode: string): Promise<any | undefined> {
    if (!this.db) await this.initialize();
    const products = await this.db!.getAllFromIndex('products', 'by-barcode', barcode);
    return products[0];
  }

  // Location operations (cached for offline lookup)
  async cacheLocation(location: any): Promise<void> {
    if (!this.db) await this.initialize();
    await this.db!.put('locations', {
      ...location,
      cachedAt: new Date().toISOString()
    });
  }

  async getLocationByBarcode(barcode: string): Promise<any | undefined> {
    if (!this.db) await this.initialize();
    const locations = await this.db!.getAllFromIndex('locations', 'by-barcode', barcode);
    return locations[0];
  }

  // Sync queue operations
  async addToSyncQueue(queueItem: any): Promise<void> {
    if (!this.db) await this.initialize();
    await this.db!.put('syncQueue', queueItem);
  }

  async getNextSyncBatch(batchSize: number = 50): Promise<any[]> {
    if (!this.db) await this.initialize();
    const now = new Date().toISOString();
    const allItems = await this.db!.getAllFromIndex('syncQueue', 'by-next-retry');
    return allItems
      .filter(item => !item.nextRetryAt || item.nextRetryAt <= now)
      .slice(0, batchSize);
  }

  async removeSyncQueueItem(queueId: string): Promise<void> {
    if (!this.db) await this.initialize();
    await this.db!.delete('syncQueue', queueId);
  }

  // Bulk operations for efficiency
  async bulkSaveEntries(entries: any[]): Promise<void> {
    if (!this.db) await this.initialize();
    const tx = this.db!.transaction('entries', 'readwrite');
    await Promise.all([
      ...entries.map(entry => tx.store.put(entry)),
      tx.done
    ]);
  }

  // Clear old cached data
  async clearStaleCache(maxAgeDays: number = 7): Promise<void> {
    if (!this.db) await this.initialize();
    const cutoff = new Date();
    cutoff.setDate(cutoff.getDate() - maxAgeDays);
    const cutoffISO = cutoff.toISOString();

    // Clear stale products
    const products = await this.db!.getAll('products');
    const staleProducts = products.filter(p => p.cachedAt < cutoffISO);
    await Promise.all(staleProducts.map(p => this.db!.delete('products', p.id)));

    // Clear stale locations
    const locations = await this.db!.getAll('locations');
    const staleLocations = locations.filter(l => l.cachedAt < cutoffISO);
    await Promise.all(staleLocations.map(l => this.db!.delete('locations', l.id)));
  }
}

export const offlineStorage = new OfflineStorageService();
```

### Background Sync Implementation

#### Service Worker Registration

```typescript
// frontend-app/src/serviceWorkerRegistration.ts

export function register(config?: {
  onSuccess?: (registration: ServiceWorkerRegistration) => void;
  onUpdate?: (registration: ServiceWorkerRegistration) => void;
}) {
  if ('serviceWorker' in navigator) {
    window.addEventListener('load', () => {
      const swUrl = `${process.env.PUBLIC_URL}/service-worker.js`;

      navigator.serviceWorker
        .register(swUrl)
        .then(registration => {
          // Register background sync
          if ('sync' in registration) {
            registration.sync.register('sync-stock-count-entries');
          }

          registration.onupdatefound = () => {
            const installingWorker = registration.installing;
            if (installingWorker == null) {
              return;
            }
            installingWorker.onstatechange = () => {
              if (installingWorker.state === 'installed') {
                if (navigator.serviceWorker.controller) {
                  // New update available
                  console.log('New content available; please refresh.');
                  if (config && config.onUpdate) {
                    config.onUpdate(registration);
                  }
                } else {
                  // Content cached for offline use
                  console.log('Content cached for offline use.');
                  if (config && config.onSuccess) {
                    config.onSuccess(registration);
                  }
                }
              }
            };
          };
        })
        .catch(error => {
          console.error('Service worker registration failed:', error);
        });
    });
  }
}
```

#### Service Worker (Background Sync)

```javascript
// public/service-worker.js

import { offlineStorage } from './offlineStorageService';
import { apiClient } from './apiClient';

// Cache static assets
const CACHE_NAME = 'stock-count-v1';
const urlsToCache = [
  '/',
  '/static/js/bundle.js',
  '/static/css/main.css',
  '/manifest.json'
];

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) => {
      return cache.addAll(urlsToCache);
    })
  );
});

// Background sync event
self.addEventListener('sync', (event) => {
  if (event.tag === 'sync-stock-count-entries') {
    event.waitUntil(syncStockCountEntries());
  }
});

async function syncStockCountEntries() {
  try {
    const syncBatch = await offlineStorage.getNextSyncBatch(50);

    for (const queueItem of syncBatch) {
      try {
        // Attempt to sync entry
        const response = await fetch(`/api/v1/reconciliation/stock-counts/${queueItem.worksheetId}/entries`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${getAuthToken()}`
          },
          body: JSON.stringify(queueItem.payload)
        });

        if (response.ok) {
          // Sync successful
          await offlineStorage.removeSyncQueueItem(queueItem.id);
          await offlineStorage.saveEntry({
            ...queueItem.payload,
            syncStatus: 'SYNCED',
            syncedAt: new Date().toISOString()
          });
        } else if (response.status === 409) {
          // Conflict - mark for manual resolution
          await offlineStorage.saveEntry({
            ...queueItem.payload,
            syncStatus: 'FAILED',
            error: 'Duplicate entry detected'
          });
          await offlineStorage.removeSyncQueueItem(queueItem.id);
        } else {
          // Retry on next sync
          const nextRetryDelay = Math.min(
            Math.pow(2, queueItem.attemptCount) * 1000,
            300000 // Max 5 minutes
          );
          const nextRetryAt = new Date(Date.now() + nextRetryDelay).toISOString();

          await offlineStorage.addToSyncQueue({
            ...queueItem,
            attemptCount: queueItem.attemptCount + 1,
            lastAttemptAt: new Date().toISOString(),
            nextRetryAt
          });
        }
      } catch (error) {
        console.error('Sync error:', error);
        // Will retry on next sync event
      }
    }
  } catch (error) {
    console.error('Background sync failed:', error);
    throw error; // Re-throw to trigger retry
  }
}

function getAuthToken() {
  // Retrieve auth token from IndexedDB or localStorage
  return localStorage.getItem('authToken');
}
```

### Network Status Detection

```typescript
// frontend-app/src/hooks/useNetworkStatus.ts

import { useState, useEffect } from 'react';

export const useNetworkStatus = () => {
  const [isOnline, setIsOnline] = useState(navigator.onLine);
  const [wasOffline, setWasOffline] = useState(false);

  useEffect(() => {
    const handleOnline = () => {
      setIsOnline(true);
      if (wasOffline) {
        // Trigger sync when coming back online
        if ('serviceWorker' in navigator && 'sync' in ServiceWorkerRegistration.prototype) {
          navigator.serviceWorker.ready.then(registration => {
            return registration.sync.register('sync-stock-count-entries');
          });
        }
        setWasOffline(false);
      }
    };

    const handleOffline = () => {
      setIsOnline(false);
      setWasOffline(true);
    };

    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);

    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
    };
  }, [wasOffline]);

  return {
    isOnline,
    wasOffline
  };
};
```

### Conflict Resolution Strategy

#### Conflict Detection

Conflicts occur when:

1. **Duplicate Entries**: Same location-product combination recorded offline and online
2. **Concurrent Edits**: Entry modified on server while offline edit pending
3. **Deleted Items**: Entry deleted on server while offline edit pending

#### Resolution Strategy

**1. Timestamp Priority (Default)**

- Compare `recordedAt` timestamps
- Most recent entry wins
- Losing entry archived with conflict flag

**2. User-Driven Resolution**

- Display conflict dialog when detected
- Show both versions side-by-side
- User selects which to keep or merges manually

**3. Server-Side Validation**

- Server validates all entries on sync
- Returns 409 Conflict if duplicate detected
- Client handles conflict based on strategy

#### Conflict Resolution Implementation

```typescript
// frontend-app/src/features/reconciliation/services/conflictResolutionService.ts

export class ConflictResolutionService {
  async resolveConflict(
    localEntry: StockCountEntry,
    serverEntry: StockCountEntry
  ): Promise<{
    resolution: 'KEEP_LOCAL' | 'KEEP_SERVER' | 'MANUAL';
    resolvedEntry?: StockCountEntry;
  }> {
    // Automatic resolution based on timestamp
    const localTime = new Date(localEntry.recordedAt).getTime();
    const serverTime = new Date(serverEntry.recordedAt).getTime();

    if (localTime > serverTime) {
      // Local is newer - keep local
      return {
        resolution: 'KEEP_LOCAL',
        resolvedEntry: localEntry
      };
    } else if (serverTime > localTime) {
      // Server is newer - keep server
      return {
        resolution: 'KEEP_SERVER',
        resolvedEntry: serverEntry
      };
    } else {
      // Same timestamp - require manual resolution
      return {
        resolution: 'MANUAL'
      };
    }
  }

  async showConflictDialog(
    localEntry: StockCountEntry,
    serverEntry: StockCountEntry
  ): Promise<'KEEP_LOCAL' | 'KEEP_SERVER'> {
    // Display modal dialog for user decision
    return new Promise((resolve) => {
      // Implementation would show a MUI Dialog component
      // For now, return server entry as default
      resolve('KEEP_SERVER');
    });
  }
}

export const conflictResolution = new ConflictResolutionService();
```

### Sync Status Management

#### Sync States

1. **PENDING** - Entry saved locally, awaiting sync
2. **SYNCING** - Currently being sent to server
3. **SYNCED** - Successfully synced to server
4. **FAILED** - Sync failed after max retries

#### Sync Hook Implementation

```typescript
// frontend-app/src/features/reconciliation/hooks/useOfflineSync.ts

import { useState, useEffect, useCallback } from 'react';
import { offlineStorage } from '../services/offlineStorageService';
import { stockCountService } from '../services/stockCountService';
import { useNetworkStatus } from '../../common/hooks/useNetworkStatus';

export const useOfflineSync = (worksheetId: string) => {
  const { isOnline } = useNetworkStatus();
  const [pendingEntries, setPendingEntries] = useState<any[]>([]);
  const [failedEntries, setFailedEntries] = useState<any[]>([]);
  const [syncStatus, setSyncStatus] = useState<'IDLE' | 'SYNCING' | 'COMPLETED' | 'FAILED'>('IDLE');
  const [syncProgress, setSyncProgress] = useState(0);

  // Load pending and failed entries
  const loadSyncQueue = useCallback(async () => {
    const pending = await offlineStorage.getEntriesBySyncStatus('PENDING');
    const failed = await offlineStorage.getEntriesBySyncStatus('FAILED');

    setPendingEntries(pending.filter(e => e.worksheetId === worksheetId));
    setFailedEntries(failed.filter(e => e.worksheetId === worksheetId));
  }, [worksheetId]);

  useEffect(() => {
    loadSyncQueue();
  }, [loadSyncQueue]);

  // Sync pending entries when online
  useEffect(() => {
    if (isOnline && pendingEntries.length > 0) {
      syncPendingEntries();
    }
  }, [isOnline, pendingEntries.length]);

  const syncPendingEntries = async () => {
    setSyncStatus('SYNCING');
    let synced = 0;
    const total = pendingEntries.length;

    for (const entry of pendingEntries) {
      try {
        // Update sync status to SYNCING
        await offlineStorage.saveEntry({
          ...entry,
          syncStatus: 'SYNCING'
        });

        // Send to server
        await stockCountService.recordEntry(worksheetId, {
          locationId: entry.locationId,
          locationBarcode: entry.locationBarcode,
          productId: entry.productId,
          productBarcode: entry.productBarcode,
          systemQuantity: entry.systemQuantity,
          countedQuantity: entry.countedQuantity,
          notes: entry.notes
        });

        // Update sync status to SYNCED
        await offlineStorage.saveEntry({
          ...entry,
          syncStatus: 'SYNCED',
          syncedAt: new Date().toISOString()
        });

        synced++;
        setSyncProgress(Math.round((synced / total) * 100));
      } catch (error: any) {
        console.error('Sync error:', error);

        // Update sync status to FAILED
        await offlineStorage.saveEntry({
          ...entry,
          syncStatus: 'FAILED',
          error: error.message,
          attemptCount: (entry.attemptCount || 0) + 1
        });
      }
    }

    setSyncStatus(synced === total ? 'COMPLETED' : 'FAILED');
    await loadSyncQueue();
  };

  const retryFailedEntry = async (entryId: string) => {
    const entry = await offlineStorage.getEntry(entryId);
    if (!entry) return;

    try {
      await offlineStorage.saveEntry({
        ...entry,
        syncStatus: 'PENDING',
        attemptCount: 0,
        error: undefined
      });

      await loadSyncQueue();

      if (isOnline) {
        await syncPendingEntries();
      }
    } catch (error) {
      console.error('Retry failed:', error);
    }
  };

  const clearSyncQueue = async () => {
    const syncedEntries = await offlineStorage.getEntriesBySyncStatus('SYNCED');
    await Promise.all(
      syncedEntries
        .filter(e => e.worksheetId === worksheetId)
        .map(e => offlineStorage.deleteEntry(e.id))
    );
    await loadSyncQueue();
  };

  return {
    pendingEntries,
    failedEntries,
    syncStatus,
    syncProgress,
    syncPending: syncPendingEntries,
    retryFailedEntry,
    clearSyncQueue
  };
};
```

### Data Validation Strategy

#### Client-Side Validation (Offline)

All server-side validation rules replicated on client for offline operation:

1. **Location Validation**
   - Location exists in cached data
   - Location barcode format valid
   - Location within worksheet scope

2. **Product Validation**
   - Product exists in cached data
   - Product barcode format valid
   - Product within worksheet scope

3. **Quantity Validation**
   - Non-negative decimal number
   - Maximum 2 decimal places
   - Reasonable range (0 - 999999)

4. **Duplicate Validation**
   - Check IndexedDB for existing location-product combination
   - Warn user if duplicate detected
   - Allow override with confirmation

#### Server-Side Validation (Sync)

Server performs authoritative validation on sync:

1. **Business Rule Validation**
   - Worksheet exists and in correct status
   - User has permission to record entries
   - Location and product valid in current context

2. **Data Integrity Validation**
   - No duplicate entries (final check)
   - Quantities within acceptable ranges
   - Timestamps reasonable (not future dates)

3. **Conflict Detection**
   - Check for concurrent edits
   - Return 409 Conflict if duplicate exists
   - Client handles conflict resolution

### Performance Optimizations

#### 1. Lazy Loading

```typescript
// Load worksheet data on demand
const loadWorksheetData = async (worksheetId: string) => {
  // Check cache first
  let worksheet = await offlineStorage.getWorksheet(worksheetId);

  if (!worksheet || isStale(worksheet)) {
    // Fetch from server if online
    if (navigator.onLine) {
      worksheet = await stockCountService.getWorksheet(worksheetId);
      await offlineStorage.saveWorksheet(worksheet);
    }
  }

  return worksheet;
};
```

#### 2. Batch Sync

```typescript
// Sync entries in batches of 50
const BATCH_SIZE = 50;

const syncInBatches = async (entries: any[]) => {
  for (let i = 0; i < entries.length; i += BATCH_SIZE) {
    const batch = entries.slice(i, i + BATCH_SIZE);
    await Promise.all(batch.map(entry => syncEntry(entry)));
  }
};
```

#### 3. Debounced Progress Updates

```typescript
// Debounce progress updates to avoid excessive re-renders
import { debounce } from 'lodash';

const updateProgressDebounced = debounce((progress: number) => {
  setSyncProgress(progress);
}, 500);
```

#### 4. Cache Expiration

```typescript
// Expire cached data after 24 hours
const CACHE_EXPIRY_HOURS = 24;

const isStale = (data: any): boolean => {
  const cacheTime = new Date(data.cachedAt).getTime();
  const now = Date.now();
  const expiryMs = CACHE_EXPIRY_HOURS * 60 * 60 * 1000;
  return now - cacheTime > expiryMs;
};
```

### Error Handling and Recovery

#### Error Categories

1. **Network Errors** - No connectivity or timeout
   - Store in IndexedDB
   - Retry with exponential backoff
   - Notify user when persistent

2. **Validation Errors** - Client or server validation failure
   - Display immediately to user
   - Do not store in IndexedDB
   - Allow user to correct

3. **Conflict Errors** - Duplicate or concurrent edit
   - Show conflict resolution dialog
   - User selects resolution strategy
   - Apply resolution and retry

4. **Authorization Errors** - Token expired or insufficient permissions
   - Refresh token automatically
   - Re-authenticate if refresh fails
   - Retry original operation

#### Retry Strategy

```typescript
const retryWithExponentialBackoff = async (
  operation: () => Promise<any>,
  maxAttempts: number = 5,
  initialDelay: number = 1000
): Promise<any> => {
  let attempt = 0;

  while (attempt < maxAttempts) {
    try {
      return await operation();
    } catch (error: any) {
      attempt++;

      if (attempt >= maxAttempts) {
        throw error;
      }

      // Exponential backoff: 1s, 2s, 4s, 8s, 16s
      const delay = initialDelay * Math.pow(2, attempt - 1);
      await new Promise(resolve => setTimeout(resolve, delay));
    }
  }
};
```

### Progressive Web App (PWA) Configuration

#### Manifest File

```json
// public/manifest.json
{
  "short_name": "WMS Stock Count",
  "name": "Warehouse Management System - Stock Count",
  "description": "Offline-capable stock count entry for warehouse operations",
  "icons": [
    {
      "src": "favicon.ico",
      "sizes": "64x64 32x32 24x24 16x16",
      "type": "image/x-icon"
    },
    {
      "src": "logo192.png",
      "type": "image/png",
      "sizes": "192x192"
    },
    {
      "src": "logo512.png",
      "type": "image/png",
      "sizes": "512x512"
    }
  ],
  "start_url": ".",
  "display": "standalone",
  "theme_color": "#1976d2",
  "background_color": "#ffffff",
  "orientation": "portrait"
}
```

#### Install Prompt

```typescript
// frontend-app/src/components/InstallPWAPrompt.tsx

export const InstallPWAPrompt: React.FC = () => {
  const [deferredPrompt, setDeferredPrompt] = useState<any>(null);
  const [showPrompt, setShowPrompt] = useState(false);

  useEffect(() => {
    const handler = (e: any) => {
      e.preventDefault();
      setDeferredPrompt(e);
      setShowPrompt(true);
    };

    window.addEventListener('beforeinstallprompt', handler);

    return () => {
      window.removeEventListener('beforeinstallprompt', handler);
    };
  }, []);

  const handleInstall = async () => {
    if (!deferredPrompt) return;

    deferredPrompt.prompt();
    const { outcome } = await deferredPrompt.userChoice;

    if (outcome === 'accepted') {
      console.log('PWA installed');
    }

    setDeferredPrompt(null);
    setShowPrompt(false);
  };

  if (!showPrompt) return null;

  return (
    <Alert
      severity="info"
      action={
        <Button color="inherit" size="small" onClick={handleInstall}>
          Install
        </Button>
      }
      onClose={() => setShowPrompt(false)}
    >
      Install this app for offline stock counting
    </Alert>
  );
};
```

---


## Domain Model Design

### Aggregates

#### StockCount Aggregate (Modified)

The `StockCount` aggregate is extended to handle entry recording with duplicate prevention and progress tracking.

```java
// services/reconciliation-service/reconciliation-domain/reconciliation-domain-core/src/main/java/com/ccbsa/wms/reconciliation/domain/core/entity/StockCount.java

package com.ccbsa.wms.reconciliation.domain.core.entity;

import com.ccbsa.common.domain.entity.AggregateRoot;
import com.ccbsa.common.domain.valueobject.*;
import com.ccbsa.wms.reconciliation.domain.core.valueobject.*;
import com.ccbsa.wms.reconciliation.domain.core.event.*;
import com.ccbsa.wms.reconciliation.domain.core.exception.*;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.*;

public class StockCount extends AggregateRoot<StockCountId> {

    private final TenantId tenantId;
    private final CountReference countReference;
    private CountType countType;
    private StockCountStatus status;
    private LocationFilter locationFilter;
    private ProductFilter productFilter;
    private UserId initiatedBy;
    private ZonedDateTime initiatedAt;
    private ZonedDateTime completedAt;
    private Notes completionNotes;

    // Entry tracking
    private final List<StockCountEntry> entries;
    private int totalExpectedEntries;
    private int totalVariances;
    private int criticalVariances;

    // Entry recording logic
    public StockCountEntryRecordedEvent recordEntry(
        LocationId locationId,
        LocationBarcode locationBarcode,
        ProductId productId,
        ProductBarcode productBarcode,
        Quantity systemQuantity,
        Quantity countedQuantity,
        UserId recordedBy,
        Notes notes
    ) {
        // Validate status
        validateCanRecordEntry();

        // Check for duplicate entry
        checkNoDuplicateEntry(locationId, productId);

        // Create entry
        StockCountEntryId entryId = new StockCountEntryId(UUID.randomUUID());
        StockCountEntry entry = StockCountEntry.builder()
            .id(entryId)
            .stockCountId(this.getId())
            .locationId(locationId)
            .locationBarcode(locationBarcode)
            .productId(productId)
            .productBarcode(productBarcode)
            .systemQuantity(systemQuantity)
            .countedQuantity(countedQuantity)
            .recordedBy(recordedBy)
            .recordedAt(ZonedDateTime.now())
            .notes(notes)
            .build();

        // Calculate variance
        entry.calculateVariance();

        // Add to entries
        this.entries.add(entry);

        // Update status if first entry
        if (this.status == StockCountStatus.DRAFT) {
            this.status = StockCountStatus.IN_PROGRESS;
        }

        // Publish event
        StockCountEntryRecordedEvent event = StockCountEntryRecordedEvent.builder()
            .stockCountId(this.getId())
            .entryId(entryId)
            .tenantId(this.tenantId)
            .locationId(locationId)
            .productId(productId)
            .systemQuantity(systemQuantity.getValue())
            .countedQuantity(countedQuantity.getValue())
            .varianceQuantity(entry.getVarianceQuantity().getValue())
            .variancePercentage(entry.getVariancePercentage())
            .recordedBy(recordedBy)
            .recordedAt(entry.getRecordedAt())
            .occurredAt(ZonedDateTime.now())
            .build();

        this.registerDomainEvent(event);

        return event;
    }

    private void validateCanRecordEntry() {
        if (this.status == StockCountStatus.COMPLETED) {
            throw new StockCountException("Cannot record entry - stock count already completed");
        }

        if (this.status == StockCountStatus.CANCELLED) {
            throw new StockCountException("Cannot record entry - stock count cancelled");
        }
    }

    private void checkNoDuplicateEntry(LocationId locationId, ProductId productId) {
        boolean exists = this.entries.stream()
            .anyMatch(entry ->
                entry.getLocationId().equals(locationId) &&
                entry.getProductId().equals(productId)
            );

        if (exists) {
            throw new DuplicateStockCountEntryException(
                String.format("Entry already exists for location %s and product %s",
                    locationId.getValue(), productId.getValue())
            );
        }
    }

    public int getRecordedEntriesCount() {
        return this.entries.size();
    }

    public int getPercentageComplete() {
        if (this.totalExpectedEntries == 0) {
            return 0;
        }
        return (int) ((this.entries.size() * 100.0) / this.totalExpectedEntries);
    }

    public Optional<StockCountEntry> getEntry(StockCountEntryId entryId) {
        return this.entries.stream()
            .filter(entry -> entry.getId().equals(entryId))
            .findFirst();
    }

    public List<StockCountEntry> getEntries() {
        return Collections.unmodifiableList(this.entries);
    }

    public boolean hasEntry(LocationId locationId, ProductId productId) {
        return this.entries.stream()
            .anyMatch(entry ->
                entry.getLocationId().equals(locationId) &&
                entry.getProductId().equals(productId)
            );
    }

    // ... other methods (initiate, complete, etc.)
}
```

#### StockCountEntry Entity

```java
// services/reconciliation-service/reconciliation-domain/reconciliation-domain-core/src/main/java/com/ccbsa/wms/reconciliation/domain/core/entity/StockCountEntry.java

package com.ccbsa.wms.reconciliation.domain.core.entity;

import com.ccbsa.common.domain.entity.BaseEntity;
import com.ccbsa.common.domain.valueobject.*;
import com.ccbsa.wms.reconciliation.domain.core.valueobject.*;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZonedDateTime;

@Getter
@Builder
public class StockCountEntry extends BaseEntity<StockCountEntryId> {

    private final StockCountEntryId id;
    private final StockCountId stockCountId;
    private final LocationId locationId;
    private final LocationBarcode locationBarcode;
    private final ProductId productId;
    private final ProductBarcode productBarcode;
    private final Quantity systemQuantity;
    private final Quantity countedQuantity;
    private Quantity varianceQuantity;
    private BigDecimal variancePercentage;
    private final UserId recordedBy;
    private final ZonedDateTime recordedAt;
    private final Notes notes;

    public void calculateVariance() {
        // Calculate variance quantity
        BigDecimal variance = this.countedQuantity.getValue()
            .subtract(this.systemQuantity.getValue());
        this.varianceQuantity = new Quantity(variance);

        // Calculate variance percentage
        if (this.systemQuantity.getValue().compareTo(BigDecimal.ZERO) > 0) {
            this.variancePercentage = variance
                .divide(this.systemQuantity.getValue(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
        } else {
            // If system quantity is zero, any counted quantity is 100% variance
            this.variancePercentage = this.countedQuantity.getValue().compareTo(BigDecimal.ZERO) > 0
                ? BigDecimal.valueOf(100.00)
                : BigDecimal.ZERO;
        }
    }

    public boolean hasVariance() {
        return this.varianceQuantity != null &&
               this.varianceQuantity.getValue().compareTo(BigDecimal.ZERO) != 0;
    }

    public boolean isSignificantVariance(BigDecimal thresholdPercentage) {
        return this.variancePercentage != null &&
               this.variancePercentage.abs().compareTo(thresholdPercentage) > 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StockCountEntry that = (StockCountEntry) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
```

### Value Objects

#### 1. StockCountEntryId

```java
// common/common-domain/src/main/java/com/ccbsa/common/domain/valueobject/StockCountEntryId.java

package com.ccbsa.common.domain.valueobject;

import java.util.UUID;

public class StockCountEntryId extends BaseId<UUID> {

    public StockCountEntryId(UUID value) {
        super(value);
    }

    public static StockCountEntryId of(UUID value) {
        return new StockCountEntryId(value);
    }

    public static StockCountEntryId generate() {
        return new StockCountEntryId(UUID.randomUUID());
    }
}
```

#### 2. LocationBarcode

```java
// common/common-domain/src/main/java/com/ccbsa/common/domain/valueobject/LocationBarcode.java

package com.ccbsa.common.domain.valueobject;

import java.util.Objects;
import java.util.regex.Pattern;

public class LocationBarcode {

    private static final Pattern BARCODE_PATTERN = Pattern.compile("^[A-Z0-9-]{5,50}$");
    private final String value;

    public LocationBarcode(String value) {
        validateBarcode(value);
        this.value = value;
    }

    private void validateBarcode(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Location barcode cannot be empty");
        }

        if (!BARCODE_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(
                "Invalid location barcode format: " + value +
                ". Must be 5-50 alphanumeric characters with hyphens allowed."
            );
        }
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LocationBarcode that = (LocationBarcode) o;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
```

#### 3. ProductBarcode

```java
// common/common-domain/src/main/java/com/ccbsa/common/domain/valueobject/ProductBarcode.java

package com.ccbsa.common.domain.valueobject;

import java.util.Objects;
import java.util.regex.Pattern;

public class ProductBarcode {

    // Support multiple barcode formats: EAN-13, UPC-A, Code 128, etc.
    private static final Pattern BARCODE_PATTERN = Pattern.compile("^[0-9A-Z-]{5,50}$");
    private final String value;

    public ProductBarcode(String value) {
        validateBarcode(value);
        this.value = value;
    }

    private void validateBarcode(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Product barcode cannot be empty");
        }

        if (!BARCODE_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(
                "Invalid product barcode format: " + value +
                ". Must be 5-50 alphanumeric characters with hyphens allowed."
            );
        }
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductBarcode that = (ProductBarcode) o;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
```

#### 4. CountReference

```java
// services/reconciliation-service/reconciliation-domain/reconciliation-domain-core/src/main/java/com/ccbsa/wms/reconciliation/domain/core/valueobject/CountReference.java

package com.ccbsa.wms.reconciliation.domain.core.valueobject;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class CountReference {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private final String value;

    private CountReference(String value) {
        this.value = value;
    }

    public static CountReference generate(String prefix, int sequence) {
        String date = LocalDate.now().format(DATE_FORMAT);
        String reference = String.format("%s-%s-%05d", prefix, date, sequence);
        return new CountReference(reference);
    }

    public static CountReference of(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Count reference cannot be empty");
        }
        return new CountReference(value);
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CountReference that = (CountReference) o;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
```

### Domain Events

#### StockCountEntryRecordedEvent

```java
// services/reconciliation-service/reconciliation-domain/reconciliation-domain-core/src/main/java/com/ccbsa/wms/reconciliation/domain/core/event/StockCountEntryRecordedEvent.java

package com.ccbsa.wms.reconciliation.domain.core.event;

import com.ccbsa.common.domain.event.DomainEvent;
import com.ccbsa.common.domain.valueobject.*;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Getter
@Builder
public class StockCountEntryRecordedEvent implements DomainEvent {

    private final StockCountId stockCountId;
    private final StockCountEntryId entryId;
    private final TenantId tenantId;
    private final LocationId locationId;
    private final ProductId productId;
    private final BigDecimal systemQuantity;
    private final BigDecimal countedQuantity;
    private final BigDecimal varianceQuantity;
    private final BigDecimal variancePercentage;
    private final UserId recordedBy;
    private final ZonedDateTime recordedAt;
    private final ZonedDateTime occurredAt;

    @Override
    public String getEventType() {
        return "StockCountEntryRecorded";
    }

    @Override
    public ZonedDateTime getOccurredAt() {
        return occurredAt;
    }

    @Override
    public String getAggregateId() {
        return stockCountId.getValue().toString();
    }
}
```

### Domain Exceptions

#### DuplicateStockCountEntryException

```java
// services/reconciliation-service/reconciliation-domain/reconciliation-domain-core/src/main/java/com/ccbsa/wms/reconciliation/domain/core/exception/DuplicateStockCountEntryException.java

package com.ccbsa.wms.reconciliation.domain.core.exception;

public class DuplicateStockCountEntryException extends RuntimeException {

    public DuplicateStockCountEntryException(String message) {
        super(message);
    }

    public DuplicateStockCountEntryException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

#### InvalidBarcodeException

```java
// services/reconciliation-service/reconciliation-domain/reconciliation-domain-core/src/main/java/com/ccbsa/wms/reconciliation/domain/core/exception/InvalidBarcodeException.java

package com.ccbsa.wms.reconciliation.domain.core.exception;

public class InvalidBarcodeException extends RuntimeException {

    public InvalidBarcodeException(String message) {
        super(message);
    }

    public InvalidBarcodeException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

### Domain Services

Domain services are NOT used for this user story (all logic in aggregate). Entry recording is purely an aggregate operation.

### Enums

#### StockCountStatus (Extended)

```java
// common/common-domain/src/main/java/com/ccbsa/common/domain/valueobject/StockCountStatus.java

package com.ccbsa.common.domain.valueobject;

public enum StockCountStatus {
    DRAFT,          // Worksheet generated, no entries recorded
    IN_PROGRESS,    // Entries being recorded
    COMPLETED,      // All entries recorded and finalized
    CANCELLED       // Count cancelled before completion
}
```

#### CountType

```java
// services/reconciliation-service/reconciliation-domain/reconciliation-domain-core/src/main/java/com/ccbsa/wms/reconciliation/domain/core/valueobject/CountType.java

package com.ccbsa.wms.reconciliation.domain.core.valueobject;

public enum CountType {
    CYCLE_COUNT,      // Subset of locations/products
    FULL_INVENTORY,   // All locations and products
    SPOT_CHECK        // Ad-hoc count of specific items
}
```

---


## Backend Implementation

### Application Service Layer

#### Command: RecordStockCountEntryCommand

```java
// services/reconciliation-service/reconciliation-domain/reconciliation-application-service/src/main/java/com/ccbsa/wms/reconciliation/application/service/command/dto/RecordStockCountEntryCommand.java

package com.ccbsa.wms.reconciliation.application.service.command.dto;

import com.ccbsa.common.domain.valueobject.*;
import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.*;
import java.math.BigDecimal;

@Value
@Builder
public class RecordStockCountEntryCommand {

    @NotNull(message = "Stock count ID is required")
    StockCountId stockCountId;

    @NotNull(message = "Tenant ID is required")
    TenantId tenantId;

    @NotNull(message = "Location ID is required")
    LocationId locationId;

    @NotNull(message = "Location barcode is required")
    LocationBarcode locationBarcode;

    @NotNull(message = "Product ID is required")
    ProductId productId;

    @NotNull(message = "Product barcode is required")
    ProductBarcode productBarcode;

    @NotNull(message = "System quantity is required")
    @DecimalMin(value = "0.0", message = "System quantity must be non-negative")
    Quantity systemQuantity;

    @NotNull(message = "Counted quantity is required")
    @DecimalMin(value = "0.0", message = "Counted quantity must be non-negative")
    Quantity countedQuantity;

    @NotNull(message = "Recorded by user ID is required")
    UserId recordedBy;

    Notes notes; // Optional
}
```

#### Command Handler: RecordStockCountEntryCommandHandler

```java
// services/reconciliation-service/reconciliation-domain/reconciliation-application-service/src/main/java/com/ccbsa/wms/reconciliation/application/service/command/RecordStockCountEntryCommandHandler.java

package com.ccbsa.wms.reconciliation.application.service.command;

import com.ccbsa.common.domain.event.DomainEventPublisher;
import com.ccbsa.wms.reconciliation.application.service.command.dto.RecordStockCountEntryCommand;
import com.ccbsa.wms.reconciliation.application.service.command.dto.RecordStockCountEntryResult;
import com.ccbsa.wms.reconciliation.application.service.port.repository.StockCountRepository;
import com.ccbsa.wms.reconciliation.domain.core.entity.StockCount;
import com.ccbsa.wms.reconciliation.domain.core.event.StockCountEntryRecordedEvent;
import com.ccbsa.wms.reconciliation.domain.core.exception.StockCountNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;

@Slf4j
@Component
@Validated
@RequiredArgsConstructor
public class RecordStockCountEntryCommandHandler {

    private final StockCountRepository stockCountRepository;
    private final DomainEventPublisher domainEventPublisher;

    @Transactional
    public RecordStockCountEntryResult handle(@Valid RecordStockCountEntryCommand command) {
        log.info("Recording stock count entry for stock count: {}, location: {}, product: {}",
            command.getStockCountId().getValue(),
            command.getLocationId().getValue(),
            command.getProductId().getValue());

        // 1. Load stock count aggregate
        StockCount stockCount = stockCountRepository
            .findById(command.getStockCountId())
            .orElseThrow(() -> new StockCountNotFoundException(
                "Stock count not found: " + command.getStockCountId().getValue()
            ));

        // 2. Record entry (domain logic handles validation and duplicate check)
        StockCountEntryRecordedEvent event = stockCount.recordEntry(
            command.getLocationId(),
            command.getLocationBarcode(),
            command.getProductId(),
            command.getProductBarcode(),
            command.getSystemQuantity(),
            command.getCountedQuantity(),
            command.getRecordedBy(),
            command.getNotes()
        );

        // 3. Save aggregate
        StockCount savedStockCount = stockCountRepository.save(stockCount);

        // 4. Publish domain event
        domainEventPublisher.publish(event);

        // 5. Build result
        RecordStockCountEntryResult result = RecordStockCountEntryResult.builder()
            .entryId(event.getEntryId())
            .stockCountId(savedStockCount.getId())
            .varianceQuantity(event.getVarianceQuantity())
            .variancePercentage(event.getVariancePercentage())
            .recordedEntriesCount(savedStockCount.getRecordedEntriesCount())
            .totalExpectedEntries(savedStockCount.getTotalExpectedEntries())
            .percentageComplete(savedStockCount.getPercentageComplete())
            .build();

        log.info("Stock count entry recorded successfully. Entry ID: {}, Variance: {}",
            result.getEntryId().getValue(),
            result.getVarianceQuantity());

        return result;
    }
}
```

#### Command Result: RecordStockCountEntryResult

```java
// services/reconciliation-service/reconciliation-domain/reconciliation-application-service/src/main/java/com/ccbsa/wms/reconciliation/application/service/command/dto/RecordStockCountEntryResult.java

package com.ccbsa.wms.reconciliation.application.service.command.dto;

import com.ccbsa.common.domain.valueobject.StockCountEntryId;
import com.ccbsa.common.domain.valueobject.StockCountId;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class RecordStockCountEntryResult {

    StockCountEntryId entryId;
    StockCountId stockCountId;
    BigDecimal varianceQuantity;
    BigDecimal variancePercentage;
    int recordedEntriesCount;
    int totalExpectedEntries;
    int percentageComplete;
}
```

### Application Layer (REST API)

#### Request DTO

```java
// services/reconciliation-service/reconciliation-application/src/main/java/com/ccbsa/wms/reconciliation/application/dto/command/RecordStockCountEntryRequestDTO.java

package com.ccbsa.wms.reconciliation.application.dto.command;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecordStockCountEntryRequestDTO {

    @NotNull(message = "Location ID is required")
    @JsonProperty("locationId")
    private String locationId;

    @NotNull(message = "Location barcode is required")
    @Size(min = 5, max = 50, message = "Location barcode must be 5-50 characters")
    @JsonProperty("locationBarcode")
    private String locationBarcode;

    @NotNull(message = "Product ID is required")
    @JsonProperty("productId")
    private String productId;

    @NotNull(message = "Product barcode is required")
    @Size(min = 5, max = 50, message = "Product barcode must be 5-50 characters")
    @JsonProperty("productBarcode")
    private String productBarcode;

    @NotNull(message = "System quantity is required")
    @DecimalMin(value = "0.0", message = "System quantity must be non-negative")
    @JsonProperty("systemQuantity")
    private BigDecimal systemQuantity;

    @NotNull(message = "Counted quantity is required")
    @DecimalMin(value = "0.0", message = "Counted quantity must be non-negative")
    @JsonProperty("countedQuantity")
    private BigDecimal countedQuantity;

    @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    @JsonProperty("notes")
    private String notes;
}
```

#### Response DTO

```java
// services/reconciliation-service/reconciliation-application/src/main/java/com/ccbsa/wms/reconciliation/application/dto/command/RecordStockCountEntryResponseDTO.java

package com.ccbsa.wms.reconciliation.application.dto.command;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecordStockCountEntryResponseDTO {

    @JsonProperty("entryId")
    private String entryId;

    @JsonProperty("stockCountId")
    private String stockCountId;

    @JsonProperty("varianceQuantity")
    private BigDecimal varianceQuantity;

    @JsonProperty("variancePercentage")
    private BigDecimal variancePercentage;

    @JsonProperty("recordedEntriesCount")
    private Integer recordedEntriesCount;

    @JsonProperty("totalExpectedEntries")
    private Integer totalExpectedEntries;

    @JsonProperty("percentageComplete")
    private Integer percentageComplete;

    @JsonProperty("recordedAt")
    private ZonedDateTime recordedAt;
}
```

#### REST Controller

```java
// services/reconciliation-service/reconciliation-application/src/main/java/com/ccbsa/wms/reconciliation/application/command/StockCountCommandController.java

package com.ccbsa.wms.reconciliation.application.command;

import com.ccbsa.common.domain.valueobject.*;
import com.ccbsa.wms.reconciliation.application.dto.command.*;
import com.ccbsa.wms.reconciliation.application.service.command.RecordStockCountEntryCommandHandler;
import com.ccbsa.wms.reconciliation.application.service.command.dto.RecordStockCountEntryCommand;
import com.ccbsa.wms.reconciliation.application.service.command.dto.RecordStockCountEntryResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.ZonedDateTime;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/reconciliation/stock-counts")
@RequiredArgsConstructor
@Tag(name = "Stock Count Commands", description = "Stock count command operations")
public class StockCountCommandController {

    private final RecordStockCountEntryCommandHandler recordStockCountEntryCommandHandler;

    @PostMapping("/{stockCountId}/entries")
    @Operation(summary = "Record stock count entry", description = "Record a counted quantity for a location-product combination")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Entry recorded successfully",
            content = @Content(schema = @Schema(implementation = RecordStockCountEntryResponseDTO.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "404", description = "Stock count not found"),
        @ApiResponse(responseCode = "409", description = "Duplicate entry - location-product already counted")
    })
    public ResponseEntity<RecordStockCountEntryResponseDTO> recordStockCountEntry(
        @PathVariable String stockCountId,
        @RequestHeader("X-Tenant-Id") String tenantId,
        @Valid @RequestBody RecordStockCountEntryRequestDTO request,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        log.info("Recording stock count entry for stock count: {}, location: {}, product: {}",
            stockCountId, request.getLocationId(), request.getProductId());

        // Build command
        RecordStockCountEntryCommand command = RecordStockCountEntryCommand.builder()
            .stockCountId(StockCountId.of(UUID.fromString(stockCountId)))
            .tenantId(TenantId.of(UUID.fromString(tenantId)))
            .locationId(LocationId.of(UUID.fromString(request.getLocationId())))
            .locationBarcode(new LocationBarcode(request.getLocationBarcode()))
            .productId(ProductId.of(UUID.fromString(request.getProductId())))
            .productBarcode(new ProductBarcode(request.getProductBarcode()))
            .systemQuantity(new Quantity(request.getSystemQuantity()))
            .countedQuantity(new Quantity(request.getCountedQuantity()))
            .recordedBy(UserId.of(UUID.fromString(userDetails.getUsername())))
            .notes(request.getNotes() != null ? new Notes(request.getNotes()) : null)
            .build();

        // Execute command
        RecordStockCountEntryResult result = recordStockCountEntryCommandHandler.handle(command);

        // Build response
        RecordStockCountEntryResponseDTO response = RecordStockCountEntryResponseDTO.builder()
            .entryId(result.getEntryId().getValue().toString())
            .stockCountId(result.getStockCountId().getValue().toString())
            .varianceQuantity(result.getVarianceQuantity())
            .variancePercentage(result.getVariancePercentage())
            .recordedEntriesCount(result.getRecordedEntriesCount())
            .totalExpectedEntries(result.getTotalExpectedEntries())
            .percentageComplete(result.getPercentageComplete())
            .recordedAt(ZonedDateTime.now())
            .build();

        log.info("Stock count entry recorded successfully. Entry ID: {}", response.getEntryId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
```

### Data Access Layer

#### JPA Entity

```java
// services/reconciliation-service/reconciliation-dataaccess/src/main/java/com/ccbsa/wms/reconciliation/dataaccess/entity/StockCountEntryJpaEntity.java

package com.ccbsa.wms.reconciliation.dataaccess.entity;

import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "stock_count_entries",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_count_location_product",
            columnNames = {"stock_count_id", "location_id", "product_id"}
        )
    },
    indexes = {
        @Index(name = "idx_entries_stock_count", columnList = "stock_count_id"),
        @Index(name = "idx_entries_location", columnList = "location_id"),
        @Index(name = "idx_entries_product", columnList = "product_id"),
        @Index(name = "idx_entries_recorded_at", columnList = "recorded_at")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockCountEntryJpaEntity {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "entry_id", updatable = false, nullable = false)
    private UUID entryId;

    @Column(name = "stock_count_id", nullable = false)
    private UUID stockCountId;

    @Column(name = "location_id", nullable = false)
    private UUID locationId;

    @Column(name = "location_barcode", length = 100)
    private String locationBarcode;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "product_barcode", length = 100)
    private String productBarcode;

    @Column(name = "system_quantity", precision = 10, scale = 2, nullable = false)
    private BigDecimal systemQuantity;

    @Column(name = "counted_quantity", precision = 10, scale = 2, nullable = false)
    private BigDecimal countedQuantity;

    // Computed columns (generated)
    @Column(
        name = "variance_quantity",
        precision = 10,
        scale = 2,
        insertable = false,
        updatable = false,
        columnDefinition = "DECIMAL(10,2) GENERATED ALWAYS AS (counted_quantity - system_quantity) STORED"
    )
    private BigDecimal varianceQuantity;

    @Column(
        name = "variance_percentage",
        precision = 5,
        scale = 2,
        insertable = false,
        updatable = false,
        columnDefinition = "DECIMAL(5,2) GENERATED ALWAYS AS (" +
            "CASE " +
            "  WHEN system_quantity = 0 THEN 100.00 " +
            "  ELSE ((counted_quantity - system_quantity) / system_quantity * 100) " +
            "END" +
        ") STORED"
    )
    private BigDecimal variancePercentage;

    @Column(name = "recorded_by", nullable = false)
    private UUID recordedBy;

    @Column(name = "recorded_at", nullable = false)
    private ZonedDateTime recordedAt;

    @Column(name = "entry_notes", length = 1000)
    private String entryNotes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = ZonedDateTime.now();
        if (this.recordedAt == null) {
            this.recordedAt = ZonedDateTime.now();
        }
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_count_id", insertable = false, updatable = false)
    private StockCountJpaEntity stockCount;
}
```

#### Repository Adapter

```java
// services/reconciliation-service/reconciliation-dataaccess/src/main/java/com/ccbsa/wms/reconciliation/dataaccess/adapter/StockCountRepositoryAdapter.java

package com.ccbsa.wms.reconciliation.dataaccess.adapter;

import com.ccbsa.common.domain.valueobject.StockCountId;
import com.ccbsa.wms.reconciliation.application.service.port.repository.StockCountRepository;
import com.ccbsa.wms.reconciliation.dataaccess.entity.StockCountJpaEntity;
import com.ccbsa.wms.reconciliation.dataaccess.entity.StockCountEntryJpaEntity;
import com.ccbsa.wms.reconciliation.dataaccess.jpa.StockCountJpaRepository;
import com.ccbsa.wms.reconciliation.dataaccess.jpa.StockCountEntryJpaRepository;
import com.ccbsa.wms.reconciliation.dataaccess.mapper.StockCountDataMapper;
import com.ccbsa.wms.reconciliation.domain.core.entity.StockCount;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockCountRepositoryAdapter implements StockCountRepository {

    private final StockCountJpaRepository stockCountJpaRepository;
    private final StockCountEntryJpaRepository stockCountEntryJpaRepository;
    private final StockCountDataMapper stockCountDataMapper;

    @Override
    @Transactional
    public StockCount save(StockCount stockCount) {
        log.debug("Saving stock count: {}", stockCount.getId().getValue());

        // Map to JPA entity
        StockCountJpaEntity jpaEntity = stockCountDataMapper.toJpaEntity(stockCount);

        // Save stock count
        StockCountJpaEntity savedEntity = stockCountJpaRepository.save(jpaEntity);

        // Save entries (including new entries)
        List<StockCountEntryJpaEntity> entryEntities = stockCount.getEntries().stream()
            .map(stockCountDataMapper::toEntryJpaEntity)
            .collect(Collectors.toList());

        stockCountEntryJpaRepository.saveAll(entryEntities);

        log.debug("Stock count saved successfully: {}", savedEntity.getStockCountId());

        return stockCountDataMapper.toDomainEntity(savedEntity, entryEntities);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StockCount> findById(StockCountId stockCountId) {
        log.debug("Finding stock count by ID: {}", stockCountId.getValue());

        Optional<StockCountJpaEntity> jpaEntity = stockCountJpaRepository
            .findById(stockCountId.getValue());

        if (jpaEntity.isEmpty()) {
            log.debug("Stock count not found: {}", stockCountId.getValue());
            return Optional.empty();
        }

        // Load entries
        List<StockCountEntryJpaEntity> entries = stockCountEntryJpaRepository
            .findByStockCountId(stockCountId.getValue());

        StockCount domainEntity = stockCountDataMapper.toDomainEntity(
            jpaEntity.get(),
            entries
        );

        log.debug("Stock count found: {} with {} entries",
            stockCountId.getValue(),
            entries.size());

        return Optional.of(domainEntity);
    }
}
```

#### JPA Repository

```java
// services/reconciliation-service/reconciliation-dataaccess/src/main/java/com/ccbsa/wms/reconciliation/dataaccess/jpa/StockCountEntryJpaRepository.java

package com.ccbsa.wms.reconciliation.dataaccess.jpa;

import com.ccbsa.wms.reconciliation.dataaccess.entity.StockCountEntryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StockCountEntryJpaRepository extends JpaRepository<StockCountEntryJpaEntity, UUID> {

    List<StockCountEntryJpaEntity> findByStockCountId(UUID stockCountId);

    @Query("SELECT e FROM StockCountEntryJpaEntity e " +
           "WHERE e.stockCountId = :stockCountId " +
           "AND e.locationId = :locationId " +
           "AND e.productId = :productId")
    Optional<StockCountEntryJpaEntity> findByStockCountIdAndLocationIdAndProductId(
        @Param("stockCountId") UUID stockCountId,
        @Param("locationId") UUID locationId,
        @Param("productId") UUID productId
    );

    @Query("SELECT COUNT(e) FROM StockCountEntryJpaEntity e WHERE e.stockCountId = :stockCountId")
    int countByStockCountId(@Param("stockCountId") UUID stockCountId);
}
```

### Messaging Layer

#### Event Publisher

```java
// services/reconciliation-service/reconciliation-messaging/src/main/java/com/ccbsa/wms/reconciliation/messaging/publisher/StockCountEventPublisher.java

package com.ccbsa.wms.reconciliation.messaging.publisher;

import com.ccbsa.common.domain.event.DomainEvent;
import com.ccbsa.common.domain.event.DomainEventPublisher;
import com.ccbsa.wms.reconciliation.domain.core.event.StockCountEntryRecordedEvent;
import com.ccbsa.wms.reconciliation.messaging.mapper.StockCountEventMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockCountEventPublisher implements DomainEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final StockCountEventMapper eventMapper;

    @Value("${kafka.topics.stock-count-entry-recorded}")
    private String stockCountEntryRecordedTopic;

    @Override
    public void publish(DomainEvent event) {
        if (event instanceof StockCountEntryRecordedEvent) {
            publishStockCountEntryRecordedEvent((StockCountEntryRecordedEvent) event);
        }
        // Handle other event types...
    }

    private void publishStockCountEntryRecordedEvent(StockCountEntryRecordedEvent event) {
        log.info("Publishing StockCountEntryRecordedEvent: Entry ID {}, Stock Count ID {}",
            event.getEntryId().getValue(),
            event.getStockCountId().getValue());

        try {
            var kafkaEvent = eventMapper.toKafkaEvent(event);
            kafkaTemplate.send(stockCountEntryRecordedTopic, kafkaEvent);

            log.info("StockCountEntryRecordedEvent published successfully");
        } catch (Exception e) {
            log.error("Failed to publish StockCountEntryRecordedEvent", e);
            throw new RuntimeException("Event publishing failed", e);
        }
    }
}
```

### Exception Handling

```java
// services/reconciliation-service/reconciliation-application/src/main/java/com/ccbsa/wms/reconciliation/application/exception/GlobalExceptionHandler.java

package com.ccbsa.wms.reconciliation.application.exception;

import com.ccbsa.wms.reconciliation.domain.core.exception.DuplicateStockCountEntryException;
import com.ccbsa.wms.reconciliation.domain.core.exception.InvalidBarcodeException;
import com.ccbsa.wms.reconciliation.domain.core.exception.StockCountNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateStockCountEntryException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateStockCountEntry(
        DuplicateStockCountEntryException ex
    ) {
        log.warn("Duplicate stock count entry: {}", ex.getMessage());

        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", ZonedDateTime.now());
        error.put("status", HttpStatus.CONFLICT.value());
        error.put("error", "Duplicate Entry");
        error.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(InvalidBarcodeException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidBarcode(
        InvalidBarcodeException ex
    ) {
        log.warn("Invalid barcode: {}", ex.getMessage());

        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", ZonedDateTime.now());
        error.put("status", HttpStatus.BAD_REQUEST.value());
        error.put("error", "Invalid Barcode");
        error.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(StockCountNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleStockCountNotFound(
        StockCountNotFoundException ex
    ) {
        log.warn("Stock count not found: {}", ex.getMessage());

        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", ZonedDateTime.now());
        error.put("status", HttpStatus.NOT_FOUND.value());
        error.put("error", "Stock Count Not Found");
        error.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
}
```

---

