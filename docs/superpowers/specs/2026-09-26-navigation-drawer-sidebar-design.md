# Navigation Drawer (Sidebar) Shell — Design

Status: Approved by user 2026-09-26. First of two sequential sub-projects
(sidebar shell, then Favorites) — see
[[project-routeforge-planned-features]] memory for the originating backlog
item (hamburger menu, confirmed 2026-09-23).

## Problem

The Simulation (home) screen's gear icon opens Settings directly. The user
wants a proper navigation drawer instead: a menu button that reveals a list
of destinations (Settings today, Favorites once built next), so future
top-level screens have one consistent place to live instead of each getting
their own dedicated icon competing for the same corner.

## Goals

1. Replace the gear `FloatingActionButton` on the Simulation screen with a
   hamburger-menu trigger that opens a drawer.
2. The drawer holds a small, generic list of destination items (icon +
   label + navigation callback). Ships with one item today: "Settings".
3. Adding a future destination (Favorites) is a one-line addition to that
   list — no structural rework.
4. Opens from the right (matching the trigger button's position), with
   standard drawer behavior (scrim, swipe-to-dismiss, back-press-to-close)
   provided by Material3, not hand-rolled.

## Non-goals

- An app-wide drawer available from every screen. Scoped to the Simulation
  screen only, matching where the trigger already lives today.
- Building the Favorites screen or any other destination beyond Settings —
  that's the next sub-project.
- Reverse geocoding / address search — separately deferred backlog items,
  not part of either sub-project right now.

## Approach

Material3 `ModalNavigationDrawer` wrapping the Simulation screen's content.
It opens from the *start* edge (left in LTR) by default; to open from the
right, the drawer is wrapped in `CompositionLocalProvider(LocalLayoutDirection
provides LayoutDirection.Rtl)`, with its *inner* content re-wrapped back to
`LayoutDirection.Ltr` (a standard, small workaround — not custom animation
code). Rejected alternative: hand-rolling a slide-in panel — would reimplement
scrim/swipe/back-press behavior `ModalNavigationDrawer` already provides.

## Data & state

No new domain/data layer — this is presentation-only. Drawer open/closed
state (`rememberDrawerState`) and the coroutine scope to call
`open()`/`close()` live in `SimulationRoot`/`SimulationScreen` (the Compose
layer), not `SimulationViewModel`/`SimulationState`/`SimulationAction` —
consistent with this project's existing "framework state stays in UI, not
hoisted to the ViewModel" convention (already followed elsewhere in this
codebase per the `android-skills:compose` `screen-structure.md` reference).

New reusable presentation-only pieces, both in
`feature/simulation/presentation` (scoped there since the drawer itself is
scoped to the Simulation screen; if a second screen needs the same drawer
shape later, extracting it into `core:design-system` is the trigger for
that, not now):

```kotlin
data class SidebarDestination(
    val icon: ImageVector,
    val labelRes: Int,
    val onClick: () -> Unit,
)
```

A `Sidebar` composable (`ModalDrawerSheet` content) rendering a list of
`SidebarDestination`s as `NavigationDrawerItem`s, and closing the drawer
after invoking the clicked item's `onClick`.

`SimulationScreen`'s composition becomes:

```kotlin
ModalNavigationDrawer(
    drawerState = drawerState,
    drawerContent = { Sidebar(destinations = listOf(
        SidebarDestination(Icons.Filled.Settings, R.string.simulation_open_settings_button) {
            onAction(SimulationAction.OnOpenSettingsClick) // existing event-channel path, unchanged
        },
    )) },
) {
    /* existing Scaffold + Box(map, pill, banners, controls) content, unchanged */
}
```

The existing `OnOpenSettingsClick` → `SimulationEvent.NavigateToSettings` →
`onOpenSettings()` callback chain (added in RF-028) is reused as-is — only
the trigger UI changes (hamburger button opening a drawer item, instead of
a standalone gear FAB), not the navigation plumbing.

## UI details

- Trigger: the existing small FAB slot (top-right, in the balanced Row from
  RF-030) now holds `Icons.Filled.Menu` instead of `Icons.Filled.Settings`,
  calling `drawerState.open()` (via `scope.launch`) instead of dispatching
  an action.
- Drawer content: a `ModalDrawerSheet` with a short header (app name or
  icon, matching this app's established card/header visual language) above
  the destination list. Each destination item shows its icon + label,
  standard Material3 `NavigationDrawerItem` styling (already theme-aware
  via `RouteForgeTheme`).

## Testing

Presentation-only, framework-state change — no new ViewModel logic to unit
test (per Constitution III, test-first applies to domain/data logic; this
sub-project adds none). Manual on-device verification: drawer opens on
trigger tap, opens from the right, closes on scrim tap/back-press/swipe,
tapping "Settings" navigates there and closes the drawer.
