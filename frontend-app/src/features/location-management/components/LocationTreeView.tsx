import { List, Paper, Typography } from '@mui/material';
import { useNavigate } from 'react-router-dom';
import { LocationTreeItem } from './LocationTreeItem';
import { LocationHierarchyLevel, LocationHierarchyQueryResult } from '../types/location';
import { EmptyState, SkeletonTable } from '../../../components/common';

interface LocationTreeViewProps {
  data?: LocationHierarchyQueryResult;
  isLoading: boolean;
  error: Error | null;
  level: LocationHierarchyLevel;
  onExpand?: (locationId: string) => void;
  onItemClick?: (locationId: string) => void;
}

export const LocationTreeView = ({
  data,
  isLoading,
  error,
  level,
  onExpand,
  onItemClick,
}: LocationTreeViewProps) => {
  const navigate = useNavigate();

  const handleExpand = (locationId: string) => {
    if (onExpand) {
      onExpand(locationId);
    }
  };

  const handleNavigate = (locationId: string) => {
    if (onItemClick) {
      onItemClick(locationId);
    } else {
      navigate(`/locations/${locationId}`);
    }
  };

  const getNextLevel = (currentLevel: LocationHierarchyLevel): LocationHierarchyLevel | null => {
    switch (currentLevel) {
      case 'warehouse':
        return 'zone';
      case 'zone':
        return 'aisle';
      case 'aisle':
        return 'rack';
      case 'rack':
        return 'bin';
      default:
        return null;
    }
  };

  if (isLoading) {
    return <SkeletonTable rows={5} columns={3} />;
  }

  if (error) {
    return (
      <Paper sx={{ p: 3 }}>
        <Typography color="error">Error loading locations: {error.message}</Typography>
      </Paper>
    );
  }

  if (!data || !data.items || data.items.length === 0) {
    return (
      <EmptyState
        title="No locations found"
        description={
          level === 'warehouse'
            ? 'Create your first warehouse to start managing locations'
            : `No ${level}s found at this level`
        }
      />
    );
  }

  const nextLevel = getNextLevel(level);
  const hasChildren = nextLevel !== null;

  return (
    <Paper>
      <List disablePadding>
        {data.items.map(item => (
          <LocationTreeItem
            key={item.location.locationId}
            item={item}
            level={0}
            onExpand={hasChildren && item.childCount > 0 ? handleExpand : undefined}
            onNavigate={handleNavigate}
            hasChildren={hasChildren && item.childCount > 0}
          />
        ))}
      </List>
    </Paper>
  );
};
