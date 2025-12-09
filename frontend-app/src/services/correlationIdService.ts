/**
 * Correlation ID service for request traceability.
 *
 * Generates and manages correlation IDs for tracking requests across
 * the frontend and backend services. Supports both session-level and
 * request-level correlation IDs.
 */

import {logger} from '../utils/logger';

const CORRELATION_ID_STORAGE_KEY = 'wms_correlation_id';
const CORRELATION_ID_HEADER = 'X-Correlation-Id';

/**
 * Generates a new correlation ID (UUID v4)
 */
const generateCorrelationId = (): string => {
    return crypto.randomUUID();
};

/**
 * Gets or creates a correlation ID for the current session.
 *
 * Strategy:
 * - First call: Generate new correlation ID and store in sessionStorage
 * - Subsequent calls: Return existing correlation ID from sessionStorage
 * - On new session: Generate new correlation ID
 *
 * @returns Correlation ID string
 */
export const getCorrelationId = (): string => {
    // Try to get existing correlation ID from sessionStorage
    if (typeof window !== 'undefined') {
        const stored = sessionStorage.getItem(CORRELATION_ID_STORAGE_KEY);
        if (stored) {
            return stored;
        }
    }

    // Generate new correlation ID
    const correlationId = generateCorrelationId();

    // Store in sessionStorage for persistence across page reloads
    if (typeof window !== 'undefined') {
        sessionStorage.setItem(CORRELATION_ID_STORAGE_KEY, correlationId);
        logger.debug('Generated new correlation ID', {correlationId});
    }

    return correlationId;
};

/**
 * Generates a new correlation ID for a specific request flow.
 * Useful for tracking multi-step operations that should have their own correlation ID.
 *
 * @returns New correlation ID (not stored in sessionStorage)
 */
export const generateRequestCorrelationId = (): string => {
    const correlationId = generateCorrelationId();
    logger.debug('Generated request-level correlation ID', {correlationId});
    return correlationId;
};

/**
 * Clears the session correlation ID.
 * Useful for logout or session reset scenarios.
 */
export const clearCorrelationId = (): void => {
    if (typeof window !== 'undefined') {
        sessionStorage.removeItem(CORRELATION_ID_STORAGE_KEY);
        logger.debug('Cleared correlation ID');
    }
};

/**
 * Gets the correlation ID header name.
 *
 * @returns Header name string
 */
export const getCorrelationIdHeader = (): string => {
    return CORRELATION_ID_HEADER;
};

export const correlationIdService = {
    getCorrelationId,
    generateRequestCorrelationId,
    clearCorrelationId,
    getCorrelationIdHeader,
};

