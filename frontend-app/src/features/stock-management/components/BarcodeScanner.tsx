import { useEffect, useRef, useState } from 'react';
import { BrowserMultiFormatReader } from '@zxing/library';
import {
  Box,
  Button,
  Dialog,
  DialogContent,
  DialogTitle,
  IconButton,
  Typography,
} from '@mui/material';
import { CameraAlt as CameraIcon, Close as CloseIcon } from '@mui/icons-material';
import { logger } from '../../../utils/logger';

interface BarcodeScannerProps {
  open: boolean;
  onClose: () => void;
  onScan: (barcode: string) => void;
}

export const BarcodeScanner = ({ open, onClose, onScan }: BarcodeScannerProps) => {
  const videoRef = useRef<HTMLVideoElement>(null);
  const [error, setError] = useState<string | null>(null);
  const [isScanning, setIsScanning] = useState(false);
  const codeReaderRef = useRef<BrowserMultiFormatReader | null>(null);

  useEffect(() => {
    if (!open) {
      return;
    }

    const codeReader = new BrowserMultiFormatReader();
    codeReaderRef.current = codeReader;

    const startScanning = async () => {
      try {
        setError(null);
        setIsScanning(true);

        // Get available video input devices
        const videoInputDevices = await codeReader.listVideoInputDevices();

        if (videoInputDevices.length === 0) {
          setError('No camera found. Please ensure your device has a camera.');
          setIsScanning(false);
          return;
        }

        // Use the first available camera (or user's preferred camera)
        const selectedDeviceId = videoInputDevices[0].deviceId;

        if (videoRef.current) {
          // Start decoding from video stream
          await codeReader.decodeFromVideoDevice(
            selectedDeviceId,
            videoRef.current,
            (result, err) => {
              if (result) {
                const barcode = result.getText();
                onScan(barcode);
                stopScanning();
                onClose();
              }
              if (err && !(err instanceof Error && err.message.includes('No QR Code'))) {
                // Ignore "No QR Code" errors as they're expected during scanning
                logger.debug('Barcode scanning error', {
                  error: err instanceof Error ? err.message : String(err),
                });
              }
            }
          );
        }
      } catch (err) {
        const errorMessage = err instanceof Error ? err.message : 'Failed to start camera';
        setError(errorMessage);
        setIsScanning(false);
        logger.error('Barcode scanner error', err instanceof Error ? err : new Error(String(err)), {
          errorMessage,
        });
      }
    };

    startScanning();

    return () => {
      stopScanning();
    };
  }, [open, onScan, onClose]);

  const stopScanning = () => {
    if (codeReaderRef.current) {
      codeReaderRef.current.reset();
      codeReaderRef.current = null;
    }
    setIsScanning(false);
  };

  const handleClose = () => {
    stopScanning();
    onClose();
  };

  return (
    <Dialog
      open={open}
      onClose={handleClose}
      maxWidth="sm"
      fullWidth
      sx={{ zIndex: 1300 }} // Ensure Dialog is above other content
    >
      <DialogTitle>
        <Box display="flex" justifyContent="space-between" alignItems="center">
          <Typography variant="h6">Scan Barcode</Typography>
          <IconButton onClick={handleClose} size="small">
            <CloseIcon />
          </IconButton>
        </Box>
      </DialogTitle>
      <DialogContent>
        <Box display="flex" flexDirection="column" alignItems="center" gap={2}>
          {error ? (
            <Box textAlign="center" p={2}>
              <Typography color="error">{error}</Typography>
              <Button
                variant="outlined"
                startIcon={<CameraIcon />}
                onClick={() => {
                  setError(null);
                  // Restart scanning by re-triggering useEffect
                  if (open) {
                    stopScanning();
                    setTimeout(() => {
                      // Force re-render to restart scanning
                      setIsScanning(false);
                    }, 100);
                  }
                }}
                sx={{ mt: 2 }}
              >
                Retry
              </Button>
            </Box>
          ) : (
            <>
              <Box
                sx={{
                  width: '100%',
                  maxWidth: 500,
                  aspectRatio: '1',
                  position: 'relative',
                  border: '2px solid',
                  borderColor: 'primary.main',
                  borderRadius: 1,
                  overflow: 'hidden',
                  backgroundColor: '#000',
                }}
              >
                <video
                  ref={videoRef}
                  style={{
                    width: '100%',
                    height: '100%',
                    objectFit: 'cover',
                  }}
                  playsInline
                />
              </Box>
              {isScanning && (
                <Typography variant="body2" color="text.secondary">
                  Point your camera at a barcode
                </Typography>
              )}
            </>
          )}
        </Box>
      </DialogContent>
    </Dialog>
  );
};
