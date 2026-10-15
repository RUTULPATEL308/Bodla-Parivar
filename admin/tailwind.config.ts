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
          DEFAULT: "#173B78",
          dark: "#102B59",
          light: "#DCE6F6",
          container: "#EAF0FA"
        },
        leaf: {
          DEFAULT: "#4F7D4A",
          dark: "#3B5E37",
          light: "#A3C49E"
        },
        golden: {
          DEFAULT: "#D9A441"
        },
        cream: {
          DEFAULT: "#FFF8E7",
          surface: "#FFFCF5"
        },
        brand: {
          text: "#292524",
          muted: "#625B53",
          border: "#E7E5E4"
        }
      },
      fontFamily: {
        gujarati: ["var(--font-anek-gujarati)", "sans-serif"],
        english: ["var(--font-dm-sans)", "sans-serif"]
      },
      fontSize: {
        xs: ["0.875rem", { lineHeight: "1.25rem" }],
        sm: ["1rem", { lineHeight: "1.5rem" }],
      }
    },
  },
  plugins: [],
};
export default config;
