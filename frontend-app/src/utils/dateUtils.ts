/**
 * Date formatting utilities for consistent date display across the application.
 */

/**
 * Formats a date string to a localized date-time string.
 * Handles null, undefined, and invalid date strings gracefully.
 *
 * @param dateString - Date string to format (ISO 8601 format expected)
 * @param options - Intl.DateTimeFormatOptions for customization
 * @returns Formatted date string or fallback text if date is invalid
 */
export function formatDateTime(
  dateString: string | null | undefined,
  options: Intl.DateTimeFormatOptions = {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }
): string {
  if (!dateString) {
    if (import.meta.env.DEV) {
      console.debug('[formatDateTime] dateString is null/undefined');
    }
    return '—';
  }

  try {
    // Handle array format [year, month, day, hour, minute, second, nanosecond]
    // that Jackson might send if WRITE_DATES_AS_TIMESTAMPS is enabled
    if (Array.isArray(dateString)) {
      if (import.meta.env.DEV) {
        console.warn('[formatDateTime] Received array format date:', dateString);
      }
      // Convert array to ISO string: [2025, 12, 8, 10, 11, 40] -> "2025-12-08T10:11:40"
      const [year, month, day, hour = 0, minute = 0, second = 0] = dateString;
      const isoString = `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}T${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}:${String(second).padStart(2, '0')}`;
      const date = new Date(isoString);
      if (isNaN(date.getTime())) {
        return '—';
      }
      return date.toLocaleString(undefined, options);
    }

    const date = new Date(dateString);

    // Check if date is valid
    if (isNaN(date.getTime())) {
      if (import.meta.env.DEV) {
        console.warn(
          '[formatDateTime] Invalid date string:',
          dateString,
          'Type:',
          typeof dateString
        );
      }
      return '—';
    }

    return date.toLocaleString(undefined, options);
  } catch (error) {
    // Fallback for any parsing errors
    if (import.meta.env.DEV) {
      console.error('[formatDateTime] Error parsing date:', dateString, error);
    }
    return '—';
  }
}

/**
 * Formats a date string to a localized date string (without time).
 *
 * @param dateString - Date string to format
 * @returns Formatted date string or fallback text if date is invalid
 */
export function formatDate(dateString: string | null | undefined): string {
  return formatDateTime(dateString, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  });
}

/**
 * Formats a date string to a localized time string (without date).
 *
 * @param dateString - Date string to format
 * @returns Formatted time string or fallback text if date is invalid
 */
export function formatTime(dateString: string | null | undefined): string {
  return formatDateTime(dateString, {
    hour: '2-digit',
    minute: '2-digit',
  });
}
