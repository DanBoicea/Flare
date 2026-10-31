// Renders every exploration screen as a phone screenshot (390x844 CSS px at 2x) with headless Chrome or Edge.
// Usage: node docs/design/explorations/render.mjs [outDir] [themes, e.g. a,b,c]
// Node 22 built-ins only. Set CHROME=/path/to/browser if it is not found automatically.
import { execFileSync } from 'node:child_process'
import { existsSync, mkdirSync } from 'node:fs'
import { dirname, join, resolve } from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'

const here = dirname(fileURLToPath(import.meta.url))
const outDir = resolve(process.argv[2] ?? join(here, 'shots'))
const themes = (process.argv[3] ?? 'a,b,c').split(',')

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

const screens = ['palette', 'compose', 'sending', 'answer', 'bounced', 'confirm', 'emergency', 'staff-list', 'staff-detail']
const shots = themes.flatMap((theme) => [
  ...screens.map((screen) => ({ theme, screen })),
  { theme, screen: 'staff-list', cvd: 'deuteranopia' },
  { theme, screen: 'staff-list', cvd: 'achromatopsia' },
])

mkdirSync(outDir, { recursive: true })
const page = pathToFileURL(join(here, 'index.html')).href
for (const { theme, screen, cvd } of shots) {
  const file = join(outDir, `${theme}-${screen}${cvd ? `-${cvd}` : ''}.png`)
  const url = `${page}?theme=${theme}&screen=${screen}&w=390${cvd ? `&cvd=${cvd}` : ''}`
  execFileSync(browser, [
    '--headless=new', '--disable-gpu', '--hide-scrollbars', '--force-device-scale-factor=2',
    '--window-size=390,844', '--virtual-time-budget=5000', `--screenshot=${file}`, url,
  ], { stdio: 'ignore' })
  console.log(file)
}
