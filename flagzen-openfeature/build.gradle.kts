plugins {
    `java-library`
}

dependencies {
    api(project(":flagzen-core"))
    implementation("dev.openfeature:sdk:1.12.2")
}
