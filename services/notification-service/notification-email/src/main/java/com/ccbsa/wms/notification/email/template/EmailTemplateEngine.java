package com.ccbsa.wms.notification.email.template;

import java.util.Map;

/**
 * Port: EmailTemplateEngine
 * <p>
 * Interface for rendering email templates.
 * Provides abstraction for template engine implementations (Thymeleaf, FreeMarker, etc.).
 */
public interface EmailTemplateEngine {
    /**
     * Renders an email template with the given context variables.
     *
     * @param templateName Name of the template (e.g., "user-created")
     * @param context      Context variables for template rendering
     * @return Rendered HTML content
     * @throws TemplateException if template rendering fails
     */
    String render(String templateName, Map<String, Object> context) throws TemplateException;
}

