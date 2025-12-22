package com.ccbsa.wms.stock.dataaccess.adapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.stock.application.service.port.service.LocationServicePort;
import com.ccbsa.wms.stock.domain.core.valueobject.Quantity;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

/**
 * Adapter: LocationServiceAdapter
 * <p>
 * Implements LocationServicePort for checking location availability from location-management-service.
 * Uses circuit breaker and retry for fault tolerance.
 */
@Component
public class LocationServiceAdapter implements LocationServicePort {
    private static final Logger logger = LoggerFactory.getLogger(LocationServiceAdapter.class);

    private static final ParameterizedTypeReference<ApiResponse<LocationAvailabilityResponse>> LOCATION_AVAILABILITY_RESPONSE_TYPE =
            new ParameterizedTypeReference<ApiResponse<LocationAvailabilityResponse>>() {
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

        logger.debug("Checking location availability from location-service: locationId={}, quantity={}, tenantId={}", locationId.getValueAsString(), requiredQuantity.getValue(),
                tenantId.getValue());

        try {
            String url =
                    String.format("%s/api/v1/locations/%s/check-availability?requiredQuantity=%d", locationServiceUrl, locationId.getValueAsString(), requiredQuantity.getValue());
            logger.debug("Calling location service: {}", url);

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Tenant-Id", tenantId.getValue());
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<ApiResponse<LocationAvailabilityResponse>> response = restTemplate.exchange(url, HttpMethod.GET, entity, LOCATION_AVAILABILITY_RESPONSE_TYPE);

            ApiResponse<LocationAvailabilityResponse> responseBody = response.getBody();
            if (response.getStatusCode() == HttpStatus.OK && responseBody != null && responseBody.getData() != null) {
                LocationAvailabilityResponse availabilityResponse = responseBody.getData();
                return mapToLocationAvailability(availabilityResponse);
            }

            logger.warn("Location service returned unexpected response: status={}", response.getStatusCode());
            return LocationAvailability.unavailable("Location service returned unexpected response");
        } catch (HttpClientErrorException.NotFound e) {
            logger.warn("Location not found: {}", locationId.getValueAsString());
            return LocationAvailability.unavailable("Location not found");
        } catch (RestClientException e) {
            logger.error("Failed to check location availability from location-service: locationId={}", locationId.getValueAsString(), e);
            throw new RuntimeException(String.format("Failed to check location availability: %s", e.getMessage()), e);
        } catch (Exception e) {
            logger.error("Unexpected error checking location availability: locationId={}", locationId.getValueAsString(), e);
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

    /**
     * Fallback method for circuit breaker.
     * Returns unavailable when circuit is open or service is down.
     */
    @SuppressFBWarnings(value = "UPM_UNCALLED_PRIVATE_METHOD", justification = "Fallback method called by Resilience4j circuit breaker via reflection")
    private LocationAvailability checkLocationAvailabilityFallback(LocationId locationId, Quantity requiredQuantity, TenantId tenantId, Exception e) {
        logger.warn("Circuit breaker fallback triggered for location availability check: locationId={}, error={}", locationId.getValueAsString(), e.getMessage());
        return LocationAvailability.unavailable("Location service unavailable");
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
}

