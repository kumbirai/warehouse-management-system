# Generate Electronic Stock Count Worksheet Implementation Plan

## US-8.1.1: Generate Electronic Stock Count Worksheet

**Service:** Reconciliation Service
**Priority:** Must Have
**Story Points:** 8
**Sprint:** Sprint 8

---

## Table of Contents

1. [Overview](#overview)
2. [UI Design](#ui-design)
3. [Domain Model Design](#domain-model-design)
4. [Backend Implementation](#backend-implementation)
5. [Frontend Implementation](#frontend-implementation)
6. [Data Flow](#data-flow)
7. [Testing Strategy](#testing-strategy)
8. [Acceptance Criteria Validation](#acceptance-criteria-validation)
9. [Implementation Checklist](#implementation-checklist)

---

## Overview

### User Story

**As a** warehouse manager
**I want** to generate electronic stock count worksheets
**So that** I can conduct cycle counts or full inventory counts without paper

### Business Requirements

- Generate electronic worksheets for stock counting (no paper required)
- Support both cycle counting (location/product subsets) and full inventory counts
- Specify count scope: by location, by product category, or warehouse-wide
- System auto-populates expected quantities from Stock Management Service
- System creates worksheet with entries for each location-product combination
- System assigns unique worksheet reference number
- System records initiator, timestamp, and count type
- Support resuming incomplete counts (worksheets persist in DRAFT status)
- Allow multiple concurrent stock counts (different locations/products)
- Prevent duplicate counts for same location-product combination

### Technical Requirements

- Follow DDD, Clean Hexagonal Architecture, CQRS, Event-Driven Choreography
- Pure Java domain core (no framework dependencies)
- Multi-tenant support with schema isolation
- Move common value objects to `common-domain` (DRY principle)
- Implement proper error handling and validation
- Integration with Stock Management Service for current stock levels
- Integration with Location Management Service for location data
- Integration with Product Service for product information

### Acceptance Criteria

1. **AC-1**: System generates electronic stock count worksheets (no paper)
2. **AC-2**: System supports cycle counting (subsets of locations/products)
3. **AC-3**: System supports full inventory counts (all locations and products)
4. **AC-4**: System auto-populates expected quantities from current stock levels
5. **AC-5**: System assigns unique worksheet reference number
6. **AC-6**: Worksheet includes location, product, expected quantity for each entry
7. **AC-7**: System saves worksheets in DRAFT status for later use
8. **AC-8**: System allows resuming incomplete counts

---

## UI Design

### Generate Stock Count Worksheet Page

**Component:** `GenerateStockCountWorksheetPage.tsx`
**Route:** `/reconciliation/stock-count/new`

**Features:**

- **Count Type Selection** - Choose between cycle count and full inventory
- **Scope Configuration** - Select locations, product categories, or warehouse-wide
- **Location Selector** - Multi-select locations for cycle counting
- **Product Filter** - Filter by category, SKU, or product attributes
- **Expected Quantity Preview** - Show estimated worksheet size and products
- **Validation** - Real-time validation of selections
- **Worksheet Generation** - Generate worksheet with progress indicator
- **Confirmation** - Display worksheet reference and next steps

### UI Flow

1. User navigates to "Generate Stock Count Worksheet" page
2. System displays count type selection:
   - **Cycle Count**: Subset of locations/products
   - **Full Inventory**: All active locations and products
3. User selects count type
4. If Cycle Count selected:
   - User selects scope type:
     - By Location (specific aisles, zones, or individual locations)
     - By Product Category (Beverages, Food, etc.)
     - By Product SKU Range (SKU-001 to SKU-100)
     - Custom (combination of filters)
   - User configures filters:
     - Location multi-select (with search/filter)
     - Product category multi-select
     - SKU range input
     - Include/exclude expired products
     - Include/exclude reserved stock
5. If Full Inventory selected:
   - User confirms warehouse-wide count
   - System displays warning about scope and impact
6. System displays preview:
   - Estimated number of worksheet entries
   - Locations included (count and list)
   - Products included (count and list)
   - Estimated duration (based on historical data)
7. User adds optional notes/comments
8. User clicks "Generate Worksheet"
9. System validates selections
10. System generates worksheet entries:
    - Query Stock Management Service for current quantities
    - Create worksheet entry for each location-product combination
    - Display progress indicator during generation
11. System displays confirmation:
    - Worksheet reference number
    - Count type and scope summary
    - Number of entries created
    - Worksheet status (DRAFT)
    - Option to start counting immediately or return to list

### Page Layout (TypeScript/React)

```typescript
import React, { useState, useEffect } from 'react';
import {
  Box,
  Paper,
  Typography,
  Grid,
  TextField,
  Button,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  Chip,
  Alert,
  AlertTitle,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  CircularProgress,
  LinearProgress,
  Autocomplete,
  Checkbox,
  FormControlLabel,
  Radio,
  RadioGroup,
  FormLabel,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Card,
  CardContent,
  Divider
} from '@mui/material';
import {
  Add as AddIcon,
  Inventory as InventoryIcon,
  CheckCircle as CheckCircleIcon,
  Warning as WarningIcon,
  Info as InfoIcon
} from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import { useGenerateStockCountWorksheet } from '../hooks/useGenerateStockCountWorksheet';
import { useLocations } from '../../location-management/hooks/useLocations';
import { useProductCategories } from '../../product/hooks/useProductCategories';
import { PageBreadcrumbs } from '../../common/PageBreadcrumbs';
import { StatusBadge } from '../../common/StatusBadge';
import type {
  CountType,
  ScopeType,
  GenerateWorksheetRequest,
  WorksheetPreview
} from '../types/stockCount';

export const GenerateStockCountWorksheetPage: React.FC = () => {
  const navigate = useNavigate();

  // State management
  const [countType, setCountType] = useState<CountType>('CYCLE_COUNT');
  const [scopeType, setScopeType] = useState<ScopeType>('BY_LOCATION');
  const [selectedLocations, setSelectedLocations] = useState<string[]>([]);
  const [selectedCategories, setSelectedCategories] = useState<string[]>([]);
  const [skuRangeStart, setSkuRangeStart] = useState<string>('');
  const [skuRangeEnd, setSkuRangeEnd] = useState<string>('');
  const [includeExpired, setIncludeExpired] = useState<boolean>(false);
  const [includeReserved, setIncludeReserved] = useState<boolean>(true);
  const [notes, setNotes] = useState<string>('');
  const [showPreview, setShowPreview] = useState<boolean>(false);
  const [showConfirmDialog, setShowConfirmDialog] = useState<boolean>(false);
  const [worksheetPreview, setWorksheetPreview] = useState<WorksheetPreview | null>(null);

  // Hooks
  const {
    generateWorksheet,
    isGenerating,
    generationProgress,
    worksheetReference,
    error,
    reset
  } = useGenerateStockCountWorksheet();

  const { locations, isLoading: locationsLoading } = useLocations();
  const { categories, isLoading: categoriesLoading } = useProductCategories();

  // Validation
  const isFormValid = (): boolean => {
    if (countType === 'FULL_INVENTORY') {
      return true;
    }

    if (scopeType === 'BY_LOCATION') {
      return selectedLocations.length > 0;
    }

    if (scopeType === 'BY_CATEGORY') {
      return selectedCategories.length > 0;
    }

    if (scopeType === 'BY_SKU_RANGE') {
      return skuRangeStart !== '' && skuRangeEnd !== '';
    }

    // Custom scope
    return selectedLocations.length > 0 ||
           selectedCategories.length > 0 ||
           (skuRangeStart !== '' && skuRangeEnd !== '');
  };

  // Handlers
  const handleCountTypeChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    setCountType(event.target.value as CountType);

    if (event.target.value === 'FULL_INVENTORY') {
      // Clear scope selections for full inventory
      setSelectedLocations([]);
      setSelectedCategories([]);
      setSkuRangeStart('');
      setSkuRangeEnd('');
    }
  };

  const handleGeneratePreview = async () => {
    const request: GenerateWorksheetRequest = {
      countType,
      scopeType: countType === 'FULL_INVENTORY' ? 'WAREHOUSE_WIDE' : scopeType,
      locationIds: selectedLocations,
      categoryIds: selectedCategories,
      skuRangeStart,
      skuRangeEnd,
      includeExpired,
      includeReserved,
      notes
    };

    // Call preview API (mock for now)
    const preview: WorksheetPreview = {
      estimatedEntries: 150,
      locationsCount: selectedLocations.length || 20,
      productsCount: 75,
      estimatedDuration: '2-3 hours'
    };

    setWorksheetPreview(preview);
    setShowPreview(true);
  };

  const handleConfirmGeneration = () => {
    setShowConfirmDialog(true);
  };

  const handleGenerate = async () => {
    const request: GenerateWorksheetRequest = {
      countType,
      scopeType: countType === 'FULL_INVENTORY' ? 'WAREHOUSE_WIDE' : scopeType,
      locationIds: selectedLocations,
      categoryIds: selectedCategories,
      skuRangeStart,
      skuRangeEnd,
      includeExpired,
      includeReserved,
      notes
    };

    await generateWorksheet(request);
    setShowConfirmDialog(false);
  };

  const handleStartCounting = () => {
    navigate(`/reconciliation/stock-count/${worksheetReference}/entry`);
  };

  const handleReturnToList = () => {
    navigate('/reconciliation/stock-count');
  };

  // Render success dialog
  if (worksheetReference) {
    return (
      <Box>
        <Paper sx={{ p: 4, textAlign: 'center', maxWidth: 600, mx: 'auto', mt: 8 }}>
          <CheckCircleIcon color="success" sx={{ fontSize: 80, mb: 2 }} />

          <Typography variant="h4" gutterBottom>
            Worksheet Generated Successfully
          </Typography>

          <Typography variant="body1" color="textSecondary" gutterBottom>
            Your stock count worksheet has been created and is ready for use.
          </Typography>

          <Box sx={{ mt: 3, p: 2, backgroundColor: 'grey.100', borderRadius: 1 }}>
            <Typography variant="body2" color="textSecondary">
              Worksheet Reference
            </Typography>
            <Typography variant="h5" fontWeight="bold" color="primary">
              {worksheetReference}
            </Typography>
          </Box>

          {worksheetPreview && (
            <Grid container spacing={2} sx={{ mt: 3 }}>
              <Grid item xs={6}>
                <Typography variant="body2" color="textSecondary">
                  Total Entries
                </Typography>
                <Typography variant="h6">
                  {worksheetPreview.estimatedEntries}
                </Typography>
              </Grid>
              <Grid item xs={6}>
                <Typography variant="body2" color="textSecondary">
                  Count Type
                </Typography>
                <Typography variant="h6">
                  {countType === 'CYCLE_COUNT' ? 'Cycle Count' : 'Full Inventory'}
                </Typography>
              </Grid>
              <Grid item xs={6}>
                <Typography variant="body2" color="textSecondary">
                  Locations
                </Typography>
                <Typography variant="h6">
                  {worksheetPreview.locationsCount}
                </Typography>
              </Grid>
              <Grid item xs={6}>
                <Typography variant="body2" color="textSecondary">
                  Products
                </Typography>
                <Typography variant="h6">
                  {worksheetPreview.productsCount}
                </Typography>
              </Grid>
            </Grid>
          )}

          <Box sx={{ mt: 4, display: 'flex', gap: 2, justifyContent: 'center' }}>
            <Button
              variant="outlined"
              onClick={handleReturnToList}
            >
              Return to List
            </Button>
            <Button
              variant="contained"
              onClick={handleStartCounting}
              startIcon={<InventoryIcon />}
            >
              Start Counting Now
            </Button>
          </Box>
        </Paper>
      </Box>
    );
  }

  return (
    <Box>
      <PageBreadcrumbs
        items={[
          { label: 'Reconciliation', href: '/reconciliation' },
          { label: 'Stock Counts', href: '/reconciliation/stock-count' },
          { label: 'Generate Worksheet' }
        ]}
      />

      <Typography variant="h4" gutterBottom>
        Generate Stock Count Worksheet
      </Typography>

      <Typography variant="body1" color="textSecondary" paragraph>
        Create an electronic worksheet for conducting stock counts. Choose between
        cycle counting specific areas or a full warehouse inventory.
      </Typography>

      {error && (
        <Alert severity="error" sx={{ mb: 3 }} onClose={reset}>
          <AlertTitle>Error Generating Worksheet</AlertTitle>
          {error}
        </Alert>
      )}

      {/* Count Type Selection */}
      <Paper sx={{ p: 3, mb: 3 }}>
        <Typography variant="h6" gutterBottom>
          Step 1: Select Count Type
        </Typography>

        <FormControl component="fieldset">
          <FormLabel component="legend">Count Type</FormLabel>
          <RadioGroup
            value={countType}
            onChange={handleCountTypeChange}
          >
            <FormControlLabel
              value="CYCLE_COUNT"
              control={<Radio />}
              label={
                <Box>
                  <Typography variant="body1" fontWeight="bold">
                    Cycle Count
                  </Typography>
                  <Typography variant="body2" color="textSecondary">
                    Count specific locations, products, or categories
                  </Typography>
                </Box>
              }
            />
            <FormControlLabel
              value="FULL_INVENTORY"
              control={<Radio />}
              label={
                <Box>
                  <Typography variant="body1" fontWeight="bold">
                    Full Inventory Count
                  </Typography>
                  <Typography variant="body2" color="textSecondary">
                    Count all products in all locations (warehouse-wide)
                  </Typography>
                </Box>
              }
            />
          </RadioGroup>
        </FormControl>

        {countType === 'FULL_INVENTORY' && (
          <Alert severity="warning" sx={{ mt: 2 }}>
            <AlertTitle>Full Inventory Count</AlertTitle>
            This will create a worksheet for all active locations and products in
            the warehouse. This operation may take several minutes and create a
            large number of entries.
          </Alert>
        )}
      </Paper>

      {/* Scope Configuration (Cycle Count Only) */}
      {countType === 'CYCLE_COUNT' && (
        <Paper sx={{ p: 3, mb: 3 }}>
          <Typography variant="h6" gutterBottom>
            Step 2: Configure Scope
          </Typography>

          <FormControl fullWidth sx={{ mb: 3 }}>
            <InputLabel>Scope Type</InputLabel>
            <Select
              value={scopeType}
              onChange={(e) => setScopeType(e.target.value as ScopeType)}
              label="Scope Type"
            >
              <MenuItem value="BY_LOCATION">By Location</MenuItem>
              <MenuItem value="BY_CATEGORY">By Product Category</MenuItem>
              <MenuItem value="BY_SKU_RANGE">By SKU Range</MenuItem>
              <MenuItem value="CUSTOM">Custom (Multiple Filters)</MenuItem>
            </Select>
          </FormControl>

          {/* Location Selection */}
          {(scopeType === 'BY_LOCATION' || scopeType === 'CUSTOM') && (
            <Box sx={{ mb: 3 }}>
              <Autocomplete
                multiple
                options={locations || []}
                getOptionLabel={(option) => `${option.code} - ${option.description}`}
                value={locations?.filter(loc => selectedLocations.includes(loc.id)) || []}
                onChange={(_, newValue) => {
                  setSelectedLocations(newValue.map(loc => loc.id));
                }}
                renderInput={(params) => (
                  <TextField
                    {...params}
                    label="Select Locations"
                    placeholder="Search locations..."
                  />
                )}
                renderTags={(value, getTagProps) =>
                  value.map((option, index) => (
                    <Chip
                      label={option.code}
                      {...getTagProps({ index })}
                      size="small"
                    />
                  ))
                }
                loading={locationsLoading}
              />
              <Typography variant="caption" color="textSecondary" sx={{ mt: 1, display: 'block' }}>
                {selectedLocations.length} location(s) selected
              </Typography>
            </Box>
          )}

          {/* Category Selection */}
          {(scopeType === 'BY_CATEGORY' || scopeType === 'CUSTOM') && (
            <Box sx={{ mb: 3 }}>
              <Autocomplete
                multiple
                options={categories || []}
                getOptionLabel={(option) => option.name}
                value={categories?.filter(cat => selectedCategories.includes(cat.id)) || []}
                onChange={(_, newValue) => {
                  setSelectedCategories(newValue.map(cat => cat.id));
                }}
                renderInput={(params) => (
                  <TextField
                    {...params}
                    label="Select Product Categories"
                    placeholder="Search categories..."
                  />
                )}
                renderTags={(value, getTagProps) =>
                  value.map((option, index) => (
                    <Chip
                      label={option.name}
                      {...getTagProps({ index })}
                      size="small"
                    />
                  ))
                }
                loading={categoriesLoading}
              />
              <Typography variant="caption" color="textSecondary" sx={{ mt: 1, display: 'block' }}>
                {selectedCategories.length} category(ies) selected
              </Typography>
            </Box>
          )}

          {/* SKU Range */}
          {(scopeType === 'BY_SKU_RANGE' || scopeType === 'CUSTOM') && (
            <Box sx={{ mb: 3 }}>
              <Grid container spacing={2}>
                <Grid item xs={12} md={6}>
                  <TextField
                    fullWidth
                    label="SKU Range Start"
                    value={skuRangeStart}
                    onChange={(e) => setSkuRangeStart(e.target.value)}
                    placeholder="e.g., SKU-001"
                  />
                </Grid>
                <Grid item xs={12} md={6}>
                  <TextField
                    fullWidth
                    label="SKU Range End"
                    value={skuRangeEnd}
                    onChange={(e) => setSkuRangeEnd(e.target.value)}
                    placeholder="e.g., SKU-100"
                  />
                </Grid>
              </Grid>
            </Box>
          )}

          <Divider sx={{ my: 3 }} />

          {/* Additional Options */}
          <Typography variant="subtitle1" gutterBottom>
            Additional Options
          </Typography>

          <FormControlLabel
            control={
              <Checkbox
                checked={includeExpired}
                onChange={(e) => setIncludeExpired(e.target.checked)}
              />
            }
            label="Include expired products"
          />

          <FormControlLabel
            control={
              <Checkbox
                checked={includeReserved}
                onChange={(e) => setIncludeReserved(e.target.checked)}
              />
            }
            label="Include reserved stock"
          />
        </Paper>
      )}

      {/* Notes */}
      <Paper sx={{ p: 3, mb: 3 }}>
        <Typography variant="h6" gutterBottom>
          Step {countType === 'CYCLE_COUNT' ? '3' : '2'}: Notes (Optional)
        </Typography>

        <TextField
          fullWidth
          multiline
          rows={3}
          label="Notes"
          value={notes}
          onChange={(e) => setNotes(e.target.value)}
          placeholder="Add any notes or comments about this stock count..."
        />
      </Paper>

      {/* Preview Section */}
      {showPreview && worksheetPreview && (
        <Paper sx={{ p: 3, mb: 3 }}>
          <Typography variant="h6" gutterBottom>
            Worksheet Preview
          </Typography>

          <Grid container spacing={3}>
            <Grid item xs={12} md={3}>
              <Card variant="outlined">
                <CardContent>
                  <Typography variant="body2" color="textSecondary">
                    Estimated Entries
                  </Typography>
                  <Typography variant="h5" color="primary">
                    {worksheetPreview.estimatedEntries}
                  </Typography>
                </CardContent>
              </Card>
            </Grid>
            <Grid item xs={12} md={3}>
              <Card variant="outlined">
                <CardContent>
                  <Typography variant="body2" color="textSecondary">
                    Locations
                  </Typography>
                  <Typography variant="h5">
                    {worksheetPreview.locationsCount}
                  </Typography>
                </CardContent>
              </Card>
            </Grid>
            <Grid item xs={12} md={3}>
              <Card variant="outlined">
                <CardContent>
                  <Typography variant="body2" color="textSecondary">
                    Products
                  </Typography>
                  <Typography variant="h5">
                    {worksheetPreview.productsCount}
                  </Typography>
                </CardContent>
              </Card>
            </Grid>
            <Grid item xs={12} md={3}>
              <Card variant="outlined">
                <CardContent>
                  <Typography variant="body2" color="textSecondary">
                    Est. Duration
                  </Typography>
                  <Typography variant="h5">
                    {worksheetPreview.estimatedDuration}
                  </Typography>
                </CardContent>
              </Card>
            </Grid>
          </Grid>

          <Alert severity="info" sx={{ mt: 2 }}>
            <AlertTitle>Preview Information</AlertTitle>
            This is an estimate based on current stock levels. Actual worksheet
            entries will be generated when you confirm.
          </Alert>
        </Paper>
      )}

      {/* Generation Progress */}
      {isGenerating && (
        <Paper sx={{ p: 3, mb: 3 }}>
          <Typography variant="h6" gutterBottom>
            Generating Worksheet...
          </Typography>

          <LinearProgress
            variant="determinate"
            value={generationProgress}
            sx={{ mb: 2 }}
          />

          <Typography variant="body2" color="textSecondary" align="center">
            {generationProgress}% complete
          </Typography>
        </Paper>
      )}

      {/* Action Buttons */}
      <Box sx={{ display: 'flex', gap: 2, justifyContent: 'flex-end' }}>
        <Button
          variant="outlined"
          onClick={() => navigate('/reconciliation/stock-count')}
          disabled={isGenerating}
        >
          Cancel
        </Button>

        {!showPreview && (
          <Button
            variant="outlined"
            onClick={handleGeneratePreview}
            disabled={!isFormValid() || isGenerating}
            startIcon={<InfoIcon />}
          >
            Preview Worksheet
          </Button>
        )}

        <Button
          variant="contained"
          onClick={handleConfirmGeneration}
          disabled={!isFormValid() || isGenerating}
          startIcon={isGenerating ? <CircularProgress size={20} /> : <AddIcon />}
        >
          {isGenerating ? 'Generating...' : 'Generate Worksheet'}
        </Button>
      </Box>

      {/* Confirmation Dialog */}
      <Dialog
        open={showConfirmDialog}
        onClose={() => setShowConfirmDialog(false)}
      >
        <DialogTitle>Confirm Worksheet Generation</DialogTitle>
        <DialogContent>
          <Typography variant="body1" gutterBottom>
            You are about to generate a stock count worksheet with the following settings:
          </Typography>

          <Box sx={{ mt: 2, p: 2, backgroundColor: 'grey.100', borderRadius: 1 }}>
            <Typography variant="body2">
              <strong>Count Type:</strong>{' '}
              {countType === 'CYCLE_COUNT' ? 'Cycle Count' : 'Full Inventory'}
            </Typography>

            {worksheetPreview && (
              <>
                <Typography variant="body2">
                  <strong>Estimated Entries:</strong> {worksheetPreview.estimatedEntries}
                </Typography>
                <Typography variant="body2">
                  <strong>Locations:</strong> {worksheetPreview.locationsCount}
                </Typography>
                <Typography variant="body2">
                  <strong>Products:</strong> {worksheetPreview.productsCount}
                </Typography>
              </>
            )}
          </Box>

          <Alert severity="info" sx={{ mt: 2 }}>
            The worksheet will be saved in DRAFT status and can be edited or
            resumed at any time.
          </Alert>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setShowConfirmDialog(false)}>
            Cancel
          </Button>
          <Button
            onClick={handleGenerate}
            variant="contained"
            autoFocus
          >
            Confirm
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};
```

### TypeScript Interfaces

```typescript
// types/stockCount.ts

export type CountType = 'CYCLE_COUNT' | 'FULL_INVENTORY';

export type ScopeType =
  | 'BY_LOCATION'
  | 'BY_CATEGORY'
  | 'BY_SKU_RANGE'
  | 'CUSTOM'
  | 'WAREHOUSE_WIDE';

export type StockCountStatus =
  | 'DRAFT'
  | 'IN_PROGRESS'
  | 'COMPLETED'
  | 'CANCELLED';

export interface GenerateWorksheetRequest {
  countType: CountType;
  scopeType: ScopeType;
  locationIds?: string[];
  categoryIds?: string[];
  skuRangeStart?: string;
  skuRangeEnd?: string;
  includeExpired: boolean;
  includeReserved: boolean;
  notes?: string;
}

export interface GenerateWorksheetResponse {
  stockCountId: string;
  worksheetReference: string;
  countType: CountType;
  status: StockCountStatus;
  entriesCount: number;
  initiatedBy: string;
  initiatedAt: string;
}

export interface WorksheetPreview {
  estimatedEntries: number;
  locationsCount: number;
  productsCount: number;
  estimatedDuration: string;
}

export interface StockCountWorksheet {
  id: string;
  reference: string;
  countType: CountType;
  status: StockCountStatus;
  scopeType: ScopeType;
  initiatedBy: string;
  initiatedAt: string;
  completedAt?: string;
  notes?: string;
  entriesCount: number;
  entries: StockCountEntry[];
}

export interface StockCountEntry {
  id: string;
  stockCountId: string;
  locationId: string;
  locationCode: string;
  productId: string;
  productCode: string;
  productDescription: string;
  expectedQuantity: number;
  countedQuantity?: number;
  variance?: number;
  recordedBy?: string;
  recordedAt?: string;
  notes?: string;
}

export interface Location {
  id: string;
  code: string;
  description: string;
  type: string;
  zone?: string;
  aisle?: string;
}

export interface ProductCategory {
  id: string;
  code: string;
  name: string;
  description?: string;
}
```

---

## Domain Model Design

### Aggregate Root: StockCount

The `StockCount` aggregate is responsible for managing the lifecycle of a stock counting operation.

**File:** `services/reconciliation-service/reconciliation-domain/reconciliation-domain-core/src/main/java/com/ccbsa/wms/reconciliation/domain/core/entity/StockCount.java`

```java
package com.ccbsa.wms.reconciliation.domain.core.entity;

import com.ccbsa.common.domain.entity.AggregateRoot;
import com.ccbsa.common.domain.valueobject.*;
import com.ccbsa.wms.reconciliation.domain.core.valueobject.*;
import com.ccbsa.wms.reconciliation.domain.core.event.*;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class StockCount extends AggregateRoot<StockCountId> {
    private final TenantId tenantId;
    private final CountType countType;
    private final ScopeConfiguration scopeConfiguration;
    private StockCountStatus status;
    private final UserId initiatedBy;
    private final ZonedDateTime initiatedAt;
    private ZonedDateTime completedAt;
    private Notes notes;
    private final List<StockCountEntry> entries;

    private StockCount(Builder builder) {
        super.setId(builder.stockCountId);
        this.tenantId = builder.tenantId;
        this.countType = builder.countType;
        this.scopeConfiguration = builder.scopeConfiguration;
        this.status = builder.status;
        this.initiatedBy = builder.initiatedBy;
        this.initiatedAt = builder.initiatedAt;
        this.completedAt = builder.completedAt;
        this.notes = builder.notes;
        this.entries = builder.entries;
    }

    /**
     * Initialize a new stock count with worksheet entries
     */
    public void initializeWorksheet(List<WorksheetEntry> worksheetEntries) {
        validateState();
        validateWorksheetEntries(worksheetEntries);

        // Create stock count entries from worksheet entries
        worksheetEntries.forEach(worksheetEntry -> {
            StockCountEntry entry = StockCountEntry.builder()
                .id(new StockCountEntryId(UUID.randomUUID()))
                .stockCountId(getId())
                .locationId(worksheetEntry.getLocationId())
                .productId(worksheetEntry.getProductId())
                .expectedQuantity(worksheetEntry.getExpectedQuantity())
                .build();

            entries.add(entry);
        });

        // Register domain event
        registerEvent(new StockCountInitiatedEvent(
            getId(),
            tenantId,
            countType,
            scopeConfiguration,
            initiatedBy,
            initiatedAt,
            entries.size()
        ));
    }

    /**
     * Validate stock count is in correct state for initialization
     */
    private void validateState() {
        if (status != StockCountStatus.DRAFT) {
            throw new IllegalStateException(
                "Stock count must be in DRAFT status to initialize worksheet"
            );
        }
    }

    /**
     * Validate worksheet entries before adding
     */
    private void validateWorksheetEntries(List<WorksheetEntry> worksheetEntries) {
        if (worksheetEntries == null || worksheetEntries.isEmpty()) {
            throw new IllegalArgumentException("Worksheet entries cannot be empty");
        }

        // Check for duplicate location-product combinations
        long uniqueCombinations = worksheetEntries.stream()
            .map(we -> we.getLocationId().getValue() + "-" + we.getProductId().getValue())
            .distinct()
            .count();

        if (uniqueCombinations != worksheetEntries.size()) {
            throw new IllegalArgumentException(
                "Worksheet contains duplicate location-product combinations"
            );
        }
    }

    /**
     * Mark stock count as in progress
     */
    public void startCounting() {
        if (status != StockCountStatus.DRAFT) {
            throw new IllegalStateException("Can only start counting from DRAFT status");
        }

        this.status = StockCountStatus.IN_PROGRESS;

        registerEvent(new StockCountStartedEvent(
            getId(),
            tenantId,
            ZonedDateTime.now()
        ));
    }

    /**
     * Cancel stock count
     */
    public void cancel() {
        if (status == StockCountStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel completed stock count");
        }

        this.status = StockCountStatus.CANCELLED;

        registerEvent(new StockCountCancelledEvent(
            getId(),
            tenantId,
            ZonedDateTime.now()
        ));
    }

    // Getters
    public TenantId getTenantId() { return tenantId; }
    public CountType getCountType() { return countType; }
    public ScopeConfiguration getScopeConfiguration() { return scopeConfiguration; }
    public StockCountStatus getStatus() { return status; }
    public UserId getInitiatedBy() { return initiatedBy; }
    public ZonedDateTime getInitiatedAt() { return initiatedAt; }
    public ZonedDateTime getCompletedAt() { return completedAt; }
    public Notes getNotes() { return notes; }
    public List<StockCountEntry> getEntries() { return new ArrayList<>(entries); }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private StockCountId stockCountId;
        private TenantId tenantId;
        private CountType countType;
        private ScopeConfiguration scopeConfiguration;
        private StockCountStatus status;
        private UserId initiatedBy;
        private ZonedDateTime initiatedAt;
        private ZonedDateTime completedAt;
        private Notes notes;
        private List<StockCountEntry> entries = new ArrayList<>();

        public Builder id(StockCountId stockCountId) {
            this.stockCountId = stockCountId;
            return this;
        }

        public Builder tenantId(TenantId tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder countType(CountType countType) {
            this.countType = countType;
            return this;
        }

        public Builder scopeConfiguration(ScopeConfiguration scopeConfiguration) {
            this.scopeConfiguration = scopeConfiguration;
            return this;
        }

        public Builder status(StockCountStatus status) {
            this.status = status;
            return this;
        }

        public Builder initiatedBy(UserId initiatedBy) {
            this.initiatedBy = initiatedBy;
            return this;
        }

        public Builder initiatedAt(ZonedDateTime initiatedAt) {
            this.initiatedAt = initiatedAt;
            return this;
        }

        public Builder completedAt(ZonedDateTime completedAt) {
            this.completedAt = completedAt;
            return this;
        }

        public Builder notes(Notes notes) {
            this.notes = notes;
            return this;
        }

        public Builder entries(List<StockCountEntry> entries) {
            this.entries = entries;
            return this;
        }

        public StockCount build() {
            return new StockCount(this);
        }
    }
}
```

### Entity: StockCountEntry

**File:** `services/reconciliation-service/reconciliation-domain/reconciliation-domain-core/src/main/java/com/ccbsa/wms/reconciliation/domain/core/entity/StockCountEntry.java`

```java
package com.ccbsa.wms.reconciliation.domain.core.entity;

import com.ccbsa.common.domain.entity.BaseEntity;
import com.ccbsa.common.domain.valueobject.*;
import com.ccbsa.wms.reconciliation.domain.core.valueobject.*;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

public class StockCountEntry extends BaseEntity<StockCountEntryId> {
    private final StockCountId stockCountId;
    private final LocationId locationId;
    private final ProductId productId;
    private final Quantity expectedQuantity;
    private Quantity countedQuantity;
    private UserId recordedBy;
    private ZonedDateTime recordedAt;
    private Notes notes;

    private StockCountEntry(Builder builder) {
        super.setId(builder.id);
        this.stockCountId = builder.stockCountId;
        this.locationId = builder.locationId;
        this.productId = builder.productId;
        this.expectedQuantity = builder.expectedQuantity;
        this.countedQuantity = builder.countedQuantity;
        this.recordedBy = builder.recordedBy;
        this.recordedAt = builder.recordedAt;
        this.notes = builder.notes;
    }

    /**
     * Record counted quantity for this entry
     */
    public void recordCount(Quantity quantity, UserId userId) {
        validateQuantity(quantity);

        this.countedQuantity = quantity;
        this.recordedBy = userId;
        this.recordedAt = ZonedDateTime.now();
    }

    /**
     * Update notes for this entry
     */
    public void updateNotes(Notes notes) {
        this.notes = notes;
    }

    /**
     * Calculate variance between expected and counted quantities
     */
    public BigDecimal calculateVariance() {
        if (countedQuantity == null) {
            return null;
        }

        return countedQuantity.getValue().subtract(expectedQuantity.getValue());
    }

    /**
     * Check if this entry has a variance
     */
    public boolean hasVariance() {
        BigDecimal variance = calculateVariance();
        return variance != null && variance.compareTo(BigDecimal.ZERO) != 0;
    }

    /**
     * Validate quantity is non-negative
     */
    private void validateQuantity(Quantity quantity) {
        if (quantity.getValue().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Counted quantity cannot be negative");
        }
    }

    // Getters
    public StockCountId getStockCountId() { return stockCountId; }
    public LocationId getLocationId() { return locationId; }
    public ProductId getProductId() { return productId; }
    public Quantity getExpectedQuantity() { return expectedQuantity; }
    public Quantity getCountedQuantity() { return countedQuantity; }
    public UserId getRecordedBy() { return recordedBy; }
    public ZonedDateTime getRecordedAt() { return recordedAt; }
    public Notes getNotes() { return notes; }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private StockCountEntryId id;
        private StockCountId stockCountId;
        private LocationId locationId;
        private ProductId productId;
        private Quantity expectedQuantity;
        private Quantity countedQuantity;
        private UserId recordedBy;
        private ZonedDateTime recordedAt;
        private Notes notes;

        public Builder id(StockCountEntryId id) {
            this.id = id;
            return this;
        }

        public Builder stockCountId(StockCountId stockCountId) {
            this.stockCountId = stockCountId;
            return this;
        }

        public Builder locationId(LocationId locationId) {
            this.locationId = locationId;
            return this;
        }

        public Builder productId(ProductId productId) {
            this.productId = productId;
            return this;
        }

        public Builder expectedQuantity(Quantity expectedQuantity) {
            this.expectedQuantity = expectedQuantity;
            return this;
        }

        public Builder countedQuantity(Quantity countedQuantity) {
            this.countedQuantity = countedQuantity;
            return this;
        }

        public Builder recordedBy(UserId recordedBy) {
            this.recordedBy = recordedBy;
            return this;
        }

        public Builder recordedAt(ZonedDateTime recordedAt) {
            this.recordedAt = recordedAt;
            return this;
        }

        public Builder notes(Notes notes) {
            this.notes = notes;
            return this;
        }

        public StockCountEntry build() {
            return new StockCountEntry(this);
        }
    }
}
```

### Value Objects

#### CountType (Enum)

**File:** `services/reconciliation-service/reconciliation-domain/reconciliation-domain-core/src/main/java/com/ccbsa/wms/reconciliation/domain/core/valueobject/CountType.java`

```java
package com.ccbsa.wms.reconciliation.domain.core.valueobject;

public enum CountType {
    CYCLE_COUNT("Cycle Count - subset of locations/products"),
    FULL_INVENTORY("Full Inventory - all locations and products");

    private final String description;

    CountType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
```

#### ScopeConfiguration (Value Object)

**File:** `services/reconciliation-service/reconciliation-domain/reconciliation-domain-core/src/main/java/com/ccbsa/wms/reconciliation/domain/core/valueobject/ScopeConfiguration.java`

```java
package com.ccbsa.wms.reconciliation.domain.core.valueobject;

import com.ccbsa.common.domain.valueobject.LocationId;
import com.ccbsa.common.domain.valueobject.ProductCategoryId;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ScopeConfiguration {
    private final ScopeType scopeType;
    private final List<LocationId> locationIds;
    private final List<ProductCategoryId> categoryIds;
    private final String skuRangeStart;
    private final String skuRangeEnd;
    private final boolean includeExpired;
    private final boolean includeReserved;

    public ScopeConfiguration(
        ScopeType scopeType,
        List<LocationId> locationIds,
        List<ProductCategoryId> categoryIds,
        String skuRangeStart,
        String skuRangeEnd,
        boolean includeExpired,
        boolean includeReserved
    ) {
        this.scopeType = scopeType;
        this.locationIds = locationIds != null ? List.copyOf(locationIds) : Collections.emptyList();
        this.categoryIds = categoryIds != null ? List.copyOf(categoryIds) : Collections.emptyList();
        this.skuRangeStart = skuRangeStart;
        this.skuRangeEnd = skuRangeEnd;
        this.includeExpired = includeExpired;
        this.includeReserved = includeReserved;

        validate();
    }

    private void validate() {
        if (scopeType == null) {
            throw new IllegalArgumentException("Scope type cannot be null");
        }

        // Validate scope type matches provided filters
        switch (scopeType) {
            case BY_LOCATION:
                if (locationIds.isEmpty()) {
                    throw new IllegalArgumentException(
                        "Location IDs required for BY_LOCATION scope"
                    );
                }
                break;
            case BY_CATEGORY:
                if (categoryIds.isEmpty()) {
                    throw new IllegalArgumentException(
                        "Category IDs required for BY_CATEGORY scope"
                    );
                }
                break;
            case BY_SKU_RANGE:
                if (skuRangeStart == null || skuRangeEnd == null) {
                    throw new IllegalArgumentException(
                        "SKU range required for BY_SKU_RANGE scope"
                    );
                }
                break;
            case WAREHOUSE_WIDE:
                // No additional validation needed
                break;
            case CUSTOM:
                // At least one filter must be provided
                if (locationIds.isEmpty() && categoryIds.isEmpty() &&
                    (skuRangeStart == null || skuRangeEnd == null)) {
                    throw new IllegalArgumentException(
                        "At least one filter required for CUSTOM scope"
                    );
                }
                break;
        }
    }

    // Getters
    public ScopeType getScopeType() { return scopeType; }
    public List<LocationId> getLocationIds() { return locationIds; }
    public List<ProductCategoryId> getCategoryIds() { return categoryIds; }
    public String getSkuRangeStart() { return skuRangeStart; }
    public String getSkuRangeEnd() { return skuRangeEnd; }
    public boolean isIncludeExpired() { return includeExpired; }
    public boolean isIncludeReserved() { return includeReserved; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScopeConfiguration that = (ScopeConfiguration) o;
        return includeExpired == that.includeExpired &&
               includeReserved == that.includeReserved &&
               scopeType == that.scopeType &&
               Objects.equals(locationIds, that.locationIds) &&
               Objects.equals(categoryIds, that.categoryIds) &&
               Objects.equals(skuRangeStart, that.skuRangeStart) &&
               Objects.equals(skuRangeEnd, that.skuRangeEnd);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scopeType, locationIds, categoryIds,
                          skuRangeStart, skuRangeEnd, includeExpired, includeReserved);
    }
}
```

#### ScopeType (Enum)

**File:** `services/reconciliation-service/reconciliation-domain/reconciliation-domain-core/src/main/java/com/ccbsa/wms/reconciliation/domain/core/valueobject/ScopeType.java`

```java
package com.ccbsa.wms.reconciliation.domain.core.valueobject;

public enum ScopeType {
    BY_LOCATION,
    BY_CATEGORY,
    BY_SKU_RANGE,
    CUSTOM,
    WAREHOUSE_WIDE
}
```

#### WorksheetEntry (Value Object)

**File:** `services/reconciliation-service/reconciliation-domain/reconciliation-domain-core/src/main/java/com/ccbsa/wms/reconciliation/domain/core/valueobject/WorksheetEntry.java`

```java
package com.ccbsa.wms.reconciliation.domain.core.valueobject;

import com.ccbsa.common.domain.valueobject.LocationId;
import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.Quantity;

import java.util.Objects;

/**
 * Represents a single entry to be included in a stock count worksheet
 */
public class WorksheetEntry {
    private final LocationId locationId;
    private final ProductId productId;
    private final Quantity expectedQuantity;

    public WorksheetEntry(
        LocationId locationId,
        ProductId productId,
        Quantity expectedQuantity
    ) {
        this.locationId = Objects.requireNonNull(locationId, "Location ID cannot be null");
        this.productId = Objects.requireNonNull(productId, "Product ID cannot be null");
        this.expectedQuantity = Objects.requireNonNull(
            expectedQuantity,
            "Expected quantity cannot be null"
        );

        if (expectedQuantity.getValue().signum() < 0) {
            throw new IllegalArgumentException("Expected quantity cannot be negative");
        }
    }

    public LocationId getLocationId() { return locationId; }
    public ProductId getProductId() { return productId; }
    public Quantity getExpectedQuantity() { return expectedQuantity; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorksheetEntry that = (WorksheetEntry) o;
        return Objects.equals(locationId, that.locationId) &&
               Objects.equals(productId, that.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(locationId, productId);
    }
}
```

### Common Domain Value Objects

These value objects should be created in the `common-domain` module for reuse across services:

#### StockCountId

**File:** `common/common-domain/src/main/java/com/ccbsa/common/domain/valueobject/StockCountId.java`

```java
package com.ccbsa.common.domain.valueobject;

import java.util.UUID;

public class StockCountId extends BaseId<UUID> {
    public StockCountId(UUID value) {
        super(value);
    }
}
```

#### StockCountEntryId

**File:** `common/common-domain/src/main/java/com/ccbsa/common/domain/valueobject/StockCountEntryId.java`

```java
package com.ccbsa.common.domain.valueobject;

import java.util.UUID;

public class StockCountEntryId extends BaseId<UUID> {
    public StockCountEntryId(UUID value) {
        super(value);
    }
}
```

#### StockCountStatus (Enum)

**File:** `common/common-domain/src/main/java/com/ccbsa/common/domain/valueobject/StockCountStatus.java`

```java
package com.ccbsa.common.domain.valueobject;

public enum StockCountStatus {
    DRAFT("Worksheet created, not yet started"),
    IN_PROGRESS("Counting in progress"),
    COMPLETED("Counting completed"),
    CANCELLED("Stock count cancelled");

    private final String description;

    StockCountStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
```

### Domain Events

#### StockCountInitiatedEvent

**File:** `services/reconciliation-service/reconciliation-domain/reconciliation-domain-core/src/main/java/com/ccbsa/wms/reconciliation/domain/core/event/StockCountInitiatedEvent.java`

```java
package com.ccbsa.wms.reconciliation.domain.core.event;

import com.ccbsa.common.domain.event.DomainEvent;
import com.ccbsa.common.domain.valueobject.*;
import com.ccbsa.wms.reconciliation.domain.core.valueobject.*;

import java.time.ZonedDateTime;

public class StockCountInitiatedEvent implements DomainEvent<StockCountId> {
    private final StockCountId stockCountId;
    private final TenantId tenantId;
    private final CountType countType;
    private final ScopeConfiguration scopeConfiguration;
    private final UserId initiatedBy;
    private final ZonedDateTime initiatedAt;
    private final int entriesCount;

    public StockCountInitiatedEvent(
        StockCountId stockCountId,
        TenantId tenantId,
        CountType countType,
        ScopeConfiguration scopeConfiguration,
        UserId initiatedBy,
        ZonedDateTime initiatedAt,
        int entriesCount
    ) {
        this.stockCountId = stockCountId;
        this.tenantId = tenantId;
        this.countType = countType;
        this.scopeConfiguration = scopeConfiguration;
        this.initiatedBy = initiatedBy;
        this.initiatedAt = initiatedAt;
        this.entriesCount = entriesCount;
    }

    @Override
    public StockCountId getAggregateId() {
        return stockCountId;
    }

    @Override
    public ZonedDateTime getOccurredOn() {
        return initiatedAt;
    }

    // Getters
    public StockCountId getStockCountId() { return stockCountId; }
    public TenantId getTenantId() { return tenantId; }
    public CountType getCountType() { return countType; }
    public ScopeConfiguration getScopeConfiguration() { return scopeConfiguration; }
    public UserId getInitiatedBy() { return initiatedBy; }
    public ZonedDateTime getInitiatedAt() { return initiatedAt; }
    public int getEntriesCount() { return entriesCount; }
}
```

#### StockCountStartedEvent

**File:** `services/reconciliation-service/reconciliation-domain/reconciliation-domain-core/src/main/java/com/ccbsa/wms/reconciliation/domain/core/event/StockCountStartedEvent.java`

```java
package com.ccbsa.wms.reconciliation.domain.core.event;

import com.ccbsa.common.domain.event.DomainEvent;
import com.ccbsa.common.domain.valueobject.*;

import java.time.ZonedDateTime;

public class StockCountStartedEvent implements DomainEvent<StockCountId> {
    private final StockCountId stockCountId;
    private final TenantId tenantId;
    private final ZonedDateTime startedAt;

    public StockCountStartedEvent(
        StockCountId stockCountId,
        TenantId tenantId,
        ZonedDateTime startedAt
    ) {
        this.stockCountId = stockCountId;
        this.tenantId = tenantId;
        this.startedAt = startedAt;
    }

    @Override
    public StockCountId getAggregateId() {
        return stockCountId;
    }

    @Override
    public ZonedDateTime getOccurredOn() {
        return startedAt;
    }

    // Getters
    public StockCountId getStockCountId() { return stockCountId; }
    public TenantId getTenantId() { return tenantId; }
    public ZonedDateTime getStartedAt() { return startedAt; }
}
```

#### StockCountCancelledEvent

**File:** `services/reconciliation-service/reconciliation-domain/reconciliation-domain-core/src/main/java/com/ccbsa/wms/reconciliation/domain/core/event/StockCountCancelledEvent.java`

```java
package com.ccbsa.wms.reconciliation.domain.core.event;

import com.ccbsa.common.domain.event.DomainEvent;
import com.ccbsa.common.domain.valueobject.*;

import java.time.ZonedDateTime;

public class StockCountCancelledEvent implements DomainEvent<StockCountId> {
    private final StockCountId stockCountId;
    private final TenantId tenantId;
    private final ZonedDateTime cancelledAt;

    public StockCountCancelledEvent(
        StockCountId stockCountId,
        TenantId tenantId,
        ZonedDateTime cancelledAt
    ) {
        this.stockCountId = stockCountId;
        this.tenantId = tenantId;
        this.cancelledAt = cancelledAt;
    }

    @Override
    public StockCountId getAggregateId() {
        return stockCountId;
    }

    @Override
    public ZonedDateTime getOccurredOn() {
        return cancelledAt;
    }

    // Getters
    public StockCountId getStockCountId() { return stockCountId; }
    public TenantId getTenantId() { return tenantId; }
    public ZonedDateTime getCancelledAt() { return cancelledAt; }
}
```

---

## Backend Implementation

### Command: GenerateStockCountWorksheetCommand

**File:** `services/reconciliation-service/reconciliation-domain/reconciliation-application-service/src/main/java/com/ccbsa/wms/reconciliation/application/service/command/dto/GenerateStockCountWorksheetCommand.java`

```java
package com.ccbsa.wms.reconciliation.application.service.command.dto;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.UserId;
import com.ccbsa.wms.reconciliation.domain.core.valueobject.CountType;
import com.ccbsa.wms.reconciliation.domain.core.valueobject.ScopeType;

import javax.validation.constraints.NotNull;
import java.util.List;

public class GenerateStockCountWorksheetCommand {
    @NotNull(message = "Tenant ID is required")
    private final TenantId tenantId;

    @NotNull(message = "User ID is required")
    private final UserId userId;

    @NotNull(message = "Count type is required")
    private final CountType countType;

    @NotNull(message = "Scope type is required")
    private final ScopeType scopeType;

    private final List<String> locationIds;
    private final List<String> categoryIds;
    private final String skuRangeStart;
    private final String skuRangeEnd;
    private final boolean includeExpired;
    private final boolean includeReserved;
    private final String notes;

    private GenerateStockCountWorksheetCommand(Builder builder) {
        this.tenantId = builder.tenantId;
        this.userId = builder.userId;
        this.countType = builder.countType;
        this.scopeType = builder.scopeType;
        this.locationIds = builder.locationIds;
        this.categoryIds = builder.categoryIds;
        this.skuRangeStart = builder.skuRangeStart;
        this.skuRangeEnd = builder.skuRangeEnd;
        this.includeExpired = builder.includeExpired;
        this.includeReserved = builder.includeReserved;
        this.notes = builder.notes;
    }

    // Getters
    public TenantId getTenantId() { return tenantId; }
    public UserId getUserId() { return userId; }
    public CountType getCountType() { return countType; }
    public ScopeType getScopeType() { return scopeType; }
    public List<String> getLocationIds() { return locationIds; }
    public List<String> getCategoryIds() { return categoryIds; }
    public String getSkuRangeStart() { return skuRangeStart; }
    public String getSkuRangeEnd() { return skuRangeEnd; }
    public boolean isIncludeExpired() { return includeExpired; }
    public boolean isIncludeReserved() { return includeReserved; }
    public String getNotes() { return notes; }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private TenantId tenantId;
        private UserId userId;
        private CountType countType;
        private ScopeType scopeType;
        private List<String> locationIds;
        private List<String> categoryIds;
        private String skuRangeStart;
        private String skuRangeEnd;
        private boolean includeExpired;
        private boolean includeReserved;
        private String notes;

        public Builder tenantId(TenantId tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder userId(UserId userId) {
            this.userId = userId;
            return this;
        }

        public Builder countType(CountType countType) {
            this.countType = countType;
            return this;
        }

        public Builder scopeType(ScopeType scopeType) {
            this.scopeType = scopeType;
            return this;
        }

        public Builder locationIds(List<String> locationIds) {
            this.locationIds = locationIds;
            return this;
        }

        public Builder categoryIds(List<String> categoryIds) {
            this.categoryIds = categoryIds;
            return this;
        }

        public Builder skuRangeStart(String skuRangeStart) {
            this.skuRangeStart = skuRangeStart;
            return this;
        }

        public Builder skuRangeEnd(String skuRangeEnd) {
            this.skuRangeEnd = skuRangeEnd;
            return this;
        }

        public Builder includeExpired(boolean includeExpired) {
            this.includeExpired = includeExpired;
            return this;
        }

        public Builder includeReserved(boolean includeReserved) {
            this.includeReserved = includeReserved;
            return this;
        }

        public Builder notes(String notes) {
            this.notes = notes;
            return this;
        }

        public GenerateStockCountWorksheetCommand build() {
            return new GenerateStockCountWorksheetCommand(this);
        }
    }
}
```

### Command Handler: GenerateStockCountWorksheetCommandHandler

**File:** `services/reconciliation-service/reconciliation-domain/reconciliation-application-service/src/main/java/com/ccbsa/wms/reconciliation/application/service/command/GenerateStockCountWorksheetCommandHandler.java`

```java
package com.ccbsa.wms.reconciliation.application.service.command;

import com.ccbsa.common.domain.valueobject.*;
import com.ccbsa.wms.reconciliation.application.service.command.dto.*;
import com.ccbsa.wms.reconciliation.application.service.port.repository.StockCountRepository;
import com.ccbsa.wms.reconciliation.application.service.port.service.StockManagementServicePort;
import com.ccbsa.wms.reconciliation.application.service.port.service.dto.StockLevelInfo;
import com.ccbsa.wms.reconciliation.domain.core.entity.StockCount;
import com.ccbsa.wms.reconciliation.domain.core.valueobject.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
public class GenerateStockCountWorksheetCommandHandler {

    private final StockCountRepository stockCountRepository;
    private final StockManagementServicePort stockManagementService;

    public GenerateStockCountWorksheetCommandHandler(
        StockCountRepository stockCountRepository,
        StockManagementServicePort stockManagementService
    ) {
        this.stockCountRepository = stockCountRepository;
        this.stockManagementService = stockManagementService;
    }

    @Transactional
    public GenerateStockCountWorksheetResult handle(GenerateStockCountWorksheetCommand command) {
        log.info("Generating stock count worksheet for tenant: {}, type: {}",
            command.getTenantId().getValue(), command.getCountType());

        // 1. Build scope configuration
        ScopeConfiguration scopeConfiguration = buildScopeConfiguration(command);

        // 2. Create stock count aggregate
        StockCount stockCount = createStockCount(command, scopeConfiguration);

        // 3. Query stock levels based on scope
        List<StockLevelInfo> stockLevels = queryStockLevels(command, scopeConfiguration);

        log.info("Found {} stock level records for worksheet generation", stockLevels.size());

        // 4. Build worksheet entries
        List<WorksheetEntry> worksheetEntries = buildWorksheetEntries(stockLevels);

        // 5. Initialize worksheet on aggregate
        stockCount.initializeWorksheet(worksheetEntries);

        // 6. Save stock count
        StockCount savedStockCount = stockCountRepository.save(stockCount);

        log.info("Stock count worksheet generated successfully. ID: {}, Entries: {}",
            savedStockCount.getId().getValue(), worksheetEntries.size());

        // 7. Return result
        return GenerateStockCountWorksheetResult.builder()
            .stockCountId(savedStockCount.getId().getValue())
            .worksheetReference(generateWorksheetReference(savedStockCount))
            .countType(savedStockCount.getCountType())
            .status(savedStockCount.getStatus())
            .entriesCount(worksheetEntries.size())
            .initiatedBy(savedStockCount.getInitiatedBy().getValue())
            .initiatedAt(savedStockCount.getInitiatedAt())
            .build();
    }

    /**
     * Build scope configuration from command
     */
    private ScopeConfiguration buildScopeConfiguration(GenerateStockCountWorksheetCommand command) {
        List<LocationId> locationIds = command.getLocationIds() != null
            ? command.getLocationIds().stream()
                .map(id -> new LocationId(UUID.fromString(id)))
                .collect(Collectors.toList())
            : new ArrayList<>();

        List<ProductCategoryId> categoryIds = command.getCategoryIds() != null
            ? command.getCategoryIds().stream()
                .map(id -> new ProductCategoryId(UUID.fromString(id)))
                .collect(Collectors.toList())
            : new ArrayList<>();

        return new ScopeConfiguration(
            command.getScopeType(),
            locationIds,
            categoryIds,
            command.getSkuRangeStart(),
            command.getSkuRangeEnd(),
            command.isIncludeExpired(),
            command.isIncludeReserved()
        );
    }

    /**
     * Create stock count aggregate in DRAFT status
     */
    private StockCount createStockCount(
        GenerateStockCountWorksheetCommand command,
        ScopeConfiguration scopeConfiguration
    ) {
        return StockCount.builder()
            .id(new StockCountId(UUID.randomUUID()))
            .tenantId(command.getTenantId())
            .countType(command.getCountType())
            .scopeConfiguration(scopeConfiguration)
            .status(StockCountStatus.DRAFT)
            .initiatedBy(command.getUserId())
            .initiatedAt(ZonedDateTime.now())
            .notes(command.getNotes() != null ? new Notes(command.getNotes()) : null)
            .entries(new ArrayList<>())
            .build();
    }

    /**
     * Query stock levels from Stock Management Service based on scope
     */
    private List<StockLevelInfo> queryStockLevels(
        GenerateStockCountWorksheetCommand command,
        ScopeConfiguration scopeConfiguration
    ) {
        // Call Stock Management Service to get current stock levels
        // This is a service port call to another microservice
        return stockManagementService.queryStockLevels(
            command.getTenantId(),
            scopeConfiguration.getLocationIds(),
            scopeConfiguration.getCategoryIds(),
            scopeConfiguration.getSkuRangeStart(),
            scopeConfiguration.getSkuRangeEnd(),
            scopeConfiguration.isIncludeExpired(),
            scopeConfiguration.isIncludeReserved()
        );
    }

    /**
     * Build worksheet entries from stock level information
     */
    private List<WorksheetEntry> buildWorksheetEntries(List<StockLevelInfo> stockLevels) {
        return stockLevels.stream()
            .map(stockLevel -> new WorksheetEntry(
                stockLevel.getLocationId(),
                stockLevel.getProductId(),
                stockLevel.getQuantity()
            ))
            .collect(Collectors.toList());
    }

    /**
     * Generate human-readable worksheet reference
     * Format: SC-YYYYMMDD-NNNN (e.g., SC-20260111-0001)
     */
    private String generateWorksheetReference(StockCount stockCount) {
        String dateStr = stockCount.getInitiatedAt()
            .format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);

        // Get sequential number for the day (simplified - in real impl, use database sequence)
        String sequenceStr = String.format("%04d",
            Math.abs(stockCount.getId().getValue().hashCode()) % 10000);

        return String.format("SC-%s-%s", dateStr, sequenceStr);
    }
}
```

### Repository Port

**File:** `services/reconciliation-service/reconciliation-domain/reconciliation-application-service/src/main/java/com/ccbsa/wms/reconciliation/application/service/port/repository/StockCountRepository.java`

```java
package com.ccbsa.wms.reconciliation.application.service.port.repository;

import com.ccbsa.common.domain.valueobject.StockCountId;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.reconciliation.domain.core.entity.StockCount;

import java.util.Optional;

public interface StockCountRepository {

    /**
     * Save stock count aggregate
     */
    StockCount save(StockCount stockCount);

    /**
     * Find stock count by ID
     */
    Optional<StockCount> findById(StockCountId stockCountId, TenantId tenantId);

    /**
     * Check if stock count exists
     */
    boolean existsById(StockCountId stockCountId, TenantId tenantId);

    /**
     * Delete stock count
     */
    void delete(StockCountId stockCountId, TenantId tenantId);
}
```

### Service Port: StockManagementServicePort

**File:** `services/reconciliation-service/reconciliation-domain/reconciliation-application-service/src/main/java/com/ccbsa/wms/reconciliation/application/service/port/service/StockManagementServicePort.java`

```java
package com.ccbsa.wms.reconciliation.application.service.port.service;

import com.ccbsa.common.domain.valueobject.*;
import com.ccbsa.wms.reconciliation.application.service.port.service.dto.StockLevelInfo;

import java.util.List;

/**
 * Port for integration with Stock Management Service
 */
public interface StockManagementServicePort {

    /**
     * Query stock levels based on scope configuration
     *
     * @param tenantId         Tenant identifier
     * @param locationIds      Optional location filter
     * @param categoryIds      Optional category filter
     * @param skuRangeStart    Optional SKU range start
     * @param skuRangeEnd      Optional SKU range end
     * @param includeExpired   Include expired products
     * @param includeReserved  Include reserved stock
     * @return List of stock level information
     */
    List<StockLevelInfo> queryStockLevels(
        TenantId tenantId,
        List<LocationId> locationIds,
        List<ProductCategoryId> categoryIds,
        String skuRangeStart,
        String skuRangeEnd,
        boolean includeExpired,
        boolean includeReserved
    );
}
```

### DTO: StockLevelInfo

**File:** `services/reconciliation-service/reconciliation-domain/reconciliation-application-service/src/main/java/com/ccbsa/wms/reconciliation/application/service/port/service/dto/StockLevelInfo.java`

```java
package com.ccbsa.wms.reconciliation.application.service.port.service.dto;

import com.ccbsa.common.domain.valueobject.LocationId;
import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.Quantity;

public class StockLevelInfo {
    private final LocationId locationId;
    private final ProductId productId;
    private final Quantity quantity;

    public StockLevelInfo(LocationId locationId, ProductId productId, Quantity quantity) {
        this.locationId = locationId;
        this.productId = productId;
        this.quantity = quantity;
    }

    public LocationId getLocationId() { return locationId; }
    public ProductId getProductId() { return productId; }
    public Quantity getQuantity() { return quantity; }
}
```

### REST Controller

**File:** `services/reconciliation-service/reconciliation-application/src/main/java/com/ccbsa/wms/reconciliation/application/command/StockCountCommandController.java`

```java
package com.ccbsa.wms.reconciliation.application.command;

import com.ccbsa.common.domain.valueobject.*;
import com.ccbsa.wms.reconciliation.application.command.dto.*;
import com.ccbsa.wms.reconciliation.application.service.command.*;
import com.ccbsa.wms.reconciliation.application.service.command.dto.*;
import com.ccbsa.wms.reconciliation.domain.core.valueobject.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/reconciliation/stock-counts")
public class StockCountCommandController {

    private final GenerateStockCountWorksheetCommandHandler generateWorksheetHandler;

    public StockCountCommandController(
        GenerateStockCountWorksheetCommandHandler generateWorksheetHandler
    ) {
        this.generateWorksheetHandler = generateWorksheetHandler;
    }

    @PostMapping("/generate-worksheet")
    public ResponseEntity<GenerateWorksheetResponseDTO> generateWorksheet(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestHeader("X-User-ID") String userId,
        @Valid @RequestBody GenerateWorksheetRequestDTO request
    ) {
        log.info("Received generate worksheet request for tenant: {}, type: {}",
            tenantId, request.getCountType());

        // Build command
        GenerateStockCountWorksheetCommand command = GenerateStockCountWorksheetCommand.builder()
            .tenantId(new TenantId(UUID.fromString(tenantId)))
            .userId(new UserId(UUID.fromString(userId)))
            .countType(CountType.valueOf(request.getCountType()))
            .scopeType(ScopeType.valueOf(request.getScopeType()))
            .locationIds(request.getLocationIds())
            .categoryIds(request.getCategoryIds())
            .skuRangeStart(request.getSkuRangeStart())
            .skuRangeEnd(request.getSkuRangeEnd())
            .includeExpired(request.isIncludeExpired())
            .includeReserved(request.isIncludeReserved())
            .notes(request.getNotes())
            .build();

        // Execute command
        GenerateStockCountWorksheetResult result = generateWorksheetHandler.handle(command);

        // Map to response DTO
        GenerateWorksheetResponseDTO response = GenerateWorksheetResponseDTO.builder()
            .stockCountId(result.getStockCountId().toString())
            .worksheetReference(result.getWorksheetReference())
            .countType(result.getCountType().name())
            .status(result.getStatus().name())
            .entriesCount(result.getEntriesCount())
            .initiatedBy(result.getInitiatedBy().toString())
            .initiatedAt(result.getInitiatedAt())
            .build();

        log.info("Worksheet generated successfully. Reference: {}", result.getWorksheetReference());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
```

### Request/Response DTOs

**File:** `services/reconciliation-service/reconciliation-application/src/main/java/com/ccbsa/wms/reconciliation/application/command/dto/GenerateWorksheetRequestDTO.java`

```java
package com.ccbsa.wms.reconciliation.application.command.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateWorksheetRequestDTO {
    @NotNull(message = "Count type is required")
    private String countType; // CYCLE_COUNT or FULL_INVENTORY

    @NotNull(message = "Scope type is required")
    private String scopeType; // BY_LOCATION, BY_CATEGORY, BY_SKU_RANGE, CUSTOM, WAREHOUSE_WIDE

    private List<String> locationIds;
    private List<String> categoryIds;
    private String skuRangeStart;
    private String skuRangeEnd;
    private boolean includeExpired;
    private boolean includeReserved;
    private String notes;
}
```

**File:** `services/reconciliation-service/reconciliation-application/src/main/java/com/ccbsa/wms/reconciliation/application/command/dto/GenerateWorksheetResponseDTO.java`

```java
package com.ccbsa.wms.reconciliation.application.command.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateWorksheetResponseDTO {
    private String stockCountId;
    private String worksheetReference;
    private String countType;
    private String status;
    private int entriesCount;
    private String initiatedBy;
    private ZonedDateTime initiatedAt;
}
```

### Repository Implementation (Data Access Layer)

**File:** `services/reconciliation-service/reconciliation-dataaccess/src/main/java/com/ccbsa/wms/reconciliation/dataaccess/adapter/StockCountRepositoryAdapter.java`

```java
package com.ccbsa.wms.reconciliation.dataaccess.adapter;

import com.ccbsa.common.domain.valueobject.StockCountId;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.reconciliation.application.service.port.repository.StockCountRepository;
import com.ccbsa.wms.reconciliation.dataaccess.jpa.StockCountJpaRepository;
import com.ccbsa.wms.reconciliation.dataaccess.mapper.StockCountDataAccessMapper;
import com.ccbsa.wms.reconciliation.domain.core.entity.StockCount;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
public class StockCountRepositoryAdapter implements StockCountRepository {

    private final StockCountJpaRepository jpaRepository;
    private final StockCountDataAccessMapper mapper;

    public StockCountRepositoryAdapter(
        StockCountJpaRepository jpaRepository,
        StockCountDataAccessMapper mapper
    ) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public StockCount save(StockCount stockCount) {
        var entity = mapper.toEntity(stockCount);
        var savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<StockCount> findById(StockCountId stockCountId, TenantId tenantId) {
        return jpaRepository.findByIdAndTenantId(
            stockCountId.getValue(),
            tenantId.getValue()
        ).map(mapper::toDomain);
    }

    @Override
    public boolean existsById(StockCountId stockCountId, TenantId tenantId) {
        return jpaRepository.existsByIdAndTenantId(
            stockCountId.getValue(),
            tenantId.getValue()
        );
    }

    @Override
    public void delete(StockCountId stockCountId, TenantId tenantId) {
        jpaRepository.deleteByIdAndTenantId(
            stockCountId.getValue(),
            tenantId.getValue()
        );
    }
}
```

---

## Frontend Implementation

### Custom Hook: useGenerateStockCountWorksheet

**File:** `frontend-app/src/features/reconciliation/hooks/useGenerateStockCountWorksheet.ts`

```typescript
import { useState } from 'react';
import { stockCountService } from '../services/stockCountService';
import type { GenerateWorksheetRequest, GenerateWorksheetResponse } from '../types/stockCount';

export const useGenerateStockCountWorksheet = () => {
  const [isGenerating, setIsGenerating] = useState(false);
  const [generationProgress, setGenerationProgress] = useState(0);
  const [worksheetReference, setWorksheetReference] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const generateWorksheet = async (request: GenerateWorksheetRequest): Promise<void> => {
    try {
      setIsGenerating(true);
      setError(null);
      setGenerationProgress(0);

      // Simulate progress for better UX
      const progressInterval = setInterval(() => {
        setGenerationProgress(prev => {
          if (prev >= 90) {
            clearInterval(progressInterval);
            return 90;
          }
          return prev + 10;
        });
      }, 300);

      // Call API
      const response = await stockCountService.generateWorksheet(request);

      clearInterval(progressInterval);
      setGenerationProgress(100);
      setWorksheetReference(response.worksheetReference);
    } catch (err: any) {
      setError(err.message || 'Failed to generate worksheet');
      setGenerationProgress(0);
    } finally {
      setIsGenerating(false);
    }
  };

  const reset = () => {
    setIsGenerating(false);
    setGenerationProgress(0);
    setWorksheetReference(null);
    setError(null);
  };

  return {
    generateWorksheet,
    isGenerating,
    generationProgress,
    worksheetReference,
    error,
    reset
  };
};
```

### Service: stockCountService

**File:** `frontend-app/src/features/reconciliation/services/stockCountService.ts`

```typescript
import apiClient from '../../../services/apiClient';
import type {
  GenerateWorksheetRequest,
  GenerateWorksheetResponse,
  StockCountWorksheet,
  WorksheetPreview
} from '../types/stockCount';

const BASE_URL = '/api/v1/reconciliation/stock-counts';

export const stockCountService = {
  /**
   * Generate a new stock count worksheet
   */
  async generateWorksheet(request: GenerateWorksheetRequest): Promise<GenerateWorksheetResponse> {
    const response = await apiClient.post<GenerateWorksheetResponse>(
      `${BASE_URL}/generate-worksheet`,
      request
    );
    return response.data;
  },

  /**
   * Get worksheet preview (estimated entries count)
   */
  async getWorksheetPreview(request: GenerateWorksheetRequest): Promise<WorksheetPreview> {
    const response = await apiClient.post<WorksheetPreview>(
      `${BASE_URL}/preview-worksheet`,
      request
    );
    return response.data;
  },

  /**
   * Get stock count worksheet by ID
   */
  async getWorksheet(stockCountId: string): Promise<StockCountWorksheet> {
    const response = await apiClient.get<StockCountWorksheet>(
      `${BASE_URL}/${stockCountId}`
    );
    return response.data;
  },

  /**
   * List all stock count worksheets
   */
  async listWorksheets(filters?: {
    status?: string;
    countType?: string;
    dateFrom?: string;
    dateTo?: string;
  }): Promise<StockCountWorksheet[]> {
    const response = await apiClient.get<StockCountWorksheet[]>(BASE_URL, {
      params: filters
    });
    return response.data;
  },

  /**
   * Cancel a stock count worksheet
   */
  async cancelWorksheet(stockCountId: string): Promise<void> {
    await apiClient.post(`${BASE_URL}/${stockCountId}/cancel`);
  }
};
```

### Additional Hooks

**File:** `frontend-app/src/features/reconciliation/hooks/useLocations.ts`

```typescript
import { useState, useEffect } from 'react';
import { locationService } from '../../location-management/services/locationService';
import type { Location } from '../types/stockCount';

export const useLocations = () => {
  const [locations, setLocations] = useState<Location[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchLocations = async () => {
      try {
        setIsLoading(true);
        const data = await locationService.getAllLocations();
        setLocations(data);
      } catch (err: any) {
        setError(err.message || 'Failed to fetch locations');
      } finally {
        setIsLoading(false);
      }
    };

    fetchLocations();
  }, []);

  return { locations, isLoading, error };
};
```

**File:** `frontend-app/src/features/reconciliation/hooks/useProductCategories.ts`

```typescript
import { useState, useEffect } from 'react';
import { productService } from '../../product/services/productService';
import type { ProductCategory } from '../types/stockCount';

export const useProductCategories = () => {
  const [categories, setCategories] = useState<ProductCategory[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchCategories = async () => {
      try {
        setIsLoading(true);
        const data = await productService.getCategories();
        setCategories(data);
      } catch (err: any) {
        setError(err.message || 'Failed to fetch categories');
      } finally {
        setIsLoading(false);
      }
    };

    fetchCategories();
  }, []);

  return { categories, isLoading, error };
};
```

---

## Data Flow

### Worksheet Generation Flow

```
┌─────────────┐
│   Frontend  │
│   (React)   │
└──────┬──────┘
       │
       │ 1. POST /api/v1/reconciliation/stock-counts/generate-worksheet
       │    {countType, scopeType, locationIds, categoryIds, ...}
       │
       ▼
┌─────────────────────────┐
│ Reconciliation Service  │
│  (REST Controller)      │
└───────────┬─────────────┘
            │
            │ 2. Build Command
            │
            ▼
┌───────────────────────────────┐
│ GenerateWorksheetCommandHandler│
└───────────┬───────────────────┘
            │
            │ 3. Create StockCount Aggregate (DRAFT status)
            │
            ▼
┌───────────────────────────────┐
│  Query Stock Management       │
│  Service for current levels   │
└───────────┬───────────────────┘
            │
            │ 4. Return List<StockLevelInfo>
            │    (location, product, quantity)
            │
            ▼
┌───────────────────────────────┐
│ Build WorksheetEntry list     │
└───────────┬───────────────────┘
            │
            │ 5. stockCount.initializeWorksheet(entries)
            │
            ▼
┌───────────────────────────────┐
│  StockCount Aggregate         │
│  - Validates entries          │
│  - Adds to entries collection │
│  - Registers domain event     │
└───────────┬───────────────────┘
            │
            │ 6. Save to database
            │
            ▼
┌───────────────────────────────┐
│  StockCountRepository         │
│  - Persist aggregate          │
│  - Persist entries            │
└───────────┬───────────────────┘
            │
            │ 7. Publish StockCountInitiatedEvent to Kafka
            │
            ▼
┌───────────────────────────────┐
│  Event Bus (Kafka)            │
│  - Notification Service       │
│  - Audit Log                  │
└───────────────────────────────┘
```

### Service Integration

```
Reconciliation Service
    │
    │ REST/HTTP Call
    │
    ▼
Stock Management Service
    - GET /api/v1/stock/query-levels
    - Filters: locations, categories, SKU range
    - Returns: List of {locationId, productId, quantity}
```

---

## Testing Strategy

### Unit Tests

**Test Class:** `StockCountTest` (Domain Core)

```java
@Test
void shouldInitializeWorksheetSuccessfully() {
    // Given
    StockCount stockCount = createDraftStockCount();
    List<WorksheetEntry> entries = createMockWorksheetEntries(10);

    // When
    stockCount.initializeWorksheet(entries);

    // Then
    assertEquals(10, stockCount.getEntries().size());
    assertEquals(StockCountStatus.DRAFT, stockCount.getStatus());
    assertEquals(1, stockCount.getDomainEvents().size());
    assertTrue(stockCount.getDomainEvents().get(0) instanceof StockCountInitiatedEvent);
}

@Test
void shouldRejectDuplicateLocationProductCombinations() {
    // Given
    StockCount stockCount = createDraftStockCount();
    LocationId location = new LocationId(UUID.randomUUID());
    ProductId product = new ProductId(UUID.randomUUID());

    List<WorksheetEntry> entries = Arrays.asList(
        new WorksheetEntry(location, product, new Quantity(BigDecimal.TEN)),
        new WorksheetEntry(location, product, new Quantity(BigDecimal.valueOf(20))) // Duplicate
    );

    // When/Then
    assertThrows(IllegalArgumentException.class, () ->
        stockCount.initializeWorksheet(entries)
    );
}

@Test
void shouldRejectInitializationIfNotInDraftStatus() {
    // Given
    StockCount stockCount = createDraftStockCount();
    stockCount.startCounting(); // Status changes to IN_PROGRESS
    List<WorksheetEntry> entries = createMockWorksheetEntries(5);

    // When/Then
    assertThrows(IllegalStateException.class, () ->
        stockCount.initializeWorksheet(entries)
    );
}
```

**Test Class:** `GenerateStockCountWorksheetCommandHandlerTest`

```java
@Test
void shouldGenerateWorksheetSuccessfully() {
    // Given
    GenerateStockCountWorksheetCommand command = createCommand();
    List<StockLevelInfo> stockLevels = createMockStockLevels(50);

    when(stockManagementService.queryStockLevels(any(), any(), any(), any(), any(), anyBoolean(), anyBoolean()))
        .thenReturn(stockLevels);

    // When
    GenerateStockCountWorksheetResult result = handler.handle(command);

    // Then
    assertNotNull(result.getStockCountId());
    assertNotNull(result.getWorksheetReference());
    assertEquals(50, result.getEntriesCount());
    assertEquals(CountType.CYCLE_COUNT, result.getCountType());
    assertEquals(StockCountStatus.DRAFT, result.getStatus());

    verify(stockCountRepository).save(any(StockCount.class));
}

@Test
void shouldBuildCorrectScopeConfiguration() {
    // Given
    List<String> locationIds = Arrays.asList(UUID.randomUUID().toString());
    List<String> categoryIds = Arrays.asList(UUID.randomUUID().toString());

    GenerateStockCountWorksheetCommand command = GenerateStockCountWorksheetCommand.builder()
        .tenantId(new TenantId(UUID.randomUUID()))
        .userId(new UserId(UUID.randomUUID()))
        .countType(CountType.CYCLE_COUNT)
        .scopeType(ScopeType.CUSTOM)
        .locationIds(locationIds)
        .categoryIds(categoryIds)
        .includeExpired(false)
        .includeReserved(true)
        .build();

    // When
    GenerateStockCountWorksheetResult result = handler.handle(command);

    // Then
    verify(stockManagementService).queryStockLevels(
        eq(command.getTenantId()),
        argThat(ids -> ids.size() == 1),
        argThat(ids -> ids.size() == 1),
        isNull(),
        isNull(),
        eq(false),
        eq(true)
    );
}
```

### Integration Tests

**Test Class:** `StockCountRepositoryIntegrationTest`

```java
@SpringBootTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class StockCountRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private StockCountRepository repository;

    @Test
    void shouldSaveAndRetrieveStockCount() {
        // Given
        StockCount stockCount = createStockCountWithEntries(5);

        // When
        StockCount saved = repository.save(stockCount);
        Optional<StockCount> retrieved = repository.findById(
            saved.getId(),
            saved.getTenantId()
        );

        // Then
        assertTrue(retrieved.isPresent());
        assertEquals(5, retrieved.get().getEntries().size());
        assertEquals(stockCount.getCountType(), retrieved.get().getCountType());
    }
}
```

### Gateway API Tests

**Test Class:** `StockCountTest` (Gateway API Tests)

**File:** `gateway-api-tests/src/test/java/com/ccbsa/wms/gateway/api/StockCountTest.java`

```java
package com.ccbsa.wms.gateway.api;

import com.ccbsa.wms.gateway.api.dto.*;
import com.ccbsa.wms.gateway.api.fixture.StockCountTestDataBuilder;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class StockCountTest extends BaseIntegrationTest {

    private StockCountTestDataBuilder dataBuilder;

    @BeforeEach
    void setUp() {
        dataBuilder = new StockCountTestDataBuilder(restTemplate, baseUrl, tenantId, authToken);
    }

    @Test
    @Order(1)
    void shouldGenerateCycleCountWorksheet() {
        // Given
        GenerateWorksheetRequest request = GenerateWorksheetRequest.builder()
            .countType("CYCLE_COUNT")
            .scopeType("BY_LOCATION")
            .locationIds(Arrays.asList(dataBuilder.getLocationId()))
            .includeExpired(false)
            .includeReserved(true)
            .notes("Cycle count for Zone A")
            .build();

        // When
        ResponseEntity<GenerateWorksheetResponse> response = dataBuilder.generateWorksheet(request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getWorksheetReference()).startsWith("SC-");
        assertThat(response.getBody().getCountType()).isEqualTo("CYCLE_COUNT");
        assertThat(response.getBody().getStatus()).isEqualTo("DRAFT");
        assertThat(response.getBody().getEntriesCount()).isGreaterThan(0);
    }

    @Test
    @Order(2)
    void shouldGenerateFullInventoryWorksheet() {
        // Given
        GenerateWorksheetRequest request = GenerateWorksheetRequest.builder()
            .countType("FULL_INVENTORY")
            .scopeType("WAREHOUSE_WIDE")
            .includeExpired(true)
            .includeReserved(true)
            .notes("Full inventory count - Q1 2026")
            .build();

        // When
        ResponseEntity<GenerateWorksheetResponse> response = dataBuilder.generateWorksheet(request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCountType()).isEqualTo("FULL_INVENTORY");
        assertThat(response.getBody().getEntriesCount()).isGreaterThan(0);
    }

    @Test
    @Order(3)
    void shouldRejectWorksheetWithInvalidScope() {
        // Given - BY_LOCATION scope without locationIds
        GenerateWorksheetRequest request = GenerateWorksheetRequest.builder()
            .countType("CYCLE_COUNT")
            .scopeType("BY_LOCATION")
            .locationIds(null) // Missing required locationIds
            .includeExpired(false)
            .includeReserved(true)
            .build();

        // When
        ResponseEntity<ErrorResponse> response = dataBuilder.generateWorksheetExpectingError(request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).contains("Location IDs required");
    }
}
```

---

## Acceptance Criteria Validation

| AC # | Acceptance Criteria | Implementation Mapping | Test Coverage |
|------|---------------------|------------------------|---------------|
| AC-1 | System generates electronic stock count worksheets (no paper) | `GenerateStockCountWorksheetCommandHandler` creates digital worksheet with entries stored in database | `shouldGenerateCycleCountWorksheet()` |
| AC-2 | System supports cycle counting (subsets of locations/products) | `ScopeType` enum with `BY_LOCATION`, `BY_CATEGORY`, `BY_SKU_RANGE`, `CUSTOM` options; `ScopeConfiguration` value object | `shouldGenerateCycleCountWorksheet()` with location filter |
| AC-3 | System supports full inventory counts (all locations and products) | `CountType.FULL_INVENTORY` with `ScopeType.WAREHOUSE_WIDE` | `shouldGenerateFullInventoryWorksheet()` |
| AC-4 | System auto-populates expected quantities from current stock levels | `StockManagementServicePort.queryStockLevels()` retrieves current quantities; `WorksheetEntry` stores expected quantity | Unit test: `shouldBuildCorrectScopeConfiguration()` |
| AC-5 | System assigns unique worksheet reference number | `generateWorksheetReference()` creates format `SC-YYYYMMDD-NNNN` | Response validation in all API tests |
| AC-6 | Worksheet includes location, product, expected quantity for each entry | `StockCountEntry` entity with `locationId`, `productId`, `expectedQuantity` fields | Database schema and repository tests |
| AC-7 | System saves worksheets in DRAFT status for later use | `StockCount` created with `status = StockCountStatus.DRAFT` | Response status validation: `assertEquals(DRAFT, status)` |
| AC-8 | System allows resuming incomplete counts | Worksheet persisted in database; frontend can retrieve by ID and continue | `shouldSaveAndRetrieveStockCount()` integration test |

---

## Implementation Checklist

### Common Domain Module

- [ ] Create `StockCountId` value object in `common-domain`
- [ ] Create `StockCountEntryId` value object in `common-domain`
- [ ] Create `StockCountStatus` enum in `common-domain`
- [ ] Update `common-domain` pom.xml if needed

### Reconciliation Service - Domain Core

- [ ] Create `CountType` enum
- [ ] Create `ScopeType` enum
- [ ] Create `ScopeConfiguration` value object
- [ ] Create `WorksheetEntry` value object
- [ ] Create `StockCount` aggregate root
- [ ] Create `StockCountEntry` entity
- [ ] Create `StockCountInitiatedEvent` domain event
- [ ] Create `StockCountStartedEvent` domain event
- [ ] Create `StockCountCancelledEvent` domain event
- [ ] Write unit tests for domain entities and value objects

### Reconciliation Service - Application Service

- [ ] Create `GenerateStockCountWorksheetCommand` DTO
- [ ] Create `GenerateStockCountWorksheetResult` DTO
- [ ] Create `GenerateStockCountWorksheetCommandHandler`
- [ ] Create `StockCountRepository` port interface
- [ ] Create `StockManagementServicePort` interface
- [ ] Create `StockLevelInfo` DTO
- [ ] Write unit tests for command handler

### Reconciliation Service - Data Access

- [ ] Create `StockCountEntity` JPA entity
- [ ] Create `StockCountEntryEntity` JPA entity
- [ ] Create `StockCountJpaRepository` interface
- [ ] Create `StockCountDataAccessMapper`
- [ ] Create `StockCountRepositoryAdapter`
- [ ] Create `StockManagementServiceAdapter` (REST client)
- [ ] Create database migration script (Flyway/Liquibase)
- [ ] Write integration tests for repository

### Reconciliation Service - Application (REST)

- [ ] Create `GenerateWorksheetRequestDTO`
- [ ] Create `GenerateWorksheetResponseDTO`
- [ ] Create `StockCountCommandController`
- [ ] Add controller exception handling
- [ ] Write controller tests

### Reconciliation Service - Messaging

- [ ] Create `StockCountEventPublisher`
- [ ] Configure Kafka topics for stock count events
- [ ] Write event publishing tests

### Frontend - Types

- [ ] Create `stockCount.ts` with TypeScript interfaces
- [ ] Define `CountType`, `ScopeType`, `StockCountStatus` types
- [ ] Define request/response interfaces

### Frontend - Services

- [ ] Create `stockCountService.ts` with API client methods
- [ ] Implement `generateWorksheet()` method
- [ ] Implement `getWorksheetPreview()` method
- [ ] Implement `getWorksheet()` method
- [ ] Implement `listWorksheets()` method

### Frontend - Hooks

- [ ] Create `useGenerateStockCountWorksheet` hook
- [ ] Create `useLocations` hook
- [ ] Create `useProductCategories` hook
- [ ] Write hook tests

### Frontend - Components

- [ ] Create `GenerateStockCountWorksheetPage` component
- [ ] Implement count type selection UI
- [ ] Implement scope configuration UI
- [ ] Implement location selector (multi-select)
- [ ] Implement category selector (multi-select)
- [ ] Implement SKU range inputs
- [ ] Implement worksheet preview display
- [ ] Implement progress indicator during generation
- [ ] Implement success confirmation dialog
- [ ] Add routing in `DashboardRouter.tsx`
- [ ] Write component tests

### Gateway API Tests

- [ ] Create `StockCountTest.java`
- [ ] Implement `shouldGenerateCycleCountWorksheet()` test
- [ ] Implement `shouldGenerateFullInventoryWorksheet()` test
- [ ] Implement `shouldRejectWorksheetWithInvalidScope()` test
- [ ] Create `StockCountTestDataBuilder`
- [ ] Create DTOs for test requests/responses
- [ ] Add test fixtures and utilities

### Documentation

- [ ] Update API documentation with new endpoints
- [ ] Add Postman/OpenAPI examples
- [ ] Document worksheet reference format
- [ ] Update user guide with worksheet generation flow

### DevOps

- [ ] Update Docker Compose for Reconciliation Service
- [ ] Configure environment variables for service integration
- [ ] Update CI/CD pipeline to include new tests
- [ ] Add health check endpoint for Reconciliation Service

---

**Implementation Status:** Ready for development

**Dependencies:**
- Stock Management Service API must support stock level queries
- Location Management Service API for location data
- Product Service API for category data

**Estimated Effort:** 8 story points (2-3 days with testing)


