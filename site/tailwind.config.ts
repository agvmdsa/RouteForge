import type { Config } from "tailwindcss";
const {
  default: flattenColorPalette,
} = require("tailwindcss/lib/util/flattenColorPalette");
const svgToDataUri = require("mini-svg-data-uri");

const config: Config = {
  content: [
    "./src/pages/**/*.{js,ts,jsx,tsx,mdx}",
    "./src/components/**/*.{js,ts,jsx,tsx,mdx}",
    "./src/app/**/*.{js,ts,jsx,tsx,mdx}",
  ],
  theme: {
    extend: {
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
  plugins: [
    // @ts-ignore
    function ({ matchUtilities, theme }) {
      matchUtilities(
        {
          // @ts-ignore
          "bg-grid": (value) => ({
            backgroundImage: `url("${svgToDataUri(
              `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 32 32" width="32" height="32" fill="none" stroke-width="2" stroke="${value}"><path d="M0 .5H31.5V32"/></svg>`
            )}")`,
          }),
        },
        { values: flattenColorPalette(theme("backgroundColor")), type: "color" }
      );
    },
  ],
};

export default config;
