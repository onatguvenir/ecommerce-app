package com.monat.ecommerce.common.config;

import com.monat.ecommerce.common.filter.CorrelationIdFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web Configuration Class.
 * <p>
 * This class is used to configure Spring MVC settings.
 * </p>
 * 
 * @Configuration indicates that this class declares one or more @Bean methods
 *                and may be processed by the Spring container
 *                to generate bean definitions and service requests for those
 *                beans at runtime.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Bean
    public FilterRegistrationBean<CorrelationIdFilter> correlationIdFilterRegistrationBean(
            CorrelationIdFilter correlationIdFilter) {
        FilterRegistrationBean<CorrelationIdFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(correlationIdFilter);
        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(1); // Set order to ensure it runs early
        return registrationBean;
    }
}
