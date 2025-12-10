/**
 * Production-grade logging utility.
 * Provides structured logging with environment-aware log levels.
 * Automatically includes correlation ID in all log entries for traceability.
 */

import { correlationIdService } from '../services/correlationIdService';

type LogLevel = 'debug' | 'info' | 'warn' | 'error';

const isDevelopment = import.meta.env.DEV;
const isProduction = import.meta.env.PROD;

/**
 * Logger configuration
 */
const config = {
  enableConsole: isDevelopment || !isProduction, // Enable in dev, disable in prod unless needed
  enableRemote: isProduction, // Enable remote logging in production
  minLevel: isDevelopment ? 'debug' : ('warn' as LogLevel),
};

/**
 * Log levels priority
 */
const logLevels: Record<LogLevel, number> = {
  debug: 0,
  info: 1,
  warn: 2,
  error: 3,
};

/**
 * Check if log level should be logged
 */
const shouldLog = (level: LogLevel): boolean => {
  return logLevels[level] >= logLevels[config.minLevel];
};

/**
 * Format log message with context
 * Automatically includes correlation ID for traceability
 */
const formatMessage = (
  level: LogLevel,
  message: string,
  context?: Record<string, unknown>
): string => {
  const timestamp = new Date().toISOString();

  // Include correlation ID in all log entries for traceability
  const correlationId =
    typeof window !== 'undefined' ? correlationIdService.getCorrelationId() : undefined;

  const enhancedContext = {
    ...context,
    ...(correlationId ? { correlationId } : {}),
  };

  const contextStr =
    enhancedContext && Object.keys(enhancedContext).length > 0
      ? ` ${JSON.stringify(enhancedContext)}`
      : '';
  return `[${timestamp}] [${level.toUpperCase()}] ${message}${contextStr}`;
};

/**
 * Send log to remote logging service (e.g., Sentry, LogRocket)
 * This is a placeholder - implement based on your logging service
 */
const sendToRemote = (
  level: LogLevel,
  message: string,
  context?: Record<string, unknown>
): void => {
  if (!config.enableRemote) {
    return;
  }

  // TODO: Integrate with your logging service (Sentry, LogRocket, etc.)
  // Example:
  // if (level === 'error') {
  //     Sentry.captureException(new Error(message), { extra: context });
  // } else {
  //     Sentry.addBreadcrumb({ message, level, data: context });
  // }

  // Placeholder implementation - parameters are intentionally unused until remote logging is integrated
  // Using void to explicitly mark as intentionally unused
  void level;
  void message;
  void context;
};

/**
 * Production-grade logger
 */
export const logger = {
  debug: (message: string, context?: Record<string, unknown>): void => {
    if (!shouldLog('debug')) {
      return;
    }
    const formatted = formatMessage('debug', message, context);
    if (config.enableConsole) {
      console.debug(formatted);
    }
    sendToRemote('debug', message, context);
  },

  info: (message: string, context?: Record<string, unknown>): void => {
    if (!shouldLog('info')) {
      return;
    }
    const formatted = formatMessage('info', message, context);
    if (config.enableConsole) {
      console.info(formatted);
    }
    sendToRemote('info', message, context);
  },

  warn: (message: string, context?: Record<string, unknown>): void => {
    if (!shouldLog('warn')) {
      return;
    }
    const formatted = formatMessage('warn', message, context);
    if (config.enableConsole) {
      console.warn(formatted);
    }
    sendToRemote('warn', message, context);
  },

  error: (message: string, error?: Error | unknown, context?: Record<string, unknown>): void => {
    if (!shouldLog('error')) {
      return;
    }
    const errorContext = {
      ...context,
      ...(error instanceof Error
        ? {
            errorMessage: error.message,
            errorStack: error.stack,
            errorName: error.name,
          }
        : { error: String(error) }),
    };
    const formatted = formatMessage('error', message, errorContext);
    if (config.enableConsole) {
      console.error(formatted, error);
    }
    sendToRemote('error', message, errorContext);
  },
};
