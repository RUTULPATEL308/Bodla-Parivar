import type { Config } from "tailwindcss";

const config: Config = {
  darkMode: 'class',
  content: [
    "./src/pages/**/*.{js,ts,jsx,tsx,mdx}",
    "./src/components/**/*.{js,ts,jsx,tsx,mdx}",
    "./src/app/**/*.{js,ts,jsx,tsx,mdx}",
  ],
  theme: {
    extend: {
      colors: {
        // High-end Royal Navy & Heritage Warm Palette
        navy: {
          DEFAULT: "#0F2744",
          50: "#F0F5FA",
          100: "#E1EBF5",
          200: "#C3D7EB",
          300: "#95BBE0",
          400: "#5D96CF",
          500: "#3274B8",
          600: "#1E589E",
          700: "#1A457D",
          800: "#163964",
          900: "#0F2744",
          950: "#09172A",
        },
        gold: {
          DEFAULT: "#D97706",
          50: "#FFFBEB",
          100: "#FEF3C7",
          200: "#FDE68A",
          300: "#FCD34D",
          400: "#FBBF24",
          500: "#F59E0B",
          600: "#D97706",
          700: "#B45309",
          800: "#92400E",
          900: "#78350F",
        },
        // Backward-compatibility aliasing for existing saffron/cream/brand tokens
        saffron: {
          DEFAULT: "#0F2744",       // Now mapped to Royal Navy
          dark: "#09172A",
          light: "#E1EBF5",
          container: "#F0F5FA",
        },
        leaf: {
          DEFAULT: "#059669",
          dark: "#047857",
          light: "#A7F3D0",
        },
        golden: {
          DEFAULT: "#D97706",
        },
        cream: {
          DEFAULT: "#F8FAFC",       // Sleek modern crisp slate surface
          surface: "#FFFFFF",
          hover: "#F1F5F9",
        },
        brand: {
          text: "#0F172A",          // Crisp deep slate
          muted: "#64748B",         // Modern slate muted
          border: "#E2E8F0",        // Refined subtle border
          accent: "#0F2744",
        },
      },
      fontFamily: {
        gujarati: ["var(--font-anek-gujarati)", "sans-serif"],
        english: ["var(--font-inter)", "var(--font-plus-jakarta)", "-apple-system", "BlinkMacSystemFont", "Segoe UI", "Roboto", "sans-serif"],
        sans: ["var(--font-inter)", "var(--font-anek-gujarati)", "-apple-system", "sans-serif"],
      },
      boxShadow: {
        'card': '0 1px 3px 0 rgba(15, 23, 42, 0.05), 0 1px 2px -1px rgba(15, 23, 42, 0.05)',
        'card-hover': '0 10px 25px -5px rgba(15, 39, 68, 0.08), 0 8px 10px -6px rgba(15, 39, 68, 0.04)',
      },
    },
  },
  plugins: [],
};
export default config;
