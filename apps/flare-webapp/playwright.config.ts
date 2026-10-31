import { defineConfig, devices } from '@playwright/test'

// Browser tests of the passenger web app in mock mode (no back end needed). `npm run test:e2e`.
// Screenshots of every screen land in test-results/screens/ (ignored by git) for a visual check.
const PORT = 5181

export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  reporter: [['list']],
  use: {
    baseURL: `http://localhost:${PORT}`,
    trace: 'retain-on-failure',
  },
  projects: [
    {
      name: 'phone',
      use: { ...devices['Desktop Chrome'], viewport: { width: 390, height: 844 }, deviceScaleFactor: 2, isMobile: true, hasTouch: true },
    },
    // 320 px wide is what a 640 px window shows at 200% zoom (WCAG 1.4.10 reflow).
    {
      name: 'narrow',
      use: { ...devices['Desktop Chrome'], viewport: { width: 320, height: 700 }, isMobile: true, hasTouch: true },
    },
    { name: 'desktop', use: { ...devices['Desktop Chrome'], viewport: { width: 1440, height: 900 } } },
  ],
  webServer: {
    command: `npx vite --port ${PORT} --strictPort`,
    port: PORT,
    reuseExistingServer: false,
    env: { VITE_MOCK: 'true' },
  },
})
