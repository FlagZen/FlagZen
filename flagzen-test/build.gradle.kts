plugins {
    `java-library`
}

dependencies {
    api(project(":flagzen-core"))
    api(platform("org.junit:junit-bom:5.11.4"))
    api("org.junit.jupiter:junit-jupiter-api")

    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.assertj:assertj-core:3.27.3")
}
