plugins {
    java
}

dependencies {
    testImplementation(project(":flagzen-core"))
    testImplementation(project(":flagzen-env"))
    testImplementation(project(":flagzen-test"))
    testImplementation(project(":flagzen-spring"))
    testImplementation(project(":flagzen-openfeature"))
    testImplementation("dev.openfeature:sdk:1.12.2")
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("io.cucumber:cucumber-java:7.22.0")
    testImplementation("io.cucumber:cucumber-junit-platform-engine:7.22.0")

    testImplementation("org.assertj:assertj-core:3.27.3")
    testImplementation("com.google.testing.compile:compile-testing:0.23.0")
    testImplementation(platform("org.springframework.boot:spring-boot-dependencies:3.4.3"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-autoconfigure")
    testImplementation("org.springframework:spring-context")
}

tasks.withType<Test> {
    useJUnitPlatform()
    systemProperty("cucumber.features", listOf(
        rootProject.projectDir.resolve("tests/acceptance/flagzen").absolutePath,
        rootProject.projectDir.resolve("tests/acceptance/flagzen-eval-context").absolutePath,
        rootProject.projectDir.resolve("tests/acceptance/flagzen-typed-variants").absolutePath,
        rootProject.projectDir.resolve("tests/acceptance/flagzen-env").absolutePath,
        rootProject.projectDir.resolve("tests/acceptance/flagzen-multi-value-variant").absolutePath,
        rootProject.projectDir.resolve("tests/acceptance/flagzen-spring").absolutePath,
        rootProject.projectDir.resolve("tests/acceptance/flagzen-openfeature").absolutePath
    ).joinToString(","))
    systemProperty("cucumber.glue", "com.flagzen.acceptance.steps")
    systemProperty("cucumber.plugin", "pretty")
    systemProperty("cucumber.filter.tags", "not @pending and not @spring-test")
}
