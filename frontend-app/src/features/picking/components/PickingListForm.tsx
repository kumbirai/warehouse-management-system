import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Grid,
  IconButton,
  Paper,
  Step,
  StepLabel,
  Stepper,
  TextField,
  Typography,
} from '@mui/material';
import { useCallback, useEffect, useState } from 'react';
import { z } from 'zod';
import { Controller, useFieldArray, useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import AddIcon from '@mui/icons-material/Add';
import DeleteIcon from '@mui/icons-material/Delete';
import SaveIcon from '@mui/icons-material/Save';
import { CreatePickingListRequest } from '../types/pickingTypes';
import { OrderFormSection } from './OrderFormSection';

const orderLineItemSchema = z.object({
  productCode: z.string().min(1, 'Product code is required'),
  quantity: z.number().positive('Quantity must be positive').int('Quantity must be an integer'),
  notes: z.string().max(500, 'Notes cannot exceed 500 characters').optional(),
});

const orderSchema = z.object({
  orderNumber: z
    .string()
    .min(1, 'Order number is required')
    .max(50, 'Order number cannot exceed 50 characters'),
  customerCode: z
    .string()
    .min(1, 'Customer code is required')
    .max(50, 'Customer code cannot exceed 50 characters'),
  customerName: z.string().max(200, 'Customer name cannot exceed 200 characters').optional(),
  priority: z.enum(['HIGH', 'NORMAL', 'LOW']),
  lineItems: z.array(orderLineItemSchema).min(1, 'At least one line item is required'),
});

const loadSchema = z.object({
  loadNumber: z
    .string()
    .min(1, 'Load number is required')
    .max(50, 'Load number cannot exceed 50 characters'),
  orders: z.array(orderSchema).min(1, 'At least one order is required'),
});

const pickingListSchema = z.object({
  loads: z.array(loadSchema).min(1, 'At least one load is required'),
  notes: z.string().max(1000, 'Notes cannot exceed 1000 characters').optional(),
});

export type PickingListFormValues = z.infer<typeof pickingListSchema>;

interface PickingListFormProps {
  onSubmit: (values: CreatePickingListRequest) => Promise<void> | void;
  isSubmitting?: boolean;
}

const STEPS = ['Load Information', 'Orders', 'Review'];

export const PickingListForm = ({ onSubmit, isSubmitting = false }: PickingListFormProps) => {
  const [activeStep, setActiveStep] = useState(0);
  const [draftSaved, setDraftSaved] = useState(false);

  const {
    control,
    handleSubmit,
    watch,
    formState: { errors },
    trigger,
  } = useForm<PickingListFormValues>({
    resolver: zodResolver(pickingListSchema),
    defaultValues: {
      loads: [
        {
          loadNumber: '',
          orders: [
            {
              orderNumber: '',
              customerCode: '',
              customerName: '',
              priority: 'NORMAL',
              lineItems: [{ productCode: '', quantity: 1, notes: '' }],
            },
          ],
        },
      ],
      notes: '',
    },
  });

  const {
    fields: loadFields,
    append: appendLoad,
    remove: removeLoad,
  } = useFieldArray({
    control,
    name: 'loads',
  });

  const loads = watch('loads');

  // Load draft from localStorage on mount
  useEffect(() => {
    const draft = localStorage.getItem('pickingListDraft');
    if (draft) {
      try {
        JSON.parse(draft);
        // You would need to set the form values here
        // For now, we'll just indicate a draft exists
      } catch (e) {
        // Invalid draft, ignore
      }
    }
  }, []);

  // Save draft to localStorage
  const saveDraft = useCallback(() => {
    const formValues = watch();
    localStorage.setItem('pickingListDraft', JSON.stringify(formValues));
    setDraftSaved(true);
    setTimeout(() => setDraftSaved(false), 2000);
  }, [watch]);

  const handleNext = async () => {
    let isValid = false;
    if (activeStep === 0) {
      isValid = await trigger('loads');
    } else if (activeStep === 1) {
      isValid = await trigger('loads');
    }

    if (isValid) {
      setActiveStep(prevActiveStep => prevActiveStep + 1);
    }
  };

  const handleBack = () => {
    setActiveStep(prevActiveStep => prevActiveStep - 1);
  };

  const handleFormSubmit = async (values: PickingListFormValues) => {
    // Clear draft on successful submit
    localStorage.removeItem('pickingListDraft');
    await onSubmit(values);
  };

  const addLoad = () => {
    appendLoad({
      loadNumber: '',
      orders: [
        {
          orderNumber: '',
          customerCode: '',
          customerName: '',
          priority: 'NORMAL',
          lineItems: [{ productCode: '', quantity: 1, notes: '' }],
        },
      ],
    });
  };

  const removeLoadAtIndex = (index: number) => {
    if (loadFields.length > 1) {
      removeLoad(index);
    }
  };

  const renderLoadInfoStep = () => (
    <Box>
      <Typography variant="h6" gutterBottom>
        Load Information
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        Enter load information. You can add multiple loads.
      </Typography>

      {loadFields.map((loadField, loadIndex) => (
        <Card key={loadField.id} sx={{ mb: 2 }}>
          <CardContent>
            <Box
              sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}
            >
              <Typography variant="subtitle1">Load {loadIndex + 1}</Typography>
              {loadFields.length > 1 && (
                <IconButton
                  onClick={() => removeLoadAtIndex(loadIndex)}
                  color="error"
                  aria-label={`Remove load ${loadIndex + 1}`}
                >
                  <DeleteIcon />
                </IconButton>
              )}
            </Box>

            <Grid container spacing={2}>
              <Grid item xs={12} sm={6}>
                <Controller
                  name={`loads.${loadIndex}.loadNumber`}
                  control={control}
                  render={({ field }) => (
                    <TextField
                      {...field}
                      label="Load Number"
                      fullWidth
                      required
                      error={!!errors.loads?.[loadIndex]?.loadNumber}
                      helperText={errors.loads?.[loadIndex]?.loadNumber?.message}
                      aria-label={`Load ${loadIndex + 1} number input`}
                    />
                  )}
                />
              </Grid>
            </Grid>
          </CardContent>
        </Card>
      ))}

      <Button
        variant="outlined"
        startIcon={<AddIcon />}
        onClick={addLoad}
        sx={{ mt: 2 }}
        aria-label="Add another load"
      >
        Add Load
      </Button>
    </Box>
  );

  const renderOrdersStep = () => (
    <Box>
      <Typography variant="h6" gutterBottom>
        Orders
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        Add orders to each load. Each order must have at least one line item.
      </Typography>

      {loadFields.map((loadField, loadIndex) => (
        <OrderFormSection
          key={loadField.id}
          control={control}
          loadIndex={loadIndex}
          loadNumber={loads[loadIndex]?.loadNumber || ''}
          errors={errors}
        />
      ))}
    </Box>
  );

  const renderReviewStep = () => {
    const formValues = watch();
    return (
      <Box>
        <Typography variant="h6" gutterBottom>
          Review
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
          Review your picking list before submitting.
        </Typography>

        <Grid container spacing={2} sx={{ mb: 3 }}>
          <Grid item xs={12}>
            <Controller
              name="notes"
              control={control}
              render={({ field }) => (
                <TextField
                  {...field}
                  label="Notes (Optional)"
                  fullWidth
                  multiline
                  rows={3}
                  error={!!errors.notes}
                  helperText={errors.notes?.message}
                />
              )}
            />
          </Grid>
        </Grid>

        {formValues.loads.map((load, loadIndex) => (
          <Card key={loadIndex} sx={{ mb: 2 }}>
            <CardContent>
              <Typography variant="subtitle1" gutterBottom>
                Load {loadIndex + 1}: {load.loadNumber}
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                {load.orders.length} order(s)
              </Typography>
              {load.orders.map((order, orderIndex) => (
                <Box
                  key={orderIndex}
                  sx={{ mb: 2, pl: 2, borderLeft: '2px solid', borderColor: 'divider' }}
                >
                  <Typography variant="subtitle2">
                    Order {orderIndex + 1}: {order.orderNumber}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    Customer: {order.customerCode} {order.customerName && `(${order.customerName})`}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    Priority: {order.priority} | Line Items: {order.lineItems.length}
                  </Typography>
                </Box>
              ))}
            </CardContent>
          </Card>
        ))}
      </Box>
    );
  };

  const getStepContent = (step: number) => {
    switch (step) {
      case 0:
        return renderLoadInfoStep();
      case 1:
        return renderOrdersStep();
      case 2:
        return renderReviewStep();
      default:
        return null;
    }
  };

  return (
    <Paper sx={{ p: 3 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h6">Create Picking List</Typography>
        <Button
          variant="outlined"
          startIcon={<SaveIcon />}
          onClick={saveDraft}
          aria-label="Save draft"
        >
          Save Draft
        </Button>
      </Box>

      {draftSaved && (
        <Alert severity="success" sx={{ mb: 2 }}>
          Draft saved successfully
        </Alert>
      )}

      <Stepper activeStep={activeStep} sx={{ mb: 4 }}>
        {STEPS.map(label => (
          <Step key={label}>
            <StepLabel>{label}</StepLabel>
          </Step>
        ))}
      </Stepper>

      <form onSubmit={handleSubmit(handleFormSubmit)}>
        {getStepContent(activeStep)}

        <Box sx={{ display: 'flex', justifyContent: 'space-between', mt: 4 }}>
          <Button
            disabled={activeStep === 0}
            onClick={handleBack}
            aria-label="Go back to previous step"
          >
            Back
          </Button>
          <Box>
            {activeStep < STEPS.length - 1 ? (
              <Button variant="contained" onClick={handleNext} aria-label="Continue to next step">
                Next
              </Button>
            ) : (
              <Button
                type="submit"
                variant="contained"
                disabled={isSubmitting}
                aria-label="Submit picking list"
              >
                {isSubmitting ? 'Submitting...' : 'Submit'}
              </Button>
            )}
          </Box>
        </Box>
      </form>
    </Paper>
  );
};
