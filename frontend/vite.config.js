import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { VitePWA } from 'vite-plugin-pwa'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    react(),
    VitePWA({
      registerType: 'autoUpdate',
      includeAssets: ['icon.svg'],
      manifest: {
        name: 'SearchLink',
        short_name: 'SearchLink',
        description: 'Alertas geolocalizadas de menores desaparecidos',
        lang: 'es',
        theme_color: '#0f172a',
        background_color: '#0f172a',
        display: 'standalone',
        start_url: '/',
        icons: [
          { src: 'icon.svg', sizes: 'any', type: 'image/svg+xml', purpose: 'any maskable' },
        ],
      },
      // Habilita el service worker también en `npm run dev` para poder probar la instalación.
      devOptions: { enabled: true },
    }),
  ],
  server: { port: 5173, host: true, allowedHosts: ['.ngrok-free.app'] },
})
