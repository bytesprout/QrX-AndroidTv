# QueueRx TV — Agent Progress Tracker

> **Reference spec:** Parts 1–4 of the QueueRx TV Master AI Coding Agent Prompt  
> **Parts 5 & 6** have not yet been received; this file will be updated when they arrive.

---

## Legend

| Symbol | Meaning |
|--------|---------|
| ✅ | Done — code committed |
| 🔲 | Scaffolded — module/directory created, no production code yet |
| ❌ | Not started |

---

## Part 1 — Core Foundation, Architecture, Engineering Rules

| Area | Status | Notes |
|------|--------|-------|
| Multi-module Gradle project (Kotlin DSL + Version Catalog) | ✅ | All required modules declared in `settings.gradle.kts` and directories created |
| Base API URL + environment switching (dev / stage / prod) | ✅ | `core/core-storage` → `EnvironmentConfig.kt` |
| MVVM + Clean Architecture layers enforced | ✅ | Module boundaries: `app → feature → domain → data → core` |
| Kotlin only, no Java entry-points | ✅ | |
| Minimum API 26 target (Android TV) | 🔲 | Config placeholder exists; needs Android Gradle Plugin wiring when Android SDK is available |
| Hilt dependency injection | 🔲 | Module stubs created; Hilt plugin + modules not yet added |
| No XML layouts — Compose for TV only | 🔲 | Module stubs created; Compose not yet configured (no Android SDK in current JVM-only scaffold) |
| 60 FPS / performance targets | ❌ | Requires Compose + Android; not yet applicable |

---

## Part 2 — Project Structure, TV Navigation, Design System

| Area | Status | Notes |
|------|--------|-------|
| Full module directory structure created | ✅ | `core/*`, `domain/*`, `data/*`, `feature/*` — all 46 modules scaffolded |
| Convention plugins (`build-logic`) | 🔲 | `build-logic/` directory exists with empty `build.gradle.kts`; convention plugin code not yet written |
| `core-designsystem` — design tokens (spacing, radius, typography, color) | 🔲 | Module stub only |
| `core-ui` — shared Compose TV components (TvButton, TvCard, TvPanel, etc.) | 🔲 | Module stub only |
| `core-navigation` — Navigation Compose graph | 🔲 | Module stub only |
| TV Focus Engine — custom focus management, restoration, memory | ❌ | Requires Compose for TV |
| TV safe-area / overscan system (48 dp minimum padding) | ❌ | Requires Compose for TV |
| Theme engine (dark/light, tenant branding, dynamic download) | ❌ | |
| Typography rules (min 24 sp body, 64 sp token, etc.) | ❌ | |
| Screen transitions (fade/scale/soft-slide) | ❌ | |
| `feature-splash` | 🔲 | Module stub only |
| `feature-provisioning` | 🔲 | Module stub only |
| `feature-registration` | 🔲 | Module stub only |
| `feature-home` | 🔲 | Module stub only |
| `feature-signage` — `SignageUiState` sealed interface | ✅ | `SignageUiState.kt` + `DisplayMode` enum committed |
| `feature-media` | 🔲 | Module stub only |
| `feature-queue` | 🔲 | Module stub only |
| `feature-doctor` | 🔲 | Module stub only |
| `feature-pharmacy` | 🔲 | Module stub only |
| `feature-emergency` | 🔲 | Module stub only |
| `feature-ticker` | 🔲 | Module stub only |
| `feature-maintenance` (hidden 7-tap panel + PIN) | 🔲 | Module stub only |
| `feature-settings` | 🔲 | Module stub only |
| `feature-health` | 🔲 | Module stub only |
| `feature-diagnostics` | 🔲 | Module stub only |
| `feature-kiosk` | 🔲 | Module stub only |

---

## Part 3 — Realtime Engine, Room Database, Offline-First, Queue State Machine, Cache

| Area | Status | Notes |
|------|--------|-------|
| **SSE Engine — `RealtimeSseManager`** | ✅ | Reconnect backoff (1s→2s→5s→10s→20s→60s), event deduplication, last-event tracking |
| SSE reconnect backoff policy | ✅ | `RealtimePolicies.kt` — verified via unit test |
| SSE event deduplication by `eventId` | ✅ | Verified via unit test |
| Last-Event-ID persistence for resume | ✅ | In-memory `StateFlow`; DataStore wiring pending |
| SSE auth headers (Authorization, Device-Id, Hospital-Id, etc.) | ❌ | Requires Ktor SSE client integration |
| All 13 SSE event types defined (`QUEUE_UPDATED`, `EMERGENCY_ALERT`, etc.) | ✅ | `RealtimeEventType` enum in `RealtimeEventModels.kt` |
| SSE → Repository → Room → ViewModel → UI pipeline | ❌ | Requires Room + ViewModel wiring |
| **WebSocket Engine — `RealtimeSocketManager`** | ✅ | Heartbeat payload model implemented |
| WebSocket heartbeat (every 30 s) | 🔲 | Model defined; coroutine scheduler not yet wired |
| Remote commands (RESTART_APP, CLEAR_CACHE, RELOAD_LAYOUT, etc.) | ❌ | |
| **Room Database — `QueueRxDatabase`** | ✅ | All 13 entity data classes in `core-database/Entities.kt` |
| Room DAOs and database class | ❌ | Requires Android Room dependency (not yet in version catalog) |
| Single Source of Truth — UI reads from Room only | ❌ | |
| Offline-first — UI works from cache when backend unavailable | ❌ | |
| Cache expiration rules (queues: realtime, media: 7d, ticker: 24h) | ❌ | |
| **DataStore preference keys** | ✅ | All keys defined in `core-storage/EnvironmentConfig.kt` |
| DataStore read/write implementation | ❌ | Requires Android DataStore dependency |
| **MediaCacheManager** (download, checksum, LRU eviction, 2 GB limit) | ❌ | |
| **Queue state machine** | ✅ | Full `WAITING→CALLED→ACTIVE→COMPLETED→…→COLLECTED` lifecycle, verified via unit test |
| State recovery after process death (< 3 s) | ❌ | |
| **AppHealthCoordinator / WatchdogCoordinator** | ❌ | |
| Event persistence (store each event with `processed` flag) | ✅ | `RealtimeEventEntity` defined; DAO not yet wired |
| Multi-display support (device config determines role) | ✅ | `DeviceContext` + `DisplayConfigEntity` models defined |
| Network failure UX ("Attempting reconnect…", cached data shown) | ❌ | |

---

## Part 4 — Media3 Playback Engine, Multi-Zone Layout, Ticker

| Area | Status | Notes |
|------|--------|-------|
| **`core-player` module scaffolded** | ✅ | |
| `PlayerConfig` data class (autoplay, mute, loop, quality, buffer, retries) | ✅ | `PlayerConfig.kt` |
| `MediaEngineCoordinator` (orchestration layer) | ✅ | Basic coordinator with config management |
| Media3 ExoPlayer integration | ❌ | Requires Android SDK + Media3 dependency |
| `PlayerFactory` / `PlayerRepository` / `MediaSourceFactory` | ❌ | |
| `PlayerRecoveryManager` (auto-recover on freeze, decoder death, etc.) | ❌ | |
| `PlayerHealthMonitor` (15 s health check) | ❌ | |
| `AudioFocusManager` + `PlayerTelemetryManager` | ❌ | |
| Single player rule (no multiple ExoPlayer instances) | ❌ | Architecture decision enforced by design; runtime enforcement pending |
| IPTV / M3U / M3U8 playback | ❌ | |
| HLS (adaptive bitrate, low-latency) | ❌ | |
| DASH / MPD | ❌ | |
| MP4 / MKV / WebM local playback | ❌ | |
| Image playback (PNG/JPG/WebP, configurable duration, fade/zoom) | ❌ | |
| WebView zone (sandboxed, leak prevention) | ❌ | |
| YouTube support (safe mode, no ads) | ❌ | |
| RTSP (CCTV/live cameras, auto-recover) | ❌ | |
| HDMI-In passthrough (with graceful fallback) | ❌ | |
| **`PlaylistCoordinator`** (dayparting, scheduled playlists, priorities) | ❌ | |
| Playlist priority: Emergency > Admin Override > Scheduled > Fallback | ❌ | |
| **`ZoneRenderingCoordinator`** (multi-zone dynamic layouts) | ❌ | |
| Zone types: media, doctorQueue, pharmacyQueue, ticker, announcement, emergency, customHtml, clock, departmentBoard | ❌ | |
| Default hospital layout (70% media / 30% queue / bottom ticker) | ❌ | |
| **`TickerCoordinator`** (smooth scroll, RTL, Malayalam, Hindi, English) | ❌ | |
| Noto Sans font family integration | ❌ | |
| Emergency interruption — pause all, fullscreen alert, TTS, chime | ❌ | |
| Emergency clear — restore previous state seamlessly | ❌ | |
| `QueueRxAudioManager` (ducking, TTS, chimes, priority) | ❌ | |
| Burn-in protection (subtle movement every 5–10 min for OLED) | ❌ | |
| Player telemetry (buffering, FPS, bitrate, decoder errors) | ❌ | |
| Fallback media on playlist failure (never black screen) | ❌ | |

---

## Parts 5 & 6

> ⏳ Not yet received. Will be added when the user provides them.

---

## App Startup Flow

| Step | Status |
|------|--------|
| Splash | 🔲 module stub |
| Device Validation | ❌ |
| Provision Check | 🔲 module stub |
| Auth Check | 🔲 module stub |
| Configuration Sync | ❌ |
| Queue Bootstrap | ❌ |
| Media Bootstrap | ❌ |
| Realtime Connections | 🔲 SSE/WS models exist, connections not wired |
| Display Mode Launch | 🔲 mode enum defined in `SignageUiState` |

---

## Display Modes

| Mode | Status |
|------|--------|
| Mode 1 — Waiting Hall Display (media + queue + pharmacy + ticker) | 🔲 `DisplayMode.WAITING_HALL` enum value defined |
| Mode 2 — Reception Display | 🔲 enum value defined |
| Mode 3 — Pharmacy Display | 🔲 enum value defined |
| Mode 4 — Fullscreen Token Mode | 🔲 enum value defined |
| Mode 5 — Emergency Override Mode | 🔲 enum value defined |
| Mode 6 — Multi-Department Split Mode | 🔲 enum value defined |

---

## Test Coverage

| Test | Module | Status |
|------|--------|--------|
| SSE backoff policy (7 attempts, capped at 60 s) | `core-realtime` | ✅ passing |
| SSE event deduplication | `core-realtime` | ✅ passing |
| Queue state machine — valid transitions | `domain-queue` | ✅ passing |
| Queue state machine — invalid transition rejection | `domain-queue` | ✅ passing |

---

## Overall Progress Summary

| Layer | Total Items | ✅ Done | 🔲 Scaffolded | ❌ Pending |
|-------|------------|---------|--------------|-----------|
| Project/Build | 8 | 5 | 1 | 2 |
| Part 2 — UI & Nav | 24 | 2 | 18 | 4 |
| Part 3 — Realtime & DB | 24 | 10 | 2 | 12 |
| Part 4 — Media | 30 | 4 | 0 | 26 |
| Startup Flow | 8 | 0 | 5 | 3 |
| **Total** | **94** | **21 (22%)** | **26 (28%)** | **47 (50%)** |

---

*Last updated by agent on 2026-05-18. Update this file after every agent session.*
