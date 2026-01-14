import { useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Alert, Box, Button, Chip, Divider, Grid, Paper, Typography } from '@mui/material';
import { DetailPageLayout } from '../../../components/layouts';
import { getBreadcrumbs, Routes } from '../../../utils/navigationUtils';
import { usePickingList } from '../hooks/usePickingList';
import { usePickingTasks } from '../hooks/usePickingTasks';
import { useCompletePickingList } from '../hooks/useCompletePickingList';
import { ActionDialog } from '../../../components/common/ActionDialog';

export const PickingListCompletionPage = () => {
  const { pickingListId } = useParams<{ pickingListId: string }>();
  const navigate = useNavigate();
  const { pickingList, isLoading, error, refetch } = usePickingList(pickingListId || '');
  const { pickingTasks, isLoading: isLoadingTasks, refetch: refetchTasks } = usePickingTasks({});
  const {
    completePickingList,
    isLoading: isCompleting,
    error: completeError,
  } = useCompletePickingList();
  const [completeDialogOpen, setCompleteDialogOpen] = useState(false);

  const handleComplete = async () => {
    if (!pickingListId) {
      return;
    }

    const result = await completePickingList(pickingListId);
    if (result) {
      setCompleteDialogOpen(false);
      refetch();
      refetchTasks();
      // Optionally navigate back to list
      // navigate(Routes.pickingLists);
    }
  };

  // Get all load IDs from the picking list
  const pickingListLoadIds = useMemo(() => {
    if (!pickingList) return new Set<string>();
    return new Set(pickingList.loads.map(load => load.loadId));
  }, [pickingList]);

  // Filter tasks that belong to this picking list's loads
  const relevantTasks = useMemo(() => {
    if (!pickingTasks?.pickingTasks || !pickingListLoadIds.size) return [];
    return pickingTasks.pickingTasks.filter(task => pickingListLoadIds.has(task.loadId));
  }, [pickingTasks, pickingListLoadIds]);

  // Calculate task status summary
  const taskStatusSummary = useMemo(() => {
    const summary = {
      total: relevantTasks.length,
      completed: 0,
      partiallyCompleted: 0,
      pending: 0,
      inProgress: 0,
    };

    relevantTasks.forEach(task => {
      switch (task.status) {
        case 'COMPLETED':
          summary.completed++;
          break;
        case 'PARTIALLY_COMPLETED':
          summary.partiallyCompleted++;
          break;
        case 'PENDING':
          summary.pending++;
          break;
        case 'IN_PROGRESS':
          summary.inProgress++;
          break;
      }
    });

    return summary;
  }, [relevantTasks]);

  // Check if all tasks are completed or partially completed
  const canComplete = useMemo(() => {
    if (!pickingList || pickingList.status !== 'PLANNED') {
      return false;
    }

    if (relevantTasks.length === 0) {
      // No tasks found - might not be planned yet
      return false;
    }

    // All tasks must be either COMPLETED or PARTIALLY_COMPLETED
    return relevantTasks.every(
      task => task.status === 'COMPLETED' || task.status === 'PARTIALLY_COMPLETED'
    );
  }, [pickingList, relevantTasks]);

  const displayReference =
    pickingList?.pickingListReference || pickingListId?.substring(0, 8) + '...' || '...';

  return (
    <>
      <DetailPageLayout
        breadcrumbs={getBreadcrumbs.pickingListComplete(displayReference)}
        title={`Complete Picking List: ${displayReference}`}
        actions={
          <Button variant="outlined" onClick={() => navigate(Routes.pickingLists)}>
            Back to List
          </Button>
        }
        isLoading={isLoading || isLoadingTasks}
        error={error?.message || completeError?.message || null}
      >
        {pickingList && (
          <Grid container spacing={3}>
            {/* Summary */}
            <Grid item xs={12}>
              <Paper elevation={1} sx={{ p: 3 }}>
                <Typography variant="h6" gutterBottom>
                  Picking List Summary
                </Typography>
                <Divider sx={{ mb: 2 }} />
                <Grid container spacing={2}>
                  <Grid item xs={12} md={4}>
                    <Typography variant="caption" color="text.secondary">
                      Status
                    </Typography>
                    <Box mt={0.5}>
                      <Chip
                        label={pickingList.status}
                        color={pickingList.status === 'COMPLETED' ? 'success' : 'default'}
                        size="small"
                      />
                    </Box>
                  </Grid>
                  <Grid item xs={12} md={4}>
                    <Typography variant="caption" color="text.secondary">
                      Load Count
                    </Typography>
                    <Typography variant="body1">{pickingList.loadCount}</Typography>
                  </Grid>
                  <Grid item xs={12} md={4}>
                    <Typography variant="caption" color="text.secondary">
                      Total Orders
                    </Typography>
                    <Typography variant="body1">{pickingList.totalOrderCount}</Typography>
                  </Grid>
                </Grid>
              </Paper>
            </Grid>

            {/* Task Status Summary */}
            <Grid item xs={12}>
              <Paper elevation={1} sx={{ p: 3 }}>
                <Typography variant="h6" gutterBottom>
                  Task Status Summary
                </Typography>
                <Divider sx={{ mb: 2 }} />
                <Alert severity="info" sx={{ mb: 2 }}>
                  All picking tasks must be completed or partially completed before the picking list
                  can be marked as complete.
                </Alert>

                {isLoadingTasks ? (
                  <Typography variant="body2" color="text.secondary">
                    Loading task status...
                  </Typography>
                ) : relevantTasks.length === 0 ? (
                  <Typography variant="body2" color="text.secondary">
                    No tasks found for this picking list. Tasks are created when the picking list is
                    processed.
                  </Typography>
                ) : (
                  <Grid container spacing={2}>
                    <Grid item xs={12} sm={6} md={3}>
                      <Box>
                        <Typography variant="caption" color="text.secondary">
                          Total Tasks
                        </Typography>
                        <Typography variant="h6">{taskStatusSummary.total}</Typography>
                      </Box>
                    </Grid>
                    <Grid item xs={12} sm={6} md={3}>
                      <Box>
                        <Typography variant="caption" color="text.secondary">
                          Completed
                        </Typography>
                        <Typography variant="h6" color="success.main">
                          {taskStatusSummary.completed}
                        </Typography>
                      </Box>
                    </Grid>
                    <Grid item xs={12} sm={6} md={3}>
                      <Box>
                        <Typography variant="caption" color="text.secondary">
                          Partially Completed
                        </Typography>
                        <Typography variant="h6" color="warning.main">
                          {taskStatusSummary.partiallyCompleted}
                        </Typography>
                      </Box>
                    </Grid>
                    <Grid item xs={12} sm={6} md={3}>
                      <Box>
                        <Typography variant="caption" color="text.secondary">
                          Pending / In Progress
                        </Typography>
                        <Typography variant="h6" color="error.main">
                          {taskStatusSummary.pending + taskStatusSummary.inProgress}
                        </Typography>
                      </Box>
                    </Grid>

                    {taskStatusSummary.pending > 0 || taskStatusSummary.inProgress > 0 ? (
                      <Grid item xs={12}>
                        <Alert severity="warning">
                          {taskStatusSummary.pending + taskStatusSummary.inProgress} task(s) are
                          still pending or in progress. Please complete all tasks before marking the
                          picking list as complete.
                        </Alert>
                      </Grid>
                    ) : taskStatusSummary.total > 0 ? (
                      <Grid item xs={12}>
                        <Alert severity="success">
                          All tasks are completed or partially completed. You can now complete the
                          picking list.
                        </Alert>
                      </Grid>
                    ) : null}
                  </Grid>
                )}
              </Paper>
            </Grid>

            {/* Completion Action */}
            {canComplete && (
              <Grid item xs={12}>
                <Paper elevation={1} sx={{ p: 3 }}>
                  <Typography variant="h6" gutterBottom>
                    Complete Picking List
                  </Typography>
                  <Divider sx={{ mb: 2 }} />
                  <Button
                    variant="contained"
                    color="success"
                    onClick={() => setCompleteDialogOpen(true)}
                    disabled={isCompleting}
                    sx={{ width: { xs: '100%', sm: 'auto' } }}
                  >
                    {isCompleting ? 'Completing...' : 'Complete Picking List'}
                  </Button>
                </Paper>
              </Grid>
            )}

            {!canComplete && pickingList.status !== 'COMPLETED' && (
              <Grid item xs={12}>
                <Alert severity="warning">
                  Cannot complete picking list. Please ensure all picking tasks are completed first.
                </Alert>
              </Grid>
            )}
          </Grid>
        )}
      </DetailPageLayout>

      <ActionDialog
        open={completeDialogOpen}
        title="Complete Picking List"
        description="Are you sure you want to mark this picking list as complete? This action will finalize the picking process and prepare it for shipping."
        confirmLabel="Complete"
        cancelLabel="Cancel"
        onConfirm={handleComplete}
        onCancel={() => setCompleteDialogOpen(false)}
        isLoading={isCompleting}
        variant="default"
      />
    </>
  );
};
