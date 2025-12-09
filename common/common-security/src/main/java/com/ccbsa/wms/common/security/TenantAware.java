package com.ccbsa.wms.common.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark methods or classes that require tenant context validation.
 * Used in combination with aspect-oriented programming to validate tenant boundaries.
 */
@Target( {ElementType.METHOD,
        ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface TenantAware {
    /**
     * Whether to validate that the tenant ID matches the aggregate's tenant ID.
     * Default is true.
     */
    boolean validate() default true;
}

