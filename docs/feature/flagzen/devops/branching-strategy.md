# Branching Strategy -- FlagZen

## Model: GitHub Flow

GitHub Flow is the selected branching model. Single long-lived branch (`main`), short-lived feature branches, PRs with review, releases from `main` via tags.

### Why GitHub Flow

|     Factor      |            Assessment             |
| --------------- | --------------------------------- |
| Team size       | Solo (scaling to small team)      |
| Release cadence | Per-feature, not scheduled        |
| Risk profile    | Low (library, not service)        |
| Maturity        | Greenfield, establishing patterns |

Rejected alternatives:

- **Trunk-based**: Viable but PR review discipline is valuable even for solo work (self-review, CI gate enforcement).
- **GitFlow**: Overhead of develop/release branches is unjustified for a library with single supported version.
- **Release branching**: Only needed when supporting multiple major versions simultaneously (future concern, not Release 1).

## Branch Rules

### main

- Protected branch
- Direct pushes: blocked (all changes via PR)
- Required status checks: `build` job (all matrix entries), `pitest` job
- Required reviews: 1 (can be self-review for solo phase; increase to 2 when team grows)
- Require linear history: yes (squash or rebase merge)
- Force push: blocked
- Deletion: blocked

### Feature branches

- Naming: `feat/{short-description}`, `fix/{short-description}`, `chore/{short-description}`
- Lifetime: ideally < 1 week
- Base: always from `main`
- Merge: squash merge to `main` (clean linear history)

### Tags

- Format: `v{MAJOR}.{MINOR}.{PATCH}` (semantic versioning)
- Created on `main` only
- Triggers release workflow
- Tags are immutable (no moving tags)

## Pipeline Triggers

|    Event     | Branch/Tag |  Workflow   |                  Jobs                  |
| ------------ | ---------- | ----------- | -------------------------------------- |
| push         | main       | ci.yml      | build (matrix), pitest                 |
| pull_request | main       | ci.yml      | build (matrix), pitest                 |
| push tag     | v*         | release.yml | build, pitest, publish, GitHub Release |

## Versioning

Semantic versioning (SemVer):

- **MAJOR**: Breaking API changes (annotation contract, SPI contract, FeatureDispatcher API)
- **MINOR**: New features, new modules, backward-compatible additions
- **PATCH**: Bug fixes, documentation, dependency updates

Version source of truth: `gradle.properties` (`flagzenVersion` property).

Development versions use `-SNAPSHOT` suffix. Release versions drop the suffix.

## Conventional Commits

All commits follow Conventional Commits specification:

- `feat:` -- new feature
- `fix:` -- bug fix
- `chore:` -- maintenance, CI, dependencies
- `docs:` -- documentation
- `refactor:` -- code restructuring
- `test:` -- test additions/changes
- `perf:` -- performance improvements

Scopes match module names: `feat(core):`, `fix(test):`, `chore(spring):`.
