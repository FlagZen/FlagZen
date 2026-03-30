plugins {
    `java-library`
}

dependencies {
    // JavaPoet for compile-time code generation (processor classpath only)
    implementation("com.squareup:javapoet:1.13.0")

    // Test dependencies
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("com.google.testing.compile:compile-testing:0.23.0")
    testImplementation("org.assertj:assertj-core:3.27.3")
}
