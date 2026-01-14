/**
 * Damage Assessment Types
 */

export enum DamageType {
  PHYSICAL = 'PHYSICAL',
  WATER = 'WATER',
  TEMPERATURE = 'TEMPERATURE',
  CONTAMINATION = 'CONTAMINATION',
  OTHER = 'OTHER',
}

export enum DamageSeverity {
  MINOR = 'MINOR',
  MODERATE = 'MODERATE',
  SEVERE = 'SEVERE',
  TOTAL = 'TOTAL',
}

export enum DamageSource {
  TRANSPORT = 'TRANSPORT',
  HANDLING = 'HANDLING',
  STORAGE = 'STORAGE',
  MANUFACTURING = 'MANUFACTURING',
  OTHER = 'OTHER',
}

export enum DamageAssessmentStatus {
  DRAFT = 'DRAFT',
  SUBMITTED = 'SUBMITTED',
  UNDER_REVIEW = 'UNDER_REVIEW',
  COMPLETED = 'COMPLETED',
  CANCELLED = 'CANCELLED',
}

export interface DamagedProductItem {
  itemId: string;
  productId: string;
  damagedQuantity: number;
  damageType: DamageType;
  damageSeverity: DamageSeverity;
  damageSource: DamageSource;
  photoUrl?: string;
  notes?: string;
}

export interface InsuranceClaimInfo {
  claimNumber: string;
  insuranceCompany: string;
  claimStatus: string;
  claimAmount: number;
}

export interface DamageAssessment {
  assessmentId: string;
  orderNumber: string;
  damageType: DamageType;
  damageSeverity: DamageSeverity;
  damageSource: DamageSource;
  damagedProductItems: DamagedProductItem[];
  insuranceClaimInfo?: InsuranceClaimInfo;
  status: DamageAssessmentStatus;
  damageNotes?: string;
  recordedAt: string;
  createdAt: string;
  lastModifiedAt: string;
}

export interface RecordDamageAssessmentRequest {
  orderNumber: string;
  damageType: DamageType;
  damageSeverity: DamageSeverity;
  damageSource: DamageSource;
  damagedProductItems: DamagedProductItemRequest[];
  insuranceClaimInfo?: InsuranceClaimInfoRequest;
  damageNotes?: string;
}

export interface DamagedProductItemRequest {
  productId: string;
  damagedQuantity: number;
  damageType: DamageType;
  damageSeverity: DamageSeverity;
  damageSource: DamageSource;
  photoUrl?: string;
  notes?: string;
}

export interface InsuranceClaimInfoRequest {
  claimNumber: string;
  insuranceCompany: string;
  claimStatus: string;
  claimAmount: number;
}

export interface RecordDamageAssessmentResponse {
  assessmentId: string;
  orderNumber: string;
  status: DamageAssessmentStatus;
}
