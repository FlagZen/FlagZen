plugins {
    java
    `maven-publish`
    signing
}

allprojects {
    group = property("group") as String
    version = property("flagzenVersion") as String

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")
    apply(plugin = "signing")

    configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        withJavadocJar()
        withSourcesJar()
    }

    tasks.withType<JavaCompile> {
        options.release.set(17)
        options.encoding = "UTF-8"
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
            showExceptions = true
            showCauses = true
            showStackTraces = true
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
    }

    // Only publish library modules, not acceptance tests or examples
    val isPublishable = project.name != "flagzen-acceptance-tests" && project.name != "flagzen-examples"

    if (isPublishable) {
        configure<PublishingExtension> {
            publications {
                create<MavenPublication>("mavenJava") {
                    from(components["java"])

                    pom {
                        name.set(project.name)
                        description.set("Type-safe polymorphic dispatch for feature flags in Java")
                        url.set("https://github.com/FlagZen/FlagZen")

                        licenses {
                            license {
                                name.set("Apache License, Version 2.0")
                                url.set("https://www.apache.org/licenses/LICENSE-2.0")
                            }
                        }

                        developers {
                            developer {
                                id.set("attila-kiss")
                                name.set("Attila Kiss")
                            }
                        }

                        scm {
                            connection.set("scm:git:git://github.com/FlagZen/FlagZen.git")
                            developerConnection.set("scm:git:ssh://github.com/FlagZen/FlagZen.git")
                            url.set("https://github.com/FlagZen/FlagZen")
                        }
                    }
                }
            }

            repositories {
                maven {
                    name = "centralPortal"
                    url = uri("https://central.sonatype.com/api/v1/publisher/deployments/download/")

                    credentials {
                        username = providers.environmentVariable("SONATYPE_USERNAME").orNull
                        password = providers.environmentVariable("SONATYPE_PASSWORD").orNull
                    }
                }
            }
        }

        configure<SigningExtension> {
            val signingKey = providers.environmentVariable("GPG_SIGNING_KEY")
            val signingPassphrase = providers.environmentVariable("GPG_SIGNING_PASSPHRASE")

            if (signingKey.isPresent) {
                useInMemoryPgpKeys(signingKey.get(), signingPassphrase.get())
                sign(extensions.getByType<PublishingExtension>().publications["mavenJava"])
            }
        }
    }
}
