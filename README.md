# kodarijava

Official Java SDK for the [Kodari API](https://kodari.ai). Fully async, built on Netty.

## Installation

### Gradle (Kotlin DSL)

```kotlin
repositories {
    maven("https://repo.kodari.ai/releases")
}

dependencies {
    implementation("ai.kodari:kodarijava:1.0.0")
}
```

### Gradle (Groovy)

```groovy
repositories {
    maven { url 'https://repo.kodari.ai/releases' }
}

dependencies {
    implementation 'ai.kodari:kodarijava:1.0.0'
}
```

### Maven

```xml
<repository>
    <id>kodari</id>
    <url>https://repo.kodari.ai/releases</url>
</repository>

<dependency>
    <groupId>ai.kodari</groupId>
    <artifactId>kodarijava</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Quick Start

Get your API key at [kodari.ai/api-keys](https://kodari.ai/api-keys).

```java
KodariClient client = KodariClient.builder()
        .credentials(KodariCredentials.create("kod-your-api-key"))
        .build();

// Moderate a chat message
client.moderate("hello everyone").thenAccept(result -> {
    if (!result.isSafe()) {
        System.out.println("Flagged: " + result.getCategory()); // toxicity, threat, doxxing, advertising, spam
        System.out.println("Severity: " + result.getSeverity()); // low, medium, high
    }
});

// Don't forget to close when done
client.close();
```

## Moderation

Multilingual chat moderation AI that classifies messages for toxicity, threats, doxxing, spam, and advertising.

```java
client.moderate("some message").thenAccept(result -> {
    result.isSafe();        // true/false
    result.getCategory();   // none, toxicity, threat, doxxing, advertising, spam
    result.getSeverity();   // none, low, medium, high

    // Convenience checks
    result.isToxic();
    result.isThreat();
    result.isDoxxing();
    result.isAdvertising();
    result.isSpam();
});
```

## Generic Model Execution

Call any model by name, even ones added after your SDK version:

```java
client.execute("moderation", "your input").thenAccept(response -> {
    response.getKodariModel();  // "moderation"
    response.getTokensCost();   // 10
    response.getResult();       // raw JsonElement
});
```

## Other Endpoints

```java
// Get current user
client.getMe().thenAccept(user -> {
    user.getName();
    user.getEmail();
    user.getRole();
});

// Generic GET/POST for any endpoint
client.get("/some/endpoint");
client.post("/some/endpoint", jsonBody);
```

## Error Handling

All exceptions extend `KodariException` (unchecked):

```java
client.moderate("message").exceptionally(throwable -> {
    Throwable cause = throwable.getCause();

    if (cause instanceof KodariAuthenticationException)
        System.out.println("Invalid API key");

    if (cause instanceof KodariRateLimitException)
        System.out.println("Rate limited, slow down");

    if (cause instanceof KodariInsufficientTokensException)
        System.out.println("Not enough tokens");

    return null;
});
```

## Blocking Usage

If you prefer synchronous calls, use `.get()`:

```java
ModerationResult result = client.moderate("message").get();
System.out.println(result.isSafe());
```

## Requirements

Java 8+
