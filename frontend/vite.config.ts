import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'node:path'

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: { '@': path.resolve(__dirname, './src') },
  },
  server: {
    port: 5173,
  },
  build: {
    rollupOptions: {
      output: {
        // Separa libs que mudam pouco em chunks próprios → melhor cache entre deploys.
        manualChunks: {
          'react-vendor': ['react', 'react-dom', 'react-router-dom'],
          'data-vendor': ['@tanstack/react-query', 'axios'],
          // recharts já fica isolado no chunk da rota /dashboard (lazy),
          // mas forçamos um chunk próprio caso seja importado em outro lugar.
          charts: ['recharts'],
        },
      },
    },
  },
})
