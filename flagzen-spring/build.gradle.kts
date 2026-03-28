plugins {
    `java-library`
}

dependencies {
    api(project(":flagzen-core"))
    compileOnly("org.springframework.boot:spring-boot-autoconfigure:3.4.3")
    compileOnly("org.springframework:spring-context:6.2.3")
    compileOnly("org.slf4j:slf4j-api:2.0.16")
}
