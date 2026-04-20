package com.monat.ecommerce.notification.domain.service;

import com.monat.ecommerce.events.notification.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

/**
 * Email notification service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private static final String CUSTOMER_NAME = "customerName";
    private static final String ORDER_NUMBER = "orderNumber";
    private static final String REASON = "reason";

    private final TemplateEngine templateEngine;
    private EmailService self; // Self-injection for @Async proxy

    @org.springframework.beans.factory.annotation.Autowired
    public void setSelf(@Lazy EmailService self) {
        this.self = self;
    }

    @Value("${application.notification.simulate-email:true}")
    private boolean simulateEmail;

    @Value("${application.notification.from-email}")
    private String fromEmail;

    /**
     * Send email notification.
     *
     * @Async: This method runs on a separate VirtualThread executor as defined in AsyncConfig. 
     *         This ensures the KafkaListener thread is not blocked, increasing consumer throughput.
     */
    @Async("taskExecutor")
    public void sendEmail(String to, String subject, String templateName, Map<String, Object> variables) {
        try {
            // Render email template
            Context context = new Context();
            context.setVariables(variables);
            String htmlContent = templateEngine.process(templateName, context);

            if (simulateEmail) {
                // Simulate email sending (log to console)
                log.info("=".repeat(80));
                log.info("📧 {} NOTIFICATION", NotificationType.EMAIL);
                log.info("From: {}", fromEmail);
                log.info("To: {}", to);
                log.info("Subject: {}", subject);
                log.info("-".repeat(80));
                log.info("Body:\n{}", htmlContent);
                log.info("=".repeat(80));
            } else {
                log.info("Sending email to: {} - Subject: {}", to, subject);
            }

        } catch (Exception e) {
            log.error("Failed to send email to: {}", to, e);
        }
    }

    /** Order confirmation email — Runs asynchronously on a VirtualThread */
    @Async("taskExecutor")
    public void sendOrderConfirmation(String email, String orderNumber, String customerName, String totalAmount) {
        Map<String, Object> variables = Map.of(
                CUSTOMER_NAME, customerName,
                ORDER_NUMBER, orderNumber,
                "totalAmount", totalAmount
        );

        self.sendEmail(email, "Order Confirmation - " + orderNumber, "order-confirmation", variables);
    }

    /** Order completed email */
    @Async("taskExecutor")
    public void sendOrderCompleted(String email, String orderNumber, String customerName) {
        Map<String, Object> variables = Map.of(
                CUSTOMER_NAME, customerName,
                ORDER_NUMBER, orderNumber
        );

        self.sendEmail(email, "Order Completed - " + orderNumber, "order-completed", variables);
    }

    /** Order cancelled email */
    @Async("taskExecutor")
    public void sendOrderCancelled(String email, String orderNumber, String customerName, String reason) {
        Map<String, Object> variables = Map.of(
                CUSTOMER_NAME, customerName,
                ORDER_NUMBER, orderNumber,
                REASON, reason
        );

        self.sendEmail(email, "Order Cancelled - " + orderNumber, "order-cancelled", variables);
    }

    /** Payment confirmation email */
    @Async("taskExecutor")
    public void sendPaymentConfirmation(String email, String orderNumber, String paymentReference, String amount) {
        Map<String, Object> variables = Map.of(
                ORDER_NUMBER, orderNumber,
                "paymentReference", paymentReference,
                "amount", amount
        );

        self.sendEmail(email, "Payment Confirmation - " + orderNumber, "payment-confirmation", variables);
    }
}
