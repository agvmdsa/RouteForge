# Content rating and target audience: answers

Checked against the app at commit `d605278` (2026-10-26). In the Play Console: **Policy and programs > App content**. The rating
questionnaire is the International Age Rating Coalition's and is worded by the Console; use the answers below as what the app does
and re-read each question before you submit.

## App details

| Item | Answer |
|------|--------|
| App or game | App |
| Free or paid | Free |
| Category | Tools |
| Ads | No ads, and no ad network in the app |
| In-app purchases or subscriptions | None |
| App access | All functionality is available without logging in or any special access; there is no account |

## Content rating questionnaire

| Topic | Answer |
|-------|--------|
| Category chosen in the questionnaire | Utility, productivity, communication or other |
| Violence, blood, gore | No |
| Sexual content or nudity | No |
| Profanity or crude humor | No |
| Controlled substances (drugs, alcohol, tobacco) | No |
| Gambling or simulated gambling | No |
| User-generated content or users interacting with each other | No: there are no accounts, no chat and nothing shared between users |
| Shares the user's location with other users | No (the simulated position is set on the user's own device and is not sent anywhere) |
| Digital purchases | No |
| Unrestricted web access | No (no web view; the only link, the copyright page behind the map credit, opens in the user's own browser) |

Expected result: the lowest age rating. Save the certificate the Console shows after the questionnaire.

## Target audience and content

| Question | Answer |
|----------|--------|
| Target age groups | 18 and over |
| Does the app appeal to children? | No |
| Designed for families? | No |

Reason for 18 and over: it is a tool that changes the location other apps see on the device; it is meant for developers and
testers, not for children.

## Other declarations that appear in App content

| Declaration | Answer |
|-------------|--------|
| Privacy policy | the public address of the policy page (see the checklist) |
| Data safety | see `data-safety.md` |
| Foreground service permissions | see `foreground-services.md` |
| Government app | No |
| Financial features | None |
| Health features | None |
| News app | No |
| COVID-19 contact tracing or status | No |
| Advertising ID | The app does not use it (the declaration asks, because apps that target Android 13 and newer must declare the permission if they use it; this one does not declare it) |

## Wording of the app on the store

The store listing must say what the app does plainly: it makes the device report a simulated location, for developers and
testers. It must not suggest cheating in games or getting around the rules of other services, and it must not claim to be
made by or connected to Google, OpenStreetMap or any map provider. See `store-listing.md`.
