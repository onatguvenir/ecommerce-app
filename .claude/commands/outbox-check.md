Verify that the specified service correctly implements the Transactional Outbox Pattern.

@.claude/rules/outbox-pattern.md

If $ARGUMENTS specifies a service name, check that service's source code.
If $ARGUMENTS is empty, ask the user which service to check.

Search for and audit:
1. Any direct calls to `kafkaTemplate.send()` inside `@Transactional` methods — flag as VIOLATION
2. Presence of an `OutboxEvent` or equivalent entity with required fields (id, topic, payload, processedAt, createdAt)
3. Presence of a `@Scheduled` poller that reads unprocessed outbox events and publishes them
4. The poller must be `@Transactional` — flag if it is not
5. Outbox events must be marked processed, not deleted — flag DELETE operations

Report each finding as PASS or VIOLATION with file and line number.
Provide a final verdict: COMPLIANT or NON-COMPLIANT with a summary of violations.
