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
    private static final ParameterizedTypeReference<ApiResponse<LocationInfoResponse>> LOCATION_INFO_RESPONSE_TYPE =
            new ParameterizedTypeReference<ApiResponse<LocationInfoResponse>>() {
            };

    private final RestTemplate restTemplate;
    private final String locationServiceUrl;

    public LocationServiceAdapter(RestTemplate restTemplate, @Value("${location.service.url:http://location-management-service:8080}") String locationServiceUrl) {
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

            ResponseEntity<ApiResponse<LocationInfoResponse>> response = restTemplate.exchange(url, HttpMethod.GET, entity, LOCATION_INFO_RESPONSE_TYPE);

            ApiResponse<LocationInfoResponse> responseBody = response.getBody();
            if (response.getStatusCode() == HttpStatus.OK && responseBody != null && responseBody.getData() != null) {
                LocationInfoResponse locationResponse = responseBody.getData();
                return Optional.of(new LocationInfo(locationResponse.getLocationId(), locationResponse.getName(), locationResponse.getDescription(), locationResponse.getCode()));
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
     * DTO for location-service info response.
     */
    private static class LocationInfoResponse {
        private String locationId;
        private String name;
        private String description;
        private String code;

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
    }
}

