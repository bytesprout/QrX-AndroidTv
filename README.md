# QrX-AndroidTv

QueueRx TV foundational enterprise scaffold with modular Clean Architecture boundaries, realtime core primitives, queue state machine logic, and baseline verification tests.

## Build and test

```bash
./gradlew test
./gradlew :app:run
```

## Implemented foundation

- Multi-module Gradle Kotlin DSL structure across `app`, `core`, `domain`, `data`, and `feature` namespaces.
- Environment configuration for QueueRx API environments (dev/stage/prod).
- Realtime base engine primitives:
  - SSE reconnect backoff policy (1, 2, 5, 10, 20, 60 seconds cap)
  - Event de-duplication by `eventId`
  - Last event tracking for resume support
  - WebSocket heartbeat payload model
- Queue lifecycle state machine for consultation and pharmacy transitions.
- Core player configuration and coordinator baseline.
- Required data entities for offline-first persistence modeling.
- UI state contract with explicit loading/success/error/empty/offline coverage.
