package com.ccbsa.common.messaging;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.ccbsa.common.domain.DomainEvent;
import com.ccbsa.common.domain.EventMetadata;

import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for enriching domain events with metadata.
 * <p>
 * Creates immutable enriched copies of events with metadata added. Uses reflection to find constructors that accept EventMetadata and extract values from the original event using
 * getters.
 * <p>
 * This maintains event immutability while allowing infrastructure layer to add traceability information.
 * <p>
 * Note: This implementation uses reflection to extract event field values via getters and create enriched copies. If enrichment fails, the original event is returned to avoid
 * breaking event publishing.
 */
@Slf4j
public final class EventEnricher {

    private EventEnricher() {
        // Utility class
    }

    /**
     * Enriches a list of events with metadata.
     *
     * @param events   List of events to enrich
     * @param metadata The metadata to add to all events
     * @return List of enriched events
     */
    public static <T extends DomainEvent<?>> List<T> enrich(List<T> events, EventMetadata metadata) {
        if (events == null || events.isEmpty()) {
            return events;
        }
        return events.stream().map(event -> enrich(event, metadata)).collect(Collectors.toList());
    }

    /**
     * Enriches a single event with metadata by creating an immutable copy.
     *
     * @param event    The original event (without metadata)
     * @param metadata The metadata to add
     * @return Enriched event with metadata, or original event if enrichment fails
     */
    @SuppressWarnings("unchecked")
    public static <T extends DomainEvent<?>> T enrich(T event, EventMetadata metadata) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }
        if (metadata == null) {
            // No metadata to add, return original event
            return event;
        }
        if (event.getMetadata() != null) {
            // Event already has metadata, return as-is
            log.debug("Event {} already has metadata, skipping enrichment", event.getClass().getSimpleName());
            return event;
        }

        try {
            Class<? extends DomainEvent<?>> eventClass = (Class<? extends DomainEvent<?>>) event.getClass();

            // Find constructor that accepts EventMetadata as last parameter
            Constructor<?> enrichedConstructor = findEnrichedConstructor(eventClass);
            if (enrichedConstructor == null) {
                log.warn("No constructor with EventMetadata found for event class: {}. Event will be published without metadata.", eventClass.getName());
                return event;
            }

            // Extract constructor parameters from original event
            Object[] originalParams = extractConstructorParameters(event, enrichedConstructor);
            // Check if extraction failed (indicated by empty array when params are expected)
            Class<?>[] paramTypes = enrichedConstructor.getParameterTypes();
            int expectedParamCount = paramTypes.length - 1; // Exclude EventMetadata
            if (originalParams.length == 0 && expectedParamCount > 0) {
                log.warn("Failed to extract constructor parameters for event class: {}. Event will be published without metadata.", eventClass.getName());
                return event;
            }

            // Add metadata as last parameter
            Object[] enrichedParams = new Object[originalParams.length + 1];
            System.arraycopy(originalParams, 0, enrichedParams, 0, originalParams.length);
            enrichedParams[enrichedParams.length - 1] = metadata;

            // Create enriched event instance
            return (T) enrichedConstructor.newInstance(enrichedParams);
        } catch (ReflectiveOperationException | IllegalArgumentException | SecurityException e) {
            log.error("Failed to enrich event {} with metadata: {}", event.getClass().getSimpleName(), e.getMessage(), e);
            // Return original event on failure to avoid breaking event publishing
            return event;
        }
    }

    /**
     * Finds a constructor that accepts EventMetadata as the last parameter.
     *
     * @param eventClass The event class
     * @return Constructor with EventMetadata parameter, or null if not found
     */
    private static Constructor<?> findEnrichedConstructor(Class<?> eventClass) {
        for (Constructor<?> constructor : eventClass.getConstructors()) {
            Class<?>[] paramTypes = constructor.getParameterTypes();
            if (paramTypes.length > 0 && paramTypes[paramTypes.length - 1] == EventMetadata.class) {
                return constructor;
            }
        }
        return null;
    }

    /**
     * Extracts constructor parameters from an event instance using getters.
     * <p>
     * Matches constructor parameters to getters by:
     * 1. Special handling for aggregateId (first parameter, usually String from parent DomainEvent)
     * 2. Matching getters to parameters by type, ensuring each getter is only used once
     * 3. Using parameter names if available (requires -parameters compiler flag)
     *
     * @param event       The event instance
     * @param constructor The constructor to extract parameters for
     * @return Array of parameter values, or empty array if extraction fails
     */
    private static Object[] extractConstructorParameters(DomainEvent<?> event, Constructor<?> constructor) {
        try {
            Class<?>[] paramTypes = constructor.getParameterTypes();
            int paramCount = paramTypes.length - 1; // Exclude EventMetadata

            // Return empty array if no parameters (excluding EventMetadata)
            if (paramCount <= 0) {
                return new Object[0];
            }

            Object[] params = new Object[paramCount];

            // Get all getter methods from the event class (excluding parent class getters like getAggregateId)
            Method[] methods = event.getClass().getMethods();
            List<Method> availableGetters = new ArrayList<>();
            for (Method method : methods) {
                String methodName = method.getName();
                if (methodName.startsWith("get") && method.getParameterCount() == 0 && !methodName.equals("getClass") && !methodName.equals("getEventId") && !methodName.equals(
                        "getAggregateId") && !methodName.equals("getAggregateType") && !methodName.equals("getOccurredOn") && !methodName.equals("getVersion")
                        && !methodName.equals("getMetadata")) {
                    availableGetters.add(method);
                }
            }

            // Get parameter names using reflection (Java 8+)
            // Note: Parameter names are only available if compiled with -parameters flag
            java.lang.reflect.Parameter[] parameters = constructor.getParameters();

            // Track which getters have been used to avoid duplicate matches
            List<Method> usedGetters = new ArrayList<>();

            // Try to match getters to constructor parameters
            for (int i = 0; i < paramCount; i++) {
                Class<?> paramType = paramTypes[i];
                String paramName = (parameters.length > i && parameters[i].isNamePresent()) ? parameters[i].getName() : null;

                // Special handling for aggregateId (first parameter, usually String)
                if (i == 0 && paramType == String.class) {
                    // Check if parameter name is aggregateId or if it's the first String parameter
                    if (paramName == null || "aggregateId".equals(paramName)) {
                        params[i] = event.getAggregateId();
                        continue;
                    }
                }

                // Try to find a getter by parameter name first (if available)
                Method matchingGetter = null;
                if (paramName != null && !paramName.isEmpty()) {
                    // Convert parameter name to getter name (e.g., "consignmentReference" -> "getConsignmentReference")
                    String getterName = "get" + Character.toUpperCase(paramName.charAt(0)) + paramName.substring(1);
                    for (Method getter : availableGetters) {
                        if (getter.getName().equals(getterName) && paramType.isAssignableFrom(getter.getReturnType()) && !usedGetters.contains(getter)) {
                            matchingGetter = getter;
                            break;
                        }
                    }
                }

                // If no match by name, try by type (but only if we haven't used this getter yet)
                if (matchingGetter == null) {
                    for (Method getter : availableGetters) {
                        if (!usedGetters.contains(getter) && paramType.isAssignableFrom(getter.getReturnType())) {
                            matchingGetter = getter;
                            break;
                        }
                    }
                }

                if (matchingGetter != null) {
                    params[i] = matchingGetter.invoke(event);
                    usedGetters.add(matchingGetter);
                } else {
                    // If no matching getter found, log and return empty array
                    log.debug("No matching getter found for parameter {} (name={}, type={}) in event {}", i, paramName, paramType.getSimpleName(),
                            event.getClass().getSimpleName());
                    // Return empty array to indicate failure (SpotBugs prefers zero-length arrays)
                    return new Object[0];
                }
            }

            return params;
        } catch (IllegalAccessException | InvocationTargetException | SecurityException e) {
            log.debug("Failed to extract constructor parameters: {}", e.getMessage());
            // Return empty array to indicate failure (SpotBugs prefers zero-length arrays)
            return new Object[0];
        }
    }
}

