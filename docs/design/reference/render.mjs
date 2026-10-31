// Renders the Flare reference screens with headless Chrome or Edge (phones at 390x844, 2x).
// Usage: node docs/design/reference/render.mjs [outDir] [screen,screen,...]
// Node 22 built-ins only. Set CHROME=/path/to/browser if it is not found automatically.
import { execFileSync } from 'node:child_process'
import { existsSync, mkdirSync } from 'node:fs'
import { dirname, join, resolve } from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'

const here = dirname(fileURLToPath(import.meta.url))
const outDir = resolve(process.argv[2] ?? join(here, 'shots'))
const only = process.argv[3]?.split(',')

const browser = [
  process.env.CHROME,
  'C:/Program Files/Google/Chrome/Application/chrome.exe',
  'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',
  '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',
  '/Applications/Microsoft Edge.app/Contents/MacOS/Microsoft Edge',
  '/usr/bin/google-chrome',
  '/usr/bin/chromium',
].find((path) => path && existsSync(path))
if (!browser) throw new Error('No Chrome or Edge found. Set CHROME=/path/to/browser.')

// [screen, width, height]; headless Chrome clamps windows to ~500px, so ?w= pins the layout width.
const phone = [390, 844]
const shots = [
  ['compose', ...phone], ['compose-open', ...phone], ['sending', ...phone], ['answer', ...phone],
  ['bounced', ...phone], ['confirm', ...phone], ['emergency', ...phone],
  ['staff-list', ...phone], ['staff-detail', ...phone],
  ['compose', 320, 640, 'small'], ['staff-list', 320, 640, 'small'],
  ['answer', 360, 780, '360'], ['bounced', 360, 780, '360'], ['emergency', 360, 780, '360'], ['compose-open', 360, 780, '360'],
  ['poster', 595, 842], ['palette', 390, 900],
].filter(([screen]) => !only || only.includes(screen))

mkdirSync(outDir, { recursive: true })
const page = pathToFileURL(join(here, 'index.html')).href
for (const [screen, width, height, suffix] of shots) {
  const file = join(outDir, `${screen}${suffix ? `-${suffix}` : ''}.png`)
  execFileSync(browser, [
    '--headless=new', '--disable-gpu', '--hide-scrollbars', '--force-device-scale-factor=2',
    `--window-size=${width},${height}`, '--virtual-time-budget=5000', `--screenshot=${file}`,
    `${page}?screen=${screen}&w=${width}`,
  ], { stdio: 'ignore' })
  console.log(file)
}
