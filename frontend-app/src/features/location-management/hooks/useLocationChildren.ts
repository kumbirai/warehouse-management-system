import { useQuery } from '@tanstack/react-query';
import { locationService } from '../services/locationService';
import { Location, LocationHierarchyQueryResult } from '../types/location';

/**
 * Hook to fetch child locations for a given location.
 * Automatically determines the correct endpoint based on location type.
 */
export const useLocationChildren = (
  location: Location | null,
  tenantId: string | null | undefined
) => {
  return useQuery<LocationHierarchyQueryResult, Error>({
    queryKey: ['locationChildren', location?.locationId, location?.type, tenantId],
    queryFn: async () => {
      if (!location || !tenantId) {
        throw new Error('Location and Tenant ID are required');
      }

      const locationType = location.type?.toUpperCase();
      const locationId = location.locationId;

      switch (locationType) {
        case 'WAREHOUSE':
          return (await locationService.listZones(locationId, tenantId)).data;
        case 'ZONE':
          return (await locationService.listAisles(locationId, tenantId)).data;
        case 'AISLE':
          return (await locationService.listRacks(locationId, tenantId)).data;
        case 'RACK':
          return (await locationService.listBins(locationId, tenantId)).data;
        case 'BIN':
          // Bins have no children
          return {
            parent: location,
            items: [],
            hierarchyLevel: 'BIN' as const,
          };
        default:
          throw new Error(`Unknown location type: ${locationType}`);
      }
    },
    enabled: !!location && !!tenantId && location.type?.toUpperCase() !== 'BIN',
    staleTime: 5 * 60 * 1000, // 5 minutes
  });
};
