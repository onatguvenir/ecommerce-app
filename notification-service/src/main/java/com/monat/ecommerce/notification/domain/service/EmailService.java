package com.monat.ecommerce.notification.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    private final TemplateEngine templateEngine;

    @Value("${application.notification.simulate-email:true}")
    private boolean simulateEmail;

    @Value("${application.notification.from-email}")
    private String fromEmail;

    /**
     * Send email notification
     */
    public void sendEmail(String to, String subject, String templateName, Map<String, Object> variables) {
        try {
            // Render email template
            Context context = new Context();
            context.setVariables(variables);
            String htmlContent = templateEngine.process(templateName, context);

            if (simulateEmail) {
                // Simulate email sending (log to console)
                log.info("=".repeat(80));
                log.info("📧 EMAIL NOTIFICATION");
                log.info("From: {}", fromEmail);
                log.info("To: {}", to);
                log.info("Subject: {}", subject);
                log.info("-".repeat(80));
                log.info("Body:\n{}", htmlContent);
                log.info("=".repeat(80));
            } else {
                // Actual SMTP sending would go here
                log.info("Sending email to: {} - Subject: {}", to, subject);
                // mailSender.send(mimeMessage);
            }

        } catch (Exception e) {
            log.error("Failed to send email to: {}", to, e);
        }
    }

    /**
     * Send order confirmation email
     */
    public void sendOrderConfirmation(String email, String orderNumber, String customerName, String totalAmount) {
        Map<String, Object> variables = Map.of(
                "customerName", customerName,
                "orderNumber", orderNumber,
                "totalAmount", totalAmount
        );

        sendEmail(email, "Order Confirmation - " + orderNumber, "order-confirmation", variables);
    }

    /**
     * Send order completed email
     */
    public void sendOrderCompleted(String email, String orderNumber, String customerName) {
        Map<String, Object> variables = Map.of(
                "customerName", customerName,
                "orderNumber", orderNumber
        );

        sendEmail(email, "Order Completed - " + orderNumber, "order-completed", variables);
    }

    /**
     * Send order cancelled email
     */
    public void sendOrderCancelled(String email, String orderNumber, String customerName, String reason) {
        Map<String, Object> variables = Map.of(
                "customerName", customerName,
                "orderNumber", orderNumber,
                "reason", reason
        );

        sendEmail(email, "Order Cancelled - " + orderNumber, "order-cancelled", variables);
    }

    /**
     * Send payment confirmation email
     */
    public void sendPaymentConfirmation(String email, String orderNumber, String paymentReference, String amount) {
        Map<String, Object> variables = Map.of(
                "orderNumber", orderNumber,
                "paymentReference", paymentReference,
                "amount", amount
        );

        sendEmail(email, "Payment Confirmation - " + orderNumber, "payment-confirmation", variables);
    }
}
