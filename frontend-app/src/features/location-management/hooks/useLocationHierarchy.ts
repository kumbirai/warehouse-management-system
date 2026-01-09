import { useCallback, useState } from 'react';
import { useAuth } from '../../../hooks/useAuth';
import { useWarehouses } from './useWarehouses';
import { useZones } from './useZones';
import { useAisles } from './useAisles';
import { useRacks } from './useRacks';
import { useBins } from './useBins';
import { LocationHierarchyLevel, LocationHierarchyQueryResult } from '../types/location';

export interface HierarchyNavigationState {
  level: LocationHierarchyLevel;
  warehouseId?: string | undefined;
  zoneId?: string | undefined;
  aisleId?: string | undefined;
  rackId?: string | undefined;
}

export interface UseLocationHierarchyResult {
  data: LocationHierarchyQueryResult | undefined;
  isLoading: boolean;
  error: Error | null;
  navigationState: HierarchyNavigationState;
  navigateToWarehouse: () => void;
  navigateToZone: (warehouseId: string) => void;
  navigateToAisle: (zoneId: string) => void;
  navigateToRack: (aisleId: string) => void;
  navigateToBin: (rackId: string) => void;
  navigateUp: () => void;
}

export const useLocationHierarchy = (): UseLocationHierarchyResult => {
  const { user } = useAuth();
  const tenantId = user?.tenantId;

  const [navigationState, setNavigationState] = useState<HierarchyNavigationState>({
    level: 'warehouse',
  });

  // Query hooks based on current navigation state
  const warehousesQuery = useWarehouses(tenantId);
  const zonesQuery = useZones(navigationState.warehouseId, tenantId);
  const aislesQuery = useAisles(navigationState.zoneId, tenantId);
  const racksQuery = useRacks(navigationState.aisleId, tenantId);
  const binsQuery = useBins(navigationState.rackId, tenantId);

  // Get current data based on level
  const getCurrentData = (): LocationHierarchyQueryResult | undefined => {
    switch (navigationState.level) {
      case 'warehouse':
        return warehousesQuery.data;
      case 'zone':
        return zonesQuery.data;
      case 'aisle':
        return aislesQuery.data;
      case 'rack':
        return racksQuery.data;
      case 'bin':
        return binsQuery.data;
      default:
        return undefined;
    }
  };

  // Get current loading state
  const getCurrentLoading = (): boolean => {
    switch (navigationState.level) {
      case 'warehouse':
        return warehousesQuery.isLoading;
      case 'zone':
        return zonesQuery.isLoading;
      case 'aisle':
        return aislesQuery.isLoading;
      case 'rack':
        return racksQuery.isLoading;
      case 'bin':
        return binsQuery.isLoading;
      default:
        return false;
    }
  };

  // Get current error
  const getCurrentError = (): Error | null => {
    switch (navigationState.level) {
      case 'warehouse':
        return warehousesQuery.error || null;
      case 'zone':
        return zonesQuery.error || null;
      case 'aisle':
        return aislesQuery.error || null;
      case 'rack':
        return racksQuery.error || null;
      case 'bin':
        return binsQuery.error || null;
      default:
        return null;
    }
  };

  // Navigation functions
  const navigateToWarehouse = useCallback(() => {
    setNavigationState({ level: 'warehouse' });
  }, []);

  const navigateToZone = useCallback((warehouseId: string) => {
    setNavigationState({ level: 'zone', warehouseId });
  }, []);

  const navigateToAisle = useCallback((zoneId: string) => {
    setNavigationState(prev => ({
      level: 'aisle',
      warehouseId: prev.warehouseId,
      zoneId,
    }));
  }, []);

  const navigateToRack = useCallback((aisleId: string) => {
    setNavigationState(prev => ({
      level: 'rack',
      warehouseId: prev.warehouseId,
      zoneId: prev.zoneId,
      aisleId,
    }));
  }, []);

  const navigateToBin = useCallback((rackId: string) => {
    setNavigationState(prev => ({
      level: 'bin',
      warehouseId: prev.warehouseId,
      zoneId: prev.zoneId,
      aisleId: prev.aisleId,
      rackId,
    }));
  }, []);

  const navigateUp = useCallback(() => {
    setNavigationState(prev => {
      switch (prev.level) {
        case 'bin':
          return {
            level: 'rack',
            warehouseId: prev.warehouseId,
            zoneId: prev.zoneId,
            aisleId: prev.aisleId,
          };
        case 'rack':
          return {
            level: 'aisle',
            warehouseId: prev.warehouseId,
            zoneId: prev.zoneId,
          };
        case 'aisle':
          return {
            level: 'zone',
            warehouseId: prev.warehouseId,
          };
        case 'zone':
          return {
            level: 'warehouse',
          };
        default:
          return prev;
      }
    });
  }, []);

  return {
    data: getCurrentData(),
    isLoading: getCurrentLoading(),
    error: getCurrentError(),
    navigationState,
    navigateToWarehouse,
    navigateToZone,
    navigateToAisle,
    navigateToRack,
    navigateToBin,
    navigateUp,
  };
};
