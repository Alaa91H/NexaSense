# Security Policy

## Scope

NexaSense is an offline sensor utility. The security-relevant surface is small:

- runtime permissions (coarse location, used only for magnetic declination),
- the diagnostic report (must never contain personal data),
- dependency supply chain.

## Supported versions

Only the latest release receives security fixes. Pre-release builds are not
supported.

## Reporting a vulnerability

Please **do not open a public issue** for security problems. Report privately
by opening a GitHub security advisory (Security → Report a vulnerability), or
email the maintainers via the address listed on the repository profile page.

Please include:

- affected version(s) and build,
- a description of the vulnerability,
- steps to reproduce,
- impact assessment if known.

You will receive an acknowledgement within 5 business days.

## What is not in scope

- Vulnerabilities in third-party dependencies — report them upstream.
- Hardware/ROM defects on specific devices (open a normal issue instead,
  including the diagnostic report).
