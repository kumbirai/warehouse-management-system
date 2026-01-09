import { useMemo, useState } from 'react';
import {
  Card,
  CardContent,
  FormControl,
  Grid,
  InputLabel,
  MenuItem,
  Paper,
  Select,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material';
import { DashboardPageLayout } from '../../../components/layouts';
import { useExpiringStock } from '../hooks/useExpiringStock';
import { StockClassificationBadge } from '../components/StockClassificationBadge';
import { StockClassification } from '../types/stockManagement';
import { formatDate } from '../../../utils/dateUtils';

export const ExpiringStockDashboard = () => {
  const [startDate, setStartDate] = useState<string>(() => {
    const date = new Date();
    return date.toISOString().split('T')[0];
  });
  const [endDate, setEndDate] = useState<string>(() => {
    const date = new Date();
    date.setDate(date.getDate() + 30);
    return date.toISOString().split('T')[0];
  });
  const [classificationFilter, setClassificationFilter] = useState<StockClassification | ''>('');

  const filters = useMemo(
    () => ({
      startDate,
      endDate,
      classification: classificationFilter || undefined,
    }),
    [startDate, endDate, classificationFilter]
  );

  const { expiringStock, isLoading, error } = useExpiringStock(filters);

  // Group by classification
  const groupedByClassification = useMemo(() => {
    const groups: Record<StockClassification, typeof expiringStock> = {
      EXPIRED: [],
      CRITICAL: [],
      NEAR_EXPIRY: [],
      NORMAL: [],
      EXTENDED_SHELF_LIFE: [],
    };

    expiringStock.forEach(item => {
      if (groups[item.classification]) {
        groups[item.classification].push(item);
      }
    });

    return groups;
  }, [expiringStock]);

  const totalExpired = groupedByClassification.EXPIRED.length;
  const totalCritical = groupedByClassification.CRITICAL.length;
  const totalNearExpiry = groupedByClassification.NEAR_EXPIRY.length;

  return (
    <DashboardPageLayout
      title="Expiring Stock Dashboard"
      subtitle="Monitor stock expiration and manage expiring inventory"
    >
      <Grid container spacing={3}>
        {/* Summary Cards */}
        <Grid item xs={12} md={4}>
          <Card sx={{ bgcolor: 'error.light' }}>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Expired Stock
              </Typography>
              <Typography variant="h3" color="error.main" sx={{ fontWeight: 'bold' }}>
                {totalExpired}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Items that have expired
              </Typography>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} md={4}>
          <Card sx={{ bgcolor: 'warning.light' }}>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Critical (7 days)
              </Typography>
              <Typography variant="h3" color="warning.main" sx={{ fontWeight: 'bold' }}>
                {totalCritical}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Items expiring within 7 days
              </Typography>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} md={4}>
          <Card sx={{ bgcolor: 'info.light' }}>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Near Expiry (30 days)
              </Typography>
              <Typography variant="h3" color="info.main" sx={{ fontWeight: 'bold' }}>
                {totalNearExpiry}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Items expiring within 30 days
              </Typography>
            </CardContent>
          </Card>
        </Grid>

        {/* Filters */}
        <Grid item xs={12}>
          <Paper elevation={1} sx={{ p: 2 }}>
            <Grid container spacing={2} alignItems="center">
              <Grid item xs={12} sm={4}>
                <FormControl fullWidth size="small">
                  <InputLabel>Start Date</InputLabel>
                  <input
                    type="date"
                    value={startDate}
                    onChange={e => setStartDate(e.target.value)}
                    style={{
                      padding: '8px',
                      border: '1px solid #ccc',
                      borderRadius: '4px',
                      width: '100%',
                    }}
                  />
                </FormControl>
              </Grid>
              <Grid item xs={12} sm={4}>
                <FormControl fullWidth size="small">
                  <InputLabel>End Date</InputLabel>
                  <input
                    type="date"
                    value={endDate}
                    onChange={e => setEndDate(e.target.value)}
                    style={{
                      padding: '8px',
                      border: '1px solid #ccc',
                      borderRadius: '4px',
                      width: '100%',
                    }}
                  />
                </FormControl>
              </Grid>
              <Grid item xs={12} sm={4}>
                <FormControl fullWidth size="small">
                  <InputLabel>Classification</InputLabel>
                  <Select
                    value={classificationFilter}
                    label="Classification"
                    onChange={e =>
                      setClassificationFilter(e.target.value as StockClassification | '')
                    }
                  >
                    <MenuItem value="">All</MenuItem>
                    <MenuItem value="EXPIRED">Expired</MenuItem>
                    <MenuItem value="CRITICAL">Critical</MenuItem>
                    <MenuItem value="NEAR_EXPIRY">Near Expiry</MenuItem>
                    <MenuItem value="NORMAL">Normal</MenuItem>
                  </Select>
                </FormControl>
              </Grid>
            </Grid>
          </Paper>
        </Grid>

        {/* Expiring Stock List */}
        <Grid item xs={12}>
          <Paper elevation={1} sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>
              Expiring Stock Items
            </Typography>
            {isLoading ? (
              <Typography>Loading...</Typography>
            ) : error ? (
              <Typography color="error">{error.message}</Typography>
            ) : expiringStock.length === 0 ? (
              <Typography color="text.secondary">
                No expiring stock found for the selected criteria.
              </Typography>
            ) : (
              <TableContainer>
                <Table>
                  <TableHead>
                    <TableRow>
                      <TableCell>Product Code</TableCell>
                      <TableCell>Location</TableCell>
                      <TableCell>Quantity</TableCell>
                      <TableCell>Expiration Date</TableCell>
                      <TableCell>Days Until Expiry</TableCell>
                      <TableCell>Classification</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {expiringStock.map(item => (
                      <TableRow key={item.stockItemId}>
                        <TableCell>
                          <Typography
                            variant="body2"
                            sx={{ fontFamily: 'monospace', fontWeight: 'medium' }}
                          >
                            {item.productCode}
                          </Typography>
                        </TableCell>
                        <TableCell>
                          <Typography variant="body2" sx={{ fontFamily: 'monospace' }}>
                            {item.locationId}
                          </Typography>
                        </TableCell>
                        <TableCell>{item.quantity}</TableCell>
                        <TableCell>{formatDate(item.expirationDate)}</TableCell>
                        <TableCell>{item.daysUntilExpiry ?? 'N/A'}</TableCell>
                        <TableCell>
                          <StockClassificationBadge classification={item.classification} />
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            )}
          </Paper>
        </Grid>
      </Grid>
    </DashboardPageLayout>
  );
};
