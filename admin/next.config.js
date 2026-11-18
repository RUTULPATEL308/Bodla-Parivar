/** @type {import('next').NextConfig} */
const nextConfig = {
  experimental: {
    agentUpgrade: "security",
  },
  reactStrictMode: true,
  images: {
    remotePatterns: [
      {
        protocol: "https",
        hostname: "**",
      },
    ],
  },
};

module.exports = nextConfig;
