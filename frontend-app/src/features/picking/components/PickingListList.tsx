import {
  Box,
  Button,
  Chip,
  CircularProgress,
  FormControl,
  InputLabel,
  MenuItem,
  Paper,
  Select,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TablePagination,
  TableRow,
  Typography,
} from '@mui/material';
import { useNavigate } from 'react-router-dom';
import { ListPickingListsQueryResult, PickingListStatus } from '../types/pickingTypes';
import { Routes } from '../../../utils/navigationUtils';
import AddIcon from '@mui/icons-material/Add';
import RefreshIcon from '@mui/icons-material/Refresh';
import VisibilityIcon from '@mui/icons-material/Visibility';

interface PickingListListProps {
  pickingLists: ListPickingListsQueryResult | null;
  isLoading: boolean;
  page: number;
  size: number;
  onPageChange: (page: number) => void;
  onSizeChange: (size: number) => void;
  status?: PickingListStatus;
  onStatusChange: (status: PickingListStatus | undefined) => void;
  onRefresh: () => Promise<void>;
}

export const PickingListList = ({
  pickingLists,
  isLoading,
  page,
  size,
  onPageChange,
  onSizeChange,
  status,
  onStatusChange,
  onRefresh,
}: PickingListListProps) => {
  const navigate = useNavigate();

  const getStatusColor = (
    status: PickingListStatus
  ): 'default' | 'primary' | 'secondary' | 'error' | 'info' | 'success' | 'warning' => {
    switch (status) {
      case 'RECEIVED':
        return 'info';
      case 'PROCESSING':
        return 'warning';
      case 'PLANNED':
        return 'success';
      case 'COMPLETED':
        return 'default';
      default:
        return 'default';
    }
  };

  if (isLoading && !pickingLists) {
    return (
      <Box display="flex" justifyContent="center" p={3}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Paper>
      <Box sx={{ p: 2, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Typography variant="h6">Picking Lists</Typography>
        <Box sx={{ display: 'flex', gap: 2 }}>
          <Button
            variant="outlined"
            startIcon={<RefreshIcon />}
            onClick={onRefresh}
            aria-label="Refresh picking lists"
          >
            Refresh
          </Button>
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => navigate(Routes.pickingListCreate)}
            aria-label="Create picking list"
          >
            Create
          </Button>
        </Box>
      </Box>

      <Box sx={{ p: 2 }}>
        <FormControl size="small" sx={{ minWidth: 200 }}>
          <InputLabel>Status</InputLabel>
          <Select
            value={status || ''}
            label="Status"
            onChange={e =>
              onStatusChange(e.target.value ? (e.target.value as PickingListStatus) : undefined)
            }
          >
            <MenuItem value="">All</MenuItem>
            <MenuItem value="RECEIVED">Received</MenuItem>
            <MenuItem value="PROCESSING">Processing</MenuItem>
            <MenuItem value="PLANNED">Planned</MenuItem>
            <MenuItem value="COMPLETED">Completed</MenuItem>
          </Select>
        </FormControl>
      </Box>

      <TableContainer>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Reference</TableCell>
              <TableCell>Status</TableCell>
              <TableCell>Load Count</TableCell>
              <TableCell>Total Orders</TableCell>
              <TableCell>Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {!pickingLists ||
            !pickingLists.pickingLists ||
            pickingLists.pickingLists.length === 0 ? (
              <TableRow>
                <TableCell colSpan={5} align="center">
                  <Typography variant="body2" color="text.secondary">
                    No picking lists found
                  </Typography>
                </TableCell>
              </TableRow>
            ) : (
              pickingLists.pickingLists.map(pickingList => (
                <TableRow key={pickingList.id} hover>
                  <TableCell>
                    <Typography
                      variant="body2"
                      sx={{ fontFamily: 'monospace', fontWeight: 'medium' }}
                    >
                      {pickingList.pickingListReference || pickingList.id}
                    </Typography>
                  </TableCell>
                  <TableCell>
                    <Chip
                      label={pickingList.status}
                      color={getStatusColor(pickingList.status)}
                      size="small"
                    />
                  </TableCell>
                  <TableCell>{pickingList.loadCount}</TableCell>
                  <TableCell>{pickingList.totalOrderCount}</TableCell>
                  <TableCell>
                    <Button
                      size="small"
                      startIcon={<VisibilityIcon />}
                      onClick={() => navigate(Routes.pickingListDetail(pickingList.id))}
                      aria-label={`View picking list ${pickingList.id}`}
                    >
                      View
                    </Button>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </TableContainer>

      {pickingLists && (
        <TablePagination
          component="div"
          count={pickingLists.totalElements}
          page={page}
          onPageChange={(_, newPage) => onPageChange(newPage)}
          rowsPerPage={size}
          onRowsPerPageChange={e => onSizeChange(parseInt(e.target.value, 10))}
          rowsPerPageOptions={[10, 20, 50, 100]}
        />
      )}
    </Paper>
  );
};
