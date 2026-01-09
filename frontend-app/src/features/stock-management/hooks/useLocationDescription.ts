import {useMemo} from 'react';
import {useLocations} from '../../location-management/hooks/useLocations';
import {useAppSelector} from '../../../store/hooks';
import {selectUser} from '../../../store/authSlice';

/**
 * Hook to get location description by location ID
 * Fetches all locations and creates a lookup map for quick access
 */
export const useLocationDescription = (locationId?: string): string | undefined => {
  const user = useAppSelector(selectUser);
  const tenantId = user?.tenantId;

  // Fetch all locations to build lookup map
  const { locations } = useLocations({
    tenantId: tenantId || undefined,
    page: 1,
    size: 1000, // Fetch enough to get all locations
  });

  // Create a map of locationId -> description (or name/code/barcode as fallback)
  const locationMap = useMemo(() => {
    const map = new Map<string, string>();
    locations.forEach(location => {
      if (location.locationId) {
        // Prefer description, then name, then code, then barcode, finally locationId
        const displayText =
          location.description ||
          location.name ||
          location.code ||
          location.barcode ||
          location.locationId;
        map.set(location.locationId, displayText);
      }
    });
    return map;
  }, [locations]);

  if (!locationId) {
    return undefined;
  }

  return locationMap.get(locationId) || locationId; // Fallback to ID if not found
};

/**
 * Hook to get a map of all location descriptions
 * Useful when displaying multiple locations
 */
export const useLocationDescriptionMap = (): Map<string, string> => {
  const user = useAppSelector(selectUser);
  const tenantId = user?.tenantId;

  // Fetch all locations to build lookup map
  const { locations } = useLocations({
    tenantId: tenantId || undefined,
    page: 1,
    size: 1000, // Fetch enough to get all locations
  });

  // Create a map of locationId -> description (or name/code/barcode as fallback)
  return useMemo(() => {
    const map = new Map<string, string>();
    locations.forEach(location => {
      if (location.locationId) {
        // Prefer description, then name, then code, then barcode, finally locationId
        const displayText =
          location.description ||
          location.name ||
          location.code ||
          location.barcode ||
          location.locationId;
        map.set(location.locationId, displayText);
      }
    });
    return map;
  }, [locations]);
};
