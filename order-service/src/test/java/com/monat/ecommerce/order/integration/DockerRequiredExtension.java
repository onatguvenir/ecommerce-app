package com.monat.ecommerce.order.integration;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.DockerClientFactory;

/**
 * JUnit 5 ExecutionCondition that skips (disables) a test class when Docker
 * is not available in the current environment.
 * Runs BEFORE Testcontainers' BeforeAllCallback, preventing any container start attempt.
 */
public class DockerRequiredExtension implements ExecutionCondition {

    private static final ConditionEvaluationResult ENABLED =
        ConditionEvaluationResult.enabled("Docker is available");
    private static final ConditionEvaluationResult DISABLED =
        ConditionEvaluationResult.disabled(
            "Docker environment not found — integration test skipped.");

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        try {
            DockerClientFactory.instance().client();
            return ENABLED;
        } catch (Exception e) {
            return DISABLED;
        }
    }
}
