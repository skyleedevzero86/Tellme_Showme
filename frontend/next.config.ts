const nextConfig = {
  reactStrictMode: true,
  turbopack: {
    root: process.cwd(),
  },
  allowedDevOrigins: ['*.ngrok-free.app', '*.ngrok.io', '*.ngrok.app'],
  async rewrites() {
    return [
      {
        source: '/api/proxy/:path*',
        destination: 'http://localhost:8080/:path*',
      },
      {
        source: '/callback.do',
        destination: 'http://localhost:8080/callback.do',
      },
    ];
  },
};

module.exports = nextConfig;
