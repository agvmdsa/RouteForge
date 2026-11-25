# Google Play: checklist from account to production

Checked against the app at commit `d605278` (2026-10-26). The release is planned for January 2027, after more changes to the
app. The rules of the Play Console are written here as the maintainer knew them in October 2026: confirm each one in the Console
when you reach it. Order matters: the first two parts are mostly waiting, so start them early.

## 1. Developer account (start early: identity checks take time)

- [ ] Register a Play Console developer account (personal, in your own name). One-time fee, currently US$ 25.
- [ ] Complete the identity verification the Console asks for, and verify the contact e-mail and phone.
- [ ] Find the testing rule shown for your account type. For new personal accounts it has been: a closed test with at least 12
      testers who stay opted in for at least 14 days in a row, before you can apply for production access. Write the exact
      figures here once the Console shows them: ______.
- [ ] Look at Google's developer verification requirements for apps installed outside Google Play in your country, because the
      downloadable APK of the project is signed by the same developer.

## 2. Places the Console needs (outside the app)

- [ ] The privacy policy at a public address: page `privacy/index.html` of the `routeforge-site` repository. It becomes public
      with GitHub Pages (a free plan needs the repository to be public); expected address:
      `https://agvmdsa.github.io/routeforge-site/privacy/`. **Do not rename or delete that repository afterwards.**
- [ ] A public contact: `agvmdsa@gmail.com` (it is shown on the store page).
- [ ] A new release key (the maintainer creates it; the key of the 1.0.0 test file is abandoned). With Play App Signing it becomes the upload key, and Google keeps the key that signs what users install.

## 3. Changes in the app before the release

| Item | Why | Status |
|------|-----|--------|
| User agent with a contact | The map, search and download servers expect a client they can identify and reach. Today the requests send `RouteForge/1.0 (+https://github.com/agvmdsa/RouteForge)` (a private address) or only the package name | not done |
| Map credit line on the three map screens, tappable | OpenStreetMap and OpenTopoMap require visible credit; today only the region catalog shows "© OpenStreetMap contributors" | not done |
| Stop cleanly at the `dataSync` time limit | Android 15 and newer give that service about six hours; the service has no handler for the stop request | not done |
| Backup rules: leave downloaded map data and the map cache out | The app allows backup with no rules, and the map regions live in the app's private files, so a backup may carry them or pass the system's size limit (about 25 MB) and then back up nothing, favorites included. The policy says favorites, routes and settings may be backed up | decide and do |
| Signed bundle (AAB) in the release workflow | Google Play takes bundles, not APKs. Today `release.yml` builds only an APK | last, as planned |
| Optional: code shrinking (R8), themed (monochrome) launcher icon | Smaller download; themed icons on Android 13 and newer | optional |

Already checked and fine: target API 37; the only native library is 16 KB page aligned and zip-aligned; 64-bit builds are
included; no background location permission; no analytics or advertising library; no cleartext traffic.

## 4. Console set-up (after the app exists in the Console)

- [ ] Create the app: name, default language English (United States), app, free, accept the declarations.
- [ ] Privacy policy address (section 2).
- [ ] App access: all functionality is available without login (`content-rating-and-audience.md`).
- [ ] Ads: none. Content rating questionnaire. Target audience: 18 and over. Other declarations of App content (same file).
- [ ] Data safety form (`data-safety.md`).
- [ ] Foreground service permissions: location and data sync, with the two videos (`foreground-services.md`).
- [ ] Store listing: texts, icon, feature graphic, screenshots (`store-listing.md`); category Tools; contact details.
- [ ] Countries and regions to publish in.

## 5. From first upload to production

1. **Internal testing.** Upload the first bundle, add yourself as a tester, install from the Play link, check the install, the
   mock location set-up and an update to a second bundle (the version number must grow with every upload).
2. **Closed testing.** Create a closed track, add the testers (as many as the rule needs, with their Google accounts), send them
   the opt-in link, and keep them opted in for the required days. Look at the pre-launch report of each upload.
3. **Apply for production access** in the Console when it allows it, and answer its questions about the testing.
4. **Production.** Create the release with release notes, start with a staged rollout if the Console offers it, and watch the
   crash and ANR reports in Play's own vitals.

## 6. Before each promotion

- [ ] The version number is higher than every earlier upload.
- [ ] `data-safety.md`, the privacy policy and the store texts still match the app (a new network call or stored data changes all
      three).
- [ ] The pre-launch report has no crash on a phone of an older and of a newer Android version.
- [ ] The release notes say what changed.
