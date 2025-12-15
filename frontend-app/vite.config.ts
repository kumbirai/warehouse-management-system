import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { VitePWA } from 'vite-plugin-pwa';
import path from 'path';

export default defineConfig({
  plugins: [
    react(),
    VitePWA({
      registerType: 'autoUpdate',
      includeAssets: ['favicon.ico', 'apple-touch-icon.png', 'mask-icon.svg'],
      manifest: {
        name: 'Warehouse Management System',
        short_name: 'WMS',
        description: 'Warehouse Management System Integration - CCBSA LDP System',
        theme_color: '#1976d2',
        icons: [],
      },
      workbox: {
        globPatterns: ['**/*.{js,css,html,ico,png,svg}'],
        runtimeCaching: [
          {
            urlPattern: /^https:\/\/api\./,
            handler: 'NetworkFirst',
            options: {
              cacheName: 'api-cache',
              networkTimeoutSeconds: 10,
              cacheableResponse: {
                statuses: [0, 200],
              },
            },
          },
        ],
      },
    }),
  ],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    host: true,
    port: 3000,
    https: {
      key: process.env.VITE_HTTPS_KEY_PATH || path.resolve(__dirname, '.certs/localhost-key.pem'),
      cert: process.env.VITE_HTTPS_CERT_PATH || path.resolve(__dirname, '.certs/localhost.pem'),
    },
    proxy: {
      '/api': {
        target: process.env.VITE_API_TARGET || 'https://localhost:8080',
        changeOrigin: true,
        secure: false, // Allow self-signed certificates
        timeout: 35000, // 35 seconds to match frontend timeout (30s) + buffer
        ws: false, // Disable WebSocket proxying
        configure: (proxy, _options) => {
          proxy.on('error', (err, req, res) => {
            console.error('Proxy error:', {
              message: err.message,
              code: err.code,
              url: req.url,
              method: req.method,
            });
            if (!res.headersSent) {
              res.writeHead(500, {
                'Content-Type': 'application/json',
              });
              res.end(
                JSON.stringify({
                  error: {
                    code: 'PROXY_ERROR',
                    message: 'Proxy connection error. Please check your connection and try again.',
                  },
                })
              );
            }
          });
          proxy.on('proxyReq', (proxyReq, req, _res) => {
            console.log('Proxying request:', {
              method: req.method,
              url: req.url,
              target: proxyReq.path,
            });
          });
          proxy.on('proxyRes', (proxyRes, req, _res) => {
            console.log('Proxy response:', {
              method: req.method,
              url: req.url,
              statusCode: proxyRes.statusCode,
            });
          });
        },
      },
    },
  },
  build: {
    outDir: 'dist',
    sourcemap: true,
    rollupOptions: {
      output: {
        manualChunks: id => {
          // Split vendor libraries into separate chunks for better caching
          if (id.includes('node_modules')) {
            // React and React DOM
            if (id.includes('react') || id.includes('react-dom')) {
              return 'react-vendor';
            }
            // Material-UI
            if (id.includes('@mui')) {
              return 'mui-vendor';
            }
            // Redux
            if (id.includes('@reduxjs') || id.includes('redux')) {
              return 'redux-vendor';
            }
            // Router
            if (id.includes('react-router')) {
              return 'router-vendor';
            }
            // Axios
            if (id.includes('axios')) {
              return 'axios-vendor';
            }
            // Other large vendor libraries
            if (id.includes('@zxing') || id.includes('dexie') || id.includes('i18next')) {
              return 'utils-vendor';
            }
            // All other node_modules
            return 'vendor';
          }
        },
      },
    },
    chunkSizeWarningLimit: 600, // Increased limit to 600KB to account for vendor chunks
  },
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: './src/tests/setup.ts',
  },
});
