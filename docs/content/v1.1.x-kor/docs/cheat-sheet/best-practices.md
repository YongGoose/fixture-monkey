---
title: "모범 사례"
weight: 100
menu:
  docs:
    parent: "cheat-sheet"
    identifier: "best-practices"
---

## 픽스쳐 몽키 모범 사례

이 가이드는 테스트에서 픽스쳐 몽키를 효과적으로 사용하기 위한 실용적인 팁과 모범 사례를 제공합니다.

### 1. 간단하고 집중된 테스트 작성하기

- **테스트에 중요한 것만 커스터마이징하기**: 테스트 동작에 영향을 주지 않는 필드의 값은 설정하지 마세요.

{{< tabpane persist=false >}}
{{< tab header="Java" lang="java">}}
// 좋음 - 테스트와 관련된 것만 설정
@Test
void shouldCalculateDiscount() {
    Product product = fixtureMonkey.giveMeBuilder(Product.class)
        .set("price", 100.0)  // 할인 계산에는 가격만 중요함
        .sample();
    
    double discount = productService.calculateDiscount(product);
    
    assertThat(discount).isEqualTo(10.0);
}
{{< /tab >}}
{{< tab header="Kotlin" lang="kotlin">}}
// 좋음 - 테스트와 관련된 것만 설정
@Test
fun shouldCalculateDiscount() {
    val product = fixtureMonkey.giveMeBuilder<Product>()
        .setExpGetter(Product::getPrice, 100.0)  // 할인 계산에는 가격만 중요함
        .sample()
    
    val discount = productService.calculateDiscount(product)
    
    assertThat(discount).isEqualTo(10.0)
}
{{< /tab >}}
{{< /tabpane>}}

### 2. 후속 조건보다 직접 속성 설정 사용하기

- **가능한 직접 속성 설정 사용하기**: 샘플 거부로 인해 성능 문제를 일으킬 수 있는 `setPostCondition` 대신 `set`이나 `size`를 사용한 직접 구성을 선호하세요.

{{< tabpane persist=false >}}
{{< tab header="Java" lang="java">}}
// 덜 효율적 - 거부 샘플링으로 인한 후속 조건 사용
@Test
void lessEfficientOrderTest() {
    Order order = fixtureMonkey.giveMeBuilder(Order.class)
        .setPostCondition(o -> o.getItems().size() > 0)    // 성능 비용: 많은 샘플을 거부할 수 있음
        .setPostCondition(o -> o.getTotalAmount() > 100)   // 추가 성능 비용
        .sample();
    
    OrderResult result = orderService.process(order);
    
    assertThat(result.isSuccessful()).isTrue();
}

// 더 효율적 - 직접 속성 설정 사용
@Test
void moreEfficientOrderTest() {
    Order order = fixtureMonkey.giveMeBuilder(Order.class)
        .size("items", 1, 5)                  // 컬렉션 크기 직접 설정
        .set("totalAmount", Arbitraries.integers().greaterThan(100)) // 유효한 범위 직접 설정
        .sample();
    
    OrderResult result = orderService.process(order);
    
    assertThat(result.isSuccessful()).isTrue();
}

// setPostCondition을 사용해야 하는 경우 - 속성 설정으로 표현할 수 없는 복잡한 유효성 검사
@Test
void complexValidationTest() {
    // 속성 설정으로 표현할 수 없는 복잡한 유효성 검사에만 setPostCondition 사용
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
// 덜 효율적 - 거부 샘플링으로 인한 후속 조건 사용
@Test
fun lessEfficientOrderTest() {
    val order = fixtureMonkey.giveMeBuilder<Order>()
        .setPostCondition { it.items.isNotEmpty() }    // 성능 비용: 많은 샘플을 거부할 수 있음
        .setPostCondition { it.totalAmount > 100 }     // 추가 성능 비용
        .sample()
    
    val result = orderService.process(order)
    
    assertThat(result.isSuccessful).isTrue()
}

// 더 효율적 - 직접 속성 설정 사용
@Test
fun moreEfficientOrderTest() {
    val order = fixtureMonkey.giveMeBuilder<Order>()
        .setExpSize(Order::getItems, 1, 5)                // 컬렉션 크기 직접 설정
        .setExpGetter(Order::getTotalAmount, Arbitraries.integers().greaterThan(100)) // 유효한 범위 직접 설정
        .sample()
    
    val result = orderService.process(order)
    
    assertThat(result.isSuccessful).isTrue()
}

// setPostCondition을 사용해야 하는 경우 - 속성 설정으로 표현할 수 없는 복잡한 유효성 검사
@Test
fun complexValidationTest() {
    // 속성 설정으로 표현할 수 없는 복잡한 유효성 검사에만 setPostCondition 사용
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

### 3. 테스트에서 과도한 명세 피하기

- **테스트 요구사항을 과하게 명세하지 않기**: 테스트해야 할 것만 테스트하세요.

{{< tabpane persist=false >}}
{{< tab header="Java" lang="java">}}
// 나쁨 - 불필요한 세부사항으로 과하게 명세된 테스트
@Test
void badTestTooManyDetails() {
    User user = fixtureMonkey.giveMeBuilder(User.class)
        .set("id", 1L)
        .set("name", "홍길동")
        .set("email", "hong@example.com")
        .set("address.street", "강남대로 123")
        .set("address.city", "서울")
        .set("address.zipCode", "06123")
        .set("registrationDate", LocalDate.of(2023, 1, 1))
        .sample();
    
    // 테스트는 이메일이 유효한지만 확인함
    assertThat(userValidator.isEmailValid(user)).isTrue();
}

// 좋음 - 테스트에 필요한 것만 명세
@Test
void goodTestOnlyNeededDetails() {
    User user = fixtureMonkey.giveMeBuilder(User.class)
        .set("email", "hong@example.com")  // 이 테스트에는 이메일만 중요함
        .sample();
    
    assertThat(userValidator.isEmailValid(user)).isTrue();
}
{{< /tab >}}
{{< tab header="Kotlin" lang="kotlin">}}
// 나쁨 - 불필요한 세부사항으로 과하게 명세된 테스트
@Test
fun badTestTooManyDetails() {
    val user = fixtureMonkey.giveMeBuilder<User>()
        .setExpGetter(User::getId, 1L)
        .setExpGetter(User::getName, "홍길동")
        .setExpGetter(User::getEmail, "hong@example.com")
        .setExpGetter(User::getAddress, { address ->
            address.setExpGetter(Address::getStreet, "강남대로 123")
                .setExpGetter(Address::getCity, "서울")
                .setExpGetter(Address::getZipCode, "06123")
        })
        .setExpGetter(User::getRegistrationDate, LocalDate.of(2023, 1, 1))
        .sample()
    
    // 테스트는 이메일이 유효한지만 확인함
    assertThat(userValidator.isEmailValid(user)).isTrue()
}

// 좋음 - 테스트에 필요한 것만 명세
@Test
fun goodTestOnlyNeededDetails() {
    val user = fixtureMonkey.giveMeBuilder<User>()
        .setExpGetter(User::getEmail, "hong@example.com")  // 이 테스트에는 이메일만 중요함
        .sample()
    
    assertThat(userValidator.isEmailValid(user)).isTrue()
}
{{< /tab >}}
{{< /tabpane>}}

### 4. 헬퍼 메서드로 테스트 가독성 높이기

- **테스트 가독성을 개선하는 헬퍼 메서드 만들기**: 픽스쳐 설정을 캡슐화하여 가독성을 향상시키세요.

{{< tabpane persist=false >}}
{{< tab header="Java" lang="java">}}
@Test
void testOrderProcessing() {
    // ArbitraryBuilder를 반환하는 헬퍼 메서드로 더 유연하게 사용
    Order standardOrder = standardOrderBuilder()
        .set("customerNote", "빠른 배송 부탁드립니다")  // 테스트별 맞춤 설정
        .sample();
    
    Customer premiumCustomer = premiumCustomerBuilder()
        .set("membershipYears", 5)  // 테스트별 맞춤 설정
        .sample();
    
    OrderResult result = orderService.process(standardOrder, premiumCustomer);
    
    assertThat(result.hasDiscount()).isTrue();
    assertThat(result.getDiscount()).isGreaterThanOrEqualTo(standardOrder.getTotalAmount() * 0.1);
}

// 인스턴스 대신 ArbitraryBuilder를 반환하는 헬퍼 메서드
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
    // 동일한 빌더를 다른 맞춤 설정으로 재사용
    Order bulkOrder = standardOrderBuilder()
        .size("items", 10, 20)  // 이 테스트를 위한 다른 설정
        .set("totalAmount", Arbitraries.integers().between(500, 1000))
        .sample();
    
    Customer vipCustomer = premiumCustomerBuilder()
        .set("membershipYears", 10)  // 이 테스트를 위한 다른 설정
        .set("vipStatus", true)
        .sample();
    
    OrderResult result = orderService.processWithSpecialDiscount(bulkOrder, vipCustomer);
    
    assertThat(result.getDiscount()).isGreaterThanOrEqualTo(bulkOrder.getTotalAmount() * 0.2);
}
{{< /tab >}}
{{< tab header="Kotlin" lang="kotlin">}}
@Test
fun testOrderProcessing() {
    // ArbitraryBuilder를 반환하는 헬퍼 메서드로 더 유연하게 사용
    val standardOrder = standardOrderBuilder()
        .setExpGetter(Order::getCustomerNote, "빠른 배송 부탁드립니다")  // 테스트별 맞춤 설정
        .sample()
    
    val premiumCustomer = premiumCustomerBuilder()
        .setExpGetter(Customer::getMembershipYears, 5)  // 테스트별 맞춤 설정
        .sample()
    
    val result = orderService.process(standardOrder, premiumCustomer)
    
    assertThat(result.hasDiscount()).isTrue()
    assertThat(result.discount).isGreaterThanOrEqualTo(standardOrder.totalAmount * 0.1)
}

// 인스턴스 대신 ArbitraryBuilder를 반환하는 헬퍼 메서드
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
    // 동일한 빌더를 다른 맞춤 설정으로 재사용
    val bulkOrder = standardOrderBuilder()
        .setExpSize(Order::getItems, 10, 20)  // 이 테스트를 위한 다른 설정
        .setExpGetter(Order::getTotalAmount, Arbitraries.integers().between(500, 1000))
        .sample()
    
    val vipCustomer = premiumCustomerBuilder()
        .setExpGetter(Customer::getMembershipYears, 10)  // 이 테스트를 위한 다른 설정
        .setExpGetter(Customer::isVipStatus, true)
        .sample()
    
    val result = orderService.processWithSpecialDiscount(bulkOrder, vipCustomer)
    
    assertThat(result.discount).isGreaterThanOrEqualTo(bulkOrder.totalAmount * 0.2)
}
{{< /tab >}}
{{< /tabpane>}}

### 5. 한 번 구성하고 여러 곳에서 재사용하기

- **특화된 픽스쳐 구성 만들기**: 공통 구성을 한 번 정의하고 재사용하세요.

{{< tabpane persist=false >}}
{{< tab header="Java" lang="java">}}
// 공통 구성 정의
public class TestFixtures {
    public static final FixtureMonkey TEST_FIXTURE_MONKEY = FixtureMonkey.builder()
        .nullInject(0.0)  // null 값 없음
        .build();
        
    public static ArbitraryBuilder<User> validUser() {
        return TEST_FIXTURE_MONKEY.giveMeBuilder(User.class)
            .set("email", "test@example.com")
            .set("active", true);
    }
}

// 테스트에서 사용
@Test
void testUserRegistration() {
    User user = TestFixtures.validUser().sample();
    
    userService.register(user);
    
    assertThat(userRepository.findByEmail(user.getEmail())).isNotNull();
}
{{< /tab >}}
{{< tab header="Kotlin" lang="kotlin">}}
// 공통 구성 정의
object TestFixtures {
    val TEST_FIXTURE_MONKEY = FixtureMonkey.builder()
        .nullInject(0.0)  // null 값 없음
        .build()
        
    fun validUser(): ArbitraryBuilder<User> {
        return TEST_FIXTURE_MONKEY.giveMeBuilder<User>()
            .setExpGetter(User::getEmail, "test@example.com")
            .setExpGetter(User::isActive, true)
    }
}

// 테스트에서 사용
@Test
fun testUserRegistration() {
    val user = TestFixtures.validUser().sample()
    
    userService.register(user)
    
    assertThat(userRepository.findByEmail(user.email)).isNotNull()
}
{{< /tab >}}
{{< /tabpane>}}

### 6. 엣지 케이스와 경계 조건 테스트하기

- **경계값으로 테스트 케이스 생성하기**: 최소/최대값과 엣지 케이스를 테스트하세요.

{{< tabpane persist=false >}}
{{< tab header="Java" lang="java">}}
@Test
void testUnderageUserCannotAccessAdultContent() {
    // 미성년자 사용자로 테스트
    User underage = fixtureMonkey.giveMeBuilder(User.class)
        .set("age", 17)  // 법적 연령 바로 아래
        .sample();
    
    assertThat(userService.canAccessAdultContent(underage)).isFalse();
}

@Test
void testOfAgeUserCanAccessAdultContent() {
    // 성인 연령 사용자로 테스트
    User ofAge = fixtureMonkey.giveMeBuilder(User.class)
        .set("age", 18)  // 정확히 법적 연령
        .sample();
    
    assertThat(userService.canAccessAdultContent(ofAge)).isTrue();
}
{{< /tab >}}
{{< tab header="Kotlin" lang="kotlin">}}
@Test
fun testUnderageUserCannotAccessAdultContent() {
    // 미성년자 사용자로 테스트
    val underage = fixtureMonkey.giveMeBuilder<User>()
        .setExpGetter(User::getAge, 17)  // 법적 연령 바로 아래
        .sample()
    
    assertThat(userService.canAccessAdultContent(underage)).isFalse()
}

@Test
fun testOfAgeUserCanAccessAdultContent() {
    // 성인 연령 사용자로 테스트
    val ofAge = fixtureMonkey.giveMeBuilder<User>()
        .setExpGetter(User::getAge, 18)  // 정확히 법적 연령
        .sample()
    
    assertThat(userService.canAccessAdultContent(ofAge)).isTrue()
}
{{< /tab >}}
{{< /tabpane>}}

### 7. 재현 가능한 테스트 만들기

- **재현성이 필요한 테스트에는 고정 시드 사용하기**: 일관된 테스트 결과를 보장합니다.

{{< tabpane persist=false >}}
{{< tab header="Java" lang="java">}}
@Test
@Seed(123L)  // 테스트를 재현 가능하게 만듦
void testComplexBehavior() {
    List<Order> orders = fixtureMonkey.giveMe(Order.class, 100);
    
    OrderSummary summary = orderService.summarize(orders);
    
    assertThat(summary.getTotalAmount()).isGreaterThan(0);
}
{{< /tab >}}
{{< tab header="Kotlin" lang="kotlin">}}
@Test
@Seed(123L)  // 테스트를 재현 가능하게 만듦
fun testComplexBehavior() {
    val orders = fixtureMonkey.giveMe<Order>(100)
    
    val summary = orderService.summarize(orders)
    
    assertThat(summary.totalAmount).isGreaterThan(0)
}
{{< /tab >}}
{{< /tabpane>}}

### 8. 타입별 생성 규칙 정의하기

- **특정 타입에 대한 생성 규칙 등록하기**: 모든 테스트에서 타입이 일관되게 생성되는 방식을 정의합니다.
- **사용 시기**: 테스트에서 특정 타입이 등장할 때마다 일관되게 생성되는 방식을 제어해야 할 때 이 접근 방식을 사용하세요.

{{< tabpane persist=false >}}
{{< tab header="Java" lang="java">}}
// 타입별 생성 규칙을 가진 픽스쳐 몽키 생성
FixtureMonkey fixtureMonkey = FixtureMonkey.builder()
    // 단순 타입에 대한 커스텀 값 등록
    .register(String.class, it -> it.giveMeBuilder("custom-string"))
    
    // Email 타입을 위한 생성 규칙 등록
    // 이 픽스쳐 몽키로 생성되는 모든 Email 인스턴스에 영향을 미침
    .register(Email.class, fixture -> fixture.giveMeBuilder(new Email("test@example.com")))
    
    // 검증이 포함된 복잡한 타입을 위한 생성 규칙
    // User 인스턴스가 생성될 때마다 이 규칙이 적용됨
    .register(User.class, fixture -> fixture
        .setPostCondition(user -> user.getAge() >= 18)
        .set("status", "ACTIVE"))
    
    // 타입에 대한 팩토리 메서드 등록
    .register(Product.class, fixture -> fixture
        .instantiate(factoryMethod("createDefault")
            .parameter(String.class, "productName"))
        .set("productName", "기본 상품"))
    
    .build();

// 커스텀 등록된 인스턴스 사용하기
String customString = fixtureMonkey.giveMeOne(String.class); // "custom-string" 반환
Email email = fixtureMonkey.giveMeOne(Email.class); // "test@example.com"이 있는 Email 반환
User user = fixtureMonkey.giveMeOne(User.class); // ACTIVE 상태의 성인 사용자 반환
{{< /tab >}}
{{< tab header="Kotlin" lang="kotlin">}}
// 타입별 생성 규칙을 가진 픽스쳐 몽키 생성
val fixtureMonkey = FixtureMonkey.builder()
    // 단순 타입에 대한 커스텀 값 등록
    .register(String::class.java) { it.giveMeBuilder("custom-string") }
    
    // Email 타입을 위한 생성 규칙 등록
    // 이 픽스쳐 몽키로 생성되는 모든 Email 인스턴스에 영향을 미침
    .register(Email::class.java) { fixture ->
        fixture.giveMeBuilder(Email("test@example.com"))
    }
    
    // 검증이 포함된 복잡한 타입을 위한 생성 규칙
    // User 인스턴스가 생성될 때마다 이 규칙이 적용됨
    .register(User::class.java) { fixture ->
        fixture
            .setPostCondition { user -> user.age >= 18 }
            .set("status", "ACTIVE")
    }
    
    // 타입에 대한 팩토리 메서드 등록
    .register(Product::class.java) { fixture ->
        fixture
            .instantiate(factoryMethod("createDefault")
                .parameter(String::class.java, "productName"))
            .set("productName", "기본 상품")
    }
    
    .build()

// 커스텀 등록된 인스턴스 사용하기
val customString = fixtureMonkey.giveMeOne<String>() // "custom-string" 반환
val email = fixtureMonkey.giveMeOne<Email>() // "test@example.com"이 있는 Email 반환
val user = fixtureMonkey.giveMeOne<User>() // ACTIVE 상태의 성인 사용자 반환
{{< /tab >}}
{{< /tabpane>}}

### 9. 복잡한 객체의 필드별 규칙 구성하기

- **객체 내 개별 필드에 대한 규칙 정의하기**: 복잡한 객체 내에서 개별 속성이 생성되는 방식을 커스터마이징합니다.
- **사용 시기**: 복잡한 객체 내의 개별 필드에 대한 세밀한 제어가 필요하고, 동일한 클래스 내의 서로 다른 속성에 다른 규칙을 적용해야 할 때 이 접근 방식을 사용하세요.

{{< tabpane persist=false >}}
{{< tab header="Java" lang="java">}}
FixtureMonkey fixtureMonkey = FixtureMonkey.builder()
    // 복잡한 타입의 각 필드에 대한 규칙 등록
    // 타입 수준 register 메서드와 달리 필드별 제어를 제공함
    .registerGroup(ProductDetails.class, group -> group
        // 각 필드마다 고유한 생성 규칙을 가질 수 있음
        .register("name", Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(50))
        .register("sku", Arbitraries.strings().numeric().ofLength(10))
        .register("description", Arbitraries.strings().ofMinLength(10).ofMaxLength(500))
        .register("inStock", true) // 테스트를 위해 항상 재고 있음
        .register("price", Arbitraries.doubles().between(1.0, 999.99))
        .register("weight", Arbitraries.doubles().between(0.1, 100.0))
    )
    .build();

// 생성된 ProductDetails의 각 필드는 지정된 특정 규칙을 따름
ProductDetails product = fixtureMonkey.giveMeOne(ProductDetails.class);
// name - 3-50자 사이의 알파벳 문자열
// sku - 정확히 10자의 숫자 문자열
// description - 10-500자 사이의 문자열
// inStock - 항상 true
// price - 1.0에서 999.99 사이
// weight - 0.1에서 100.0 사이
{{< /tab >}}
{{< tab header="Kotlin" lang="kotlin">}}
val fixtureMonkey = FixtureMonkey.builder()
    // 복잡한 타입의 각 필드에 대한 규칙 등록
    // 타입 수준 register 메서드와 달리 필드별 제어를 제공함
    .registerGroup(ProductDetails::class.java) { group ->
        group
            // 각 필드마다 고유한 생성 규칙을 가질 수 있음
            .register("name", Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(50))
            .register("sku", Arbitraries.strings().numeric().ofLength(10))
            .register("description", Arbitraries.strings().ofMinLength(10).ofMaxLength(500))
            .register("inStock", true) // 테스트를 위해 항상 재고 있음
            .register("price", Arbitraries.doubles().between(1.0, 999.99))
            .register("weight", Arbitraries.doubles().between(0.1, 100.0))
    }
    .build()

// 생성된 ProductDetails의 각 필드는 지정된 특정 규칙을 따름
val product = fixtureMonkey.giveMeOne<ProductDetails>()
// name - 3-50자 사이의 알파벳 문자열  
// sku - 정확히 10자의 숫자 문자열
// description - 10-500자 사이의 문자열
// inStock - 항상 true
// price - 1.0에서 999.99 사이
// weight - 0.1에서 100.0 사이
{{< /tab >}}
{{< /tabpane>}} 
