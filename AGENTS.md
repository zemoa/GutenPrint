# AGENTS.md

## Context

Kotlin Android project. The main module is `app/`.

## Required References

- `specs/High-level functionnal specification.md` defines the functional scope and ensures feature consistency.
- `specs/ARCHITECTURE.md` defines the architectural principles and technical consistency.
- `specs/DEVELOPPEMENT.md` defines the development rules and acceptance criteria.

Before making any change, review the relevant documents.

## Validation

Before considering a change complete, run:

```bash
./gradlew test lint assembleDebug
```

Also run `./gradlew connectedAndroidTest` for changes that depend on Android, the user interface,
or platform integration.
