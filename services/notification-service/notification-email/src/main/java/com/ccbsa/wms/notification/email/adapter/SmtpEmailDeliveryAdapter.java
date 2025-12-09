package com.ccbsa.wms.notification.email.adapter;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import com.ccbsa.common.domain.valueobject.EmailAddress;
import com.ccbsa.wms.notification.application.service.command.dto.DeliveryResult;
import com.ccbsa.wms.notification.application.service.port.delivery.NotificationDeliveryPort;
import com.ccbsa.wms.notification.application.service.port.service.TenantServicePort;
import com.ccbsa.wms.notification.application.service.port.service.UserServicePort;
import com.ccbsa.wms.notification.domain.core.entity.Notification;
import com.ccbsa.wms.notification.domain.core.valueobject.NotificationChannel;
import com.ccbsa.wms.notification.domain.core.valueobject.NotificationType;
import com.ccbsa.wms.notification.email.config.EmailConfigurationProperties;
import com.ccbsa.wms.notification.email.config.SmtpConfiguration;
import com.ccbsa.wms.notification.email.template.EmailTemplateEngine;
import com.ccbsa.wms.notification.email.template.TemplateException;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

/**
 * Adapter: SmtpEmailDeliveryAdapter
 * <p>
 * Implements NotificationDeliveryPort for email delivery via SMTP.
 * Sends emails using JavaMailSender configured for MailHog (or other SMTP servers).
 * <p>
 * Only enabled when notification.email.enabled is true (default: true).
 */
@Component
@ConditionalOnProperty(name = "notification.email.enabled", havingValue = "true", matchIfMissing = true)
public class SmtpEmailDeliveryAdapter implements NotificationDeliveryPort {
    private static final Logger logger = LoggerFactory.getLogger(SmtpEmailDeliveryAdapter.class);
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JavaMailSender mailSender;
    private final SmtpConfiguration smtpConfig;
    private final EmailConfigurationProperties emailConfig;
    private final UserServicePort userServicePort;
    private final TenantServicePort tenantServicePort;
    private final EmailTemplateEngine templateEngine;

    public SmtpEmailDeliveryAdapter(
            JavaMailSender mailSender,
            SmtpConfiguration smtpConfig,
            EmailConfigurationProperties emailConfig,
            UserServicePort userServicePort,
            TenantServicePort tenantServicePort,
            EmailTemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.smtpConfig = smtpConfig;
        this.emailConfig = emailConfig;
        this.userServicePort = userServicePort;
        this.tenantServicePort = tenantServicePort;
        this.templateEngine = templateEngine;
    }

    @Override
    public boolean supports(NotificationChannel channel) {
        return channel == NotificationChannel.EMAIL;
    }

    @Override
    public DeliveryResult send(Notification notification, NotificationChannel channel) {
        if (channel != NotificationChannel.EMAIL) {
            return DeliveryResult.failure("SmtpEmailDeliveryAdapter only supports EMAIL channel");
        }

        try {
            // 1. Get recipient email (prefer stored email from event, fallback to user service)
            EmailAddress recipientEmailAddress = getRecipientEmail(notification);
            String emailValue = recipientEmailAddress.getValue();

            // 2. Validate email address format
            if (!isValidEmail(emailValue)) {
                logger.warn("Invalid email address format: notificationId={}, email={}",
                        notification.getId(), emailValue);
                return DeliveryResult.failure(
                        String.format("Invalid email address format: %s", emailValue));
            }

            logger.debug("Sending email to: {}, notificationId={}, type={}",
                    emailValue, notification.getId(), notification.getType());

            // 3. Build email message
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // 4. Set email headers
            helper.setFrom(smtpConfig.getFromAddress());
            helper.setTo(emailValue);
            helper.setSubject(notification.getTitle().getValue());

            // Set Reply-To if configured
            if (emailConfig.getReplyTo() != null && !emailConfig.getReplyTo().isEmpty()) {
                helper.setReplyTo(emailConfig.getReplyTo());
            }

            // Add custom headers for tracking
            message.setHeader("X-Notification-ID", notification.getId().getValue().toString());
            message.setHeader("X-Notification-Type", notification.getType().name());

            // 5. Render HTML email body using template engine
            String htmlContent = renderEmailBody(notification);
            helper.setText(htmlContent, true);

            // 6. Send email
            mailSender.send(message);

            // 7. Extract message ID if available
            String messageId = message.getMessageID();
            logger.info("Email sent successfully: notificationId={}, recipient={}, type={}, messageId={}",
                    notification.getId(), emailValue, notification.getType(), messageId);

            return DeliveryResult.success(messageId);

        } catch (TemplateException e) {
            logger.error("Failed to render email template: notificationId={}, type={}, error={}",
                    notification.getId(), notification.getType(), e.getMessage(), e);
            return DeliveryResult.failure(
                    String.format("Failed to render email template: %s", e.getMessage()));
        } catch (MessagingException e) {
            logger.error("Failed to create email message: notificationId={}, recipient={}, type={}, error={}",
                    notification.getId(), getRecipientEmail(notification).getValue(),
                    notification.getType(), e.getMessage(), e);
            return DeliveryResult.failure(
                    String.format("Failed to create email message: %s", e.getMessage()));
        } catch (MailException e) {
            logger.error("Failed to send email: notificationId={}, recipient={}, type={}, error={}",
                    notification.getId(), getRecipientEmail(notification).getValue(),
                    notification.getType(), e.getMessage(), e);
            return DeliveryResult.failure(
                    String.format("Failed to send email: %s", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error sending email: notificationId={}, type={}, error={}",
                    notification.getId(), notification.getType(), e.getMessage(), e);
            return DeliveryResult.failure(
                    String.format("Unexpected error: %s", e.getMessage()));
        }
    }

    /**
     * Gets recipient email address.
     * <p>
     * Primary: Uses email stored in notification (from event payload) - zero latency.
     * Fallback: Retrieves from user-service via REST API (for edge cases like resending old notifications).
     *
     * @param notification Notification entity
     * @return Recipient email address
     */
    private EmailAddress getRecipientEmail(Notification notification) {
        // Prefer email stored in notification (from event payload)
        if (notification.getRecipientEmail() != null) {
            logger.debug("Using email from notification aggregate: notificationId={}", notification.getId());
            return notification.getRecipientEmail();
        }

        // Fallback: Retrieve from user-service (for edge cases like resending old notifications)
        logger.debug("Email not in notification, retrieving from user-service: notificationId={}, userId={}",
                notification.getId(), notification.getRecipientUserId());
        return userServicePort.getUserEmail(notification.getRecipientUserId());
    }

    /**
     * Validates email address format using regex pattern.
     *
     * @param email Email address to validate
     * @return true if email format is valid
     */
    private boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        try {
            // Use JavaMail InternetAddress for validation
            InternetAddress emailAddr = new InternetAddress(email);
            emailAddr.validate();
            return EMAIL_PATTERN.matcher(email).matches();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Renders email body using template engine.
     * Maps NotificationType to template name and builds context from notification.
     *
     * @param notification Notification entity
     * @return Rendered HTML content
     * @throws TemplateException if template rendering fails
     */
    private String renderEmailBody(Notification notification) throws TemplateException {
        String templateName = getTemplateName(notification.getType());
        Map<String, Object> context = buildTemplateContext(notification);

        logger.debug("Rendering email template: templateName={}, notificationId={}, contextKeys={}",
                templateName, notification.getId(), context.keySet());

        return templateEngine.render(templateName, context);
    }

    /**
     * Maps NotificationType to template name.
     *
     * @param type Notification type
     * @return Template name (without .html extension)
     */
    private String getTemplateName(NotificationType type) {
        return switch (type) {
            case USER_CREATED -> "user-created";
            case USER_ROLE_ASSIGNED -> "user-role-assigned";
            case USER_ROLE_REMOVED -> "user-role-removed";
            case USER_UPDATED, USER_ACTIVATED, USER_SUSPENDED -> "user-updated";
            case USER_DEACTIVATED -> "user-deactivated";
            case TENANT_CREATED -> "tenant-created";
            case TENANT_ACTIVATED -> "tenant-activated";
            case TENANT_DEACTIVATED -> "tenant-deactivated";
            case TENANT_SUSPENDED -> "tenant-suspended";
            default -> "user-updated"; // Default fallback template
        };
    }

    /**
     * Builds template context from notification entity.
     * Extracts available information and formats for template rendering.
     *
     * @param notification Notification entity
     * @return Template context map
     */
    private Map<String, Object> buildTemplateContext(Notification notification) {
        Map<String, Object> context = new HashMap<>();

        // Basic notification info
        context.put("title", notification.getTitle().getValue());
        context.put("message", notification.getMessage().getValue());
        context.put("notificationId", notification.getId().getValue().toString());
        context.put("type", notification.getType().name());

        // Tenant info - use the actual tenant ID from notification
        String tenantIdValue = notification.getTenantId().getValue();
        context.put("tenantId", tenantIdValue);

        // Fetch and include full tenant details for tenant-related notifications
        if (isTenantNotification(notification.getType())) {
            try {
                tenantServicePort.getTenantDetails(notification.getTenantId())
                        .ifPresent(details -> {
                            context.put("tenantName", details.name());
                            context.put("tenantStatus", details.status());
                            if (details.emailAddress() != null) {
                                context.put("tenantEmail", details.emailAddress());
                            }
                            if (details.phone() != null && !details.phone().isEmpty()) {
                                context.put("tenantPhone", details.phone());
                            }
                            if (details.address() != null && !details.address().isEmpty()) {
                                context.put("tenantAddress", details.address());
                            }
                        });
            } catch (Exception e) {
                logger.warn("Failed to fetch tenant details for notification: notificationId={}, tenantId={}, error={}",
                        notification.getId(), tenantIdValue, e.getMessage());
                // Continue without tenant details - template will show basic info
            }
        }

        // User info
        if (notification.getRecipientEmail() != null) {
            context.put("email", notification.getRecipientEmail().getValue());
        }
        context.put("userId", notification.getRecipientUserId().getValue());

        // Timestamps
        if (notification.getCreatedAt() != null) {
            context.put("createdAt", notification.getCreatedAt().format(DATE_FORMATTER));
        }
        if (notification.getSentAt() != null) {
            context.put("sentAt", notification.getSentAt().format(DATE_FORMATTER));
        }

        // Support email for footer
        if (emailConfig.getSupportEmail() != null) {
            context.put("supportEmail", emailConfig.getSupportEmail());
        }

        // Extract additional context from message for specific notification types
        extractAdditionalContext(notification, context);

        return context;
    }

    /**
     * Checks if the notification type is tenant-related.
     *
     * @param type Notification type
     * @return true if this is a tenant-related notification
     */
    private boolean isTenantNotification(NotificationType type) {
        return type == NotificationType.TENANT_CREATED
                || type == NotificationType.TENANT_ACTIVATED
                || type == NotificationType.TENANT_DEACTIVATED
                || type == NotificationType.TENANT_SUSPENDED;
    }

    /**
     * Extracts additional context from notification message for specific types.
     * Parses message content to extract structured data when available.
     *
     * @param notification Notification entity
     * @param context      Template context map to populate
     */
    private void extractAdditionalContext(Notification notification, Map<String, Object> context) {
        String message = notification.getMessage().getValue();
        NotificationType type = notification.getType();

        // Extract role name from role assignment/removal messages
        if (type == NotificationType.USER_ROLE_ASSIGNED || type == NotificationType.USER_ROLE_REMOVED) {
            // Message format: "The role 'ROLE_NAME' has been assigned/removed..."
            int roleStart = message.indexOf("'");
            int roleEnd = message.indexOf("'", roleStart + 1);
            if (roleStart >= 0 && roleEnd > roleStart) {
                String roleName = message.substring(roleStart + 1, roleEnd);
                context.put("roleName", roleName);
                if (type == NotificationType.USER_ROLE_ASSIGNED) {
                    context.put("assignedAt", notification.getCreatedAt() != null
                            ? notification.getCreatedAt().format(DATE_FORMATTER) : null);
                } else {
                    context.put("removedAt", notification.getCreatedAt() != null
                            ? notification.getCreatedAt().format(DATE_FORMATTER) : null);
                }
            }
        }

        // For user-created, try to extract username from message
        if (type == NotificationType.USER_CREATED) {
            // Message format: "Welcome! Your account has been created. Username: USERNAME"
            int usernameStart = message.indexOf("Username: ");
            if (usernameStart >= 0) {
                String username = message.substring(usernameStart + 10).trim();
                context.put("username", username);
            }
        }
    }
}

