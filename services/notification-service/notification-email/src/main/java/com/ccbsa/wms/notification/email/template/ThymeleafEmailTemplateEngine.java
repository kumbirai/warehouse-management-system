package com.ccbsa.wms.notification.email.template;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Adapter: ThymeleafEmailTemplateEngine
 * <p>
 * Implements EmailTemplateEngine using Thymeleaf template engine. Renders HTML email templates from resources/templates/email/ directory.
 */
@Component
@SuppressFBWarnings(value = "EI_EXPOSE_REP2",
        justification = "TemplateEngine is a managed bean and treated as immutable port")
public class ThymeleafEmailTemplateEngine
        implements EmailTemplateEngine {
    private static final Logger logger = LoggerFactory.getLogger(ThymeleafEmailTemplateEngine.class);
    private static final String TEMPLATE_PREFIX = "email/";
    private static final String TEMPLATE_SUFFIX = ".html";

    private final TemplateEngine templateEngine;

    public ThymeleafEmailTemplateEngine(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    @Override
    public String render(String templateName, Map<String, Object> context) throws TemplateException {
        try {
            String fullTemplateName = TEMPLATE_PREFIX + templateName + TEMPLATE_SUFFIX;
            logger.debug("Rendering email template: {} with context keys: {}", fullTemplateName, context.keySet());

            Context thymeleafContext = new Context();
            context.forEach(thymeleafContext::setVariable);

            String rendered = templateEngine.process(fullTemplateName, thymeleafContext);
            logger.debug("Successfully rendered email template: {}", fullTemplateName);

            return rendered;
        } catch (Exception e) {
            logger.error("Failed to render email template: {}", templateName, e);
            throw new TemplateException(String.format("Failed to render email template '%s': %s", templateName, e.getMessage()), e);
        }
    }
}

