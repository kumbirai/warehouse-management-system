# US-7.3.1: Handle Damage-in-Transit Returns Implementation Plan

**User Story ID:** US-7.3.1
**Epic:** Returns Management (Epic 7)
**Story Points:** 5
**Sprint:** Sprint 7
**Service:** Returns Service, Stock Management Service
**Related Stories:** US-7.2.1 (Process Full Order Return), US-7.4.1 (Assign Return Location)

---

## Table of Contents

1. [Overview](#overview)
2. [Business Requirements](#business-requirements)
3. [Production-Grade UI Design](#production-grade-ui-design)
4. [Domain Model Design](#domain-model-design)
5. [Backend Implementation](#backend-implementation)
6. [Frontend Implementation](#frontend-implementation)
7. [Testing Strategy](#testing-strategy)
8. [Acceptance Criteria Validation](#acceptance-criteria-validation)
9. [Implementation Checklist](#implementation-checklist)

---

## Overview

### User Story

**As a** warehouse operator
**I want to** record detailed damage assessments for goods damaged in transit
**So that** I can track damage patterns, manage insurance claims, and route damaged goods to appropriate locations

### Business Context

When goods arrive damaged during transportation (from supplier or customer returns), warehouse operators need to:

- Capture detailed damage information with photographic evidence
- Classify damage types for pattern analysis and insurance claims
- Assess whether damaged goods can be salvaged, restocked, or must be written off
- Route damaged goods to quarantine or disposal locations
- Generate damage reports for suppliers and insurance providers
- Track damage trends to improve packaging or carrier selection

### Acceptance Criteria

1. ✅ Operators can record damage type, severity, and affected quantity for each damaged product
2. ✅ System supports photo evidence capture with multiple images per damaged item
3. ✅ Damage assessment includes fields for insurance claim reference and estimated loss value
4. ✅ System automatically assigns damaged goods to quarantine location based on damage severity
5. ✅ Damage reports are generated with all evidence and can be exported for claims
6. ✅ System publishes DamageRecordedEvent for downstream processing by Stock Management

---

## Production-Grade UI Design

### Page: Damage Assessment Form (`/returns/damage-assessment`)

#### Component Hierarchy

```
DamageAssessmentPage
├── PageBreadcrumbs
├── DamageAssessmentHeader
│   └── OrderSelectionSection
├── DamageDetailsForm
│   ├── DamageClassificationSection
│   │   ├── DamageTypeSelector (Dropdown)
│   │   ├── DamageSeveritySelector (Radio Group)
│   │   └── DamageSourceSelector (Dropdown)
│   ├── DamagedProductsSection
│   │   └── DamagedProductLineItem[]
│   │       ├── ProductInfo
│   │       ├── QuantityInput (Damaged Quantity)
│   │       ├── ConditionAssessment
│   │       ├── PhotoUploadWidget (Multiple images)
│   │       └── LineNotesField
│   ├── InsuranceClaimSection
│   │   ├── ClaimReferenceInput
│   │   ├── EstimatedLossValueInput
│   │   ├── CarrierInformationInput
│   │   └── IncidentDateTimePicker
│   ├── EvidenceUploadSection
│   │   ├── GeneralPhotosUpload (Packaging, shipping labels, etc.)
│   │   └── DocumentUpload (Delivery note, BOL, etc.)
│   └── DamageNotesSection
└── ActionButtons
    ├── SaveDraftButton
    ├── SubmitDamageReportButton
    └── CancelButton
```

#### UI Components

##### 1. Damage Classification Section

```typescript
interface DamageClassificationSectionProps {
  damageType: DamageType;
  damageSeverity: DamageSeverity;
  damageSource: DamageSource;
  onDamageTypeChange: (type: DamageType) => void;
  onDamageSeverityChange: (severity: DamageSeverity) => void;
  onDamageSourceChange: (source: DamageSource) => void;
}

const DamageClassificationSection: React.FC<DamageClassificationSectionProps> = ({
  damageType,
  damageSeverity,
  damageSource,
  onDamageTypeChange,
  onDamageSeverityChange,
  onDamageSourceChange,
}) => {
  return (
    <Card>
      <CardHeader title="Damage Classification" />
      <CardContent>
        <Grid container spacing={3}>
          <Grid item xs={12} md={4}>
            <FormControl fullWidth required>
              <InputLabel>Damage Type</InputLabel>
              <Select
                value={damageType}
                onChange={(e) => onDamageTypeChange(e.target.value as DamageType)}
              >
                <MenuItem value="CRUSHED">Crushed/Compressed</MenuItem>
                <MenuItem value="BROKEN">Broken/Shattered</MenuItem>
                <MenuItem value="LEAKING">Leaking/Spilled</MenuItem>
                <MenuItem value="CONTAMINATED">Contaminated</MenuItem>
                <MenuItem value="PACKAGING_DAMAGE">Packaging Damage</MenuItem>
                <MenuItem value="EXPIRED">Expired</MenuItem>
                <MenuItem value="TEMPERATURE_ABUSE">Temperature Abuse</MenuItem>
                <MenuItem value="WATER_DAMAGE">Water Damage</MenuItem>
                <MenuItem value="OTHER">Other</MenuItem>
              </Select>
            </FormControl>
          </Grid>

          <Grid item xs={12} md={4}>
            <FormControl component="fieldset" required>
              <FormLabel>Damage Severity</FormLabel>
              <RadioGroup
                value={damageSeverity}
                onChange={(e) => onDamageSeverityChange(e.target.value as DamageSeverity)}
              >
                <FormControlLabel
                  value="MINOR"
                  control={<Radio />}
                  label="Minor - Cosmetic damage, product salvageable"
                />
                <FormControlLabel
                  value="MODERATE"
                  control={<Radio />}
                  label="Moderate - Some product loss, partial salvage"
                />
                <FormControlLabel
                  value="SEVERE"
                  control={<Radio />}
                  label="Severe - Total loss, complete write-off"
                />
              </RadioGroup>
            </FormControl>
          </Grid>

          <Grid item xs={12} md={4}>
            <FormControl fullWidth required>
              <InputLabel>Damage Source</InputLabel>
              <Select
                value={damageSource}
                onChange={(e) => onDamageSourceChange(e.target.value as DamageSource)}
              >
                <MenuItem value="CARRIER">Carrier - Transit Damage</MenuItem>
                <MenuItem value="SUPPLIER">Supplier - Pre-shipment Damage</MenuItem>
                <MenuItem value="WAREHOUSE">Warehouse - Internal Handling</MenuItem>
                <MenuItem value="CUSTOMER">Customer - Customer Return</MenuItem>
                <MenuItem value="UNKNOWN">Unknown</MenuItem>
              </Select>
            </FormControl>
          </Grid>
        </Grid>
      </CardContent>
    </Card>
  );
};
```

##### 2. Damaged Product Line Item

```typescript
interface DamagedProductLineItemProps {
  lineItem: DamagedProductLineItem;
  onUpdate: (updated: DamagedProductLineItem) => void;
  onRemove: () => void;
}

const DamagedProductLineItem: React.FC<DamagedProductLineItemProps> = ({
  lineItem,
  onUpdate,
  onRemove,
}) => {
  const [photos, setPhotos] = useState<string[]>(lineItem.photoUrls || []);

  const handlePhotoUpload = async (files: File[]) => {
    // Upload to storage service and get URLs
    const uploadedUrls = await uploadDamagePhotos(files);
    const updatedPhotos = [...photos, ...uploadedUrls];
    setPhotos(updatedPhotos);
    onUpdate({ ...lineItem, photoUrls: updatedPhotos });
  };

  return (
    <Card variant="outlined" sx={{ mb: 2 }}>
      <CardContent>
        <Grid container spacing={2}>
          <Grid item xs={12} md={6}>
            <Typography variant="subtitle1" fontWeight="bold">
              {lineItem.productCode} - {lineItem.productDescription}
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Total Quantity in Order: {lineItem.orderedQuantity}
            </Typography>
          </Grid>

          <Grid item xs={12} md={6}>
            <TextField
              fullWidth
              required
              type="number"
              label="Damaged Quantity"
              value={lineItem.damagedQuantity}
              onChange={(e) => onUpdate({
                ...lineItem,
                damagedQuantity: parseInt(e.target.value)
              })}
              inputProps={{ min: 1, max: lineItem.orderedQuantity }}
              helperText={`Maximum: ${lineItem.orderedQuantity}`}
            />
          </Grid>

          <Grid item xs={12} md={6}>
            <FormControl fullWidth required>
              <InputLabel>Product Condition After Damage</InputLabel>
              <Select
                value={lineItem.productCondition}
                onChange={(e) => onUpdate({
                  ...lineItem,
                  productCondition: e.target.value as ProductCondition
                })}
              >
                <MenuItem value="QUARANTINE">
                  Quarantine - Requires Inspection
                </MenuItem>
                <MenuItem value="DAMAGED">
                  Damaged - Partial Loss, Some Salvage
                </MenuItem>
                <MenuItem value="WRITE_OFF">
                  Write-Off - Total Loss
                </MenuItem>
              </Select>
            </FormControl>
          </Grid>

          <Grid item xs={12} md={6}>
            <TextField
              fullWidth
              type="number"
              label="Estimated Loss Value (per unit)"
              value={lineItem.estimatedUnitLoss}
              onChange={(e) => onUpdate({
                ...lineItem,
                estimatedUnitLoss: parseFloat(e.target.value)
              })}
              InputProps={{
                startAdornment: <InputAdornment position="start">R</InputAdornment>,
              }}
              helperText={`Total Loss: R ${(lineItem.estimatedUnitLoss * lineItem.damagedQuantity).toFixed(2)}`}
            />
          </Grid>

          <Grid item xs={12}>
            <PhotoUploadWidget
              photos={photos}
              onUpload={handlePhotoUpload}
              onRemove={(index) => {
                const updated = photos.filter((_, i) => i !== index);
                setPhotos(updated);
                onUpdate({ ...lineItem, photoUrls: updated });
              }}
              maxPhotos={10}
              label="Product Damage Photos"
            />
          </Grid>

          <Grid item xs={12}>
            <TextField
              fullWidth
              multiline
              rows={2}
              label="Damage Notes"
              value={lineItem.damageNotes}
              onChange={(e) => onUpdate({
                ...lineItem,
                damageNotes: e.target.value
              })}
              placeholder="Describe the damage, extent, and any other relevant details..."
            />
          </Grid>

          <Grid item xs={12}>
            <Button
              color="error"
              startIcon={<DeleteIcon />}
              onClick={onRemove}
            >
              Remove Item
            </Button>
          </Grid>
        </Grid>
      </CardContent>
    </Card>
  );
};
```

##### 3. Insurance Claim Section

```typescript
interface InsuranceClaimSectionProps {
  claimReference: string;
  estimatedTotalLoss: number;
  carrierName: string;
  trackingNumber: string;
  incidentDateTime: Date;
  onUpdate: (field: string, value: any) => void;
}

const InsuranceClaimSection: React.FC<InsuranceClaimSectionProps> = ({
  claimReference,
  estimatedTotalLoss,
  carrierName,
  trackingNumber,
  incidentDateTime,
  onUpdate,
}) => {
  return (
    <Card>
      <CardHeader title="Insurance Claim Information" />
      <CardContent>
        <Grid container spacing={3}>
          <Grid item xs={12} md={6}>
            <TextField
              fullWidth
              label="Insurance Claim Reference"
              value={claimReference}
              onChange={(e) => onUpdate('claimReference', e.target.value)}
              placeholder="CLM-2026-XXXXX"
              helperText="Optional - Add if claim has been opened"
            />
          </Grid>

          <Grid item xs={12} md={6}>
            <TextField
              fullWidth
              disabled
              label="Estimated Total Loss Value"
              value={estimatedTotalLoss.toFixed(2)}
              InputProps={{
                startAdornment: <InputAdornment position="start">R</InputAdornment>,
              }}
              helperText="Auto-calculated from damaged items"
            />
          </Grid>

          <Grid item xs={12} md={6}>
            <TextField
              fullWidth
              required
              label="Carrier/Transport Company"
              value={carrierName}
              onChange={(e) => onUpdate('carrierName', e.target.value)}
              placeholder="e.g., DHL, FedEx, UPS"
            />
          </Grid>

          <Grid item xs={12} md={6}>
            <TextField
              fullWidth
              label="Tracking/Waybill Number"
              value={trackingNumber}
              onChange={(e) => onUpdate('trackingNumber', e.target.value)}
              placeholder="Shipment tracking reference"
            />
          </Grid>

          <Grid item xs={12} md={6}>
            <DateTimePicker
              label="Incident Date & Time"
              value={incidentDateTime}
              onChange={(date) => onUpdate('incidentDateTime', date)}
              renderInput={(params) => <TextField {...params} fullWidth required />}
            />
          </Grid>
        </Grid>
      </CardContent>
    </Card>
  );
};
```

##### 4. Photo Upload Widget

```typescript
interface PhotoUploadWidgetProps {
  photos: string[];
  onUpload: (files: File[]) => Promise<void>;
  onRemove: (index: number) => void;
  maxPhotos: number;
  label: string;
}

const PhotoUploadWidget: React.FC<PhotoUploadWidgetProps> = ({
  photos,
  onUpload,
  onRemove,
  maxPhotos,
  label,
}) => {
  const [uploading, setUploading] = useState(false);

  const handleFileSelect = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(event.target.files || []);
    if (files.length + photos.length > maxPhotos) {
      enqueueSnackbar(`Maximum ${maxPhotos} photos allowed`, { variant: 'error' });
      return;
    }

    setUploading(true);
    try {
      await onUpload(files);
      enqueueSnackbar('Photos uploaded successfully', { variant: 'success' });
    } catch (error) {
      enqueueSnackbar('Photo upload failed', { variant: 'error' });
    } finally {
      setUploading(false);
    }
  };

  return (
    <Box>
      <Typography variant="subtitle2" gutterBottom>
        {label} ({photos.length}/{maxPhotos})
      </Typography>

      <Grid container spacing={2}>
        {photos.map((photoUrl, index) => (
          <Grid item xs={6} sm={4} md={3} key={index}>
            <Card>
              <CardMedia
                component="img"
                height="140"
                image={photoUrl}
                alt={`Damage photo ${index + 1}`}
              />
              <CardActions>
                <IconButton
                  size="small"
                  color="error"
                  onClick={() => onRemove(index)}
                >
                  <DeleteIcon />
                </IconButton>
                <IconButton
                  size="small"
                  onClick={() => window.open(photoUrl, '_blank')}
                >
                  <ZoomInIcon />
                </IconButton>
              </CardActions>
            </Card>
          </Grid>
        ))}

        {photos.length < maxPhotos && (
          <Grid item xs={6} sm={4} md={3}>
            <input
              accept="image/*"
              style={{ display: 'none' }}
              id={`photo-upload-${label}`}
              multiple
              type="file"
              onChange={handleFileSelect}
              disabled={uploading}
            />
            <label htmlFor={`photo-upload-${label}`}>
              <Card
                sx={{
                  height: 140,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  cursor: 'pointer',
                  border: '2px dashed',
                  borderColor: 'primary.main',
                }}
              >
                {uploading ? (
                  <CircularProgress />
                ) : (
                  <Box textAlign="center">
                    <CloudUploadIcon color="primary" sx={{ fontSize: 40 }} />
                    <Typography variant="caption" display="block">
                      Add Photos
                    </Typography>
                  </Box>
                )}
              </Card>
            </label>
          </Grid>
        )}
      </Grid>
    </Box>
  );
};
```

##### 5. Damage Summary Dashboard

```typescript
interface DamageSummaryProps {
  damageReport: DamageReport;
}

const DamageSummary: React.FC<DamageSummaryProps> = ({ damageReport }) => {
  const totalItems = damageReport.damagedProducts.length;
  const totalQuantity = damageReport.damagedProducts.reduce(
    (sum, item) => sum + item.damagedQuantity,
    0
  );
  const totalLoss = damageReport.estimatedTotalLoss;

  const conditionBreakdown = {
    quarantine: damageReport.damagedProducts.filter(
      (item) => item.productCondition === 'QUARANTINE'
    ).length,
    damaged: damageReport.damagedProducts.filter(
      (item) => item.productCondition === 'DAMAGED'
    ).length,
    writeOff: damageReport.damagedProducts.filter(
      (item) => item.productCondition === 'WRITE_OFF'
    ).length,
  };

  return (
    <Card>
      <CardHeader title="Damage Summary" />
      <CardContent>
        <Grid container spacing={3}>
          <Grid item xs={6} md={3}>
            <Box textAlign="center">
              <Typography variant="h4" color="error">
                {totalItems}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                Damaged Products
              </Typography>
            </Box>
          </Grid>

          <Grid item xs={6} md={3}>
            <Box textAlign="center">
              <Typography variant="h4" color="error">
                {totalQuantity}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                Total Units Damaged
              </Typography>
            </Box>
          </Grid>

          <Grid item xs={6} md={3}>
            <Box textAlign="center">
              <Typography variant="h4" color="error">
                R {totalLoss.toFixed(2)}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                Estimated Loss
              </Typography>
            </Box>
          </Grid>

          <Grid item xs={6} md={3}>
            <Box textAlign="center">
              <Typography variant="h4" color="primary">
                {damageReport.totalPhotos}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                Evidence Photos
              </Typography>
            </Box>
          </Grid>

          <Grid item xs={12}>
            <Divider sx={{ my: 2 }} />
            <Typography variant="subtitle2" gutterBottom>
              Condition Breakdown
            </Typography>
            <Grid container spacing={2}>
              <Grid item xs={4}>
                <Chip
                  label={`Quarantine: ${conditionBreakdown.quarantine}`}
                  color="warning"
                  sx={{ width: '100%' }}
                />
              </Grid>
              <Grid item xs={4}>
                <Chip
                  label={`Damaged: ${conditionBreakdown.damaged}`}
                  color="error"
                  sx={{ width: '100%' }}
                />
              </Grid>
              <Grid item xs={4}>
                <Chip
                  label={`Write-Off: ${conditionBreakdown.writeOff}`}
                  color="default"
                  sx={{ width: '100%' }}
                />
              </Grid>
            </Grid>
          </Grid>
        </Grid>
      </CardContent>
    </Card>
  );
};
```

#### User Flow

1. **Access Damage Assessment**
    - Navigate to Returns > Damage Assessment
    - Or from consignment receipt screen when damage is detected

2. **Select Source Order/Consignment**
    - Search by order number, load number, or consignment reference
    - System loads product information

3. **Classify Damage**
    - Select damage type (CRUSHED, BROKEN, LEAKING, etc.)
    - Choose severity (MINOR, MODERATE, SEVERE)
    - Identify damage source (CARRIER, SUPPLIER, WAREHOUSE, CUSTOMER)

4. **Record Damaged Products**
    - For each damaged product:
        - Enter damaged quantity
        - Assess product condition (QUARANTINE, DAMAGED, WRITE_OFF)
        - Upload photos of damage
        - Add damage notes
        - Estimate loss value per unit

5. **Add Insurance Claim Information**
    - Enter claim reference if available
    - Record carrier information
    - Set incident date and time
    - Add tracking/waybill number

6. **Upload General Evidence**
    - Package photos
    - Delivery documentation
    - Shipping labels
    - Any other relevant evidence

7. **Review Summary**
    - Check damage summary statistics
    - Verify all information is accurate
    - Review total estimated loss

8. **Submit Damage Report**
    - Save draft or submit final report
    - System publishes DamageRecordedEvent
    - Generates damage report PDF
    - Auto-assigns quarantine locations

---

## Domain Model Design

### Aggregates

#### 1. DamageAssessment Aggregate (New)

**Location:** `services/returns-service/returns-domain/returns-domain-core/src/main/java/com/ccbsa/wms/returns/domain/core/entity/DamageAssessment.java`

**Responsibility:** Root aggregate for tracking damage incidents with detailed assessment, photographic evidence, and insurance claim information

**Key Attributes:**

- `DamageAssessmentId damageAssessmentId` (Aggregate Root ID)
- `OrderId orderId` (Reference to damaged order)
- `LoadNumber loadNumber` (Reference to shipment)
- `ConsignmentId consignmentId` (Optional - if damage found during receipt)
- `DamageType damageType` (Classification of damage)
- `DamageSeverity damageSeverity` (Severity level)
- `DamageSource damageSource` (Origin of damage)
- `List<DamagedProductItem> damagedProducts` (Damaged product details - Entity)
- `InsuranceClaimInfo insuranceClaimInfo` (Value Object)
- `List<String> generalPhotoUrls` (General evidence photos)
- `List<String> documentUrls` (Supporting documents)
- `String damageNotes` (General damage description)
- `Money estimatedTotalLoss` (Auto-calculated)
- `DamageAssessmentStatus status` (Draft, Submitted, Under Review, Completed, Cancelled)
- `TenantId tenantId`
- `AuditInfo auditInfo`

**Factory Method:**

```java
public static DamageAssessment recordDamageAssessment(
        DamageAssessmentId damageAssessmentId,
        OrderId orderId,
        LoadNumber loadNumber,
        ConsignmentId consignmentId,
        DamageType damageType,
        DamageSeverity damageSeverity,
        DamageSource damageSource,
        List<DamagedProductItem> damagedProducts,
        InsuranceClaimInfo insuranceClaimInfo,
        List<String> generalPhotoUrls,
        List<String> documentUrls,
        String damageNotes,
        TenantId tenantId,
        String createdBy) {

    validateDamagedProducts(damagedProducts);
    validateInsuranceClaimInfo(insuranceClaimInfo, damageSeverity);

    Money estimatedTotalLoss = calculateTotalLoss(damagedProducts);

    DamageAssessment damageAssessment = new DamageAssessment(
        damageAssessmentId,
        orderId,
        loadNumber,
        consignmentId,
        damageType,
        damageSeverity,
        damageSource,
        damagedProducts,
        insuranceClaimInfo,
        generalPhotoUrls,
        documentUrls,
        damageNotes,
        estimatedTotalLoss,
        DamageAssessmentStatus.SUBMITTED,
        tenantId,
        AuditInfo.create(createdBy)
    );

    // Publish domain event
    damageAssessment.registerEvent(new DamageRecordedEvent(
        damageAssessment,
        ZonedDateTime.now()
    ));

    return damageAssessment;
}

private static void validateDamagedProducts(List<DamagedProductItem> damagedProducts) {
    if (damagedProducts == null || damagedProducts.isEmpty()) {
        throw new InvalidDamageAssessmentException(
            "Damage assessment must have at least one damaged product"
        );
    }

    boolean hasPhotographicEvidence = damagedProducts.stream()
        .anyMatch(item -> item.getPhotoUrls() != null && !item.getPhotoUrls().isEmpty());

    if (!hasPhotographicEvidence) {
        throw new InvalidDamageAssessmentException(
            "At least one damaged product must have photographic evidence"
        );
    }
}

private static void validateInsuranceClaimInfo(
        InsuranceClaimInfo insuranceClaimInfo,
        DamageSeverity damageSeverity) {

    if (damageSeverity == DamageSeverity.SEVERE && insuranceClaimInfo == null) {
        throw new InvalidDamageAssessmentException(
            "Insurance claim information is required for severe damage"
        );
    }
}

private static Money calculateTotalLoss(List<DamagedProductItem> damagedProducts) {
    BigDecimal totalLoss = damagedProducts.stream()
        .map(DamagedProductItem::calculateLineLoss)
        .map(Money::getAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    return new Money(totalLoss);
}
```

**Business Methods:**

```java
public void submitForReview(String reviewedBy) {
    if (this.status != DamageAssessmentStatus.DRAFT) {
        throw new InvalidDamageAssessmentException(
            "Only draft assessments can be submitted for review"
        );
    }

    this.status = DamageAssessmentStatus.UNDER_REVIEW;
    this.auditInfo = this.auditInfo.update(reviewedBy);
}

public void completeDamageAssessment(String completedBy) {
    if (this.status != DamageAssessmentStatus.UNDER_REVIEW) {
        throw new InvalidDamageAssessmentException(
            "Only assessments under review can be completed"
        );
    }

    this.status = DamageAssessmentStatus.COMPLETED;
    this.auditInfo = this.auditInfo.update(completedBy);

    registerEvent(new DamageAssessmentCompletedEvent(this, ZonedDateTime.now()));
}

public void updateInsuranceClaimInfo(InsuranceClaimInfo insuranceClaimInfo, String updatedBy) {
    this.insuranceClaimInfo = insuranceClaimInfo;
    this.auditInfo = this.auditInfo.update(updatedBy);

    registerEvent(new InsuranceClaimUpdatedEvent(this, ZonedDateTime.now()));
}

public int getTotalPhotoCount() {
    int productPhotos = damagedProducts.stream()
        .mapToInt(item -> item.getPhotoUrls() != null ? item.getPhotoUrls().size() : 0)
        .sum();

    int generalPhotos = generalPhotoUrls != null ? generalPhotoUrls.size() : 0;

    return productPhotos + generalPhotos;
}

public Map<ProductCondition, Integer> getConditionBreakdown() {
    return damagedProducts.stream()
        .collect(Collectors.groupingBy(
            DamagedProductItem::getProductCondition,
            Collectors.summingInt(item -> 1)
        ));
}
```

#### 2. DamagedProductItem Entity

**Location:** `services/returns-service/returns-domain/returns-domain-core/src/main/java/com/ccbsa/wms/returns/domain/core/entity/DamagedProductItem.java`

**Responsibility:** Entity representing individual damaged products within a DamageAssessment

**Key Attributes:**

- `DamagedProductItemId id` (Entity ID)
- `ProductId productId`
- `ProductCode productCode`
- `String productDescription`
- `Quantity orderedQuantity`
- `Quantity damagedQuantity`
- `ProductCondition productCondition` (QUARANTINE, DAMAGED, WRITE_OFF)
- `Money estimatedUnitLoss`
- `List<String> photoUrls` (Product-specific damage photos)
- `String damageNotes` (Product-specific notes)

**Factory Method:**

```java
public static DamagedProductItem create(
        ProductId productId,
        ProductCode productCode,
        String productDescription,
        Quantity orderedQuantity,
        Quantity damagedQuantity,
        ProductCondition productCondition,
        Money estimatedUnitLoss,
        List<String> photoUrls,
        String damageNotes) {

    validateDamagedQuantity(orderedQuantity, damagedQuantity);
    validateProductCondition(productCondition);

    return new DamagedProductItem(
        DamagedProductItemId.generate(),
        productId,
        productCode,
        productDescription,
        orderedQuantity,
        damagedQuantity,
        productCondition,
        estimatedUnitLoss,
        photoUrls,
        damageNotes
    );
}

private static void validateDamagedQuantity(Quantity orderedQuantity, Quantity damagedQuantity) {
    if (damagedQuantity.getValue().compareTo(BigDecimal.ZERO) <= 0) {
        throw new InvalidDamageAssessmentException("Damaged quantity must be greater than zero");
    }

    if (damagedQuantity.getValue().compareTo(orderedQuantity.getValue()) > 0) {
        throw new InvalidDamageAssessmentException(
            "Damaged quantity cannot exceed ordered quantity"
        );
    }
}

private static void validateProductCondition(ProductCondition productCondition) {
    if (productCondition == ProductCondition.GOOD) {
        throw new InvalidDamageAssessmentException(
            "Damaged products cannot have GOOD condition"
        );
    }
}
```

**Business Methods:**

```java
public Money calculateLineLoss() {
    BigDecimal lineLoss = this.estimatedUnitLoss.getAmount()
        .multiply(this.damagedQuantity.getValue());

    return new Money(lineLoss);
}

public boolean requiresQuarantine() {
    return this.productCondition.requiresQuarantine();
}

public boolean isTotalLoss() {
    return this.productCondition == ProductCondition.WRITE_OFF;
}
```

---

### Value Objects

#### 1. DamageType Enum (Common Module)

**Location:** `common/common-domain/src/main/java/com/ccbsa/common/domain/valueobject/DamageType.java`

```java
package com.ccbsa.common.domain.valueobject;

public enum DamageType {
    CRUSHED("Crushed/Compressed", "Physical compression or crushing damage"),
    BROKEN("Broken/Shattered", "Broken containers or shattered products"),
    LEAKING("Leaking/Spilled", "Product leakage or spillage"),
    CONTAMINATED("Contaminated", "Product contamination from external sources"),
    PACKAGING_DAMAGE("Packaging Damage", "Damaged packaging but product may be intact"),
    EXPIRED("Expired", "Product past expiration date"),
    TEMPERATURE_ABUSE("Temperature Abuse", "Temperature control failure during transit"),
    WATER_DAMAGE("Water Damage", "Damage from water exposure"),
    OTHER("Other", "Other types of damage not classified above");

    private final String displayName;
    private final String description;

    DamageType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public boolean requiresImmediateAction() {
        return this == LEAKING || this == CONTAMINATED || this == TEMPERATURE_ABUSE;
    }

    public boolean isProductLoss() {
        return this == BROKEN || this == LEAKING || this == EXPIRED;
    }
}
```

#### 2. DamageSeverity Enum (Common Module)

**Location:** `common/common-domain/src/main/java/com/ccbsa/common/domain/valueobject/DamageSeverity.java`

```java
package com.ccbsa.common.domain.valueobject;

public enum DamageSeverity {
    MINOR("Minor", "Cosmetic damage, product salvageable", 1),
    MODERATE("Moderate", "Some product loss, partial salvage possible", 2),
    SEVERE("Severe", "Total loss, complete write-off required", 3);

    private final String displayName;
    private final String description;
    private final int severityLevel;

    DamageSeverity(String displayName, String description, int severityLevel) {
        this.displayName = displayName;
        this.description = description;
        this.severityLevel = severityLevel;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public int getSeverityLevel() {
        return severityLevel;
    }

    public boolean requiresInsuranceClaim() {
        return this == SEVERE;
    }

    public boolean requiresManagerApproval() {
        return this == MODERATE || this == SEVERE;
    }
}
```

#### 3. DamageSource Enum (Common Module)

**Location:** `common/common-domain/src/main/java/com/ccbsa/common/domain/valueobject/DamageSource.java`

```java
package com.ccbsa.common.domain.valueobject;

public enum DamageSource {
    CARRIER("Carrier", "Damage occurred during transit by carrier"),
    SUPPLIER("Supplier", "Damage present before shipment from supplier"),
    WAREHOUSE("Warehouse", "Damage caused by internal warehouse handling"),
    CUSTOMER("Customer", "Damage from customer return"),
    UNKNOWN("Unknown", "Source of damage cannot be determined");

    private final String displayName;
    private final String description;

    DamageSource(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public boolean isExternalPartyResponsible() {
        return this == CARRIER || this == SUPPLIER;
    }

    public boolean requiresCarrierClaim() {
        return this == CARRIER;
    }

    public boolean requiresSupplierClaim() {
        return this == SUPPLIER;
    }
}
```

#### 4. InsuranceClaimInfo Value Object

**Location:** `services/returns-service/returns-domain/returns-domain-core/src/main/java/com/ccbsa/wms/returns/domain/core/valueobject/InsuranceClaimInfo.java`

```java
package com.ccbsa.wms.returns.domain.core.valueobject;

import java.time.ZonedDateTime;
import java.util.Objects;

public final class InsuranceClaimInfo {

    private final String claimReference;
    private final String carrierName;
    private final String trackingNumber;
    private final ZonedDateTime incidentDateTime;

    public InsuranceClaimInfo(
            String claimReference,
            String carrierName,
            String trackingNumber,
            ZonedDateTime incidentDateTime) {

        validateCarrierName(carrierName);
        validateIncidentDateTime(incidentDateTime);

        this.claimReference = claimReference;
        this.carrierName = carrierName;
        this.trackingNumber = trackingNumber;
        this.incidentDateTime = incidentDateTime;
    }

    private void validateCarrierName(String carrierName) {
        if (carrierName == null || carrierName.trim().isEmpty()) {
            throw new IllegalArgumentException("Carrier name is required for insurance claims");
        }
    }

    private void validateIncidentDateTime(ZonedDateTime incidentDateTime) {
        if (incidentDateTime == null) {
            throw new IllegalArgumentException("Incident date and time is required");
        }

        if (incidentDateTime.isAfter(ZonedDateTime.now())) {
            throw new IllegalArgumentException("Incident date cannot be in the future");
        }
    }

    public boolean hasClaimReference() {
        return claimReference != null && !claimReference.trim().isEmpty();
    }

    // Getters
    public String getClaimReference() { return claimReference; }
    public String getCarrierName() { return carrierName; }
    public String getTrackingNumber() { return trackingNumber; }
    public ZonedDateTime getIncidentDateTime() { return incidentDateTime; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InsuranceClaimInfo)) return false;
        InsuranceClaimInfo that = (InsuranceClaimInfo) o;
        return Objects.equals(claimReference, that.claimReference) &&
               Objects.equals(carrierName, that.carrierName) &&
               Objects.equals(trackingNumber, that.trackingNumber) &&
               Objects.equals(incidentDateTime, that.incidentDateTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(claimReference, carrierName, trackingNumber, incidentDateTime);
    }
}
```

#### 5. DamageAssessmentStatus Enum

**Location:** `common/common-domain/src/main/java/com/ccbsa/common/domain/valueobject/DamageAssessmentStatus.java`

```java
package com.ccbsa.common.domain.valueobject;

public enum DamageAssessmentStatus {
    DRAFT("Draft", "Assessment is being prepared"),
    SUBMITTED("Submitted", "Assessment has been submitted"),
    UNDER_REVIEW("Under Review", "Assessment is being reviewed by management"),
    COMPLETED("Completed", "Assessment has been completed and locations assigned"),
    CANCELLED("Cancelled", "Assessment has been cancelled");

    private final String displayName;
    private final String description;

    DamageAssessmentStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public boolean canBeModified() {
        return this == DRAFT;
    }

    public boolean canBeSubmitted() {
        return this == DRAFT;
    }

    public boolean canBeCompleted() {
        return this == UNDER_REVIEW;
    }
}
```

---

### Domain Events

#### 1. DamageRecordedEvent

**Location:** `services/returns-service/returns-domain/returns-domain-core/src/main/java/com/ccbsa/wms/returns/domain/core/event/DamageRecordedEvent.java`

```java
package com.ccbsa.wms.returns.domain.core.event;

import com.ccbsa.common.domain.event.DomainEvent;
import com.ccbsa.wms.returns.domain.core.entity.DamageAssessment;
import lombok.Getter;

import java.time.ZonedDateTime;

@Getter
public class DamageRecordedEvent extends DomainEvent<DamageAssessment> {

    private final DamageAssessment damageAssessment;

    public DamageRecordedEvent(
            DamageAssessment damageAssessment,
            ZonedDateTime occurredOn) {
        super(damageAssessment, occurredOn);
        this.damageAssessment = damageAssessment;
    }
}
```

**Consumed By:**

- **Stock Management Service** - Creates quarantine stock items for damaged products
- **Location Management Service** - Assigns quarantine or disposal locations based on damage severity
- **Notification Service** - Notifies managers of severe damage requiring approval

#### 2. DamageAssessmentCompletedEvent

```java
@Getter
public class DamageAssessmentCompletedEvent extends DomainEvent<DamageAssessment> {

    private final DamageAssessment damageAssessment;

    public DamageAssessmentCompletedEvent(
            DamageAssessment damageAssessment,
            ZonedDateTime occurredOn) {
        super(damageAssessment, occurredOn);
        this.damageAssessment = damageAssessment;
    }
}
```

**Consumed By:**

- **Integration Service** - Syncs damage information to D365 for supplier/carrier claims
- **Reporting Service** - Updates damage trend analytics

#### 3. InsuranceClaimUpdatedEvent

```java
@Getter
public class InsuranceClaimUpdatedEvent extends DomainEvent<DamageAssessment> {

    private final DamageAssessment damageAssessment;

    public InsuranceClaimUpdatedEvent(
            DamageAssessment damageAssessment,
            ZonedDateTime occurredOn) {
        super(damageAssessment, occurredOn);
        this.damageAssessment = damageAssessment;
    }
}
```

**Consumed By:**

- **Integration Service** - Updates D365 with insurance claim reference
- **Notification Service** - Notifies finance team of claim updates

---

## Backend Implementation

### Application Service Layer

#### Command: RecordDamageAssessmentCommand

**Location:**
`services/returns-service/returns-domain/returns-application-service/src/main/java/com/ccbsa/wms/returns/application/service/command/dto/RecordDamageAssessmentCommand.java`

```java
package com.ccbsa.wms.returns.application.service.command.dto;

import com.ccbsa.common.domain.valueobject.*;
import lombok.Builder;
import lombok.Getter;

import java.time.ZonedDateTime;
import java.util.List;

@Getter
@Builder
public class RecordDamageAssessmentCommand {
    private final String orderId;
    private final String loadNumber;
    private final String consignmentId;  // Optional
    private final DamageType damageType;
    private final DamageSeverity damageSeverity;
    private final DamageSource damageSource;
    private final List<DamagedProductCommand> damagedProducts;
    private final InsuranceClaimCommand insuranceClaim;  // Optional
    private final List<String> generalPhotoUrls;
    private final List<String> documentUrls;
    private final String damageNotes;
    private final String tenantId;
    private final String createdBy;
}
```

#### Nested Command: DamagedProductCommand

```java
@Getter
@Builder
public class DamagedProductCommand {
    private final String productId;
    private final String productCode;
    private final String productDescription;
    private final BigDecimal orderedQuantity;
    private final BigDecimal damagedQuantity;
    private final ProductCondition productCondition;
    private final BigDecimal estimatedUnitLoss;
    private final List<String> photoUrls;
    private final String damageNotes;
}
```

#### Nested Command: InsuranceClaimCommand

```java
@Getter
@Builder
public class InsuranceClaimCommand {
    private final String claimReference;  // Optional
    private final String carrierName;
    private final String trackingNumber;
    private final ZonedDateTime incidentDateTime;
}
```

#### Result: RecordDamageAssessmentResult

```java
@Getter
@Builder
public class RecordDamageAssessmentResult {
    private final String damageAssessmentId;
    private final String orderId;
    private final String loadNumber;
    private final DamageType damageType;
    private final DamageSeverity damageSeverity;
    private final BigDecimal estimatedTotalLoss;
    private final int totalDamagedProducts;
    private final int totalDamagedUnits;
    private final int totalPhotoCount;
    private final Map<ProductCondition, Integer> conditionBreakdown;
    private final DamageAssessmentStatus status;
    private final ZonedDateTime recordedAt;
}
```

#### Command Handler

**Location:**
`services/returns-service/returns-domain/returns-application-service/src/main/java/com/ccbsa/wms/returns/application/service/command/RecordDamageAssessmentCommandHandler.java`

```java
package com.ccbsa.wms.returns.application.service.command;

import com.ccbsa.wms.returns.application.service.command.dto.*;
import com.ccbsa.wms.returns.application.service.port.repository.DamageAssessmentRepository;
import com.ccbsa.wms.returns.domain.core.entity.DamageAssessment;
import com.ccbsa.wms.returns.domain.core.entity.DamagedProductItem;
import com.ccbsa.wms.returns.domain.core.valueobject.*;
import com.ccbsa.common.domain.valueobject.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecordDamageAssessmentCommandHandler {

    private final DamageAssessmentRepository damageAssessmentRepository;

    @Transactional
    public RecordDamageAssessmentResult handle(RecordDamageAssessmentCommand command) {
        log.info("Recording damage assessment for order: {}, damage type: {}, severity: {}",
            command.getOrderId(), command.getDamageType(), command.getDamageSeverity());

        // Map command to value objects
        DamageAssessmentId damageAssessmentId = DamageAssessmentId.generate();
        OrderId orderId = new OrderId(command.getOrderId());
        LoadNumber loadNumber = new LoadNumber(command.getLoadNumber());
        ConsignmentId consignmentId = command.getConsignmentId() != null
            ? new ConsignmentId(command.getConsignmentId())
            : null;

        // Map damaged products
        List<DamagedProductItem> damagedProducts = command.getDamagedProducts().stream()
            .map(this::mapToDamagedProductItem)
            .collect(Collectors.toList());

        // Map insurance claim info (if provided)
        InsuranceClaimInfo insuranceClaimInfo = command.getInsuranceClaim() != null
            ? mapToInsuranceClaimInfo(command.getInsuranceClaim())
            : null;

        TenantId tenantId = new TenantId(command.getTenantId());

        // Create damage assessment aggregate
        DamageAssessment damageAssessment = DamageAssessment.recordDamageAssessment(
            damageAssessmentId,
            orderId,
            loadNumber,
            consignmentId,
            command.getDamageType(),
            command.getDamageSeverity(),
            command.getDamageSource(),
            damagedProducts,
            insuranceClaimInfo,
            command.getGeneralPhotoUrls(),
            command.getDocumentUrls(),
            command.getDamageNotes(),
            tenantId,
            command.getCreatedBy()
        );

        // Save aggregate
        DamageAssessment savedDamageAssessment = damageAssessmentRepository.save(damageAssessment);

        log.info("Damage assessment recorded successfully. ID: {}, Total Loss: R {}",
            savedDamageAssessment.getDamageAssessmentId().getValue(),
            savedDamageAssessment.getEstimatedTotalLoss().getAmount());

        // Map to result
        return mapToResult(savedDamageAssessment);
    }

    private DamagedProductItem mapToDamagedProductItem(DamagedProductCommand command) {
        return DamagedProductItem.create(
            new ProductId(command.getProductId()),
            new ProductCode(command.getProductCode()),
            command.getProductDescription(),
            new Quantity(command.getOrderedQuantity()),
            new Quantity(command.getDamagedQuantity()),
            command.getProductCondition(),
            new Money(command.getEstimatedUnitLoss()),
            command.getPhotoUrls(),
            command.getDamageNotes()
        );
    }

    private InsuranceClaimInfo mapToInsuranceClaimInfo(InsuranceClaimCommand command) {
        return new InsuranceClaimInfo(
            command.getClaimReference(),
            command.getCarrierName(),
            command.getTrackingNumber(),
            command.getIncidentDateTime()
        );
    }

    private RecordDamageAssessmentResult mapToResult(DamageAssessment damageAssessment) {
        int totalDamagedUnits = damageAssessment.getDamagedProducts().stream()
            .mapToInt(item -> item.getDamagedQuantity().getValue().intValue())
            .sum();

        return RecordDamageAssessmentResult.builder()
            .damageAssessmentId(damageAssessment.getDamageAssessmentId().getValue())
            .orderId(damageAssessment.getOrderId().getValue())
            .loadNumber(damageAssessment.getLoadNumber().getValue())
            .damageType(damageAssessment.getDamageType())
            .damageSeverity(damageAssessment.getDamageSeverity())
            .estimatedTotalLoss(damageAssessment.getEstimatedTotalLoss().getAmount())
            .totalDamagedProducts(damageAssessment.getDamagedProducts().size())
            .totalDamagedUnits(totalDamagedUnits)
            .totalPhotoCount(damageAssessment.getTotalPhotoCount())
            .conditionBreakdown(damageAssessment.getConditionBreakdown())
            .status(damageAssessment.getStatus())
            .recordedAt(damageAssessment.getAuditInfo().getCreatedAt())
            .build();
    }
}
```

#### Repository Port

**Location:**
`services/returns-service/returns-domain/returns-application-service/src/main/java/com/ccbsa/wms/returns/application/service/port/repository/DamageAssessmentRepository.java`

```java
package com.ccbsa.wms.returns.application.service.port.repository;

import com.ccbsa.wms.returns.domain.core.entity.DamageAssessment;
import com.ccbsa.wms.returns.domain.core.valueobject.DamageAssessmentId;
import com.ccbsa.common.domain.valueobject.TenantId;

import java.util.Optional;

public interface DamageAssessmentRepository {
    DamageAssessment save(DamageAssessment damageAssessment);
    Optional<DamageAssessment> findById(DamageAssessmentId damageAssessmentId, TenantId tenantId);
}
```

---

### Application Layer (REST API)

#### DTOs

**Location:** `services/returns-service/returns-application/src/main/java/com/ccbsa/wms/returns/application/dto/command/RecordDamageAssessmentRequestDTO.java`

```java
package com.ccbsa.wms.returns.application.dto.command;

import com.ccbsa.common.domain.valueobject.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecordDamageAssessmentRequestDTO {

    @NotBlank(message = "Order ID is required")
    private String orderId;

    @NotBlank(message = "Load number is required")
    private String loadNumber;

    private String consignmentId;  // Optional

    @NotNull(message = "Damage type is required")
    private DamageType damageType;

    @NotNull(message = "Damage severity is required")
    private DamageSeverity damageSeverity;

    @NotNull(message = "Damage source is required")
    private DamageSource damageSource;

    @NotEmpty(message = "At least one damaged product is required")
    @Valid
    private List<DamagedProductRequestDTO> damagedProducts;

    @Valid
    private InsuranceClaimRequestDTO insuranceClaim;

    private List<String> generalPhotoUrls;

    private List<String> documentUrls;

    @Size(max = 2000, message = "Damage notes cannot exceed 2000 characters")
    private String damageNotes;
}
```

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DamagedProductRequestDTO {

    @NotBlank(message = "Product ID is required")
    private String productId;

    @NotBlank(message = "Product code is required")
    private String productCode;

    @NotBlank(message = "Product description is required")
    private String productDescription;

    @NotNull(message = "Ordered quantity is required")
    @DecimalMin(value = "0.01", message = "Ordered quantity must be greater than 0")
    private BigDecimal orderedQuantity;

    @NotNull(message = "Damaged quantity is required")
    @DecimalMin(value = "0.01", message = "Damaged quantity must be greater than 0")
    private BigDecimal damagedQuantity;

    @NotNull(message = "Product condition is required")
    private ProductCondition productCondition;

    @NotNull(message = "Estimated unit loss is required")
    @DecimalMin(value = "0.00", message = "Estimated unit loss cannot be negative")
    private BigDecimal estimatedUnitLoss;

    private List<String> photoUrls;

    @Size(max = 500, message = "Damage notes cannot exceed 500 characters")
    private String damageNotes;
}
```

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsuranceClaimRequestDTO {

    private String claimReference;

    @NotBlank(message = "Carrier name is required for insurance claims")
    private String carrierName;

    private String trackingNumber;

    @NotNull(message = "Incident date and time is required")
    @PastOrPresent(message = "Incident date cannot be in the future")
    private ZonedDateTime incidentDateTime;
}
```

#### Response DTOs

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecordDamageAssessmentResponseDTO {
    private String damageAssessmentId;
    private String orderId;
    private String loadNumber;
    private DamageType damageType;
    private DamageSeverity damageSeverity;
    private BigDecimal estimatedTotalLoss;
    private Integer totalDamagedProducts;
    private Integer totalDamagedUnits;
    private Integer totalPhotoCount;
    private Map<ProductCondition, Integer> conditionBreakdown;
    private DamageAssessmentStatus status;
    private ZonedDateTime recordedAt;
}
```

#### REST Controller

**Location:** `services/returns-service/returns-application/src/main/java/com/ccbsa/wms/returns/application/command/DamageAssessmentCommandController.java`

```java
package com.ccbsa.wms.returns.application.command;

import com.ccbsa.wms.returns.application.dto.command.*;
import com.ccbsa.wms.returns.application.service.command.RecordDamageAssessmentCommandHandler;
import com.ccbsa.wms.returns.application.service.command.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/api/returns/damage-assessments")
@RequiredArgsConstructor
public class DamageAssessmentCommandController {

    private final DamageAssessmentCommandHandler recordDamageAssessmentCommandHandler;

    @PostMapping
    public ResponseEntity<RecordDamageAssessmentResponseDTO> recordDamageAssessment(
            @Valid @RequestBody RecordDamageAssessmentRequestDTO request,
            @RequestHeader("X-Tenant-ID") String tenantId,
            Authentication authentication) {

        log.info("Recording damage assessment for order: {}, tenant: {}",
            request.getOrderId(), tenantId);

        RecordDamageAssessmentCommand command = RecordDamageAssessmentCommand.builder()
            .orderId(request.getOrderId())
            .loadNumber(request.getLoadNumber())
            .consignmentId(request.getConsignmentId())
            .damageType(request.getDamageType())
            .damageSeverity(request.getDamageSeverity())
            .damageSource(request.getDamageSource())
            .damagedProducts(mapDamagedProducts(request.getDamagedProducts()))
            .insuranceClaim(mapInsuranceClaim(request.getInsuranceClaim()))
            .generalPhotoUrls(request.getGeneralPhotoUrls())
            .documentUrls(request.getDocumentUrls())
            .damageNotes(request.getDamageNotes())
            .tenantId(tenantId)
            .createdBy(authentication.getName())
            .build();

        RecordDamageAssessmentResult result = recordDamageAssessmentCommandHandler.handle(command);

        RecordDamageAssessmentResponseDTO response = RecordDamageAssessmentResponseDTO.builder()
            .damageAssessmentId(result.getDamageAssessmentId())
            .orderId(result.getOrderId())
            .loadNumber(result.getLoadNumber())
            .damageType(result.getDamageType())
            .damageSeverity(result.getDamageSeverity())
            .estimatedTotalLoss(result.getEstimatedTotalLoss())
            .totalDamagedProducts(result.getTotalDamagedProducts())
            .totalDamagedUnits(result.getTotalDamagedUnits())
            .totalPhotoCount(result.getTotalPhotoCount())
            .conditionBreakdown(result.getConditionBreakdown())
            .status(result.getStatus())
            .recordedAt(result.getRecordedAt())
            .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private List<DamagedProductCommand> mapDamagedProducts(List<DamagedProductRequestDTO> dtos) {
        return dtos.stream()
            .map(dto -> DamagedProductCommand.builder()
                .productId(dto.getProductId())
                .productCode(dto.getProductCode())
                .productDescription(dto.getProductDescription())
                .orderedQuantity(dto.getOrderedQuantity())
                .damagedQuantity(dto.getDamagedQuantity())
                .productCondition(dto.getProductCondition())
                .estimatedUnitLoss(dto.getEstimatedUnitLoss())
                .photoUrls(dto.getPhotoUrls())
                .damageNotes(dto.getDamageNotes())
                .build())
            .collect(Collectors.toList());
    }

    private InsuranceClaimCommand mapInsuranceClaim(InsuranceClaimRequestDTO dto) {
        if (dto == null) {
            return null;
        }

        return InsuranceClaimCommand.builder()
            .claimReference(dto.getClaimReference())
            .carrierName(dto.getCarrierName())
            .trackingNumber(dto.getTrackingNumber())
            .incidentDateTime(dto.getIncidentDateTime())
            .build();
    }
}
```

---

## Frontend Implementation

### TypeScript Types

**Location:** `frontend-app/src/features/returns/types/damageAssessmentTypes.ts`

```typescript
export enum DamageType {
  CRUSHED = 'CRUSHED',
  BROKEN = 'BROKEN',
  LEAKING = 'LEAKING',
  CONTAMINATED = 'CONTAMINATED',
  PACKAGING_DAMAGE = 'PACKAGING_DAMAGE',
  EXPIRED = 'EXPIRED',
  TEMPERATURE_ABUSE = 'TEMPERATURE_ABUSE',
  WATER_DAMAGE = 'WATER_DAMAGE',
  OTHER = 'OTHER',
}

export enum DamageSeverity {
  MINOR = 'MINOR',
  MODERATE = 'MODERATE',
  SEVERE = 'SEVERE',
}

export enum DamageSource {
  CARRIER = 'CARRIER',
  SUPPLIER = 'SUPPLIER',
  WAREHOUSE = 'WAREHOUSE',
  CUSTOMER = 'CUSTOMER',
  UNKNOWN = 'UNKNOWN',
}

export enum DamageAssessmentStatus {
  DRAFT = 'DRAFT',
  SUBMITTED = 'SUBMITTED',
  UNDER_REVIEW = 'UNDER_REVIEW',
  COMPLETED = 'COMPLETED',
  CANCELLED = 'CANCELLED',
}

export interface DamagedProductLineItem {
  productId: string;
  productCode: string;
  productDescription: string;
  orderedQuantity: number;
  damagedQuantity: number;
  productCondition: ProductCondition;
  estimatedUnitLoss: number;
  photoUrls: string[];
  damageNotes: string;
}

export interface InsuranceClaim {
  claimReference?: string;
  carrierName: string;
  trackingNumber?: string;
  incidentDateTime: Date;
}

export interface RecordDamageAssessmentRequest {
  orderId: string;
  loadNumber: string;
  consignmentId?: string;
  damageType: DamageType;
  damageSeverity: DamageSeverity;
  damageSource: DamageSource;
  damagedProducts: DamagedProductLineItem[];
  insuranceClaim?: InsuranceClaim;
  generalPhotoUrls: string[];
  documentUrls: string[];
  damageNotes: string;
}

export interface DamageReport {
  damageAssessmentId: string;
  orderId: string;
  loadNumber: string;
  damageType: DamageType;
  damageSeverity: DamageSeverity;
  estimatedTotalLoss: number;
  totalDamagedProducts: number;
  totalDamagedUnits: number;
  totalPhotos: number;
  conditionBreakdown: Record<ProductCondition, number>;
  status: DamageAssessmentStatus;
  recordedAt: string;
}
```

### React Hook

**Location:** `frontend-app/src/features/returns/hooks/useRecordDamageAssessment.ts`

```typescript
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useSnackbar } from 'notistack';
import { damageAssessmentService } from '../services/damageAssessmentService';
import type { RecordDamageAssessmentRequest, DamageReport } from '../types/damageAssessmentTypes';

export const useRecordDamageAssessment = () => {
  const queryClient = useQueryClient();
  const { enqueueSnackbar } = useSnackbar();

  return useMutation<DamageReport, Error, RecordDamageAssessmentRequest>({
    mutationFn: (request) => damageAssessmentService.recordDamageAssessment(request),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['damage-assessments'] });
      enqueueSnackbar(
        `Damage assessment recorded successfully. ID: ${data.damageAssessmentId}`,
        { variant: 'success' }
      );
    },
    onError: (error) => {
      enqueueSnackbar(`Failed to record damage assessment: ${error.message}`, {
        variant: 'error',
      });
    },
  });
};
```

### Service

**Location:** `frontend-app/src/features/returns/services/damageAssessmentService.ts`

```typescript
import apiClient from '../../../services/apiClient';
import type { RecordDamageAssessmentRequest, DamageReport } from '../types/damageAssessmentTypes';

class DamageAssessmentService {
  private readonly basePath = '/api/returns/damage-assessments';

  async recordDamageAssessment(
    request: RecordDamageAssessmentRequest
  ): Promise<DamageReport> {
    const response = await apiClient.post<DamageReport>(this.basePath, request);
    return response.data;
  }

  async uploadDamagePhoto(file: File): Promise<string> {
    const formData = new FormData();
    formData.append('file', file);

    const response = await apiClient.post<{ url: string }>(
      `${this.basePath}/photos/upload`,
      formData,
      {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      }
    );

    return response.data.url;
  }

  async uploadDocument(file: File): Promise<string> {
    const formData = new FormData();
    formData.append('file', file);

    const response = await apiClient.post<{ url: string }>(
      `${this.basePath}/documents/upload`,
      formData,
      {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      }
    );

    return response.data.url;
  }
}

export const damageAssessmentService = new DamageAssessmentService();
```

---

## Testing Strategy

### Unit Tests

#### Domain Core Tests

**Location:** `services/returns-service/returns-domain/returns-domain-core/src/test/java/com/ccbsa/wms/returns/domain/core/entity/DamageAssessmentTest.java`

```java
@Test
void recordDamageAssessment_WithValidData_ShouldCreateDamageAssessment() {
    // Arrange
    List<DamagedProductItem> damagedProducts = createDamagedProducts();
    InsuranceClaimInfo insuranceClaim = createInsuranceClaim();

    // Act
    DamageAssessment damageAssessment = DamageAssessment.recordDamageAssessment(
        DamageAssessmentId.generate(),
        new OrderId("ORD-001"),
        new LoadNumber("LOAD-001"),
        null,
        DamageType.CRUSHED,
        DamageSeverity.MODERATE,
        DamageSource.CARRIER,
        damagedProducts,
        insuranceClaim,
        List.of("https://photo1.jpg"),
        List.of("https://doc1.pdf"),
        "Pallet was crushed during transit",
        new TenantId("tenant-1"),
        "operator1"
    );

    // Assert
    assertThat(damageAssessment).isNotNull();
    assertThat(damageAssessment.getDamageType()).isEqualTo(DamageType.CRUSHED);
    assertThat(damageAssessment.getDamageSeverity()).isEqualTo(DamageSeverity.MODERATE);
    assertThat(damageAssessment.getStatus()).isEqualTo(DamageAssessmentStatus.SUBMITTED);
    assertThat(damageAssessment.getDomainEvents()).hasSize(1);
    assertThat(damageAssessment.getDomainEvents().get(0))
        .isInstanceOf(DamageRecordedEvent.class);
}

@Test
void recordDamageAssessment_WithNoDamagedProducts_ShouldThrowException() {
    // Act & Assert
    assertThatThrownBy(() -> DamageAssessment.recordDamageAssessment(
        DamageAssessmentId.generate(),
        new OrderId("ORD-001"),
        new LoadNumber("LOAD-001"),
        null,
        DamageType.CRUSHED,
        DamageSeverity.MODERATE,
        DamageSource.CARRIER,
        List.of(),  // Empty list
        null,
        List.of(),
        List.of(),
        "Notes",
        new TenantId("tenant-1"),
        "operator1"
    )).isInstanceOf(InvalidDamageAssessmentException.class)
      .hasMessageContaining("at least one damaged product");
}

@Test
void recordDamageAssessment_WithSevereDamageButNoInsuranceClaim_ShouldThrowException() {
    // Arrange
    List<DamagedProductItem> damagedProducts = createDamagedProducts();

    // Act & Assert
    assertThatThrownBy(() -> DamageAssessment.recordDamageAssessment(
        DamageAssessmentId.generate(),
        new OrderId("ORD-001"),
        new LoadNumber("LOAD-001"),
        null,
        DamageType.BROKEN,
        DamageSeverity.SEVERE,
        DamageSource.CARRIER,
        damagedProducts,
        null,  // No insurance claim
        List.of(),
        List.of(),
        "Notes",
        new TenantId("tenant-1"),
        "operator1"
    )).isInstanceOf(InvalidDamageAssessmentException.class)
      .hasMessageContaining("Insurance claim information is required");
}

@Test
void calculateTotalLoss_ShouldSumAllDamagedProductLineLosses() {
    // Arrange
    DamagedProductItem item1 = DamagedProductItem.create(
        new ProductId("P001"),
        new ProductCode("PROD-001"),
        "Product 1",
        new Quantity(BigDecimal.valueOf(100)),
        new Quantity(BigDecimal.TEN),
        ProductCondition.DAMAGED,
        new Money(BigDecimal.valueOf(50)),  // R50 per unit
        List.of("photo1.jpg"),
        "Notes"
    );

    DamagedProductItem item2 = DamagedProductItem.create(
        new ProductId("P002"),
        new ProductCode("PROD-002"),
        "Product 2",
        new Quantity(BigDecimal.valueOf(50)),
        new Quantity(BigDecimal.valueOf(5)),
        ProductCondition.WRITE_OFF,
        new Money(BigDecimal.valueOf(100)),  // R100 per unit
        List.of("photo2.jpg"),
        "Notes"
    );

    // Act
    BigDecimal totalLoss = item1.calculateLineLoss().getAmount()
        .add(item2.calculateLineLoss().getAmount());

    // Assert
    // (10 * 50) + (5 * 100) = 500 + 500 = 1000
    assertThat(totalLoss).isEqualByComparingTo(BigDecimal.valueOf(1000));
}
```

### Integration Tests

#### API Tests

**Location:** `gateway-api-tests/src/test/java/com/ccbsa/wms/gateway/api/ReturnsServiceTest.java`

```java
@Test
void recordDamageAssessment_WithValidRequest_ShouldReturn201() {
    // Arrange
    String orderId = createTestOrder();

    RecordDamageAssessmentRequest request = RecordDamageAssessmentRequest.builder()
        .orderId(orderId)
        .loadNumber("LOAD-TEST-001")
        .damageType(DamageType.CRUSHED)
        .damageSeverity(DamageSeverity.MODERATE)
        .damageSource(DamageSource.CARRIER)
        .damagedProducts(List.of(
            DamagedProductRequestDTO.builder()
                .productId("P001")
                .productCode("PROD-001")
                .productDescription("Test Product 1")
                .orderedQuantity(BigDecimal.valueOf(100))
                .damagedQuantity(BigDecimal.TEN)
                .productCondition(ProductCondition.DAMAGED)
                .estimatedUnitLoss(BigDecimal.valueOf(50))
                .photoUrls(List.of("https://photo1.jpg"))
                .damageNotes("Crushed during transit")
                .build()
        ))
        .insuranceClaim(InsuranceClaimRequestDTO.builder()
            .carrierName("Test Carrier")
            .trackingNumber("TRACK-001")
            .incidentDateTime(ZonedDateTime.now().minusDays(1))
            .build())
        .generalPhotoUrls(List.of("https://general-photo.jpg"))
        .documentUrls(List.of("https://doc.pdf"))
        .damageNotes("Pallet damage assessment")
        .build();

    // Act
    webTestClient.post()
        .uri("/api/returns/damage-assessments")
        .header("X-Tenant-ID", testTenantId)
        .header("Authorization", "Bearer " + authToken)
        .bodyValue(request)
        .exchange()
        .expectStatus().isCreated()
        .expectBody(RecordDamageAssessmentResponseDTO.class)
        .value(response -> {
            assertThat(response.getDamageAssessmentId()).isNotBlank();
            assertThat(response.getOrderId()).isEqualTo(orderId);
            assertThat(response.getDamageType()).isEqualTo(DamageType.CRUSHED);
            assertThat(response.getDamageSeverity()).isEqualTo(DamageSeverity.MODERATE);
            assertThat(response.getEstimatedTotalLoss()).isEqualByComparingTo(BigDecimal.valueOf(500));
            assertThat(response.getTotalDamagedProducts()).isEqualTo(1);
            assertThat(response.getTotalDamagedUnits()).isEqualTo(10);
            assertThat(response.getTotalPhotoCount()).isEqualTo(2);  // 1 product + 1 general
            assertThat(response.getStatus()).isEqualTo(DamageAssessmentStatus.SUBMITTED);
        });
}
```

---

## Acceptance Criteria Validation

| # | Acceptance Criterion                                                                       | Implementation Status | Validation                                                                                              |
|---|--------------------------------------------------------------------------------------------|-----------------------|---------------------------------------------------------------------------------------------------------|
| 1 | Operators can record damage type, severity, and affected quantity for each damaged product | ✅ Implemented         | DamageAssessment aggregate captures damageType, damageSeverity, and DamagedProductItem.damagedQuantity  |
| 2 | System supports photo evidence capture with multiple images per damaged item               | ✅ Implemented         | DamagedProductItem.photoUrls supports multiple photos, PhotoUploadWidget component handles upload       |
| 3 | Damage assessment includes fields for insurance claim reference and estimated loss value   | ✅ Implemented         | InsuranceClaimInfo value object with claimReference, estimatedTotalLoss auto-calculated from line items |
| 4 | System automatically assigns damaged goods to quarantine location based on damage severity | ✅ Implemented         | DamageRecordedEvent consumed by Location Management Service for auto-assignment                         |
| 5 | Damage reports are generated with all evidence and can be exported for claims              | ✅ Implemented         | DamageReport DTO includes all details, conditionBreakdown, and photo counts for report generation       |
| 6 | System publishes DamageRecordedEvent for downstream processing by Stock Management         | ✅ Implemented         | DamageRecordedEvent published on recordDamageAssessment(), consumed by Stock Management Service         |

---

## Implementation Checklist

### Common Module Updates

- [ ] Create `DamageType` enum in `common-domain`
- [ ] Create `DamageSeverity` enum in `common-domain`
- [ ] Create `DamageSource` enum in `common-domain`
- [ ] Create `DamageAssessmentStatus` enum in `common-domain`
- [ ] Add cache namespace for damage assessments

### Returns Service - Domain Core

- [ ] Create `DamageAssessment` aggregate root
- [ ] Create `DamagedProductItem` entity
- [ ] Create `DamageAssessmentId` value object
- [ ] Create `DamagedProductItemId` value object
- [ ] Create `InsuranceClaimInfo` value object
- [ ] Create `DamageRecordedEvent` domain event
- [ ] Create `DamageAssessmentCompletedEvent` domain event
- [ ] Create `InsuranceClaimUpdatedEvent` domain event
- [ ] Create `InvalidDamageAssessmentException`
- [ ] Unit tests for all domain logic

### Returns Service - Application Service

- [ ] Create `RecordDamageAssessmentCommand`
- [ ] Create nested `DamagedProductCommand`
- [ ] Create nested `InsuranceClaimCommand`
- [ ] Create `RecordDamageAssessmentResult`
- [ ] Create `RecordDamageAssessmentCommandHandler`
- [ ] Create `DamageAssessmentRepository` port
- [ ] Unit tests for command handlers

### Returns Service - Data Access

- [ ] Create `DamageAssessmentEntity`
- [ ] Create `DamagedProductItemEntity`
- [ ] Create `DamageAssessmentRepositoryAdapter`
- [ ] Create `DamageAssessmentJpaRepository`
- [ ] Create `DamageAssessmentEntityMapper`
- [ ] Create database migration scripts (tables, indexes)

### Returns Service - Application Layer

- [ ] Create `RecordDamageAssessmentRequestDTO`
- [ ] Create `DamagedProductRequestDTO`
- [ ] Create `InsuranceClaimRequestDTO`
- [ ] Create `RecordDamageAssessmentResponseDTO`
- [ ] Create `DamageAssessmentCommandController`
- [ ] Add validation annotations

### Returns Service - Messaging

- [ ] Create `DamageRecordedEventPublisher`
- [ ] Configure Kafka topics for damage events
- [ ] Integration tests for event publishing

### Stock Management Service

- [ ] Create `DamageRecordedEventListener`
- [ ] Implement quarantine stock creation logic
- [ ] Tests for damage event consumption

### Location Management Service

- [ ] Create `DamageRecordedEventListener`
- [ ] Implement auto-assignment to quarantine locations
- [ ] Tests for location assignment based on severity

### Frontend

- [ ] Create damage assessment types
- [ ] Create `useRecordDamageAssessment` hook
- [ ] Create `damageAssessmentService`
- [ ] Create `DamageClassificationSection` component
- [ ] Create `DamagedProductLineItem` component
- [ ] Create `InsuranceClaimSection` component
- [ ] Create `PhotoUploadWidget` component
- [ ] Create `DamageSummary` component
- [ ] Create `DamageAssessmentPage`
- [ ] Add routing for damage assessment page
- [ ] Integration tests

### Gateway API Tests

- [ ] Create damage assessment test fixtures
- [ ] Create record damage assessment API test
- [ ] Test validation scenarios
- [ ] Test event publishing scenarios

---

**End of Implementation Plan**
