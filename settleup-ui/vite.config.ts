
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// During local dev, we assume services are on localhost ports per your app configs.
// We mount them under path prefixes to avoid CORS in dev.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api/membership': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api\/membership/, '')
      },
      '/api/expense': {
        target: 'http://localhost:8082',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api\/expense/, '')
      },
      '/api/settlement': {
        target: 'http://localhost:8083',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api\/settlement/, '')
      }
    }
  },
  build: {
    outDir: 'dist'
  }
})
