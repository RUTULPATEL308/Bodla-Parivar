import type { Config } from "tailwindcss";

const config: Config = {
  content: [
    "./src/pages/**/*.{js,ts,jsx,tsx,mdx}",
    "./src/components/**/*.{js,ts,jsx,tsx,mdx}",
    "./src/app/**/*.{js,ts,jsx,tsx,mdx}",
  ],
  theme: {
    extend: {
      colors: {
        saffron: {
          DEFAULT: "#D97706",
          dark: "#B45309",
          light: "#FDE68A",
          container: "#FEF3C7"
        },
        leaf: {
          DEFAULT: "#4F7D4A",
          dark: "#3B5E37",
          light: "#A3C49E"
        },
        golden: {
          DEFAULT: "#E9A23B"
        },
        cream: {
          DEFAULT: "#FFF8E7",
          surface: "#FFFCF5"
        },
        brand: {
          text: "#292524",
          muted: "#78716C",
          border: "#E7E5E4"
        }
      },
      fontFamily: {
        gujarati: ["Anek Gujarati", "sans-serif"],
        english: ["DM Sans", "sans-serif"]
      }
    },
  },
  plugins: [],
};
export default config;
