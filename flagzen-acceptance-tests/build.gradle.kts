plugins {
    java
}

dependencies {
    testImplementation(project(":flagzen-core"))
    testImplementation(project(":flagzen-test"))
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("io.cucumber:cucumber-java:7.22.0")
    testImplementation("io.cucumber:cucumber-junit-platform-engine:7.22.0")
    testImplementation("org.assertj:assertj-core:3.27.3")
    testImplementation("com.google.testing.compile:compile-testing:0.21.0")
}

tasks.withType<Test> {
    useJUnitPlatform()
    systemProperty("cucumber.features", listOf(
        rootProject.projectDir.resolve("tests/acceptance/flagzen").absolutePath,
        rootProject.projectDir.resolve("tests/acceptance/flagzen-eval-context").absolutePath
    ).joinToString(","))
    systemProperty("cucumber.glue", "com.flagzen.acceptance.steps")
    systemProperty("cucumber.plugin", "pretty")
    systemProperty("cucumber.filter.tags", "not @pending")
}
