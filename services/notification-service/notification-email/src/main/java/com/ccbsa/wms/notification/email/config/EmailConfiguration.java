package com.ccbsa.wms.notification.email.config;

import java.util.Properties;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

/**
 * Configuration: EmailConfiguration
 * <p>
 * Configures JavaMailSender bean for SMTP email delivery. Only enabled when notification.email.enabled is true (default: true).
 */
@Configuration
@ConditionalOnProperty(name = "notification.email.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties( {SmtpConfiguration.class, EmailConfigurationProperties.class})
public class EmailConfiguration {

    @Bean
    public JavaMailSender javaMailSender(SmtpConfiguration smtpConfig) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(smtpConfig.getHost());
        mailSender.setPort(smtpConfig.getPort());

        // Set username and password if provided
        if (smtpConfig.getUsername() != null && !smtpConfig.getUsername().isEmpty()) {
            mailSender.setUsername(smtpConfig.getUsername());
        }
        if (smtpConfig.getPassword() != null && !smtpConfig.getPassword().isEmpty()) {
            mailSender.setPassword(smtpConfig.getPassword());
        }

        // Configure JavaMail properties
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "false"); // MailHog doesn't require auth
        props.put("mail.smtp.starttls.enable", "false");
        props.put("mail.debug", "false");

        return mailSender;
    }

    /**
     * Configures Thymeleaf TemplateEngine for email templates. Templates are loaded from classpath:templates/email/
     */
    @Bean
    public TemplateEngine emailTemplateEngine() {
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(emailTemplateResolver());
        return templateEngine;
    }

    private ClassLoaderTemplateResolver emailTemplateResolver() {
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("templates/email/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setCharacterEncoding("UTF-8");
        templateResolver.setCacheable(false); // Disable cache for development, enable in production
        templateResolver.setOrder(1);
        return templateResolver;
    }
}

