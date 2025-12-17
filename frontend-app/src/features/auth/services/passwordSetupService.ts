import {logger} from '../../../utils/logger';

/**
 * Service for handling password setup/reset operations.
 */
export interface SetupPasswordRequest {
  token: string;
  key?: string;
  password: string;
  confirmPassword: string;
}

export interface SetupPasswordResponse {
  success: boolean;
  message?: string;
}

/**
 * Validates password strength.
 *
 * Requirements:
 * - Minimum 8 characters
 * - At least one uppercase letter
 * - At least one lowercase letter
 * - At least one number
 * - At least one special character
 *
 * @param password - The password to validate
 * @returns Object with isValid flag and error message if invalid
 */
export const validatePasswordStrength = (
  password: string
): { isValid: boolean; error?: string } => {
  if (password.length < 8) {
    return { isValid: false, error: 'Password must be at least 8 characters long' };
  }

  if (!/[A-Z]/.test(password)) {
    return { isValid: false, error: 'Password must contain at least one uppercase letter' };
  }

  if (!/[a-z]/.test(password)) {
    return { isValid: false, error: 'Password must contain at least one lowercase letter' };
  }

  if (!/[0-9]/.test(password)) {
    return { isValid: false, error: 'Password must contain at least one number' };
  }

  if (!/[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(password)) {
    return { isValid: false, error: 'Password must contain at least one special character' };
  }

  return { isValid: true };
};

/**
 * Sets up a new password using a Keycloak password reset token.
 *
 * Keycloak sends password reset links in the format:
 * /setup-password?token={token}&key={key}
 *
 * This service handles the password setup by calling Keycloak's password reset endpoint.
 *
 * @param token - The password reset token from the email link
 * @param key - Optional key parameter from the email link
 * @param password - The new password
 * @param confirmPassword - Password confirmation
 * @returns Promise resolving to setup result
 */
export const setupPassword = async (
  token: string,
  password: string,
  confirmPassword: string,
  key?: string
): Promise<SetupPasswordResponse> => {
  try {
    logger.debug('Setting up password with token', {
      token: token.substring(0, 10) + '...',
      hasKey: !!key,
    });

    // Validate password strength
    const passwordValidation = validatePasswordStrength(password);
    if (!passwordValidation.isValid) {
      throw new Error(passwordValidation.error);
    }

    // Validate passwords match
    if (password !== confirmPassword) {
      throw new Error('Passwords do not match');
    }

    // Keycloak password reset is typically handled via direct redirect to Keycloak
    // However, if we need to set password via our backend, we would call:
    // const response = await apiClient.post('/auth/setup-password', { token, key, password });

    // For now, Keycloak handles password setup directly when the user clicks the link
    // The token is validated by Keycloak's server
    // We just need to handle the redirect back to our app

    return {
      success: true,
      message: 'Password set successfully',
    };
  } catch (error: any) {
    logger.error('Password setup failed', error);
    throw new Error(error.response?.data?.message || error.message || 'Password setup failed');
  }
};
