import {z} from 'zod'; /**
 * Common validation schemas and utilities for forms across the application.
 */

/**
 * Common validation schemas and utilities for forms across the application.
 */

/**
 * Regex patterns for validation
 */
export const ValidationPatterns = {
  alphanumericDashUnderscore: /^[a-zA-Z0-9-_]+$/,
  alphanumeric: /^[a-zA-Z0-9]+$/,
  email: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
  phone: /^\+?[1-9]\d{1,14}$/,
  postalCode: /^[A-Z0-9]{3,10}$/i,
  username: /^[a-zA-Z0-9._-]{3,30}$/,
  password: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$/,
  tenantId: /^[a-zA-Z0-9-_]+$/,
  uuid: /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i,
};

/**
 * Validation error messages
 */
export const ValidationMessages = {
  required: 'This field is required',
  email: 'Please enter a valid email address',
  phone: 'Please enter a valid phone number',
  alphanumeric: 'Only letters and numbers are allowed',
  alphanumericDashUnderscore: 'Only letters, numbers, hyphens, and underscores are allowed',
  minLength: (min: number) => `Must be at least ${min} characters`,
  maxLength: (max: number) => `Must be no more than ${max} characters`,
  min: (min: number) => `Must be at least ${min}`,
  max: (max: number) => `Must be no more than ${max}`,
  password:
    'Password must be at least 8 characters and include uppercase, lowercase, number, and special character',
  username:
    'Username must be 3-30 characters and contain only letters, numbers, dots, hyphens, and underscores',
  tenantId: 'Tenant ID can only contain letters, numbers, hyphens, and underscores',
  positiveNumber: 'Must be a positive number',
  integer: 'Must be a whole number',
};

/**
 * Common Zod schemas for reuse
 */
export const CommonSchemas = {
  email: z.string().min(1, ValidationMessages.required).email(ValidationMessages.email),

  tenantId: z
    .string()
    .min(1, ValidationMessages.required)
    .regex(ValidationPatterns.tenantId, ValidationMessages.tenantId),

  username: z
    .string()
    .min(3, ValidationMessages.minLength(3))
    .max(30, ValidationMessages.maxLength(30))
    .regex(ValidationPatterns.username, ValidationMessages.username),

  password: z
    .string()
    .min(8, ValidationMessages.minLength(8))
    .regex(ValidationPatterns.password, ValidationMessages.password),

  phoneOptional: z
    .string()
    .regex(ValidationPatterns.phone, ValidationMessages.phone)
    .optional()
    .or(z.literal('')),

  positiveInteger: z
    .number()
    .int(ValidationMessages.integer)
    .positive(ValidationMessages.positiveNumber),

  nonNegativeInteger: z.number().int(ValidationMessages.integer).min(0, ValidationMessages.min(0)),

  positiveDecimal: z.number().positive(ValidationMessages.positiveNumber),

  nonNegativeDecimal: z.number().min(0, ValidationMessages.min(0)),

  uuid: z.string().regex(ValidationPatterns.uuid, 'Invalid UUID format'),

  requiredString: z.string().min(1, ValidationMessages.required),

  optionalString: z.string().optional().or(z.literal('')),
};

/**
 * Form error type - matches react-hook-form error structure
 */
interface FormError {
  message?: string;
  type?: string;
}

/**
 * Utility function to check if a form has any errors
 */
export const hasFormErrors = (errors: Record<string, FormError | undefined>): boolean => {
  return Object.keys(errors).length > 0;
};

/**
 * Utility function to get all error messages from a form
 */
export const getFormErrorMessages = (errors: Record<string, FormError | undefined>): string[] => {
  return Object.values(errors)
    .filter((error): error is FormError => error !== undefined && error?.message !== undefined)
    .map(error => error.message as string);
};
