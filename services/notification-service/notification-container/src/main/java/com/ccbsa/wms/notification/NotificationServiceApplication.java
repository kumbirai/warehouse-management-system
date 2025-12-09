package com.ccbsa.wms.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Notification Service Application
 * <p>
 * Provides:
 * - Notification management and delivery
 * - Notification preferences management
 * - Notification history tracking
 * - Service discovery via Eureka (auto-configured)
 * <p>
 * Note: Eureka client is auto-configured when spring-cloud-starter-netflix-eureka-client is on the classpath.
 */
@SpringBootApplication(scanBasePackages = {"com.ccbsa.wms.notification",
        "com.ccbsa.wms.common.security",
        "com.ccbsa.wms.notification.email"})
@EnableJpaRepositories(basePackages = "com.ccbsa.wms.notification.dataaccess.jpa")
@EntityScan(basePackages = "com.ccbsa.wms.notification.dataaccess.entity")
public class NotificationServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class,
                args);
    }
}

