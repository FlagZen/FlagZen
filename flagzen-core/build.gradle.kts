plugins {
    `java-library`
    id("info.solidsoft.pitest") version "1.19.0-rc.3"
}

pitest {
    targetClasses.set(listOf("com.flagzen.*"))
    targetTests.set(listOf("com.flagzen.*"))
    threads.set(4)
    outputFormats.set(listOf("HTML", "XML"))
    timestampedReports.set(false)
    junit5PluginVersion.set("1.2.1")
}

dependencies {
    // JavaPoet for compile-time code generation (processor classpath only)
    implementation("com.squareup:javapoet:1.13.0")

    // Test dependencies
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("com.google.testing.compile:compile-testing:0.21.0")
    testImplementation("org.assertj:assertj-core:3.27.3")
}
