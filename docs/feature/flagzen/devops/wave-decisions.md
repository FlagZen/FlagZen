# Wave Decisions -- FlagZen DEVOPS

## Context

- **Feature ID**: flagzen
- **Date**: 2026-03-26
- **Wave**: DEVOPS (platform-architect)
- **Prior wave**: DESIGN (solution-architect) -- APPROVED, all phases complete, peer review passed

## Decisions

|  #  |                Decision                 |                                                           Rationale                                                            |
| --- | --------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------ |
| 1   | Deployment target: Maven Central        | Library JARs, not service deployment. Maven Central is the standard Java artifact repository.                                  |
| 2   | Container orchestration: None           | Library -- no runtime deployment, no containers.                                                                               |
| 3   | CI/CD platform: GitHub Actions          | Project hosted on GitHub. Native integration, free for OSS, matrix builds.                                                     |
| 4   | Existing infrastructure: Greenfield     | No prior CI/CD, IaC, or workflows exist.                                                                                       |
| 5   | Observability: None                     | Library -- consumers handle their own observability. No runtime to monitor.                                                    |
| 6   | Deployment strategy: Tag-based releases | `v*` tags trigger release workflow. No canary/blue-green/rolling -- inapplicable to libraries.                                 |
| 7   | Continuous learning: No                 | Not applicable at this project stage.                                                                                          |
| 8   | Git branching: GitHub Flow              | Feature branches + PRs to main. Releases from main via tags. Appropriate for solo/small team with per-feature release cadence. |
| 9   | Mutation testing: Per-feature           | PITest already configured in flagzen-core (plugin 1.19.0-rc.3). Runs in CI after build. Kill rate gate: >= 80%.                |

## Artifacts Produced

|         Artifact         |              File               |  Status   |
| ------------------------ | ------------------------------- | --------- |
| CI workflow              | `.github/workflows/ci.yml`      | Complete  |
| Release workflow         | `.github/workflows/release.yml` | Complete  |
| Gradle publishing config | `build.gradle.kts` (root)       | Complete  |
| Environment inventory    | `devops/environments.yaml`      | Complete  |
| CI/CD pipeline docs      | `devops/ci-cd-pipeline.md`      | Complete  |
| Branching strategy       | `devops/branching-strategy.md`  | Complete  |
| Wave decisions           | `devops/wave-decisions.md`      | This file |
| CLAUDE.md update         | `CLAUDE.md` (project root)      | Complete  |

## Artifacts NOT Produced (with justification)

|           Artifact            |                                      Reason                                       |
| ----------------------------- | --------------------------------------------------------------------------------- |
| platform-architecture.md      | Library, not a deployed service                                                   |
| observability-design.md       | No runtime to observe                                                             |
| monitoring-alerting.md        | No service to monitor                                                             |
| infrastructure-integration.md | No infrastructure to integrate                                                    |
| continuous-learning.md        | Deferred                                                                          |
| kpi-instrumentation.md        | Outcome KPIs (stars, downloads) are external metrics, not runtime instrumentation |

## Quality Gates

- [x] CI workflow tests across Java 17, 21, 25
- [x] Mutation testing gate (>= 80% kill rate)
- [x] Release workflow validates tag-version consistency
- [x] GPG signing for Maven Central compliance
- [x] Automated changelog generation
- [x] Branching strategy documented with branch protection rules
- [x] All decisions documented with rationale
