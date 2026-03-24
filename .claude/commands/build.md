Build the specified microservice module.

If no service name is provided in $ARGUMENTS, ask the user which service to build before proceeding.

Run: `mvn compile -pl $ARGUMENTS --also-make -q`

Valid service names: api-gateway, user-service, product-service, inventory-service, cart-service, order-service, payment-service, notification-service

After building, report any compilation errors clearly with the file path and line number. If the build succeeds, confirm with the module name.
