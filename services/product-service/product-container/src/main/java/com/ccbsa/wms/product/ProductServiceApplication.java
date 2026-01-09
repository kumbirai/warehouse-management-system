package com.ccbsa.wms.product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.ccbsa.wms.common.dataaccess.config.MultiTenantDataAccessConfig;

@SpringBootApplication(scanBasePackages = {"com.ccbsa.wms.product", "com.ccbsa.wms.common.security", "com.ccbsa.common.cache"})
@EnableJpaRepositories(basePackages = "com.ccbsa.wms.product.dataaccess.jpa")
@EntityScan(basePackages = "com.ccbsa.wms.product.dataaccess.entity")
@Import(MultiTenantDataAccessConfig.class)
public class ProductServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProductServiceApplication.class, args);
    }
}
