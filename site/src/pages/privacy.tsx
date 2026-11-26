import Head from "next/head";
import { MaxWidthWrapper } from "@/components/utils/MaxWidthWrapper";
import { Prose } from "@/components/prose/Prose";
import { CONTACT_EMAIL } from "@/components/utils/links";

export default function Privacy() {
  return (
    <>
      <Head>
        <title>RouteForge: Privacy Policy</title>
        <meta
          name="description"
          content="What the RouteForge app keeps on your device and which public services it contacts."
        />
      </Head>
      <main>
        <MaxWidthWrapper className="relative max-w-3xl pb-20 pt-28">
          <h1 className="mb-2 font-display text-3xl font-semibold tracking-tight text-espresso md:text-5xl">
            RouteForge: Privacy Policy
          </h1>
          <p className="mb-8 text-coffee/70">Last updated: 25 November 2026</p>
          <Prose>
            <h2>In short</h2>
            <ul>
              <li>
                RouteForge has no account, no ads, no analytics and no tracking.
                The developer receives no data from the app.
              </li>
              <li>
                Your real location is read on your device only, to show where
                you are. It is never sent anywhere and never saved.
              </li>
              <li>
                The app contacts a few public map and search services, which see
                your network address and what you ask them for. They are listed
                below.
              </li>
              <li>Your favorites, saved routes and settings stay on your device.</li>
            </ul>

            <h2>What RouteForge is</h2>
            <p>
              RouteForge is an Android tool for developers and testers. It makes
              your device report a simulated location, for example along a route
              you plan. To do that you choose RouteForge as the “mock location
              app” in Android’s Developer options. While a simulation runs,
              other apps on your device that read the device location receive
              the simulated position instead of the real one. RouteForge does
              not send the position it simulates to anyone. The only coordinates
              it sends are those of a spot you choose on the map, described
              below.
            </p>

            <h2>Information that stays on your device</h2>
            <h3>Your real location</h3>
            <p>
              If you allow location access, the app reads your position from
              Android’s location service on the device, to show where you are on
              the map and to rank search results by distance from you. It is
              kept in memory only while the app is open. It is not written to
              storage, and it is not sent to the developer or to any service. If
              you do not allow location access, the app still works, but it
              cannot show your real position.
            </p>
            <h3>Favorites, routes and settings</h3>
            <p>
              Places you save, saved routes, the map style, the storage limit
              for map data and which tours you have seen are stored in the app’s
              private storage on your device. Nothing is sent to a server. They
              are removed when you clear the app’s data or uninstall the app. If
              Android’s backup is turned on for your Google account, Android may
              include this data in your device backup; that backup is handled by
              Google under Google’s own privacy policy.
            </p>
            <h3>Files you open</h3>
            <p>A GPX or JSON route file that you open or import is read on the device only.</p>
            <h3>Offline map data</h3>
            <p>
              Downloaded map regions and recently shown map pictures are kept on
              the device so that the app can work offline. You can delete
              downloaded regions in the app.
            </p>

            <h2>Information sent to other services</h2>
            <p>
              RouteForge makes network requests only in the cases below. Every
              request uses an encrypted connection (HTTPS). Every request
              carries the app’s name, its version and the developer’s contact
              address, and, as with any internet request, the service sees your
              network address (IP address). None of these requests contains your
              real location. The address lookup is the only one that sends
              coordinates, and they are those of a spot you chose.
            </p>
            <ul>
              <li>
                <strong>Map pictures.</strong> When a map is shown, the app asks
                a map server for the pictures of the area on screen:{" "}
                <code>tile.openstreetmap.org</code> (OpenStreetMap, Standard
                style) or <code>tile.opentopomap.org</code> (OpenTopoMap,
                Topographic style). The server sees which map area is requested.
                If you look at the place where you are, this reveals an
                approximate area of interest; it is not your device’s reported
                position.
              </li>
              <li>
                <strong>Place search.</strong> The text you type in the search
                screen is sent to Nominatim, the search service of the
                OpenStreetMap Foundation (<code>nominatim.openstreetmap.org</code>
                ), to find matching places. RouteForge keeps no search history.
              </li>
              <li>
                <strong>Address of a spot.</strong> When you tap a spot on the
                map to add a waypoint, to teleport there or to save it as a
                favorite, or when a place with no name arrives from search or
                from another app, the app asks Nominatim for the address of that
                spot, so that it can show the address above the coordinates and
                offer it as the name of a favorite. The request carries the
                latitude and longitude of the spot you chose and the language of
                your device. It is never your device’s own position. The app
                makes at most one such request a second, keeps the latest answer
                in memory only, and does not make the request when you are
                offline.
              </li>
              <li>
                <strong>Offline map regions.</strong> When you download a region,
                the app downloads routing data files from <code>brouter.de</code>{" "}
                (BRouter). The names of the files identify the map areas you
                chose.
              </li>
              <li>
                <strong>Shared map links.</strong> If you share a short Google
                Maps link (<code>maps.app.goo.gl</code>) to RouteForge, the app
                makes one request to that address to learn where it redirects,
                reading only the response headers, so that it can read the
                coordinates. Nothing about your device or your position is sent.
              </li>
            </ul>
            <p>
              These services are run by other organizations and have their own
              privacy policies:{" "}
              <a href="https://osmfoundation.org/wiki/Privacy_Policy">
                OpenStreetMap Foundation
              </a>
              , <a href="https://opentopomap.org/about">OpenTopoMap</a>,{" "}
              <a href="https://brouter.de/">BRouter</a> and{" "}
              <a href="https://policies.google.com/privacy">Google</a>.
            </p>

            <h2>What RouteForge does not do</h2>
            <ul>
              <li>It has no user accounts, no ads and no advertising identifier.</li>
              <li>
                It has no analytics, tracking or crash-reporting component, and
                it sends nothing to the developer.
              </li>
              <li>It does not read your contacts, photos, messages, microphone or camera.</li>
              <li>It does not sell or share personal information.</li>
            </ul>
            <p>
              If you installed RouteForge from Google Play, Google may give the
              developer overall statistics such as the number of installs and
              crash reports, under Google’s own policies. RouteForge itself sends
              nothing for this.
            </p>

            <h2>Permissions</h2>
            <ul>
              <li>
                <strong>Location:</strong> to show your real position and rank
                search results by distance. Used while the app is in use; the
                app does not ask for background location access.
              </li>
              <li>
                <strong>Notifications:</strong> to show the controls (pause,
                resume, stop) of a running simulation and the progress of map
                downloads.
              </li>
              <li>
                <strong>Internet and network state:</strong> for the requests
                listed above and to tell you when you are offline.
              </li>
              <li>
                <strong>Foreground services (location, data sync):</strong> to
                keep a simulation running, and a map download going, while the
                screen is off or the app is in the background. Both are started
                by you and show a notification.
              </li>
              <li>
                <strong>Mock location app:</strong> you choose RouteForge in
                Android’s Developer options; the app cannot do this by itself.
              </li>
            </ul>

            <h2>Children</h2>
            <p>
              RouteForge is not directed to children, and it does not knowingly
              collect personal information from anyone.
            </p>

            <h2>Keeping and deleting data</h2>
            <p>
              The developer holds no data about you. Data on your device is
              removed by clearing the app’s data or uninstalling the app. The
              services listed above keep their own logs under their own
              policies.
            </p>

            <h2>Responsible use</h2>
            <p>
              RouteForge is meant for development, testing and demonstration. Do
              not use it to deceive other people or to break the rules of other
              services.
            </p>

            <h2>Changes to this policy</h2>
            <p>
              If the app starts handling data differently, this page is updated
              and the date at the top changes.
            </p>

            <h2>Contact</h2>
            <p>
              Questions about this policy:{" "}
              <a href={`mailto:${CONTACT_EMAIL}`}>{CONTACT_EMAIL}</a>.
            </p>
          </Prose>
        </MaxWidthWrapper>
      </main>
    </>
  );
}
