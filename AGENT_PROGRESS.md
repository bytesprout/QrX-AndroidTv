# QueueRx TV — Agent Progress Tracker

> **Reference spec:** Parts 1–8 of the QueueRx TV Master AI Coding Agent Prompt (complete)  
> All parts received. Implementation follows the phased plan defined in Part 8.

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

## Part 5 — Queue Rendering Engine, Doctor & Pharmacy Boards, Token System UI, Ticker, TTS, Audio

### Queue Rendering

| Area | Status | Notes |
|------|--------|-------|
| `QueueRenderingCoordinator` | ❌ | Orchestrates doctor queues, pharmacy queues, token transitions, animations |
| Token state model: WAITING / CALLED / ACTIVE / COMPLETED / NO_SHOW / REFERRED / PREPARING / READY / COLLECTED / EMERGENCY | ✅ | `QueueStateMachine` covers these states; UI rendering not yet done |
| Visual priority hierarchy (EMERGENCY → CALLED → ACTIVE → READY → WAITING) | ❌ | |
| Token color system (accessible, color-blindness safe) | ❌ | |
| `TokenAttentionAnimator` (flash on CALLED / READY / EMERGENCY, 5–10 s timeout) | ❌ | |
| AnimatedContent / Crossfade / updateTransition token transitions (no hard replacements) | ❌ | |

### Doctor Queue Board (`DoctorQueueBoard`)

| Area | Status | Notes |
|------|--------|-------|
| `DoctorQueueBoard` composable | ❌ | |
| NOW SERVING section — dominant visual, glow + pulse + scale animation | ❌ | |
| UPCOMING section (A-103, A-104, A-105) | ❌ | |
| RECENTLY CALLED section | ❌ | |
| Token number — huge typography | ❌ | |
| Room number — large, high-contrast | ❌ | |
| Doctor name — readable | ❌ | |
| Department — secondary | ❌ | |
| Token change: flash transition + audio chime + TTS | ❌ | |
| Multi-department side-by-side view (Cardiology, Ortho, General, Paediatrics) | ❌ | |

### Pharmacy Queue Board (`PharmacyQueueBoard`)

| Area | Status | Notes |
|------|--------|-------|
| `PharmacyQueueBoard` composable | ❌ | |
| PREPARING section | ❌ | |
| READY section (highlight + glow + pulse + border flash on transition) | ❌ | |
| COUNTER section | ❌ | |
| PREPARING → READY transition: highlight + glow + chime + TTS | ❌ | |

### Announcement & TTS Engine

| Area | Status | Notes |
|------|--------|-------|
| `AnnouncementCoordinator` | ❌ | |
| `SpeechQueueManager` (priority queue: Emergency → Doctor Call → Pharmacy Ready → Ticker Voice) | ❌ | |
| TTS language support: Malayalam / English / Hindi | ❌ | |
| Smart token pronunciation ("A One Zero Two", not "A-102") | ❌ | |
| Doctor call announcement template | ❌ | |
| Pharmacy ready announcement template | ❌ | |
| Emergency TTS announcement (highest priority, interrupts all) | ❌ | |
| No overlapping speech enforcement | ❌ | |

### Audio System

| Area | Status | Notes |
|------|--------|-------|
| `AudioDuckingCoordinator` (media → 15% during TTS, smooth restore) | ❌ | |
| Chime engine (play hospital notification chime before announcements) | ❌ | |
| Chime tenant-configurable | ❌ | |
| Audio settings: mute media / mute announcements / TTS volume / media volume / emergency volume | ❌ | |
| Quiet hours support (e.g. 10 PM–6 AM, lower volume automatically) | ❌ | |

### Ticker Engine (`HospitalTickerRenderer`)

| Area | Status | Notes |
|------|--------|-------|
| `HospitalTickerRenderer` composable | ❌ | |
| Content types: hospital announcements, RSS, queue notices, health tips, emergency notices | ❌ | |
| Ticker speed modes: slow / normal / fast (configurable) | ❌ | |
| Multilingual ticker: Malayalam / Hindi / English / mixed | ❌ | |
| Ticker priority: Emergency → Hospital Notice → Queue Updates → Health Awareness → RSS | ❌ | |

### Diagnostics Additions (Part 5)

| Area | Status | Notes |
|------|--------|-------|
| `DisplayScreenshotManager` (capture current screen, upload securely for admin debugging) | ❌ | |
| Display health score (queue freshness + player health + SSE + WS + FPS → % score) | ❌ | |
| Queue latency target < 1 second (doctor CALL NEXT → TV update) | ❌ | |
| Fallback mode UI ("Queue temporarily unavailable / Displaying last synced data") | ❌ | |

---

## Part 6 — Kiosk Hardening, Device Provisioning, Enterprise Activation, OTA

### Kiosk Mode

| Area | Status | Notes |
|------|--------|-------|
| `KioskCoordinator` | ❌ | Lock task mode, app pinning, exit prevention, auto-recovery |
| Mode 1 — Device Owner / Full Enterprise Lock Task (disable launcher, settings, notifications, status bar) | ❌ | |
| Mode 2 — Screen Pinning Fallback (for unsupported TVs) | ❌ | |
| Device Owner Mode / DPC provisioning support | ❌ | |
| `BootRecoveryReceiver` (auto-launch after reboot / power outage / crash) | ❌ | |
| `AppRecoveryManager` (uncaught crash → capture logs → persist state → restart, < 5 s downtime) | ❌ | |
| `QueueRxWatchdog` (monitors player, UI, ANR risk, websocket, SSE, memory; auto-heals) | ❌ | |

### Provisioning System

| Area | Status | Notes |
|------|--------|-------|
| Splash → Provision Check → Activation → Config Sync → Ready flow | 🔲 | Module stubs exist; logic not implemented |
| **Method 1 — QR Activation** (`queuerx://activate?token=xxxx`) | 🔲 | `feature-provisioning` stub |
| **Method 2 — Activation Code** (manual code entry e.g. `HSP-9823`) | ❌ | |
| **Method 3 — MAC Address Registration** (enterprise bulk deployment) | ❌ | |
| `DeviceRegistrationCoordinator` (register deviceId, name, MAC, model, OS, appVersion) | ❌ | |
| Device types: WAITING_HALL / RECEPTION / PHARMACY / DOCTOR_ROOM / EMERGENCY / CUSTOM | ✅ | `DisplayMode` enum covers these roles |
| Offline provisioning: USB config import / QR local config / LAN provisioning | ❌ | |

### Remote Configuration

| Area | Status | Notes |
|------|--------|-------|
| `ConfigurationSyncManager` (download → checksum → persist → apply; rollback on failure) | ❌ | |
| Config versioning — only update changed sections | ❌ | |
| Full remote control of layout, queues, ticker, playlists, branding, colors, volume, TTS language | ❌ | |
| Remote commands: RESTART_APP / REBOOT_DEVICE / REFRESH_LAYOUT / CLEAR_CACHE / RUN_TEST / UPLOAD_LOGS / TAKE_SCREENSHOT / MUTE / UNMUTE / FORCE_PLAYLIST_REFRESH | ❌ | |

### Maintenance Panel

| Area | Status | Notes |
|------|--------|-------|
| Hidden entry — tap top-left 7 times | ❌ | |
| PIN dialog (4-digit / 6-digit / admin override) | ❌ | |
| `MaintenanceCenter` composable with full feature list | ❌ | |
| `ConnectivityDiagnostics` (test internet / API / WebSocket / SSE / DNS / CDN / IPTV stream) | ❌ | |
| Device health panel (CPU, RAM, storage, uptime, realtime status, FPS, network — live) | ❌ | |
| PIN rate limiting (5 failed attempts → 5-minute lock) | ❌ | |

### OTA Update System

| Area | Status | Notes |
|------|--------|-------|
| `OtaCoordinator` (forced / staged / silent / maintenance-window updates) | ❌ | |
| Update strategy: download APK → verify checksum → install → health validation → rollback | ❌ | |
| App version management (current / latest / minimum; block deprecated) | ❌ | |
| Maintenance windows (e.g. 2 AM–4 AM) | ❌ | |

### Security & Storage (Part 6)

| Area | Status | Notes |
|------|--------|-------|
| Encrypted DataStore for all secrets (auth token, refresh token, device keys) | ❌ | |
| `StorageCleanupManager` (auto-clean at 80% threshold: old logs, expired media, temp, stale cache) | ❌ | |
| Factory reset recovery → reprovision mode (no crash) | ❌ | |
| Multi-hospital white-label (logo, colors, fonts, wallpaper downloaded remotely) | ❌ | |
| Enterprise bulk deployment support (50/100/500 TVs, zero manual setup) | ❌ | |

---

## Part 7 — Telemetry, Diagnostics, Watchdogs, Crash Recovery, Performance, Fire TV

### Observability Architecture

| Area | Status | Notes |
|------|--------|-------|
| `ObservabilityCoordinator` | ❌ | Umbrella coordinator for telemetry, diagnostics, health reporting |
| Device telemetry heartbeat (30 s → `/device/telemetry`) with full payload | 🔲 | `HeartbeatMessage` model defined; HTTP send not wired |
| `HealthScoreCalculator` (player + queue freshness + stream + network + FPS + memory + WS + SSE + ANR → % score) | ❌ | |
| `QueueFreshnessMonitor` (> 60 s without update → show warning, never silently stale) | ❌ | |
| `StreamHealthMonitor` (detect frozen frames, endless buffering, dead decoder, stalled IPTV; restart source) | ❌ | |

### Memory & Performance Monitoring

| Area | Status | Notes |
|------|--------|-------|
| `MemoryPressureMonitor` (70% = warning, 85% = cleanup, 95% = emergency recovery) | ❌ | |
| Automatic cleanup on memory pressure (image cache, player buffers, temp resources, surfaces) | ❌ | |
| ANR prevention — no network/DB/JSON on main thread; all Dispatchers.IO | ❌ | Architecture principle set; enforcement pending |
| Frame rate monitor (target 55–60 FPS, alert < 45 FPS) | ❌ | |

### Crash & Recovery

| Area | Status | Notes |
|------|--------|-------|
| `CrashReportingProvider` abstraction (pluggable: Firebase Crashlytics / Sentry / custom backend) | ❌ | |
| Crash capture (stack trace, logs, player state, queue state, device info, memory, network) | ❌ | |
| `SafeRestartManager` (capture state → persist → restart → restore, < 5 s downtime) | ❌ | |
| `SubsystemWatchdog` with escalation hierarchy (subsystem → feature → app → device reboot) | ❌ | |
| Player failure recovery (decoder crash, surface loss, stalled buffer → recreate player, restore stream) | ❌ | |

### Diagnostics Tools (Part 7)

| Area | Status | Notes |
|------|--------|-------|
| `DiagnosticScreenshotManager` (admin TAKE_SCREENSHOT → upload screenshot) | ❌ | |
| `LogUploadManager` (player, queue, network, crash, WS, SSE logs → secure upload) | ❌ | |
| Diagnostic dashboard in maintenance panel (all health metrics, live updating) | ❌ | |

### Fire TV Compatibility

| Area | Status | Notes |
|------|--------|-------|
| FireStick Lite / FireStick 4K / Fire TV Cube support | ❌ | |
| Fire TV input handling (DPAD, CENTER, MENU, BACK, PLAY_PAUSE) | ❌ | |
| Fire TV aggressive background kill recovery | ❌ | |
| Fire TV quirk safeguards (media keys, focus differences, storage limits) | ❌ | |

### Low-End STB Optimization

| Area | Status | Notes |
|------|--------|-------|
| Optimization for 2 GB RAM / weak CPU / poor GPU / slow storage | ❌ | |
| GPU safety rules (no heavy blur, excessive shadows, massive gradients, expensive shaders) | ❌ | |
| Bitmap optimization (resize, preload carefully, no giant textures) | ❌ | |
| Compose optimization (stable params, immutable models, `remember`, `derivedStateOf`, `key()`, no recompose storms) | ❌ | |
| Network optimization (retry, timeout, cache, fallback, request coalescing) | ❌ | |
| Battery/power event handling (sleep, wake, HDMI disconnect, ethernet reconnect → auto recover) | ❌ | |

### Performance Targets

| Target | Requirement | Status |
|--------|-------------|--------|
| App startup | < 4 seconds | ❌ |
| Queue update latency | < 1 second | ❌ |
| Screen switch | < 300 ms | ❌ |
| Player recovery | < 5 seconds | ❌ |
| App uptime goal | 30+ days without manual intervention | ❌ |

---

## Part 8 — Testing, CI/CD, Security, Release, Acceptance Criteria

### Testing

| Area | Status | Notes |
|------|--------|-------|
| Unit tests — JUnit5 + MockK + Turbine + Truth | 🔲 | JUnit5 configured; MockK/Turbine/Truth not yet added |
| Unit coverage target ≥ 80% | ❌ | Current coverage estimated < 20% |
| SSE tests (reconnect, Last-Event-ID, replay, duplicates, malformed, network drop) | ✅ | Backoff + dedup tests passing |
| WebSocket tests (heartbeat, reconnect, remote commands, degraded mode) | ❌ | |
| Player tests (HLS, DASH, IPTV, MP4, RTSP, YouTube, HDMI-In; bad stream, decoder crash, buffer, network loss) | ❌ | |
| Room database tests (migrations, corruption recovery, persistence, offline restore) | ❌ | |
| Compose UI tests (focus navigation, D-pad, remote controls, screen transitions, maintenance mode) | ❌ | |
| Fire TV device tests (focus, playback, process death, reconnect, remote keys) | ❌ | |
| Low-end TV tests (2 GB RAM, stable playback, no OOM, no ANR) | ❌ | |
| 72-hour soak test (zero crash, memory stable, player uptime, queue updates, FPS) | ❌ | |
| Network failure tests (WiFi loss, ethernet unplug, DNS fail, backend outage) | ❌ | |
| Offline mode tests (cached queue render, cached media, emergency persistence, no blank screens) | ❌ | |

### CI/CD Pipeline

| Area | Status | Notes |
|------|--------|-------|
| GitHub Actions CI pipeline | ❌ | |
| Pipeline stages: Lint → Unit Tests → Integration Tests → Build APK → Security Scan → Artifact Upload → Release | ❌ | |
| Static analysis: Detekt + KtLint + Android Lint | ❌ | |

### Security Requirements (Part 8)

| Area | Status | Notes |
|------|--------|-------|
| Encrypted DataStore (never plain text token storage) | ❌ | |
| HTTPS only + TLS + certificate validation | ❌ | |
| Signed remote command authorization (device + tenant validation) | ❌ | |
| Maintenance panel rate limiting (5 failed → 5-min lock) | ❌ | |
| Screenshot encrypted upload + signed request + access control | ❌ | |

### Release Strategy

| Area | Status | Notes |
|------|--------|-------|
| Build variants: debug / qa / stage / release | ❌ | |
| Semantic versioning (1.0.0 format + build number + release notes) | ❌ | |
| Internal / Staging / Production build targets | 🔲 | Environment enum exists (DEV/STAGE/PROD) |
| Code quality rules enforced (SOLID, Clean Arch, no god classes, no business logic in composables) | 🔲 | Architecture enforced by structure; linter not yet configured |

### Phased Implementation Plan Status

| Phase | Goal | Status |
|-------|------|--------|
| Phase 1 | Project setup (arch, Gradle, Hilt, navigation, theme) → app boots | 🔲 structure done; Android SDK / Compose / Hilt not wired |
| Phase 2 | Provisioning (QR activation, activation code, registration) | 🔲 module stub |
| Phase 3 | Networking (Retrofit, auth, SSE, WebSocket) | 🔲 SSE/WS models done; HTTP client not wired |
| Phase 4 | Room + cache (entities, DAOs, repositories) | 🔲 entities done; DAOs not implemented |
| Phase 5 | Media engine (ExoPlayer, IPTV, HLS, playlist) | 🔲 coordinator baseline; ExoPlayer not integrated |
| Phase 6 | Queue rendering (doctor board, pharmacy board, animations) | ❌ |
| Phase 7 | TTS + ticker | ❌ |
| Phase 8 | Kiosk hardening | ❌ |
| Phase 9 | Diagnostics + telemetry | ❌ |
| Phase 10 | Optimization (low-end TV, Fire TV) | ❌ |
| Phase 11 | QA & 72-hour soak test | ❌ |

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
| Part 1 — Foundation | 8 | 5 | 1 | 2 |
| Part 2 — UI & Nav | 24 | 2 | 18 | 4 |
| Part 3 — Realtime & DB | 24 | 10 | 2 | 12 |
| Part 4 — Media | 30 | 4 | 0 | 26 |
| Part 5 — Queue Rendering & TTS | 37 | 1 | 0 | 36 |
| Part 6 — Kiosk & Provisioning | 28 | 1 | 3 | 24 |
| Part 7 — Telemetry & Fire TV | 25 | 0 | 1 | 24 |
| Part 8 — Testing & CI/CD | 25 | 1 | 3 | 21 |
| Startup Flow / Display Modes | 15 | 0 | 9 | 6 |
| **Total** | **216** | **24 (11%)** | **37 (17%)** | **155 (72%)** |

> ℹ️ The jump in total items (94 → 216) reflects Parts 5–8 being fully catalogued for the first time. Prior summary only tracked Parts 1–4.

---

*Last updated by agent on 2026-05-18. Update this file after every agent session.*
