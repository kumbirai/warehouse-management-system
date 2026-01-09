import {
  Box,
  Button,
  Card,
  CardContent,
  FormControl,
  Grid,
  IconButton,
  InputLabel,
  MenuItem,
  Select,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material';
import { Control, Controller, FieldErrors, useFieldArray } from 'react-hook-form';
import AddIcon from '@mui/icons-material/Add';
import DeleteIcon from '@mui/icons-material/Delete';
import { PickingListFormValues } from './PickingListForm';

interface OrderFormSectionProps {
  control: Control<PickingListFormValues>;
  loadIndex: number;
  loadNumber: string;
  errors?: FieldErrors<PickingListFormValues>;
}

export const OrderFormSection = ({
  control,
  loadIndex,
  loadNumber,
  errors,
}: OrderFormSectionProps) => {
  const {
    fields: orderFields,
    append: appendOrder,
    remove: removeOrder,
  } = useFieldArray({
    control,
    name: `loads.${loadIndex}.orders`,
  });

  return (
    <Card sx={{ mb: 3 }}>
      <CardContent>
        <Typography variant="subtitle1" gutterBottom>
          Load {loadIndex + 1}: {loadNumber || 'Unnamed'}
        </Typography>

        {orderFields.map((orderField, orderIndex) => (
          <OrderItem
            key={orderField.id}
            control={control}
            loadIndex={loadIndex}
            orderIndex={orderIndex}
            orderFieldsLength={orderFields.length}
            onRemove={() => removeOrder(orderIndex)}
            errors={errors}
          />
        ))}

        <Button
          variant="outlined"
          startIcon={<AddIcon />}
          onClick={() =>
            appendOrder({
              orderNumber: '',
              customerCode: '',
              customerName: '',
              priority: 'NORMAL',
              lineItems: [{ productCode: '', quantity: 1, notes: '' }],
            })
          }
          sx={{ mt: 2 }}
          aria-label="Add order"
        >
          Add Order
        </Button>
      </CardContent>
    </Card>
  );
};

interface OrderItemProps {
  control: Control<PickingListFormValues>;
  loadIndex: number;
  orderIndex: number;
  orderFieldsLength: number;
  onRemove: () => void;
  errors?: FieldErrors<PickingListFormValues>;
}

const OrderItem = ({
  control,
  loadIndex,
  orderIndex,
  orderFieldsLength,
  onRemove,
  errors,
}: OrderItemProps) => {
  const {
    fields: lineItemFields,
    append: appendLineItem,
    remove: removeLineItem,
  } = useFieldArray({
    control,
    name: `loads.${loadIndex}.orders.${orderIndex}.lineItems`,
  });

  return (
    <Card variant="outlined" sx={{ mb: 2, p: 2 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Typography variant="subtitle2">Order {orderIndex + 1}</Typography>
        {orderFieldsLength > 1 && (
          <IconButton
            onClick={onRemove}
            color="error"
            size="small"
            aria-label={`Remove order ${orderIndex + 1}`}
          >
            <DeleteIcon />
          </IconButton>
        )}
      </Box>

      <Grid container spacing={2} sx={{ mb: 2 }}>
        <Grid item xs={12} sm={6}>
          <Controller
            name={`loads.${loadIndex}.orders.${orderIndex}.orderNumber`}
            control={control}
            render={({ field }) => (
              <TextField
                {...field}
                label="Order Number"
                fullWidth
                required
                error={!!errors?.loads?.[loadIndex]?.orders?.[orderIndex]?.orderNumber}
                helperText={errors?.loads?.[loadIndex]?.orders?.[orderIndex]?.orderNumber?.message}
              />
            )}
          />
        </Grid>
        <Grid item xs={12} sm={6}>
          <Controller
            name={`loads.${loadIndex}.orders.${orderIndex}.customerCode`}
            control={control}
            render={({ field }) => (
              <TextField
                {...field}
                label="Customer Code"
                fullWidth
                required
                error={!!errors?.loads?.[loadIndex]?.orders?.[orderIndex]?.customerCode}
                helperText={errors?.loads?.[loadIndex]?.orders?.[orderIndex]?.customerCode?.message}
              />
            )}
          />
        </Grid>
        <Grid item xs={12} sm={6}>
          <Controller
            name={`loads.${loadIndex}.orders.${orderIndex}.customerName`}
            control={control}
            render={({ field }) => (
              <TextField
                {...field}
                label="Customer Name (Optional)"
                fullWidth
                error={!!errors?.loads?.[loadIndex]?.orders?.[orderIndex]?.customerName}
                helperText={errors?.loads?.[loadIndex]?.orders?.[orderIndex]?.customerName?.message}
              />
            )}
          />
        </Grid>
        <Grid item xs={12} sm={6}>
          <Controller
            name={`loads.${loadIndex}.orders.${orderIndex}.priority`}
            control={control}
            render={({ field }) => (
              <FormControl fullWidth required>
                <InputLabel>Priority</InputLabel>
                <Select {...field} label="Priority">
                  <MenuItem value="HIGH">High</MenuItem>
                  <MenuItem value="NORMAL">Normal</MenuItem>
                  <MenuItem value="LOW">Low</MenuItem>
                </Select>
              </FormControl>
            )}
          />
        </Grid>
      </Grid>

      <Typography variant="subtitle2" gutterBottom>
        Line Items
      </Typography>
      <TableContainer>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Product Code</TableCell>
              <TableCell>Quantity</TableCell>
              <TableCell>Notes</TableCell>
              <TableCell width={50}>Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {lineItemFields.map((lineItemField, lineItemIndex) => (
              <TableRow key={lineItemField.id}>
                <TableCell>
                  <Controller
                    name={`loads.${loadIndex}.orders.${orderIndex}.lineItems.${lineItemIndex}.productCode`}
                    control={control}
                    render={({ field }) => (
                      <TextField
                        {...field}
                        size="small"
                        fullWidth
                        required
                        error={
                          !!errors?.loads?.[loadIndex]?.orders?.[orderIndex]?.lineItems?.[
                            lineItemIndex
                          ]?.productCode
                        }
                      />
                    )}
                  />
                </TableCell>
                <TableCell>
                  <Controller
                    name={`loads.${loadIndex}.orders.${orderIndex}.lineItems.${lineItemIndex}.quantity`}
                    control={control}
                    render={({ field }) => (
                      <TextField
                        {...field}
                        type="number"
                        size="small"
                        fullWidth
                        required
                        onChange={e => field.onChange(parseInt(e.target.value) || 0)}
                        error={
                          !!errors?.loads?.[loadIndex]?.orders?.[orderIndex]?.lineItems?.[
                            lineItemIndex
                          ]?.quantity
                        }
                      />
                    )}
                  />
                </TableCell>
                <TableCell>
                  <Controller
                    name={`loads.${loadIndex}.orders.${orderIndex}.lineItems.${lineItemIndex}.notes`}
                    control={control}
                    render={({ field }) => (
                      <TextField {...field} size="small" fullWidth placeholder="Optional notes" />
                    )}
                  />
                </TableCell>
                <TableCell>
                  {lineItemFields.length > 1 && (
                    <IconButton
                      onClick={() => removeLineItem(lineItemIndex)}
                      color="error"
                      size="small"
                      aria-label={`Remove line item ${lineItemIndex + 1}`}
                    >
                      <DeleteIcon />
                    </IconButton>
                  )}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
      <Button
        variant="outlined"
        size="small"
        startIcon={<AddIcon />}
        onClick={() => appendLineItem({ productCode: '', quantity: 1, notes: '' })}
        sx={{ mt: 1 }}
        aria-label="Add line item"
      >
        Add Line Item
      </Button>
    </Card>
  );
};
