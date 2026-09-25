# Offline Map Storage Quota & Settings Screen — Design

Status: Approved by user 2026-09-25. Ready for implementation planning.

## Problem

RouteForge downloads offline map segments (`.rd5` files) into
`filesDir/routing/segments/` with no cap. There is no way to bound how much
disk space this uses, and no way to reclaim space automatically — the user
has to manually notice and never gets prompted. There is also no global
settings entry point in the app today; the only door into region management
is the "Manage offline regions" button inside route creation
(`RouteRequestScreen` → `RegionCatalogRoute`).

## Goals

1. A general Settings screen, reachable via a gear icon on the main
   (Simulation) screen.
2. A user-configurable maximum storage budget for downloaded region data.
3. Automatic, silent eviction of the least-recently-*used* (not merely
   least-recently-*downloaded*) regions when the budget would otherwise be
   exceeded.
4. Never evict a region the current route draft or the active/loaded route
   actually needs.

## Non-goals

- A general-purpose app settings hub with many sections. Only one section
  ("Offline map storage") exists for now. If an unrelated section is needed
  later, that is the trigger to extract a dedicated `feature:settings`
  module — not before.
- Solving the case where protected (currently-needed) regions alone exceed
  the configured quota. Eviction simply stops in that case; the total may
  stay over budget. This is an accepted limitation.
- Tracking usage at anything finer than "region" granularity (no per-tile
  tracking within a multi-tile manifest region).

## Module placement

No new Gradle module. Every capability this feature needs (region catalog,
downloads, on-disk `.rd5` files) already lives in `feature:routing`. Adding
it there avoids introducing this codebase's first cross-feature module
dependency (today, every feature depends only on `core:domain` and
`core:design-system`, never on another feature).

New/changed files, all in `feature/routing/domain` and
`feature/routing/presentation` unless noted:

- `domain/RegionUsageTracker.kt` — interface.
- `domain/StorageQuotaStore.kt` — interface.
- `domain/usecase/RecordRegionUsageUseCase.kt`
- `domain/usecase/ObserveStorageQuotaUseCase.kt`
- `domain/usecase/SetStorageQuotaUseCase.kt`
- `domain/usecase/EnforceStorageQuotaUseCase.kt`
- `domain/usecase/GetStorageUsageSummaryUseCase.kt` (used vs. total bytes
  across all currently-downloaded regions)
- `domain/RegionCatalog.kt` — add a method to get real on-disk size for a
  region's tiles (needed by both the usage summary and eviction).
- `feature/routing/data/SharedPreferencesRegionUsageTracker.kt`
- `feature/routing/data/SharedPreferencesStorageQuotaStore.kt`
- `feature/routing/data/di/RoutingDataModule.kt` — bind the two new stores.
- `feature/routing/domain/usecase/ComputeRouteUseCase.kt` — record usage for
  every touched region on success.
- `feature/routing/domain/usecase/DownloadRegionUseCase.kt` — record usage
  for the just-downloaded region, then call `EnforceStorageQuotaUseCase`.
- `feature/routing/presentation/SettingsScreen.kt`,
  `SettingsViewModel.kt`, `SettingsState.kt`, `SettingsAction.kt`,
  `SettingsRoute.kt` (new `@Serializable data object`)
- `feature/routing/presentation/RoutingNavGraph.kt` — register
  `SettingsRoute`. `routingGraph` already receives `navController` (used
  today for `RouteRequestRoot`'s `onOpenRegionCatalog`), so
  `SettingsRoot`'s "Manage downloads" callback is wired the same way:
  `onManageDownloads = { navController.navigate(RegionCatalogRoute) }`.
- `feature/routing/presentation/di/RoutingPresentationModule.kt` — register
  `SettingsViewModel` + new use cases.
- `feature/simulation/presentation/SimulationNavGraph.kt` — add
  `onOpenSettings: () -> Unit` param, threaded to `SimulationRoot`.
- `feature/simulation/presentation/SimulationScreen.kt` — gear
  `IconButton`/FAB at `Alignment.TopEnd`, calling `onOpenSettings`.
- `app/src/main/kotlin/com/routeforge/app/MainActivity.kt` — wire
  `onOpenSettings = { navController.navigate(SettingsRoute) }` from the
  simulation graph call site to the routing graph's `SettingsRoute`.

## Data model & persistence

No new dependency. Both stores use a dedicated `SharedPreferences` file via
`androidContext().getSharedPreferences(name, MODE_PRIVATE)`, following the
JSON-via-kotlinx.serialization pattern already used for
`regions-manifest.json`.

**`StorageQuotaStore`**
```kotlin
interface StorageQuotaStore {
    fun observeMaxBytes(): Flow<Long?> // null = unlimited
    fun setMaxBytes(maxBytes: Long?)
}
```
Backing pref key: `"max_storage_bytes"` (Long, absent/`-1` = unlimited).
Default when never set: `1_000_000_000L` (1 GB).

**`RegionUsageTracker`**
```kotlin
interface RegionUsageTracker {
    fun recordUsed(regionIds: Set<String>, atMillis: Long = System.currentTimeMillis())
    fun lastUsedAt(regionId: String): Long?
    fun clear(regionId: String)
}
```
Backing pref key: `"region_usage_json"`, a JSON-encoded
`Map<String, RegionUsageRecord>` where
`RegionUsageRecord(lastUsedAtMillis: Long, useCount: Int)`.

## Usage-tracking hook

`ComputeRouteUseCase` (Guided mode's real route computation) already has
`RegionCatalog` injected. On `Result.Success`, compute the distinct regions
touched by the (snapped) points — the same logic
`ComputeRequiredRegionsUseCase` already implements — and call
`regionUsageTracker.recordUsed(touchedRegionIds)`. Free-roam mode does not
touch region data and is not instrumented.

`DownloadRegionUseCase` calls `recordUsed(setOf(regionId))` immediately
after a successful download, so a region is never evicted before its first
real use, then calls `EnforceStorageQuotaUseCase()`.

## Eviction algorithm (`EnforceStorageQuotaUseCase`)

```
quota = storageQuotaStore.observeMaxBytes().first() ?: return // unlimited
totalBytes = sum of real .rd5 file sizes on disk (segmentDirectory)
if totalBytes <= quota: return

protectedIds = regionsTouching(draftWaypointsHolder.points.value) +
               regionsTouching(lastComputedRouteHolder.route.value?.points ?: emptyList())
               // via regionCatalog.regionContaining(lat, lon), same helper ComputeRequiredRegionsUseCase uses

candidates = regionCatalog.listRegions()
    .filter { it.status != RegionStatus.NOT_DOWNLOADED && it.id !in protectedIds }
    .sortedBy { regionUsageTracker.lastUsedAt(it.id) ?: 0L } // never-tracked = oldest, evicted first

for region in candidates:
    if totalBytes <= quota: break
    delete region's tileIds files from segmentDirectory
    regionUsageTracker.clear(region.id)
    totalBytes -= (bytes actually freed)
```

Runs synchronously (file I/O, off the main thread — same coroutine context
`DownloadRegionUseCase` already runs in) after every successful download,
and once right after `SetStorageQuotaUseCase` lowers the quota (so tightening
the budget takes effect immediately, silently).

`RegionCatalog` gains one new method to support both the usage-summary
display and eviction's byte accounting:
```kotlin
fun actualSizeOnDiskBytes(region: Region): Long // sums File.length() for region.tileIds present in segmentDirectory
```

## Settings screen UI

`SettingsScreen` (new, `feature/routing/presentation`), reached via
`SettingsRoute`. One section, "Offline map storage":

- A progress bar + label showing `usedBytes` of `quotaBytes` (or just
  "X MB used" with no bar when quota is Unlimited) — same visual language
  as `SetupHeader`'s progress bar (`LinearProgressIndicator` +
  `MaterialTheme.colorScheme` roles, card via `Surface`/`surfaceVariant`).
- A row of quota choice chips: **500 MB, 1 GB (default), 2 GB, 5 GB,
  Unlimited** — `FilterChip` row, single-select, instead of a slider (this
  is a rarely-touched, coarse-grained choice; discrete chips beat precise
  dragging per this app's established input-method guidance).
- A "Manage downloads" `Button` that navigates to the existing
  `RegionCatalogRoute` (unchanged screen).

`SimulationScreen` gets a gear `IconButton` at `Alignment.TopEnd`
(`Icons.Filled.Settings`, already available via
`compose.material.icons.extended`), calling the new `onOpenSettings`
callback — same shape as the existing `onPlanRoute`/`onOpenSetup` callbacks.

## Testing

- `SharedPreferencesStorageQuotaStore` / `SharedPreferencesRegionUsageTracker`
  — instrumented or Robolectric-style test if the project has that
  infrastructure; otherwise cover the domain-facing use cases against fakes,
  matching how `DeveloperSettingsDataSource` is faked today
  (`FakeDeveloperSettingsDataSource`).
- `EnforceStorageQuotaUseCaseTest` — the core logic to verify: (a) no-op
  under quota, (b) evicts oldest-used-first until under quota, (c) never
  evicts a region in `protectedIds`, (d) stops (does not loop/crash) when
  protected regions alone exceed quota.
- `ComputeRouteUseCaseTest` — extend existing tests to assert
  `recordUsed` is called with the correct region ids on success, and not
  called on failure.
- `DownloadRegionUseCaseTest` — extend to assert usage is recorded and
  `EnforceStorageQuotaUseCase` is invoked after a successful download.
- `SettingsViewModelTest` — quota selection updates state and calls
  `SetStorageQuotaUseCase`; usage summary reflects
  `GetStorageUsageSummaryUseCase`.

## Open edge cases explicitly accepted, not solved

- Protected regions alone exceeding quota: eviction stops, total stays over
  budget.
- Lowering the quota below what's currently protected: same as above.
- No UI feedback when eviction actually happens (silent, per approved
  design) — nothing to test for a snackbar/log because there isn't one.
