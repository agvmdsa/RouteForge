# Foreground service declarations

Checked against the app at commit `d605278` (2026-10-26). In the Play Console: **Policy and programs > App content >
Foreground service permissions**. The Console asks, for each foreground service type the app declares, what it is used for, what
the user would lose without it, and for a link to a short video that shows the feature. The app declares two types.

| Type | Permission | Service | Started by |
|------|------------|---------|------------|
| `location` | `FOREGROUND_SERVICE_LOCATION` | `SimulationForegroundService` | the user teleporting to a place, pressing Start on a route, or using the joystick (each begins a simulated-location session) |
| `dataSync` | `FOREGROUND_SERVICE_DATA_SYNC` | `RegionDownloadForegroundService` | the user pressing Download on a map region |

The app does not ask for background location access (`ACCESS_BACKGROUND_LOCATION` is not declared), so the Console's
background location declaration does not apply.

## 1. Location

**What it is used for.** Keeping a simulated location running. RouteForge makes the device report a simulated position along a
route or from the joystick. The simulation must keep updating while the screen is off or another app is in front, because
the point of the app is that the app being tested reads the simulated position while the tester uses it. A notification with
Pause, Resume and Stop stays on screen while it runs.

**User-facing feature.** "Simulate a location" (teleport, play a route, use the joystick): the user starts it, sees the
notification, and stops it from the app or from the notification.

**Impact if the task cannot run.** Android would stop the app a few seconds after it leaves the screen, the simulated position
would stop updating and the device would go back to its real position in the middle of a test.

**Video shots** (30 to 60 seconds, screen recording, fictional positions only):
1. The app on the Simulate tab with a short route loaded; the notification permission prompt is allowed.
2. Press Start; the position moves on the map; the persistent notification appears with Pause, Resume and Stop.
3. Go to the home screen and open another maps app, which shows the simulated position moving.
4. Turn the screen off and on: the notification is still there and the position kept moving.
5. Press Stop in the notification; the notification disappears and the simulation ends.

## 2. Data sync

**What it is used for.** Downloading offline map regions. A region is made of routing data files that can add up to hundreds of
megabytes. The download keeps running while the user does something else, and shows a progress notification.

**User-facing feature.** "Download a map region": the user picks a region in the catalog and presses Download.

**Impact if the task cannot run.** The download would stop when the app leaves the screen, and the user would have to keep the
app open for the whole download.

**Video shots** (20 to 40 seconds):
1. Settings > Manage downloads; pick a region and press Download.
2. The progress notification appears; leave the app.
3. Come back after a moment: progress went on; when it ends, the region is listed as downloaded and the notification is gone.

## Things to re-check before the release

- Android 15 and newer give a `dataSync` service about six hours before the system asks it to stop. The app has no handler for
  that request yet; the declaration is only honest about the feature once the service stops cleanly at the limit.
- If a later Android version asks for a different task type for long downloads, this declaration and the service change
  together.
- Take the video with fictional positions and without personal favorites on screen.
