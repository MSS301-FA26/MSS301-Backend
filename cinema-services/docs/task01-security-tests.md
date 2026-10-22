# Task 01 security regression tests

From the `cinema-services` directory, with Java 21 and Maven installed, run:

```sh
mvn clean package -Dtest=AiGatewayHttpIntegrationTest,TrustedGatewayFilterTest,GatewayHttpIntegrationTest,SecurityRegressionTest,VnpayCallbackIntegrationTest,PaymentBookingValidationTest,PaymentIdentityTest,IdentityGatewayCompatibilityTest,CatalogGatewayCompatibilityTest,CinemaSecurityServiceTest,AdminBookingServiceTest -Dsurefire.failIfNoSpecifiedTests=false
```

The HTTP harness uses the Maven wrapper to build the Gateway in a fresh temporary directory for each test application, then runs that executable JAR. It does not use an existing `api-gateway/target` artifact. The test-specific gateway route forwards to a local test server; it does not exercise production routing. The command deliberately selects the listed tests and excludes other test classes, including PostgreSQL concurrency and BookingSeatConcurrencyIntegrationTest. No real database or VNPay service is used.
