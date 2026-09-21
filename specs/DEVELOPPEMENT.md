# DEVELOPMENT

## Purpose

This document defines the development rules and minimum acceptance criteria for a change. It complements the principles described in `specs/ARCHITECTURE.md`.

A rule in this document must be verifiable in the code, through a command, or during review. Any exception must be explicitly justified in the pull request.

## Commits

Commits follow the Conventional Commits format:

```text
<type>(<scope>): <description>
```

The allowed types are `feat`, `fix`, `refactor`, `test`, `docs`, `build`, `ci`, and `chore`. The description is imperative, concise, and does not end with a period.

Examples:

```text
feat(printing): add print job cancellation
fix(drivers): reject unsupported media sizes
test(printers): cover duplicate discovery
```

A commit must remain focused and must not mix a functional change with an unrelated refactoring. The commit message describes the intent of the change, not its detailed implementation.

## Validation Before Committing

Before creating a commit, the following commands must succeed:

```text
./gradlew test lint assembleDebug
```

Instrumented tests that require a device or emulator must be run when a change affects Android, the user interface, or platform integration:

```text
./gradlew connectedAndroidTest
```

A pull request must include these validations in CI. Any disabled, ignored, or weakened test must be justified in the pull request.

## TDD

Business features follow the Red, Green, Refactor cycle:

1. Write a test that describes the expected behavior and verify that it fails.
2. Write the minimum implementation that makes it pass.
3. Refactor without changing the covered behavior.

The domain is tested first and must not depend on an infrastructure implementation. Domain tests are written using the Given, When, Then structure:

- **Given**: initial state and preconditions;
- **When**: business action performed;
- **Then**: result and observable effects.

The test name must express the behavior being verified. A test must not verify internal details that are not necessary for that behavior.

## Test Scope

- Domain unit tests cover business rules, nominal cases, errors, and important boundaries.
- Tests for views, stores, and adapters verify only their own responsibility.
- Integration tests verify interactions between components or with real or simulated infrastructure.
- Instrumented tests verify Android-dependent behavior and do not replace domain unit tests.

A test must remain independent, deterministic, and readable. It must not be added solely to increase coverage.

## Comments

Code must be clear enough not to require comments describing how it works. Comments that paraphrase the code are forbidden.

A comment is acceptable only when it documents information that code cannot clearly express, for example:

- a constraint imposed by Android, a printer, or an external library;
- the reason for a non-obvious decision;
- a temporary workaround, along with its removal condition.

A comment must describe the intent or constraint, remain accurate after code changes, and be removed when it is no longer relevant.

## Review

A change is acceptable when:

- its expected behavior is covered by appropriate tests;
- it respects the dependencies and responsibilities defined in the architecture;
- the applicable validations pass;
- exceptions to these rules are documented and approved.
