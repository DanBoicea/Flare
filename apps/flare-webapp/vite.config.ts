import react from "@vitejs/plugin-react";
import { defineConfig } from "vitest/config";

// https://vite.dev/config/
export default defineConfig({
	plugins: [react()],
	test: {
		// Unit and component tests (src/**/*.test.ts[x]) run in jsdom. Browser tests live in e2e/ (Playwright).
		environment: "jsdom",
		include: ["src/**/*.test.{ts,tsx}"],
		setupFiles: ["./src/test/setup.ts"],
	},
	server: {
		allowedHosts: [".ngrok-free.app", ".ngrok-free.dev", ".ngrok.app"],
		// Dev only: with VITE_API_BASE_URL empty, the app calls /api on its own origin and Vite forwards it to the
		// back end, so one tunnel (ngrok) serves both and no CORS is involved. The Origin header is dropped, or
		// Spring would see a cross-origin request from the tunnel's domain and reject it.
		proxy: {
			"/api": {
				target: "http://localhost:8080",
				configure: (proxy) => {
					proxy.on("proxyReq", (proxyReq) => proxyReq.removeHeader("origin"));
				},
			},
		},
	},
});
