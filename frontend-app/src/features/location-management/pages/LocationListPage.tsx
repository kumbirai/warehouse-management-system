import { Button, Stack } from '@mui/material';
import { useNavigate } from 'react-router-dom';
import { Add as AddIcon, ArrowBack as ArrowBackIcon } from '@mui/icons-material';

import { ListPageLayout } from '../../../components/layouts';
import { Routes } from '../../../utils/navigationUtils';
import { LocationTreeView } from '../components/LocationTreeView';
import { useLocationHierarchy } from '../hooks/useLocationHierarchy';

export const LocationListPage = () => {
  const navigate = useNavigate();
  const {
    data,
    isLoading,
    error,
    navigationState,
    navigateToZone,
    navigateToAisle,
    navigateToRack,
    navigateToBin,
    navigateUp,
  } = useLocationHierarchy();

  // Build breadcrumbs based on current hierarchy level
  const getBreadcrumbItems = () => {
    const items: Array<{ label: string; href?: string }> = [
      { label: 'Dashboard', href: '/dashboard' },
      { label: 'Locations', href: Routes.locations },
    ];

    if (navigationState.level !== 'warehouse' && data?.parent) {
      const parentName = data.parent.name || data.parent.code || data.parent.barcode;
      items.push({ label: parentName });
    }

    return items;
  };

  // Get page title based on current level
  const getPageTitle = () => {
    switch (navigationState.level) {
      case 'warehouse':
        return 'Warehouses';
      case 'zone':
        return 'Zones';
      case 'aisle':
        return 'Aisles';
      case 'rack':
        return 'Racks';
      case 'bin':
        return 'Bins';
      default:
        return 'Locations';
    }
  };

  // Get page description based on current level
  const getPageDescription = () => {
    switch (navigationState.level) {
      case 'warehouse':
        return 'Navigate through warehouse locations hierarchy';
      case 'zone':
        return `Zones in ${data?.parent?.name || data?.parent?.code || 'warehouse'}`;
      case 'aisle':
        return `Aisles in ${data?.parent?.name || data?.parent?.code || 'zone'}`;
      case 'rack':
        return `Racks in ${data?.parent?.name || data?.parent?.code || 'aisle'}`;
      case 'bin':
        return `Bins in ${data?.parent?.name || data?.parent?.code || 'rack'}`;
      default:
        return 'Manage warehouse locations and storage areas';
    }
  };

  return (
    <ListPageLayout
      breadcrumbs={getBreadcrumbItems()}
      title={getPageTitle()}
      description={getPageDescription()}
      actions={
        <Stack direction="row" spacing={1}>
          {navigationState.level !== 'warehouse' && (
            <Button variant="outlined" startIcon={<ArrowBackIcon />} onClick={navigateUp}>
              Back
            </Button>
          )}
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => navigate(Routes.locationCreate)}
          >
            Create Location
          </Button>
        </Stack>
      }
      isLoading={isLoading}
      error={error?.message || null}
    >
      <LocationTreeView
        data={data}
        isLoading={isLoading}
        error={error}
        level={navigationState.level}
        onExpand={locationId => {
          switch (navigationState.level) {
            case 'warehouse':
              navigateToZone(locationId);
              break;
            case 'zone':
              navigateToAisle(locationId);
              break;
            case 'aisle':
              navigateToRack(locationId);
              break;
            case 'rack':
              navigateToBin(locationId);
              break;
            default:
              break;
          }
        }}
      />
    </ListPageLayout>
  );
};
