# Security Policy

## Reporting a Vulnerability

If you discover a security vulnerability in FlagZen, please report it responsibly.

**Do not open a public GitHub issue for security vulnerabilities.**

Instead, use [GitHub's private vulnerability reporting](https://github.com/FlagZen/FlagZen/security/advisories/new) to submit your report.

Include:

- A description of the vulnerability
- Steps to reproduce
- The affected module(s) and version(s)
- Any potential impact assessment

## Response Timeline

- **Acknowledgment**: within 48 hours
- **Initial assessment**: within 1 week
- **Fix or mitigation**: depends on severity, but we aim for 30 days for critical issues

## Scope

FlagZen is a compile-time library with no network access, no data storage, and no authentication. The primary security surface is:

- **Annotation processor**: runs at compile time in the developer's build environment
- **Generated proxy code**: executes at runtime in the application's JVM
- **FlagProvider implementations**: delegate to external systems (env vars, OpenFeature) — security of those systems is outside FlagZen's scope

## Supported Versions

| Version | Supported |
| ------- | --------- |
| 1.1.x   | Yes       |
| < 1.1   | No        |
