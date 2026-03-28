plugins {
    java
}

dependencies {
    implementation(project(":flagzen-core"))
    implementation(project(":flagzen-test"))
    implementation(project(":flagzen-env"))
    implementation(project(":flagzen-key-mapping"))
    // Spring and OpenFeature examples need optional deps
    compileOnly(project(":flagzen-spring"))
    compileOnly(project(":flagzen-openfeature"))

    annotationProcessor(project(":flagzen-core"))

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.assertj:assertj-core:3.27.3")
    testImplementation(project(":flagzen-test"))
    testImplementation(project(":flagzen-env"))
}
