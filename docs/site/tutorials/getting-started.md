# Tutorial: Getting Started with FlagZen

Learn to build your first feature flag in 10 minutes. By the end, you'll have working code that dispatches between two variants — and you'll understand exactly what's happening at compile time.

## What You'll Learn

- Add FlagZen to a Gradle project
- Define a feature as a Java interface
- Implement two variants
- Wire up the dispatcher
- See a flag change the code path in real time

## Prerequisites

- Java 17 or later
- Gradle 7+
- A text editor and terminal
- Familiarity with Java interfaces and annotations

## Step 1: Create a Minimal Gradle Project

Create a new directory for your project:

```bash
mkdir flagzen-first-flag && cd flagzen-first-flag
```

Create `build.gradle`:

```gradle
plugins {
    id 'java'
}

group = 'com.example'
version = '1.0.0'

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.flagzen:flagzen-core:1.1.0")
    annotationProcessor("com.flagzen:flagzen-core:1.1.0")
}
```

Create the directory structure:

```bash
mkdir -p src/main/java/com/example
```

## Step 2: Define Your First Feature Interface

Create `src/main/java/com/example/Greeting.java`:

```java
package com.example;

import com.flagzen.Feature;

@Feature("greeting")
public interface Greeting {
    String greet(String name);
}
```

This interface is your feature flag. The `@Feature("greeting")` annotation tells FlagZen: "The value of the 'greeting' flag controls which implementation runs."

Notice: You're not writing any logic yet. You're just defining the interface that callers will use.

## Step 3: Implement the First Variant

Create `src/main/java/com/example/FormalGreeting.java`:

```java
package com.example;

import com.flagzen.Variant;

@Variant(value = "FORMAL", of = Greeting.class)
public class FormalGreeting implements Greeting {
    @Override
    public String greet(String name) {
        return "Good morning, " + name + ".";
    }
}
```

The `@Variant(value = "FORMAL", of = Greeting.class)` annotation says: "When the 'greeting' flag is set to FORMAL, use this implementation."

Type the code and compile:

```bash
./gradlew build
```

You should see no errors. FlagZen's annotation processor just ran and generated a proxy class behind the scenes.

## Step 4: Implement the Second Variant

Create `src/main/java/com/example/CasualGreeting.java`:

```java
package com.example;

import com.flagzen.Variant;

@Variant(value = "CASUAL", of = Greeting.class)
public class CasualGreeting implements Greeting {
    @Override
    public String greet(String name) {
        return "Hey " + name + "!";
    }
}
```

Compile again:

```bash
./gradlew build
```

Now FlagZen knows about both variants. The annotation processor has updated the generated proxy.

## Step 5: Create the Dispatcher

Create `src/main/java/com/example/Main.java`:

```java
package com.example;

import com.flagzen.internal.DefaultFeatureDispatcher;
import com.flagzen.internal.InMemoryFlagProvider;

public class Main {
    public static void main(String[] args) {
        // Create an in-memory flag provider (for this tutorial)
        InMemoryFlagProvider provider = new InMemoryFlagProvider();
        provider.set("greeting", "FORMAL");

        // Create a dispatcher that knows how to resolve flags
        DefaultFeatureDispatcher dispatcher = new DefaultFeatureDispatcher(provider);

        // Ask the dispatcher for a Greeting
        // It returns a proxy that routes to FormalGreeting
        Greeting greeting = dispatcher.resolve(Greeting.class);

        // Call the method
        String result = greeting.greet("Alice");
        System.out.println(result);
    }
}
```

Compile and run:

```bash
./gradlew build
java -cp build/classes/java/main:build/libs/* com.example.Main
```

You should see:

```
Good morning, Alice.
```

## Step 6: Change the Flag and See the Dispatch Change

Modify `Main.java` — change the flag value from `FORMAL` to `CASUAL`:

```java
provider.set("greeting", "CASUAL");
```

Recompile and run:

```bash
./gradlew build
java -cp build/classes/java/main:build/libs/* com.example.Main
```

Now you see:

```
Hey Alice!
```

Same `dispatcher.resolve(Greeting.class)` call. Same `greeting.greet("Alice")` method. The output changed because the flag value changed — and no restart was needed.

This is the core insight: **FlagZen's generated proxy checks the flag value on every method call.** You change the flag, and the next call goes to the new variant.

## Step 7: Add a Third Variant (Without Changing Dispatch Code)

Create `src/main/java/com/example/FriendsGreeting.java`:

```java
package com.example;

import com.flagzen.Variant;

@Variant(value = "FRIENDS", of = Greeting.class)
public class FriendsGreeting implements Greeting {
    @Override
    public String greet(String name) {
        return "What's up, " + name + "? Good to see you!";
    }
}
```

Update `Main.java` to try all three:

```java
package com.example;

import com.flagzen.internal.DefaultFeatureDispatcher;
import com.flagzen.internal.InMemoryFlagProvider;

public class Main {
    public static void main(String[] args) {
        InMemoryFlagProvider provider = new InMemoryFlagProvider();
        DefaultFeatureDispatcher dispatcher = new DefaultFeatureDispatcher(provider);
        Greeting greeting = dispatcher.resolve(Greeting.class);

        // Try FORMAL
        provider.set("greeting", "FORMAL");
        System.out.println(greeting.greet("Alice"));

        // Try CASUAL
        provider.set("greeting", "CASUAL");
        System.out.println(greeting.greet("Bob"));

        // Try FRIENDS
        provider.set("greeting", "FRIENDS");
        System.out.println(greeting.greet("Charlie"));
    }
}
```

Recompile and run:

```bash
./gradlew build
java -cp build/classes/java/main:build/libs/* com.example.Main
```

Output:

```
Good morning, Alice.
Hey Bob!
What's up, Charlie? Good to see you!
```

Notice: You never touched the dispatcher code. You just added a new `@Variant` class, recompiled, and it worked. FlagZen regenerated the proxy to include the new variant.

## Step 8: Understand What Just Happened

At compile time, FlagZen's annotation processor did this:

1. Found your `@Feature` interface `Greeting`
2. Found all `@Variant` classes that implement it
3. Generated a proxy class (hidden in the `build/` directory) called `Greeting_FlagZenProxy`
4. That proxy knows how to:
   - Ask the provider for the current flag value
   - Match that value against all known variants
   - Instantiate and call the matching variant

At runtime, when you call `dispatcher.resolve(Greeting.class)`, it returns the `Greeting_FlagZenProxy` instance. When you call `greeting.greet("Alice")`, the proxy checks the flag and routes to the active variant.

All of this is determined at compile time. Zero reflection. Your code compiles down to direct method calls.

## You've Done It

Congratulations! You've built your first feature flag. You've seen:

- How to define a feature interface
- How variants map to implementations
- How the dispatcher routes calls
- How flags change behavior without restarting code
- How to add new variants without changing dispatch logic

From here:

- **Ready for a realistic example?** See [Tutorial: First Feature Flag](first-feature-flag.md) — build a checkout flow with multiple variants.
- **Want to test feature flags?** See [Tutorial: Testing](testing.md) — pin flags in unit tests.
- **Need to integrate with your codebase?** See the [flagzen-core README](https://github.com/FlagZen/FlagZen/blob/main/flagzen-core/README.md) for production patterns.
