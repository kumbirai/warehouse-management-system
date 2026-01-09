package com.ccbsa.wms.stock.dataaccess.adapter;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.ccbsa.common.application.api.ApiResponse;
import com.ccbsa.common.domain.valueobject.Quantity;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.stock.application.service.port.service.LocationServicePort;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;

/**
 * Adapter: LocationServiceAdapter
 * <p>
 * Implements LocationServicePort for checking location availability from location-management-service.
 * Uses circuit breaker and retry for fault tolerance.
 */
@Component
@Slf4j
public class LocationServiceAdapter implements LocationServicePort {
    private static final ParameterizedTypeReference<ApiResponse<LocationAvailabilityResponse>> LOCATION_AVAILABILITY_RESPONSE_TYPE =
            new ParameterizedTypeReference<ApiResponse<LocationAvailabilityResponse>>() {
            };
    private static final ParameterizedTypeReference<ApiResponse<LocationQueryResultResponse>> LOCATION_INFO_RESPONSE_TYPE =
            new ParameterizedTypeReference<ApiResponse<LocationQueryResultResponse>>() {
            };

    private final RestTemplate restTemplate;
    private final String locationServiceUrl;

    public LocationServiceAdapter(RestTemplate restTemplate, @Value("${location.service.url:http://location-management-service}") String locationServiceUrl) {
        this.restTemplate = restTemplate;
        this.locationServiceUrl = locationServiceUrl;
    }

    @Override
    @CircuitBreaker(name = "locationService", fallbackMethod = "checkLocationAvailabilityFallback")
    @Retry(name = "locationService")
    public LocationAvailability checkLocationAvailability(LocationId locationId, Quantity requiredQuantity, TenantId tenantId) {

        log.debug("Checking location availability from location-service: locationId={}, quantity={}, tenantId={}", locationId.getValueAsString(), requiredQuantity.getValue(),
                tenantId.getValue());

        try {
            String url = String.format("%s/api/v1/location-management/locations/%s/check-availability?requiredQuantity=%d", locationServiceUrl, locationId.getValueAsString(),
                    requiredQuantity.getValue());
            log.debug("Calling location service: {}", url);

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Tenant-Id", tenantId.getValue());
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<ApiResponse<LocationAvailabilityResponse>> response = restTemplate.exchange(url, HttpMethod.GET, entity, LOCATION_AVAILABILITY_RESPONSE_TYPE);

            ApiResponse<LocationAvailabilityResponse> responseBody = response.getBody();
            if (response.getStatusCode() == HttpStatus.OK && responseBody != null && responseBody.getData() != null) {
                LocationAvailabilityResponse availabilityResponse = responseBody.getData();
                return mapToLocationAvailability(availabilityResponse);
            }

            log.warn("Location service returned unexpected response: status={}", response.getStatusCode());
            return LocationAvailability.unavailable("Location service returned unexpected response");
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Location not found: {}", locationId.getValueAsString());
            return LocationAvailability.unavailable("Location not found");
        } catch (RestClientException e) {
            log.error("Failed to check location availability from location-service: locationId={}", locationId.getValueAsString(), e);
            throw new RuntimeException(String.format("Failed to check location availability: %s", e.getMessage()), e);
        } catch (Exception e) {
            log.error("Unexpected error checking location availability: locationId={}", locationId.getValueAsString(), e);
            throw new RuntimeException(String.format("Failed to check location availability: %s", e.getMessage()), e);
        }
    }

    /**
     * Maps LocationAvailabilityResponse to LocationAvailability.
     */
    private LocationAvailability mapToLocationAvailability(LocationAvailabilityResponse response) {
        if (!response.isAvailable()) {
            return LocationAvailability.unavailable(response.getReason());
        }

        if (!response.hasCapacity()) {
            Integer availableCapacity = response.getAvailableCapacity();
            Quantity availableQuantity = availableCapacity != null ? Quantity.of(availableCapacity) : null;
            return LocationAvailability.insufficientCapacity(availableQuantity);
        }

        Integer availableCapacity = response.getAvailableCapacity();
        Quantity availableQuantity = availableCapacity != null ? Quantity.of(availableCapacity) : null;
        return LocationAvailability.available(availableQuantity);
    }

    @Override
    @CircuitBreaker(name = "locationService", fallbackMethod = "getLocationInfoFallback")
    @Retry(name = "locationService")
    public Optional<LocationInfo> getLocationInfo(LocationId locationId, TenantId tenantId) {
        log.debug("Getting location info from location-service: locationId={}, tenantId={}", locationId.getValueAsString(), tenantId.getValue());

        try {
            String url = String.format("%s/api/v1/location-management/locations/%s", locationServiceUrl, locationId.getValueAsString());
            log.debug("Calling location service: {}", url);

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Tenant-Id", tenantId.getValue());
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<ApiResponse<LocationQueryResultResponse>> response = restTemplate.exchange(url, HttpMethod.GET, entity, LOCATION_INFO_RESPONSE_TYPE);

            ApiResponse<LocationQueryResultResponse> responseBody = response.getBody();
            if (response.getStatusCode() == HttpStatus.OK && responseBody != null && responseBody.getData() != null) {
                LocationQueryResultResponse locationResponse = responseBody.getData();

                // Extract coordinates if available
                LocationServicePort.LocationCoordinates coordinates = null;
                if (locationResponse.getCoordinates() != null) {
                    coordinates = new LocationServicePort.LocationCoordinates(locationResponse.getCoordinates().getZone(), locationResponse.getCoordinates().getAisle(),
                            locationResponse.getCoordinates().getRack(), locationResponse.getCoordinates().getLevel());
                }

                return Optional.of(new LocationServicePort.LocationInfo(locationResponse.getLocationId(), locationResponse.getName(), locationResponse.getDescription(),
                        locationResponse.getCode(), locationResponse.getType(), coordinates));
            }

            log.warn("Location service returned unexpected response: status={}", response.getStatusCode());
            return Optional.empty();
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Location not found: {}", locationId.getValueAsString());
            return Optional.empty();
        } catch (RestClientException e) {
            log.error("Failed to get location info from location-service: locationId={}", locationId.getValueAsString(), e);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Unexpected error getting location info: locationId={}", locationId.getValueAsString(), e);
            return Optional.empty();
        }
    }

    /**
     * Fallback method for circuit breaker.
     * Returns unavailable when circuit is open or service is down.
     */
    @SuppressFBWarnings(value = "UPM_UNCALLED_PRIVATE_METHOD", justification = "Fallback method called by Resilience4j circuit breaker via reflection")
    private LocationAvailability checkLocationAvailabilityFallback(LocationId locationId, Quantity requiredQuantity, TenantId tenantId, Exception e) {
        log.warn("Circuit breaker fallback triggered for location availability check: locationId={}, error={}", locationId.getValueAsString(), e.getMessage());
        return LocationAvailability.unavailable("Location service unavailable");
    }

    /**
     * Fallback method for circuit breaker.
     * Returns empty when circuit is open or service is down.
     */
    @SuppressFBWarnings(value = "UPM_UNCALLED_PRIVATE_METHOD", justification = "Fallback method called by Resilience4j circuit breaker via reflection")
    private Optional<LocationInfo> getLocationInfoFallback(LocationId locationId, TenantId tenantId, Exception e) {
        log.warn("Circuit breaker fallback triggered for location info: locationId={}, error={}", locationId.getValueAsString(), e.getMessage());
        return Optional.empty();
    }

    /**
     * DTO for location-service availability response.
     */
    private static class LocationAvailabilityResponse {
        private boolean available;
        private boolean hasCapacity;
        private Integer availableCapacity;
        private String reason;

        public boolean isAvailable() {
            return available;
        }

        @SuppressWarnings("unused")
        public void setAvailable(boolean available) {
            this.available = available;
        }

        public boolean hasCapacity() {
            return hasCapacity;
        }

        @SuppressWarnings("unused")
        public void setHasCapacity(boolean hasCapacity) {
            this.hasCapacity = hasCapacity;
        }

        public Integer getAvailableCapacity() {
            return availableCapacity;
        }

        @SuppressWarnings("unused")
        public void setAvailableCapacity(Integer availableCapacity) {
            this.availableCapacity = availableCapacity;
        }

        public String getReason() {
            return reason;
        }

        @SuppressWarnings("unused")
        public void setReason(String reason) {
            this.reason = reason;
        }
    }

    /**
     * DTO for location-service query result response.
     */
    private static class LocationQueryResultResponse {
        private String locationId;
        private String name;
        private String description;
        private String code;
        private String type;
        private LocationCoordinatesResponse coordinates;

        public String getLocationId() {
            return locationId;
        }

        @SuppressWarnings("unused")
        public void setLocationId(String locationId) {
            this.locationId = locationId;
        }

        public String getName() {
            return name;
        }

        @SuppressWarnings("unused")
        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        @SuppressWarnings("unused")
        public void setDescription(String description) {
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        @SuppressWarnings("unused")
        public void setCode(String code) {
            this.code = code;
        }

        public String getType() {
            return type;
        }

        @SuppressWarnings("unused")
        public void setType(String type) {
            this.type = type;
        }

        public LocationCoordinatesResponse getCoordinates() {
            return coordinates;
        }

        @SuppressWarnings("unused")
        public void setCoordinates(LocationCoordinatesResponse coordinates) {
            this.coordinates = coordinates;
        }
    }

    /**
     * DTO for location coordinates in response.
     */
    private static class LocationCoordinatesResponse {
        private String zone;
        private String aisle;
        private String rack;
        private String level;

        public String getZone() {
            return zone;
        }

        @SuppressWarnings("unused")
        public void setZone(String zone) {
            this.zone = zone;
        }

        public String getAisle() {
            return aisle;
        }

        @SuppressWarnings("unused")
        public void setAisle(String aisle) {
            this.aisle = aisle;
        }

        public String getRack() {
            return rack;
        }

        @SuppressWarnings("unused")
        public void setRack(String rack) {
            this.rack = rack;
        }

        public String getLevel() {
            return level;
        }

        @SuppressWarnings("unused")
        public void setLevel(String level) {
            this.level = level;
        }
    }
}

