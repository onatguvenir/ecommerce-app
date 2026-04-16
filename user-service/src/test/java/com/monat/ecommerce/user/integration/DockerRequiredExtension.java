package com.monat.ecommerce.user.integration;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.DockerClientFactory;

/**
 * JUnit 5 ExecutionCondition that skips (disables) a test class when Docker
 * is not available in the current environment.
 *
 * <p>Execution order in JUnit 5:
 * <ol>
 *   <li>ExecutionCondition evaluations (this class) — runs FIRST</li>
 *   <li>BeforeAllCallback extensions (e.g. Testcontainers, Spring context load)</li>
 *   <li>@BeforeAll methods</li>
 * </ol>
 *
 * <p>Because this condition runs BEFORE the Testcontainers BeforeAllCallback,
 * no container start is attempted when Docker is unavailable. The test is simply
 * reported as SKIPPED instead of FAILED.
 */
public class DockerRequiredExtension implements ExecutionCondition {

    private static final ConditionEvaluationResult ENABLED =
        ConditionEvaluationResult.enabled("Docker is available");
    private static final ConditionEvaluationResult DISABLED =
        ConditionEvaluationResult.disabled(
            "Docker environment not found — integration test skipped. "
            + "Ensure Docker Desktop is running and accessible.");

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
