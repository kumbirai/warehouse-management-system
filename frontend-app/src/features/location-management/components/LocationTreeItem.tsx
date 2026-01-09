import { useState } from 'react';
import {
  Box,
  IconButton,
  ListItem,
  ListItemButton,
  Stack,
  Typography,
  Chip,
} from '@mui/material';
import {
  ExpandMore as ExpandMoreIcon,
  ChevronRight as ChevronRightIcon,
  LocationOn as LocationIcon,
} from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import { LocationHierarchyItem } from '../types/location';
import { StatusBadge, getStatusVariant } from '../../../components/common';

interface LocationTreeItemProps {
  item: LocationHierarchyItem;
  level: number;
  onExpand?: (locationId: string) => void;
  onNavigate?: (locationId: string) => void;
  expanded?: boolean;
  hasChildren: boolean;
}

export const LocationTreeItem = ({
  item,
  level,
  onExpand,
  onNavigate,
  expanded = false,
  hasChildren,
}: LocationTreeItemProps) => {
  const navigate = useNavigate();
  const [isExpanded, setIsExpanded] = useState(expanded);

  const handleExpandClick = (e: React.MouseEvent) => {
    e.stopPropagation();
    if (hasChildren && onExpand) {
      setIsExpanded(!isExpanded);
      onExpand(item.location.locationId);
    }
  };

  const handleItemClick = () => {
    if (onNavigate) {
      onNavigate(item.location.locationId);
    } else {
      navigate(`/locations/${item.location.locationId}`);
    }
  };

  const displayName = item.location.name || item.location.code || item.location.barcode;
  const indent = level * 2;

  return (
    <>
      <ListItem
        disablePadding
        sx={{
          pl: `${indent}rem`,
          '&:hover': {
            bgcolor: 'action.hover',
          },
        }}
      >
        <ListItemButton onClick={handleItemClick} sx={{ py: 1.5 }}>
          <Stack direction="row" spacing={1} alignItems="center" sx={{ width: '100%' }}>
            {hasChildren ? (
              <IconButton
                size="small"
                onClick={handleExpandClick}
                sx={{ mr: 0.5 }}
                aria-label={isExpanded ? 'Collapse' : 'Expand'}
              >
                {isExpanded ? <ExpandMoreIcon /> : <ChevronRightIcon />}
              </IconButton>
            ) : (
              <Box sx={{ width: 40, display: 'flex', justifyContent: 'center' }}>
                <LocationIcon fontSize="small" color="action" />
              </Box>
            )}

            <Box sx={{ flexGrow: 1, minWidth: 0 }}>
              <Stack direction="row" spacing={1} alignItems="center">
                <Typography
                  variant="body1"
                  sx={{
                    fontWeight: level === 0 ? 'medium' : 'normal',
                    overflow: 'hidden',
                    textOverflow: 'ellipsis',
                    whiteSpace: 'nowrap',
                  }}
                >
                  {displayName}
                </Typography>
                {item.location.code && item.location.code !== displayName && (
                  <Typography variant="caption" color="text.secondary" sx={{ fontFamily: 'monospace' }}>
                    ({item.location.code})
                  </Typography>
                )}
              </Stack>
              {item.location.description && (
                <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 0.5 }}>
                  {item.location.description}
                </Typography>
              )}
            </Box>

            <Stack direction="row" spacing={1} alignItems="center">
              <StatusBadge
                label={item.location.status}
                variant={getStatusVariant(item.location.status)}
                size="small"
              />
              {hasChildren && (
                <Chip
                  label={`${item.childCount} ${item.childCount === 1 ? 'child' : 'children'}`}
                  size="small"
                  variant="outlined"
                  sx={{ fontSize: '0.7rem' }}
                />
              )}
            </Stack>
          </Stack>
        </ListItemButton>
      </ListItem>
    </>
  );
};
