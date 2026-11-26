import type { Config } from "tailwindcss";

const config: Config = {
  content: [
    "./src/pages/**/*.{js,ts,jsx,tsx,mdx}",
    "./src/components/**/*.{js,ts,jsx,tsx,mdx}",
    "./src/app/**/*.{js,ts,jsx,tsx,mdx}",
  ],
  theme: {
    extend: {
      fontFamily: {
        // Self-hosted through @fontsource-variable (imported in _app.tsx): no request to a font service.
        display: ['"Fraunces Variable"', "Georgia", "Cambria", "serif"],
        mono: ['"JetBrains Mono Variable"', "ui-monospace", "SFMono-Regular", "Menlo", "monospace"],
        sans: ['"Instrument Sans Variable"', "ui-sans-serif", "system-ui", "sans-serif"],
      },
      colors: {
        // The palette of the app's own logo and theme (RouteForgeTheme.kt, MapColors.kt).
        cream: { DEFAULT: "#F6ECDD", deep: "#EEDFC8", surface: "#FBF5EC" },
        tan: { DEFAULT: "#DCC7A9", deep: "#C7AE8A" },
        espresso: "#4E2F1B",
        coffee: "#6B4226",
        ink: "#2B1D12",
        route: "#C1521C",
      },
    },
  },
  plugins: [],
};

export default config;
