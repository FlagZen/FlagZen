plugins {
    `java-library`
}

dependencies {
    api(project(":flagzen-core"))
    implementation("com.launchdarkly:launchdarkly-java-server-sdk:7.13.1")

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.assertj:assertj-core:3.27.3")
    testImplementation("org.mockito:mockito-core:5.23.0")
}
