import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      // Forward all /api calls to Spring Boot during dev
      '/api': {
        target: 'http://host.docker.internal:8080',
        changeOrigin: true,
      },
      '/auth': {                          // ← add this
            target: 'http://host.docker.internal:8080',
            changeOrigin: true,
    }
  }
 }
})
