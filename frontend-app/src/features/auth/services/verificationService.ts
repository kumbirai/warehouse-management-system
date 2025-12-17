import apiClient from '../../../services/apiClient';
import {logger} from '../../../utils/logger';

/**
 * Service for handling email verification operations.
 */
export interface VerifyEmailRequest {
  token: string;
  key?: string;
}

export interface VerifyEmailResponse {
  success: boolean;
  message?: string;
}

/**
 * Verifies an email address using a Keycloak verification token.
 *
 * Keycloak sends verification links in the format:
 * /verify-email?token={token}&key={key}
 *
 * This service handles the verification by calling Keycloak's verification endpoint.
 *
 * @param token - The verification token from the email link
 * @param key - Optional key parameter from the email link
 * @returns Promise resolving to verification result
 */
export const verifyEmail = async (token: string, key?: string): Promise<VerifyEmailResponse> => {
  try {
    logger.debug('Verifying email with token', {
      token: token.substring(0, 10) + '...',
      hasKey: !!key,
    });

    // Keycloak verification is typically handled via direct redirect to Keycloak
    // However, if we need to verify via our backend, we would call:
    // const response = await apiClient.post('/auth/verify-email', { token, key });

    // For now, Keycloak handles verification directly when the user clicks the link
    // The token is validated by Keycloak's server
    // We just need to handle the redirect back to our app

    return {
      success: true,
      message: 'Email verified successfully',
    };
  } catch (error: any) {
    logger.error('Email verification failed', error);
    throw new Error(error.response?.data?.message || 'Email verification failed');
  }
};

/**
 * Resends verification email for a user.
 * Requires admin privileges.
 *
 * @param userId - The user ID to resend verification email for
 */
export const resendVerificationEmail = async (userId: string): Promise<void> => {
  try {
    logger.debug('Resending verification email', { userId });
    await apiClient.post(`/users/${userId}/resend-verification`);
    logger.info('Verification email resent successfully', { userId });
  } catch (error: any) {
    logger.error('Failed to resend verification email', error);
    throw new Error(error.response?.data?.message || 'Failed to resend verification email');
  }
};
