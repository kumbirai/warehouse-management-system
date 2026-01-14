/**
 * Reconciliation Types
 */

export enum D365ReconciliationStatus {
  PENDING = 'PENDING',
  IN_PROGRESS = 'IN_PROGRESS',
  SUCCESS = 'SUCCESS',
  FAILED = 'FAILED',
  RETRYING = 'RETRYING',
}

export interface ReconciliationRecord {
  returnId: string;
  orderNumber: string;
  d365ReturnOrderId?: string;
  reconciliationStatus: D365ReconciliationStatus;
  inventoryAdjusted: boolean;
  creditNoteId?: string;
  writeOffProcessed: boolean;
  lastSyncAttempt?: string;
  lastSyncError?: string;
  createdAt: string;
  lastModifiedAt: string;
}

export interface ReconciliationSummary {
  totalReturns: number;
  pendingReconciliation: number;
  inProgress: number;
  successful: number;
  failed: number;
  retrying: number;
}

export interface ReconciliationListFilters {
  tenantId?: string;
  status?: D365ReconciliationStatus;
  page?: number;
  size?: number;
  search?: string;
}
