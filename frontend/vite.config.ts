import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig(({ mode }) => {
  // Cargar variables de entorno del sistema y de archivos .env
  const env = { ...loadEnv(mode, process.cwd(), ''), ...process.env }
  const targetApiUrl = env.VITE_API_URL || env.TRASTCMS_BASE_URL || 'http://localhost:8080'
  const siteHost = env.VITE_SITE_HOST || ''
  const port = Number(env.VITE_PORT) || 5173

  return {
    plugins: [vue(), tailwindcss()],
    resolve: {
      dedupe: ['vue', 'vue-router'],
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url)),
        '@plugins': fileURLToPath(new URL('../plugins', import.meta.url))
      }
    },
    server: {
      port,
      fs: { allow: [fileURLToPath(new URL('..', import.meta.url))] },
      proxy: {
        '/api': {
          target: targetApiUrl,
          changeOrigin: true,
          secure: false,
          headers: siteHost ? { 'X-Forwarded-Host': siteHost } : undefined
        },
        '/actuator': {
          target: targetApiUrl,
          changeOrigin: true,
          secure: false,
          headers: siteHost ? { 'X-Forwarded-Host': siteHost } : undefined
        }
      }
    },
    build: {
      outDir: 'dist',
      emptyOutDir: true,
      sourcemap: false,
      chunkSizeWarningLimit: 1400
    }
  }
})
