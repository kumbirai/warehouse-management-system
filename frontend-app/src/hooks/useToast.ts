import { useCallback } from 'react';
import { toast, ToastOptions } from 'react-toastify';

export const useToast = () => {
  const showSuccess = useCallback((message: string, options?: ToastOptions) => {
    toast.success(message, {
      position: 'top-right',
      autoClose: 5000,
      hideProgressBar: false,
      closeOnClick: true,
      pauseOnHover: true,
      draggable: true,
      ...options,
    });
  }, []);

  const showError = useCallback((message: string, options?: ToastOptions) => {
    toast.error(message, {
      position: 'top-right',
      autoClose: 7000,
      hideProgressBar: false,
      closeOnClick: true,
      pauseOnHover: true,
      draggable: true,
      ...options,
    });
  }, []);

  const showInfo = useCallback((message: string, options?: ToastOptions) => {
    toast.info(message, {
      position: 'top-right',
      autoClose: 5000,
      hideProgressBar: false,
      closeOnClick: true,
      pauseOnHover: true,
      draggable: true,
      ...options,
    });
  }, []);

  const showWarning = useCallback((message: string, options?: ToastOptions) => {
    toast.warning(message, {
      position: 'top-right',
      autoClose: 6000,
      hideProgressBar: false,
      closeOnClick: true,
      pauseOnHover: true,
      draggable: true,
      ...options,
    });
  }, []);

  return {
    success: showSuccess,
    error: showError,
    info: showInfo,
    warning: showWarning,
  };
};
