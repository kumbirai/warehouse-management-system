package com.ccbsa.common.messaging.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Custom deserializer for Object.class that uses PROPERTY format (@class property).
 * <p>
 * ARCHITECTURAL FIX: This deserializer ensures that when deserializing to Object.class, Jackson uses PROPERTY format (@class property in JSON) instead of the default WRAPPER_ARRAY
 * format.
 * <p>
 * This is critical for cross-service event consumption where events are serialized with
 *
 * @class property (PROPERTY format) but need to be deserialized as Map&lt;String, Object&gt; for loose coupling and flexibility.
 * <p>
 * CRITICAL FIX: Always deserializes to Map&lt;String, Object&gt; regardless of @class property. The @class property is preserved in the Map for event type detection, but we do NOT
 * attempt to deserialize to the actual class type. This
 * prevents deserialization failures when event classes don't have default constructors (which is the case for immutable domain events).
 * <p>
 * Implementation: Reads the JSON as a tree and always deserializes to Map, preserving all properties including @class. This bypasses Jackson's default typing mechanism which
 * expects WRAPPER_ARRAY format, and avoids deserialization failures
 * for immutable domain events.
 */
public class ObjectPropertyFormatDeserializer extends JsonDeserializer<Object> {

    public ObjectPropertyFormatDeserializer(ObjectMapper objectMapper) {
        // ObjectMapper parameter kept for API consistency, but we use the context's mapper
        // to avoid circular dependencies and ensure proper configuration
    }

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        // Read the JSON as a tree to bypass Jackson's type resolution
        JsonNode node = p.getCodec().readTree(p);

        // CRITICAL FIX: Manually convert JsonNode to Map to bypass Jackson's type resolution
        // This ensures:
        // 1. Loose coupling - listeners don't need event classes in their classpath
        // 2. Flexibility - listeners can extract data using flexible methods
        // 3. No deserialization failures - immutable domain events don't need default constructors
        // 4. @class property is preserved in Map as a string value for event type detection
        //
        // IMPORTANT: We manually convert the JsonNode to Map to completely bypass Jackson's
        // type resolution mechanism. This prevents Jackson from trying to resolve @class to
        // actual class types, which would fail for immutable domain events without default constructors.
        return convertToMap(node);
    }

    /**
     * Converts a JsonNode to a Map, recursively handling nested objects and arrays. This bypasses Jackson's type resolution and ensures @class property is preserved as a string.
     *
     * @param node The JsonNode to convert
     * @return Map representation of the JSON node
     */
    private Map<String, Object> convertToMap(JsonNode node) {
        Map<String, Object> map = new HashMap<>();

        if (node.isObject()) {
            Iterator<String> fieldNames = node.fieldNames();
            while (fieldNames.hasNext()) {
                String key = fieldNames.next();
                JsonNode value = node.get(key);

                if (value.isObject()) {
                    map.put(key, convertToMap(value));
                } else if (value.isArray()) {
                    map.put(key, convertToArray(value));
                } else if (value.isNull()) {
                    map.put(key, null);
                } else if (value.isBoolean()) {
                    map.put(key, value.booleanValue());
                } else if (value.isInt()) {
                    map.put(key, value.intValue());
                } else if (value.isLong()) {
                    map.put(key, value.longValue());
                } else if (value.isDouble() || value.isFloat()) {
                    map.put(key, value.doubleValue());
                } else {
                    // String or other text values (including @class property)
                    map.put(key, value.asText());
                }
            }
        }

        return map;
    }

    /**
     * Converts a JsonNode array to a Java List.
     *
     * @param node The JsonNode array to convert
     * @return List representation of the JSON array
     */
    private List<Object> convertToArray(JsonNode node) {
        List<Object> list = new ArrayList<>();

        if (node.isArray()) {
            for (JsonNode element : node) {
                if (element.isObject()) {
                    list.add(convertToMap(element));
                } else if (element.isArray()) {
                    list.add(convertToArray(element));
                } else if (element.isNull()) {
                    list.add(null);
                } else if (element.isBoolean()) {
                    list.add(element.booleanValue());
                } else if (element.isInt()) {
                    list.add(element.intValue());
                } else if (element.isLong()) {
                    list.add(element.longValue());
                } else if (element.isDouble() || element.isFloat()) {
                    list.add(element.doubleValue());
                } else {
                    list.add(element.asText());
                }
            }
        }

        return list;
    }
}

