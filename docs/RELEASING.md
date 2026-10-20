# Releasing RouteForge

A release is made by pushing a tag `vX.Y.Z` on a commit of `main`. GitHub then re-runs the checks, builds the app, signs it with the
release key and publishes a Release with generated notes, `RouteForge-vX.Y.Z.apk` and `RouteForge-vX.Y.Z.apk.sha256`. Nothing else
is needed after the push. The version comes from the tag: `vX.Y.Z` gives version name `X.Y.Z` and version number
`X × 10000 + Y × 100 + Z`.

## One-time setup

Do this once, before the first release. Nothing in the repository holds the key.

### 1. Create the release key

On your own machine, **outside** the repository:

```sh
keytool -genkeypair -v -keystore routeforge-release.jks -alias routeforge \
  -keyalg RSA -keysize 4096 -validity 10000
```

`keytool` asks for a store password and a key password (they may be the same) and for your name. Use a password manager for the
passwords. Remember the alias (`routeforge` above).

### 2. Back it up, and know what losing it means

Copy `routeforge-release.jks` and both passwords to at least two places that are **not** the repository and **not** GitHub (for
example your password manager's attachment and an offline drive).

Android only accepts an update signed by the same key as the installed app. **If the key is lost, nobody can update an installed
RouteForge**; they would have to uninstall it (losing its data) and install a build signed with a new key. If the key **leaks**,
delete the secrets (step 4) at once, create a new key, publish a new release, and tell users to reinstall: devices that already
trust the old key keep trusting it, so a leaked key cannot be revoked, only replaced.

### 3. Create the `release` environment

The key must be readable only by release runs, not by a workflow run from some branch. In the repository: **Settings → Environments →
New environment**, name it `release`; under **Deployment branches and tags** choose **Selected branches and tags**, add a rule of
type **Tag** with the pattern `v*`. (Optionally add yourself under **Required reviewers**, so each release waits for your approval.)

### 4. Add the four secrets to that environment

They must be secrets **of the `release` environment**, not repository secrets (a repository secret is readable by any branch's
workflow). With the GitHub CLI:

```sh
base64 -i routeforge-release.jks | tr -d '\n' | gh secret set RELEASE_KEYSTORE_BASE64 --env release   # Linux: base64 -w0
gh secret set RELEASE_KEYSTORE_PASSWORD --env release   # prompts for the value; it is not kept in your shell history
gh secret set RELEASE_KEY_ALIAS --env release
gh secret set RELEASE_KEY_PASSWORD --env release
```

The names must be exactly these four. Check that no repository-level copies exist: `gh secret list` (without `--env`) must not show
them; delete any that do (`gh secret delete NAME`).

## Cutting a release

1. Make sure `main` is green (the badge in the README, or the Actions tab) and contains everything you want to ship.
2. Pick the version. It is `vX.Y.Z` with whole numbers, no suffix, no leading zeros, `Y` and `Z` between 0 and 99, and its number
   must be larger than that of **every other** `vX.Y.Z` tag, released or not.
3. Tag and push:

   ```sh
   git checkout main && git pull
   git tag v1.0.0
   git push origin v1.0.0
   ```

4. Watch the **Release** run in the Actions tab. Within about 30 minutes the Release appears with notes, the APK and its checksum.
5. Check the download: `shasum -a 256 -c RouteForge-v1.0.0.apk.sha256` (in the folder with both files).

## When a release run fails

Nothing is published when a run fails. The run's first failing job says why; the usual causes:

| Message | What to do |
|---------|------------|
| `… is not a release tag: it must be exactly vX.Y.Z` | Use a tag like `v1.2.3`. |
| `minor and patch must be between 0 and 99` | Choose another version. |
| `not larger than … releases must increase` | Choose a larger version. If the tag it names was never released, delete that tag first (below). |
| `… is not on the main branch` | Merge the commit into `main`, then tag the merged commit. |
| `A Release for … already exists` | A version is released once. Choose the next version. |
| the `ci` job failed | Fix the problem on `main`, delete the tag (below) and tag the fixed commit. |
| `These signing secrets are missing or empty` | Add the named secrets to the `release` environment (step 4). |
| `not validly signed` or `does not carry version …` | Open the `build-signed` log; the key or the build is wrong. |
| the run was interrupted while publishing | A draft may be left. Push the tag again (delete and re-create it); the draft is replaced. |

To delete a tag so it can be pushed again:

```sh
git tag -d v1.0.0
git push origin :refs/tags/v1.0.0
```

## Names this project checks (warnings only)

On every pull request the checks warn, without ever blocking the merge, when:

- the branch is not named `NNN-short-name` (three digits, a hyphen, lowercase words joined by hyphens, e.g. `018-ci-cd-pipeline`);
- a commit's subject is not `RF-NNN: summary`, with `NNN` the next number after the highest `RF-NNN` on `main` (see
  `git log origin/main --format=%s | grep -oE '^RF-[0-9]+' | sort -t- -k2 -n | tail -1`).

## Never

Never commit the key or a password, paste them into an issue, a log or a chat, or add them as repository-level secrets.
