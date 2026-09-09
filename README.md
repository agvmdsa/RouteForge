# RouteForge

Android app that simulates device location (mock location / GPS injection) with real road-following routes — computed fully offline, on-device, no server or API key required.

## Why

Most "fake GPS" apps on the Play Store connect waypoints with a straight line and ignore basic physical consistency (speed, bearing, accuracy), which makes the simulated position trivially inconsistent with real sensor data. RouteForge aims to do this properly:

- **Real roads** — routes snap to actual streets, computed offline, with no rate limits.
- **Physically realistic movement** — speed, bearing, and accuracy stay internally consistent.
- **Guided setup** — a step-by-step flow for the Developer Options ritual Android requires to enable mock location.
- **No ads, open source.**

## Status

Early stage — actively being designed and built. Not yet on the Play Store.

## License

MIT — see [LICENSE](LICENSE).
