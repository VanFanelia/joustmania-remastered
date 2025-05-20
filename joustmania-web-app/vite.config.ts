import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import svgr from 'vite-plugin-svgr'
import legacy from '@vitejs/plugin-legacy'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss(), svgr(), legacy({
    targets: ['defaults', 'Chrome >= 46'], // oder 'Android >= 6'
    additionalLegacyPolyfills: ['regenerator-runtime/runtime'], // for async/await
  }),],
  build: {
    target: 'es5',
    polyfillModulePreload: true,
  }
})
