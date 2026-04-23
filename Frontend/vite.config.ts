import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    react(),
    tailwindcss(),
  ],
  build: {
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (id.includes('node_modules')) {
            // Extrage React și librăriile asociate (react-dom, react-router) într-un chunk dedicat
            if (id.includes('react')) {
              return 'vendor-react';
            }
            // Pune restul librăriilor externe în alt chunk
            return 'vendor-general';
          }
        }
      }
    }
  }
})