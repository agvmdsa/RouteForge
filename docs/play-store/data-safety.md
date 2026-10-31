# Data safety form: answers

Checked against the app at commit `d605278` (2026-10-26); the address lookup row, its notes and the log statement were checked again after RF-222 (2026-10-31). The Play Console words its questions in its own way and changes them
now and then: read each question there before copying an answer. The answers below say what the app does.

Decision of the maintainer (2026-10-28): the form declares the map area that tile and region requests reveal as approximate
location, and the searched text as search history, both collected and shared. The device's own position is declared as not
collected.

## What the app sends off the device

| Request | Service | What the service receives |
|---------|---------|---------------------------|
| Map pictures | `tile.openstreetmap.org` (Standard), `a/b/c.tile.opentopomap.org` (Topographic) | tile coordinates of the area on screen, network address, user agent |
| Place search | `nominatim.openstreetmap.org/search` | the typed text (`q`), `format`, `limit`, network address, user agent |
| Address of a spot | `nominatim.openstreetmap.org/reverse` | the latitude and longitude (`lat`, `lon`, six decimals) of a spot the user tapped on a map or of a place with no name, `format`, `zoom`, `addressdetails`, the device language (`Accept-Language`), network address, user agent; at most one request a second, none for 10 minutes after the service refuses the app |
| Region download | `brouter.de/brouter/segments4/` | file names that identify the chosen map tiles, network address, user agent |
| Shared short map link | the host of the link (`maps.app.goo.gl`) | the link itself, network address, user agent; one request, response headers only, no redirect followed |

Nothing else leaves the device: no analytics, advertising, crash-reporting or sign-in library is in the app (checked in the
dependency list), the last known real location is kept in memory only, and a search of the log calls found none that writes the user's position or search text (the one hit logs a coordinate that Nominatim returned in a result it could not parse).
No cleartext traffic is allowed (no network security configuration, and the app targets an Android version that blocks it), so
every request is encrypted.

The address lookup sends only a position the user chose (a tap on a map, or a place that arrived with no name), never the device's own
position, and there is no setting to turn it off (decided 2026-10-31); offline it simply does not happen. The latest answer is kept in
memory only. Its failure logs name the kind of failure and never the request address.

## Section: data collection and security

| Question | Answer |
|----------|--------|
| Does the app collect or share any of the required user data types? | Yes |
| Is all of the user data collected encrypted in transit? | Yes |
| Can users request that their data be deleted? | The developer receives and holds no data, so there is nothing to delete; if the Console forces a choice, pick the option for apps that keep no data, and say so in the policy (it does) |
| Does the app follow the Families policy? | No (not directed to children) |
| Independent security review | No |

## Section: data types

Everything not listed is **not collected** (personal info, financial info, health and fitness, messages, photos and videos,
audio, files and documents, calendar, contacts, web browsing, app info and performance, device or other identifiers).

| Data type | Collected | Shared | Required or optional | Purpose | Notes |
|-----------|-----------|--------|----------------------|---------|-------|
| Location: approximate location | Yes | Yes (map servers, routing data server) | Required for the map to show | App functionality | It is the map area requested, not the device's position. Leave "processed ephemerally" unchecked: the servers' retention is theirs |
| Location: precise location | No | No | n/a | n/a | The device's real position is read on the device only and held in memory; the simulated position is set on the device only. The exact coordinates of a spot the user taps are sent to the address service: that is a position the user chose, not the device's; how to declare it is the maintainer's decision when the form is filled (it may be answered as approximate location, like the map area, or as precise location of a chosen spot) |
| App activity: in-app search history | Yes | Yes (Nominatim) | Optional (the user chooses to search) | App functionality | The app keeps no history itself; the text is sent to answer the search |
| Device or other IDs | No | No | n/a | n/a | The servers see the network address like any internet request; the policy says so |

## Things to re-check before the release

- A new network call, a new library or a new kind of stored data changes this page, the privacy policy and the Console answers.
- The privacy policy (page `privacy/index.html` of the separate `routeforge-site` repository) gets a sentence about the address lookup once it is
  confirmed on a device; until then the page does not mention it.
- The user agent of the requests will carry the contact address once that change is made; the policy already says it does.
- The Console's form may list network (IP) addresses under a data type in a later version: if it does, add it to the rows
  above and to the policy.
