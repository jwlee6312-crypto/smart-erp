import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'
import { VitePWA } from 'vite-plugin-pwa'

export default defineConfig({
	plugins: [
		vue(),
		vueDevTools(),
		VitePWA({
			registerType: 'autoUpdate',
			manifest: {
				name: 'SmartCore',
				short_name: 'ERP',
				start_url: '/',
				display: 'standalone',
				theme_color: '#ffffff',
				background_color: '#ffffff',
				icons: [],
			},
			workbox: {
				cleanupOutdatedCaches: true,
				clientsClaim: true,
				skipWaiting: true,
			},
		}),
	],
	resolve: {
		alias: {
			'@': fileURLToPath(new URL('./src', import.meta.url)),
		},
	},
	server: {
		host: '0.0.0.0',
		port: 5173,
		fs: {
			strict: false,
		},
		proxy: {
			// 💡 이미지 리소스 요청을 백엔드로 전달 (Context Path 보정)
			'/Upload_Images': {
				target: 'http://127.0.0.1:8080',
				changeOrigin: true,
				secure: false,
				rewrite: (path) => path.replace(/^\/Upload_Images/, '/api/Upload_Images'),
				configure: (proxy, _options) => {
					proxy.on('error', (err, _req, _res) => {
						// 🚀 [보정] ECONNREFUSED 오류 시 콘솔 스팸 방지
					});
				},
			},
			// 💡 나머지 /api 요청은 그대로 백엔드로 전달
			'/api': {
				target: 'http://127.0.0.1:8080',
				changeOrigin: true,
				secure: false,
				configure: (proxy, _options) => {
					proxy.on('error', (err, _req, _res) => {
						// 🚀 [보정] ECONNREFUSED 오류 시 콘솔 스팸 방지
					});
				},
			},
		},
	},
})
