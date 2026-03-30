plugins {
    java
    id("com.vanniktech.maven.publish") version "0.30.0" apply false
    id("info.solidsoft.pitest") version "1.19.0-rc.3" apply false
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

    configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
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
        apply(plugin = "info.solidsoft.pitest")

        configure<info.solidsoft.gradle.pitest.PitestPluginExtension> {
            targetClasses.set(listOf("com.flagzen.*"))
            targetTests.set(listOf("com.flagzen.*"))
            threads.set(4)
            outputFormats.set(listOf("HTML", "XML"))
            timestampedReports.set(false)
            junit5PluginVersion.set("1.2.1")
        }

        apply(plugin = "com.vanniktech.maven.publish")

        configure<com.vanniktech.maven.publish.MavenPublishBaseExtension> {
            publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL)
            signAllPublications()

            coordinates(project.group.toString(), project.name, project.version.toString())

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
                        id.set("attilafejer")
                        name.set("Attila Fejér")
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
}
