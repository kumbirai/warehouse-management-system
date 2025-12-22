import { useState, useRef, useEffect, forwardRef } from 'react';
import { TextField, InputAdornment, IconButton, TextFieldProps } from '@mui/material';
import { QrCodeScanner as BarcodeIcon } from '@mui/icons-material';
import { BarcodeScanner } from '../../features/stock-management/components/BarcodeScanner';

interface BarcodeInputProps extends Omit<TextFieldProps, 'onChange' | 'value'> {
  value: string;
  onChange: (value: string) => void;
  onScan?: (barcode: string) => void;
  enableCamera?: boolean;
  autoSubmitOnEnter?: boolean;
}

/**
 * BarcodeInput component that supports both keyboard input (handheld scanners)
 * and camera-based barcode scanning.
 *
 * Handheld scanners typically act as keyboards, so they will automatically
 * populate the input field. The camera button allows users to scan barcodes
 * using their device's camera.
 */
export const BarcodeInput = forwardRef<HTMLInputElement, BarcodeInputProps>(
  (
    { value, onChange, onScan, enableCamera = true, autoSubmitOnEnter = false, ...textFieldProps },
    ref
  ) => {
    const [scannerOpen, setScannerOpen] = useState(false);
    const inputRef = useRef<HTMLInputElement>(null);

    // Handle barcode scan from camera
    const handleScan = (barcode: string) => {
      onChange(barcode);
      if (onScan) {
        onScan(barcode);
      }
      setScannerOpen(false);
    };

    // Handle Enter key press (for handheld scanners that send Enter after scanning)
    const handleKeyPress = (e: React.KeyboardEvent<HTMLInputElement>) => {
      if (e.key === 'Enter' && value.trim()) {
        if (autoSubmitOnEnter && onScan) {
          onScan(value.trim());
        }
        // Prevent form submission if this is in a form
        if (!autoSubmitOnEnter) {
          e.preventDefault();
        }
      }
    };

    // Use forwarded ref if provided, otherwise use internal ref
    // forwardRef can pass either a RefObject or a callback ref
    const actualRef = ref || inputRef;

    // Auto-focus on mount for better UX with handheld scanners
    useEffect(() => {
      if (textFieldProps.autoFocus) {
        const element = typeof actualRef === 'function' ? null : actualRef?.current;
        if (element) {
          element.focus();
        }
      }
    }, [textFieldProps.autoFocus, actualRef]);

    // Merge InputProps to preserve existing adornments
    const cameraButton = enableCamera ? (
      <IconButton
        edge="end"
        onClick={() => setScannerOpen(true)}
        aria-label="Scan barcode"
        size="small"
      >
        <BarcodeIcon />
      </IconButton>
    ) : null;

    // Combine endAdornment with camera button if both exist
    const existingEndAdornment = textFieldProps.InputProps?.endAdornment;
    const endAdornment =
      enableCamera && cameraButton ? (
        existingEndAdornment ? (
          <>
            {existingEndAdornment}
            {cameraButton}
          </>
        ) : (
          <InputAdornment position="end">{cameraButton}</InputAdornment>
        )
      ) : (
        existingEndAdornment
      );

    const mergedInputProps = {
      ...textFieldProps.InputProps,
      endAdornment,
    };

    return (
      <>
        <TextField
          {...textFieldProps}
          inputRef={actualRef}
          value={value}
          onChange={e => onChange(e.target.value)}
          onKeyPress={handleKeyPress}
          InputProps={mergedInputProps}
        />
        {enableCamera && (
          <BarcodeScanner
            open={scannerOpen}
            onClose={() => setScannerOpen(false)}
            onScan={handleScan}
          />
        )}
      </>
    );
  }
);

BarcodeInput.displayName = 'BarcodeInput';
