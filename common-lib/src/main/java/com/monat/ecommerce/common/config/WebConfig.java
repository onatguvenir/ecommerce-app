package com.monat.ecommerce.common.config;

import com.monat.ecommerce.common.filter.CorrelationIdFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Global Web Configuration for microservices.
 * 
 * Educational Note:
 * This class registers cross-cutting concerns like CorrelationIdFilter 
 * which ensures logs can be traced across service boundaries using a unique ID.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Registers the CorrelationIdFilter to the servlet container.
     * We use FilterRegistrationBean instead of @WebFilter for programmatic control of order.
     */
    @Bean
    public FilterRegistrationBean<CorrelationIdFilter> correlationIdFilterRegistrationBean(
            CorrelationIdFilter correlationIdFilter) {
        FilterRegistrationBean<CorrelationIdFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(correlationIdFilter);
        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(1); // Ensure it runs before security/other filters to capture ID early
        return registrationBean;
    }
}
