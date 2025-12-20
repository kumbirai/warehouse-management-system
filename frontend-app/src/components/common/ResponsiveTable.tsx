import { ReactNode } from 'react';
import {
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Card,
  CardContent,
  Stack,
  Typography,
  Box,
  useMediaQuery,
  useTheme,
} from '@mui/material';

export interface Column<T> {
  key: string;
  label: string;
  render: (item: T) => ReactNode;
  hideOnMobile?: boolean;
}

interface ResponsiveTableProps<T> {
  data: T[];
  columns: Column<T>[];
  getRowKey: (item: T) => string | number;
  onRowClick?: (item: T) => void;
  emptyMessage?: string;
  mobileCardRender?: (item: T) => ReactNode;
}

export function ResponsiveTable<T>({
  data,
  columns,
  getRowKey,
  onRowClick,
  emptyMessage = 'No items found',
  mobileCardRender,
}: ResponsiveTableProps<T>) {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));

  if (data.length === 0) {
    return (
      <Paper sx={{ p: 4, textAlign: 'center' }}>
        <Typography color="text.secondary">{emptyMessage}</Typography>
      </Paper>
    );
  }

  // Mobile view - render as cards
  if (isMobile) {
    return (
      <Stack spacing={2}>
        {data.map((item) => (
          <Card
            key={getRowKey(item)}
            onClick={onRowClick ? () => onRowClick(item) : undefined}
            sx={{
              cursor: onRowClick ? 'pointer' : 'default',
              '&:hover': onRowClick ? { bgcolor: 'action.hover' } : {},
            }}
          >
            <CardContent>
              {mobileCardRender ? (
                mobileCardRender(item)
              ) : (
                <Stack spacing={1.5}>
                  {columns
                    .filter((col) => !col.hideOnMobile)
                    .map((column) => (
                      <Box key={column.key}>
                        <Typography variant="caption" color="text.secondary" display="block">
                          {column.label}
                        </Typography>
                        <Box mt={0.5}>{column.render(item)}</Box>
                      </Box>
                    ))}
                </Stack>
              )}
            </CardContent>
          </Card>
        ))}
      </Stack>
    );
  }

  // Desktop view - render as table
  return (
    <TableContainer component={Paper}>
      <Table>
        <TableHead>
          <TableRow>
            {columns.map((column) => (
              <TableCell key={column.key}>{column.label}</TableCell>
            ))}
          </TableRow>
        </TableHead>
        <TableBody>
          {data.map((item) => (
            <TableRow
              key={getRowKey(item)}
              onClick={onRowClick ? () => onRowClick(item) : undefined}
              hover={!!onRowClick}
              sx={{ cursor: onRowClick ? 'pointer' : 'default' }}
            >
              {columns.map((column) => (
                <TableCell key={column.key}>{column.render(item)}</TableCell>
              ))}
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  );
}
