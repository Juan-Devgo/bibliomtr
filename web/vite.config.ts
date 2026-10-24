/// <reference types="vitest/config" />
import path from 'node:path'
import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    // En desarrollo, /api se redirige al backend Spring Boot (./gradlew bootRun).
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
  // Vitest + Testing Library para los criterios de aceptación de la interfaz (spec 002).
  test: {
    environment: 'jsdom',
    setupFiles: './src/test/setup.ts',
  },
})
