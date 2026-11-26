# RouteForge site

The public site of RouteForge: a landing page and the privacy policy of the app. A Next.js project (pages router, Tailwind CSS, framer-motion)
built as a static export, so the published site is plain HTML with no server and no request to a third party.

```sh
cd site
npm ci            # install what package-lock.json pins
npm run dev       # http://localhost:3000
npm run build     # writes the site to site/out
```

Vercel builds and serves it (project `routeforge-website`, root directory `site`, domain `routeforge.andgabx.dev`); that setting lives in the
Vercel dashboard, not in this repository. The `Site` workflow only checks every change to this folder (install, lint, build). The policy page
(`src/pages/privacy.tsx`) must stay true to what the app does: change it together with `docs/play-store/data-safety.md` whenever the app
starts sending something new off the device.
