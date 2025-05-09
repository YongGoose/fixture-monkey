---
title: "Best Practices"
weight: 100
menu:
  docs:
    parent: "cheat-sheet"
    identifier: "best-practices"
---

This guide provides practical tips and best practices for using Fixture Monkey effectively in your tests.

### 1. Keep Tests Simple and Focused

- **Only customize what matters for the test**: Don't set values for fields that don't affect the test's behavior.

{{< tabpane persist=false >}}
{{< tab header="Java" lang="java">}}
// Good - only set what's relevant to the test
@Test
void shouldCalculateDiscount() {
    Product product = fixtureMonkey.giveMeBuilder(Product.class)
        .set("price", 100.0)  // Only price matters for discount calculation
        .sample();
    
    double discount = productService.calculateDiscount(product);
    
    assertThat(discount).isEqualTo(10.0);
}
{{< /tab >}}
{{< tab header="Kotlin" lang="kotlin">}}
// Good - only set what's relevant to the test
@Test
fun shouldCalculateDiscount() {
    val product = fixtureMonkey.giveMeBuilder<Product>()
        .setExpGetter(Product::getPrice, 100.0)  // Only price matters for discount calculation
        .sample()
    
    val discount = productService.calculateDiscount(product)
    
    assertThat(discount).isEqualTo(10.0)
}
{{< /tab >}}
{{< /tabpane>}}

### 2. Prefer Direct Property Setting Over Post-Conditions

- **Use direct property setting when possible**: Instead of using `setPostCondition` which can cause performance issues due to rejection sampling, prefer direct configuration with `set` or `size`.

{{< tabpane persist=false >}}
{{< tab header="Java" lang="java">}}
// Less efficient - uses post-conditions with rejection sampling
@Test
void lessEfficientOrderTest() {
    Order order = fixtureMonkey.giveMeBuilder(Order.class)
        .setPostCondition(o -> o.getItems().size() > 0)    // Performance cost: may reject many samples
        .setPostCondition(o -> o.getTotalAmount() > 100)   // Additional performance cost
        .sample();
    
    OrderResult result = orderService.process(order);
    
    assertThat(result.isSuccessful()).isTrue();
}

// More efficient - uses direct property setting
@Test
void moreEfficientOrderTest() {
    Order order = fixtureMonkey.giveMeBuilder(Order.class)
        .size("items", 1, 5)                  // Directly set collection size
        .set("totalAmount", Arbitraries.integers().greaterThan(100)) // Directly set valid range
        .sample();
    
    OrderResult result = orderService.process(order);
    
    assertThat(result.isSuccessful()).isTrue();
}

// When to use setPostCondition - for truly complex validation that cannot be expressed with property setting
@Test
void complexValidationTest() {
    // Only use setPostCondition for complex validations that cannot be expressed otherwise
    Invoice invoice = fixtureMonkey.giveMeBuilder(Invoice.class)
        .set("items", fixtureMonkey.giveMe(InvoiceItem.class, 3))
        .set("customerType", CustomerType.BUSINESS)
        .setPostCondition(inv -> inv.calculateTotal().compareTo(inv.getItems().stream()
                .map(InvoiceItem::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add)) == 0)
        .sample();
        
    assertThat(invoiceService.validate(invoice)).isTrue();
}
{{< /tab >}}
{{< tab header="Kotlin" lang="kotlin">}}
// Less efficient - uses post-conditions with rejection sampling
@Test
fun lessEfficientOrderTest() {
    val order = fixtureMonkey.giveMeBuilder<Order>()
        .setPostCondition { it.items.isNotEmpty() }    // Performance cost: may reject many samples
        .setPostCondition { it.totalAmount > 100 }     // Additional performance cost
        .sample()
    
    val result = orderService.process(order)
    
    assertThat(result.isSuccessful).isTrue()
}

// More efficient - uses direct property setting
@Test
fun moreEfficientOrderTest() {
    val order = fixtureMonkey.giveMeBuilder<Order>()
        .setExpSize(Order::getItems, 1, 5)                // Directly set collection size
        .setExpGetter(Order::getTotalAmount, Arbitraries.integers().greaterThan(100)) // Directly set valid range
        .sample()
    
    val result = orderService.process(order)
    
    assertThat(result.isSuccessful).isTrue()
}

// When to use setPostCondition - for truly complex validation that cannot be expressed with property setting
@Test
fun complexValidationTest() {
    // Only use setPostCondition for complex validations that cannot be expressed otherwise
    val invoice = fixtureMonkey.giveMeBuilder<Invoice>()
        .setExpGetter(Invoice::getItems, fixtureMonkey.giveMe<InvoiceItem>(3))
        .setExpGetter(Invoice::getCustomerType, CustomerType.BUSINESS)
        .setPostCondition { inv -> 
            inv.calculateTotal() == inv.items.map { it.amount }.fold(BigDecimal.ZERO, BigDecimal::add)
        }
        .sample()
        
    assertThat(invoiceService.validate(invoice)).isTrue()
}
{{< /tab >}}
{{< /tabpane>}}

### 3. Avoid Over-Specification in Tests

- **Don't overspecify test requirements**: Test only what needs to be tested.

{{< tabpane persist=false >}}
{{< tab header="Java" lang="java">}}
// Bad - overspecified test with unnecessary details
@Test
void badTestTooManyDetails() {
    User user = fixtureMonkey.giveMeBuilder(User.class)
        .set("id", 1L)
        .set("name", "John")
        .set("email", "john@example.com")
        .set("address.street", "123 Main St")
        .set("address.city", "New York")
        .set("address.zipCode", "10001")
        .set("registrationDate", LocalDate.of(2023, 1, 1))
        .sample();
    
    // Test is just checking if email is valid
    assertThat(userValidator.isEmailValid(user)).isTrue();
}

// Good - only specify what's needed for the test
@Test
void goodTestOnlyNeededDetails() {
    User user = fixtureMonkey.giveMeBuilder(User.class)
        .set("email", "john@example.com")  // Only email matters for this test
        .sample();
    
    assertThat(userValidator.isEmailValid(user)).isTrue();
}
{{< /tab >}}
{{< tab header="Kotlin" lang="kotlin">}}
// Bad - overspecified test with unnecessary details
@Test
fun badTestTooManyDetails() {
    val user = fixtureMonkey.giveMeBuilder<User>()
        .setExpGetter(User::getId, 1L)
        .setExpGetter(User::getName, "John")
        .setExpGetter(User::getEmail, "john@example.com")
        .setExpGetter(User::getAddress, { address ->
            address.setExpGetter(Address::getStreet, "123 Main St")
                .setExpGetter(Address::getCity, "New York")
                .setExpGetter(Address::getZipCode, "10001")
        })
        .setExpGetter(User::getRegistrationDate, LocalDate.of(2023, 1, 1))
        .sample()
    
    // Test is just checking if email is valid
    assertThat(userValidator.isEmailValid(user)).isTrue()
}

// Good - only specify what's needed for the test
@Test
fun goodTestOnlyNeededDetails() {
    val user = fixtureMonkey.giveMeBuilder<User>()
        .setExpGetter(User::getEmail, "john@example.com")  // Only email matters for this test
        .sample()
    
    assertThat(userValidator.isEmailValid(user)).isTrue()
}
{{< /tab >}}
{{< /tabpane>}}

### 4. Make Tests Readable with Helper Methods

- **Create helper methods to improve test readability**: Encapsulate fixture setup for better readability.

{{< tabpane persist=false >}}
{{< tab header="Java" lang="java">}}
@Test
void testOrderProcessing() {
    // Helper methods returning ArbitraryBuilder for more flexibility
    Order standardOrder = standardOrderBuilder()
        .set("customerNote", "Please deliver quickly")  // Test-specific customization
        .sample();
    
    Customer premiumCustomer = premiumCustomerBuilder()
        .set("membershipYears", 5)  // Test-specific customization
        .sample();
    
    OrderResult result = orderService.process(standardOrder, premiumCustomer);
    
    assertThat(result.hasDiscount()).isTrue();
    assertThat(result.getDiscount()).isGreaterThanOrEqualTo(standardOrder.getTotalAmount() * 0.1);
}

// Helper methods return ArbitraryBuilder instead of instances
private ArbitraryBuilder<Order> standardOrderBuilder() {
    return fixtureMonkey.giveMeBuilder(Order.class)
        .size("items", 3, 5)
        .set("totalAmount", Arbitraries.integers().between(100, 500));
}

private ArbitraryBuilder<Customer> premiumCustomerBuilder() {
    return fixtureMonkey.giveMeBuilder(Customer.class)
        .set("premiumMember", true)
        .set("membershipYears", 2);
}

@Test
void testOrderWithSpecialDiscount() {
    // Reuse the same builder with different customizations
    Order bulkOrder = standardOrderBuilder()
        .size("items", 10, 20)  // Different configuration for this test
        .set("totalAmount", Arbitraries.integers().between(500, 1000))
        .sample();
    
    Customer vipCustomer = premiumCustomerBuilder()
        .set("membershipYears", 10)  // Different configuration for this test
        .set("vipStatus", true)
        .sample();
    
    OrderResult result = orderService.processWithSpecialDiscount(bulkOrder, vipCustomer);
    
    assertThat(result.getDiscount()).isGreaterThanOrEqualTo(bulkOrder.getTotalAmount() * 0.2);
}
{{< /tab >}}
{{< tab header="Kotlin" lang="kotlin">}}
@Test
fun testOrderProcessing() {
    // Helper methods returning ArbitraryBuilder for more flexibility
    val standardOrder = standardOrderBuilder()
        .setExpGetter(Order::getCustomerNote, "Please deliver quickly")  // Test-specific customization
        .sample()
    
    val premiumCustomer = premiumCustomerBuilder()
        .setExpGetter(Customer::getMembershipYears, 5)  // Test-specific customization
        .sample()
    
    val result = orderService.process(standardOrder, premiumCustomer)
    
    assertThat(result.hasDiscount()).isTrue()
    assertThat(result.discount).isGreaterThanOrEqualTo(standardOrder.totalAmount * 0.1)
}

// Helper methods return ArbitraryBuilder instead of instances
private fun standardOrderBuilder(): ArbitraryBuilder<Order> {
    return fixtureMonkey.giveMeBuilder<Order>()
        .setExpSize(Order::getItems, 3, 5)
        .setExpGetter(Order::getTotalAmount, Arbitraries.integers().between(100, 500))
}

private fun premiumCustomerBuilder(): ArbitraryBuilder<Customer> {
    return fixtureMonkey.giveMeBuilder<Customer>()
        .setExpGetter(Customer::isPremiumMember, true)
        .setExpGetter(Customer::getMembershipYears, 2)
}

@Test
fun testOrderWithSpecialDiscount() {
    // Reuse the same builder with different customizations
    val bulkOrder = standardOrderBuilder()
        .setExpSize(Order::getItems, 10, 20)  // Different configuration for this test
        .setExpGetter(Order::getTotalAmount, Arbitraries.integers().between(500, 1000))
        .sample()
    
    val vipCustomer = premiumCustomerBuilder()
        .setExpGetter(Customer::getMembershipYears, 10)  // Different configuration for this test
        .setExpGetter(Customer::isVipStatus, true)
        .sample()
    
    val result = orderService.processWithSpecialDiscount(bulkOrder, vipCustomer)
    
    assertThat(result.discount).isGreaterThanOrEqualTo(bulkOrder.totalAmount * 0.2)
}
{{< /tab >}}
{{< /tabpane>}}

### 5. Configure Once, Reuse Everywhere

- **Create specialized fixture configurations**: Define common configurations once and reuse them.

{{< tabpane persist=false >}}
{{< tab header="Java" lang="java">}}
// Define common configurations
public class TestFixtures {
    public static final FixtureMonkey TEST_FIXTURE_MONKEY = FixtureMonkey.builder()
        .nullInject(0.0)  // No null values
        .build();
        
    public static ArbitraryBuilder<User> validUser() {
        return TEST_FIXTURE_MONKEY.giveMeBuilder(User.class)
            .set("email", "test@example.com")
            .set("active", true);
    }
}

// Use in tests
@Test
void testUserRegistration() {
    User user = TestFixtures.validUser().sample();
    
    userService.register(user);
    
    assertThat(userRepository.findByEmail(user.getEmail())).isNotNull();
}
{{< /tab >}}
{{< tab header="Kotlin" lang="kotlin">}}
// Define common configurations
object TestFixtures {
    val TEST_FIXTURE_MONKEY = FixtureMonkey.builder()
        .nullInject(0.0)  // No null values
        .build()
        
    fun validUser(): ArbitraryBuilder<User> {
        return TEST_FIXTURE_MONKEY.giveMeBuilder<User>()
            .setExpGetter(User::getEmail, "test@example.com")
            .setExpGetter(User::isActive, true)
    }
}

// Use in tests
@Test
fun testUserRegistration() {
    val user = TestFixtures.validUser().sample()
    
    userService.register(user)
    
    assertThat(userRepository.findByEmail(user.email)).isNotNull()
}
{{< /tab >}}
{{< /tabpane>}}

### 6. Test Edge Cases and Boundary Conditions

- **Generate test cases with boundary values**: Test min/max values and edge cases.

{{< tabpane persist=false >}}
{{< tab header="Java" lang="java">}}
@Test
void testUnderageUserCannotAccessAdultContent() {
    // Test with underage user
    User underage = fixtureMonkey.giveMeBuilder(User.class)
        .set("age", 17)  // Just below legal age
        .sample();
    
    assertThat(userService.canAccessAdultContent(underage)).isFalse();
}

@Test
void testOfAgeUserCanAccessAdultContent() {
    // Test with exactly of-age user
    User ofAge = fixtureMonkey.giveMeBuilder(User.class)
        .set("age", 18)  // Exactly legal age
        .sample();
    
    assertThat(userService.canAccessAdultContent(ofAge)).isTrue();
}
{{< /tab >}}
{{< tab header="Kotlin" lang="kotlin">}}
@Test
fun testUnderageUserCannotAccessAdultContent() {
    // Test with underage user
    val underage = fixtureMonkey.giveMeBuilder<User>()
        .setExpGetter(User::getAge, 17)  // Just below legal age
        .sample()
    
    assertThat(userService.canAccessAdultContent(underage)).isFalse()
}

@Test
fun testOfAgeUserCanAccessAdultContent() {
    // Test with exactly of-age user
    val ofAge = fixtureMonkey.giveMeBuilder<User>()
        .setExpGetter(User::getAge, 18)  // Exactly legal age
        .sample()
    
    assertThat(userService.canAccessAdultContent(ofAge)).isTrue()
}
{{< /tab >}}
{{< /tabpane>}}

### 7. Make Tests Reproducible

- **Use a fixed seed for tests that need reproducibility**: This ensures consistent test results.

{{< tabpane persist=false >}}
{{< tab header="Java" lang="java">}}
@Test
@Seed(123L)  // Makes the test reproducible
void testComplexBehavior() {
    List<Order> orders = fixtureMonkey.giveMe(Order.class, 100);
    
    OrderSummary summary = orderService.summarize(orders);
    
    assertThat(summary.getTotalAmount()).isGreaterThan(0);
}
{{< /tab >}}
{{< tab header="Kotlin" lang="kotlin">}}
@Test
@Seed(123L)  // Makes the test reproducible
fun testComplexBehavior() {
    val orders = fixtureMonkey.giveMe<Order>(100)
    
    val summary = orderService.summarize(orders)
    
    assertThat(summary.totalAmount).isGreaterThan(0)
}
{{< /tab >}}
{{< /tabpane>}}

### 8. Define Type-Specific Generation Rules

- **Register custom rules for specific types**: Define how types should be generated consistently across all tests.
- **When to use**: Use this approach when you need to control how a specific type is generated everywhere it appears in your tests.

{{< tabpane persist=false >}}
{{< tab header="Java" lang="java">}}
// Create custom Fixture Monkey with type-specific generation rules
FixtureMonkey fixtureMonkey = FixtureMonkey.builder()
    // Register a custom value for a simple type
    .register(String.class, it -> it.giveMeBuilder("custom-string"))
    
    // Register a custom rule for Email type
    // This affects ALL Email instances created by this fixture monkey
    .register(Email.class, fixture -> fixture.giveMeBuilder(new Email("test@example.com")))
    
    // Register a custom rule for a complex type with validation
    // Applies these rules whenever User instances are created
    .register(User.class, fixture -> fixture
        .setPostCondition(user -> user.getAge() >= 18)
        .set("status", "ACTIVE"))
    
    // Register a factory method for a type
    .register(Product.class, fixture -> fixture
        .instantiate(factoryMethod("createDefault")
            .parameter(String.class, "productName"))
        .set("productName", "Standard Product"))
    
    .build();

// Using the custom registered instance
String customString = fixtureMonkey.giveMeOne(String.class); // Returns "custom-string"
Email email = fixtureMonkey.giveMeOne(Email.class); // Returns Email with "test@example.com"
User user = fixtureMonkey.giveMeOne(User.class); // Returns an adult user with ACTIVE status
{{< /tab >}}
{{< tab header="Kotlin" lang="kotlin">}}
// Create custom Fixture Monkey with type-specific generation rules
val fixtureMonkey = FixtureMonkey.builder()
    // Register a custom value for a simple type
    .register(String::class.java) { it.giveMeBuilder("custom-string") }
    
    // Register a custom rule for Email type
    // This affects ALL Email instances created by this fixture monkey
    .register(Email::class.java) { fixture ->
        fixture.giveMeBuilder(Email("test@example.com"))
    }
    
    // Register a custom rule for a complex type with validation
    // Applies these rules whenever User instances are created
    .register(User::class.java) { fixture ->
        fixture
            .setPostCondition { user -> user.age >= 18 }
            .set("status", "ACTIVE")
    }
    
    // Register a factory method for a type
    .register(Product::class.java) { fixture ->
        fixture
            .instantiate(factoryMethod("createDefault")
                .parameter(String::class.java, "productName"))
            .set("productName", "Standard Product")
    }
    
    .build()

// Using the custom registered instance
val customString = fixtureMonkey.giveMeOne<String>() // Returns "custom-string"
val email = fixtureMonkey.giveMeOne<Email>() // Returns Email with "test@example.com"
val user = fixtureMonkey.giveMeOne<User>() // Returns an adult user with ACTIVE status
{{< /tab >}}
{{< /tabpane>}}

### 9. Configure Field-Level Rules for Complex Objects

- **Define rules for individual fields within objects**: Customize how individual properties are generated within complex objects.
- **When to use**: Use this approach when you need fine-grained control over individual fields within a complex object, applying different rules to different properties within the same class.

{{< tabpane persist=false >}}
{{< tab header="Java" lang="java">}}
FixtureMonkey fixtureMonkey = FixtureMonkey.builder()
    // Register rules for each field in a complex type
    // This provides field-by-field control, unlike the type-level register method
    .registerGroup(ProductDetails.class, group -> group
        // Each field can have its own specific generation rules
        .register("name", Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(50))
        .register("sku", Arbitraries.strings().numeric().ofLength(10))
        .register("description", Arbitraries.strings().ofMinLength(10).ofMaxLength(500))
        .register("inStock", true) // Always in stock for tests
        .register("price", Arbitraries.doubles().between(1.0, 999.99))
        .register("weight", Arbitraries.doubles().between(0.1, 100.0))
    )
    .build();

// The generated ProductDetails will have each field following its specific rule
ProductDetails product = fixtureMonkey.giveMeOne(ProductDetails.class);
// name - alphabetic string between 3-50 chars
// sku - numeric string of exactly 10 chars
// description - any string between 10-500 chars
// inStock - always true
// price - between 1.0 and 999.99
// weight - between 0.1 and 100.0
{{< /tab >}}
{{< tab header="Kotlin" lang="kotlin">}}
val fixtureMonkey = FixtureMonkey.builder()
    // Register rules for each field in a complex type
    // This provides field-by-field control, unlike the type-level register method
    .registerGroup(ProductDetails::class.java) { group ->
        group
            // Each field can have its own specific generation rules
            .register("name", Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(50))
            .register("sku", Arbitraries.strings().numeric().ofLength(10))
            .register("description", Arbitraries.strings().ofMinLength(10).ofMaxLength(500))
            .register("inStock", true) // Always in stock for tests
            .register("price", Arbitraries.doubles().between(1.0, 999.99))
            .register("weight", Arbitraries.doubles().between(0.1, 100.0))
    }
    .build()

// The generated ProductDetails will have each field following its specific rule
val product = fixtureMonkey.giveMeOne<ProductDetails>()
// name - alphabetic string between 3-50 chars  
// sku - numeric string of exactly 10 chars
// description - any string between 10-500 chars
// inStock - always true
// price - between 1.0 and 999.99
// weight - between 0.1 and 100.0
{{< /tab >}}
{{< /tabpane>}} 
