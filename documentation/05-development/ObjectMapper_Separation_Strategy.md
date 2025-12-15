# ObjectMapper Separation Strategy

## Problem Statement

The original implementation used a single `ObjectMapper` bean for both:

1. **Kafka message serialization/deserialization** - Requires type information (@class property) for polymorphic event handling
2. **REST API response serialization** - Must NOT include type information for frontend consumption

Using `@Primary` annotation was a code smell indicating a design issue where two different concerns were competing for the same resource.

## Production-Grade Solution

### Design Principles

1. **Explicit Bean Naming**: Use named beans to clearly separate concerns
2. **Qualifier-Based Injection**: Explicitly inject the correct ObjectMapper by name
3. **No @Primary Workarounds**: Avoid ambiguous bean resolution
4. **Clear Separation of Concerns**: Kafka and REST API have distinct ObjectMapper configurations

### Implementation

#### 1. Kafka ObjectMapper (`kafkaObjectMapper`)

**Location**: `common/common-messaging/src/main/java/com/ccbsa/common/messaging/config/KafkaConfig.java`

```java
@Bean("kafkaObjectMapper")
public ObjectMapper kafkaObjectMapper() {
    // Configured with type information for polymorphic event deserialization
    // Includes @class property in JSON for event type detection
}
```

**Characteristics**:

- Includes type information (@class property) via mixins
- Configured for PROPERTY format (not WRAPPER_ARRAY)
- Used exclusively for Kafka message serialization/deserialization
- All Kafka beans explicitly inject this by name using `@Qualifier("kafkaObjectMapper")`

#### 2. REST API ObjectMapper (`objectMapper` with @Primary)

**Location**: `services/user-service/user-container/src/main/java/com/ccbsa/wms/user/config/WebMvcConfig.java`

```java
@Bean
public Jackson2ObjectMapperBuilder jackson2ObjectMapperBuilder() {
    return new Jackson2ObjectMapperBuilder()
        .modules(new JavaTimeModule())
        .featuresToDisable(WRITE_DATES_AS_TIMESTAMPS)
        .postConfigurer(objectMapper -> {
            objectMapper.setDefaultTyping(null); // Explicitly disable type information
        });
}

@Bean
@Primary
public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
    ObjectMapper mapper = builder.build();
    mapper.setDefaultTyping(null); // Explicitly ensure no type information
    return mapper;
}
```

**Characteristics**:

- NO type information included
- Clean JSON suitable for frontend consumption
- Marked as `@Primary` for HTTP message conversion (Spring Boot's default)
- Built from `Jackson2ObjectMapperBuilder` to ensure proper configuration
- **Note**: The `@Primary` annotation here is a design choice for HTTP default, not a workaround. Kafka and Redis use explicit named beans.

### Bean Usage

#### Kafka Components

All Kafka-related beans explicitly inject `kafkaObjectMapper`:

```java
@Bean
public ProducerFactory<String, Object> producerFactory(
    @Qualifier("kafkaObjectMapper") ObjectMapper kafkaObjectMapper) {
    // Uses kafkaObjectMapper
}

@Bean
public ConsumerFactory<String, Object> consumerFactory(
    @Qualifier("kafkaObjectMapper") ObjectMapper kafkaObjectMapper) {
    // Uses kafkaObjectMapper
}
```

#### REST API Components

Spring Boot's `MappingJackson2HttpMessageConverter` automatically uses the `@Primary` ObjectMapper bean for HTTP message conversion. The REST API ObjectMapper is built from
`Jackson2ObjectMapperBuilder` and marked as `@Primary` to serve as the default for HTTP.

**Important**: The `@Primary` annotation on the REST API ObjectMapper is a design choice for HTTP defaults, not a workaround. All other ObjectMappers (Kafka, Redis) are explicitly
named and injected using `@Qualifier`.

### Benefits

1. **Clear Separation**: Explicit bean naming for Kafka and Redis, @Primary for HTTP default
2. **Maintainability**: Easy to modify Kafka, Redis, or REST API serialization independently
3. **Type Safety**: Compile-time checking ensures correct ObjectMapper is used
4. **Design Clarity**: @Primary is used intentionally for HTTP default, not as a workaround
5. **Production-Ready**: Follows Spring best practices for bean configuration

### Migration Notes

When adding new services:

1. **For Kafka**: Always inject `@Qualifier("kafkaObjectMapper") ObjectMapper`
2. **For REST API**:
    - Configure `Jackson2ObjectMapperBuilder` bean
    - Create `@Primary ObjectMapper` bean built from the builder (for HTTP message conversion)
3. **For Redis Cache**: Use `@Qualifier("redisCacheObjectMapper") ObjectMapper` (if needed)
4. **@Primary Usage**: Only use `@Primary` for the REST API ObjectMapper (HTTP default). All other ObjectMappers should be explicitly named and injected with `@Qualifier`.

### Verification

To verify the separation is working:

1. **Kafka messages** should include `@class` property in JSON
2. **REST API responses** should NOT include type information (no @class, no array wrappers)
3. **No compilation errors** about ambiguous bean resolution
4. **No @Primary annotations** in ObjectMapper configuration

## Related Files

- `common/common-messaging/src/main/java/com/ccbsa/common/messaging/config/KafkaConfig.java`
- `services/user-service/user-container/src/main/java/com/ccbsa/wms/user/config/WebMvcConfig.java`
- `services/user-service/user-container/src/main/java/com/ccbsa/wms/user/config/UserServiceConfiguration.java`

