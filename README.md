# RouteForge

Android app that simulates device location (mock location / GPS injection) with real road-following routes — computed fully offline, on-device, no server or API key required.

## Why

Most "fake GPS" apps on the Play Store connect waypoints with a straight line and ignore basic physical consistency (speed, bearing, accuracy), which makes the simulated position trivially inconsistent with real sensor data. RouteForge aims to do this properly:

- **Real roads, by choice** — when a route is ready to play, RouteForge checks whether every leg can be driven along real streets. If so, you choose between **Guided** (follows real roads) and **Free-roam** (goes straight through your points, for off-road or unrestricted movement); if not, only Free-roam is offered — never a broken option.
- **Physically realistic movement** — speed, bearing, and accuracy stay internally consistent.
- **Guided setup** — a step-by-step flow for the Developer Options ritual Android requires to enable mock location.
- **Always-on joystick** — free-direction movement is available at any time, including as a deliberate, confirmed interrupt of a route that's currently playing.
- **No ads, open source.**

## Basic usage

- **Build a route** — tap the map to add waypoints, drag to reorder, tap a waypoint to edit or delete it, undo any change.
- **Choose how it plays** — Guided (real roads) and Free-roam (straight lines) are offered based on what's actually computable for your points; pick one to lock it in for that playback session. Before playing, RouteForge checks whether the map data Guided needs is downloaded and offers to fetch it — offline map coverage is computed on demand for anywhere in the world (BRouter's global tile grid), not limited to a fixed list of regions.
- **Import/export** — load a route from a JSON or GPX file, or export any route you've built back to either format.
- **Tune playback** — a shared speed selector (0-150 km/h) and an execution mode (once, N times, or loop), both adjustable before and during playback.
- **Joystick** — drag to move freely in any direction at any time; touching it during an active route asks for confirmation first, then hands off control from where the route was interrupted.

## Status

Early stage — actively being designed and built. Not yet on the Play Store.

## License

MIT — see [LICENSE](LICENSE).
