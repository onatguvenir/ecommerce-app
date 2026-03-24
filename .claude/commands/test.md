Run tests for the specified microservice module.

If no service name is provided in $ARGUMENTS, ask the user which service to test before proceeding.

Run: `mvn test -pl $ARGUMENTS --also-make`

Valid service names: api-gateway, user-service, product-service, inventory-service, cart-service, order-service, payment-service, notification-service

After running, report:
- Number of tests run, passed, failed, skipped
- Full output for any failing tests including the assertion that failed and the stack trace
- If all tests pass, confirm with a summary
