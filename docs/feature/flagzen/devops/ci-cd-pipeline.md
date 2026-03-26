# CI/CD Pipeline -- FlagZen

## Context

FlagZen is a Java library published to Maven Central. There is no service deployment, no containers, no runtime observability. The pipeline focuses on build correctness, multi-version compatibility, quality gates, and release publishing.

## Pipeline Overview

```
PR / push to main                              Tag v*
      |                                          |
      v                                          v
  [CI Workflow]                          [Release Workflow]
      |                                          |
      v                                          v
  build (matrix)                         validate tag version
  Java 17, 21, 25                                |
      |                                          v
      v                                    build + test
  pitest (after build)                           |
  kill rate >= 80%                               v
      |                                      pitest
      v                                    kill rate >= 80%
  upload artifacts                               |
                                                 v
                                           GPG sign
                                                 |
                                                 v
                                         publish to Maven Central
                                                 |
                                                 v
                                        create GitHub Release
                                        (auto-generated changelog)
```

## CI Workflow (.github/workflows/ci.yml)

### Triggers

- `push` to `main` branch
- `pull_request` targeting `main` branch

### Jobs

#### 1. build (matrix)

Runs in parallel across Java 17, 21, and 25.

| Step | Description | Gate |
|------|-------------|------|
| Checkout | Clone repository | -- |
| Setup Java | Temurin distribution, matrix version | -- |
| Setup Gradle | gradle/actions/setup-gradle@v4 (caches dependencies) | -- |
| Build and test | `./gradlew build` (compile, unit tests, acceptance tests) | All tests pass |
| Upload test reports | Artifact upload, 14-day retention | -- |

#### 2. pitest (depends on build)

Runs after all matrix builds succeed. Uses Java 25.

| Step | Description | Gate |
|------|-------------|------|
| Run PITest | `./gradlew :flagzen-core:pitest` | -- |
| Check kill rate | Parse report, verify >= 80% | Kill rate >= 80% |
| Upload PITest report | Artifact upload, 14-day retention | -- |

### Quality Gates

| Gate | Type | Threshold | Stage |
|------|------|-----------|-------|
| Compilation | Blocking | Zero errors | build |
| Unit tests | Blocking | 100% pass | build |
| Acceptance tests | Blocking | 100% pass | build |
| Multi-version compat | Blocking | Pass on Java 17, 21, 25 | build |
| Mutation kill rate | Blocking | >= 80% | pitest |

## Release Workflow (.github/workflows/release.yml)

### Triggers

- `push` of tags matching `v*`

### Steps

| Step | Description | Gate |
|------|-------------|------|
| Tag validation | Verify tag version matches `gradle.properties` | Versions must match |
| Build and test | Full `./gradlew build` | All tests pass |
| PITest | Mutation testing on flagzen-core | Kill rate >= 80% |
| GPG import | Import signing key from secrets | -- |
| Publish | `publishToSonatype closeAndReleaseSonatypeStagingRepository` | Sonatype staging passes |
| Changelog | Generate from conventional commits since last tag | -- |
| GitHub Release | Create release with changelog and Maven coordinates | -- |

### Required Secrets

| Secret | Purpose |
|--------|---------|
| `SONATYPE_USERNAME` | Maven Central publishing authentication |
| `SONATYPE_PASSWORD` | Maven Central publishing authentication |
| `GPG_PRIVATE_KEY` | Artifact signing (Maven Central requirement) |
| `GPG_PASSPHRASE` | GPG key passphrase |

## Gradle Publishing Configuration

The root `build.gradle.kts` configures:

- `maven-publish` plugin for POM generation and artifact publication
- `signing` plugin for GPG signing (Maven Central requirement)
- `io.github.gradle-nexus.publish-plugin` for Sonatype OSSRH staging and release
- POM metadata: name, description, URL, Apache 2.0 license, SCM, developer info
- Publications for each publishable submodule (flagzen-core, flagzen-test)

Signing uses in-memory key from environment variables (CI-friendly, no keyring required).

## Release Process

1. Update version in `gradle.properties` (remove `-SNAPSHOT` suffix)
2. Commit: `chore: release v{version}`
3. Tag: `git tag v{version}`
4. Push: `git push origin main --tags`
5. Release workflow runs automatically
6. After release, bump to next snapshot version

## DORA Metrics Targets

| Metric | Target | Rationale |
|--------|--------|-----------|
| Deployment frequency | Per feature (tag-based) | Library releases are deliberate, not continuous |
| Lead time for changes | < 1 day (merge to release) | Tag push triggers automated publish |
| Change failure rate | < 5% | Multi-version matrix + mutation testing catch regressions |
| Time to restore | < 1 hour | Yank from Maven Central + hotfix tag |
