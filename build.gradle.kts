plugins {
    java
    `maven-publish`
    signing
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
}

allprojects {
    group = property("group") as String
    version = property("flagzenVersion") as String

    repositories {
        mavenCentral()
    }
}

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
            username.set(providers.environmentVariable("SONATYPE_USERNAME"))
            password.set(providers.environmentVariable("SONATYPE_PASSWORD"))
        }
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

    // Only publish library modules, not acceptance tests
    val isPublishable = project.name != "flagzen-acceptance-tests"

    if (isPublishable) {
        configure<PublishingExtension> {
            publications {
                create<MavenPublication>("mavenJava") {
                    from(components["java"])

                    pom {
                        name.set(project.name)
                        description.set("Type-safe polymorphic dispatch for feature flags in Java")
                        url.set("https://github.com/attila-kiss/flagzen")

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
                            connection.set("scm:git:git://github.com/attila-kiss/flagzen.git")
                            developerConnection.set("scm:git:ssh://github.com/attila-kiss/flagzen.git")
                            url.set("https://github.com/attila-kiss/flagzen")
                        }
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
