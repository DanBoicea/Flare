import { expect, test, type Page } from '@playwright/test'
import en from '../src/i18n/en.json' with { type: 'json' }
import ro from '../src/i18n/ro.json' with { type: 'json' }

type Lang = 'ro' | 'en'
const strings = { ro, en }

// Mock mode picks the outcome by keyword (src/api/mock.ts). Texts avoid Romanian letters on purpose,
// so a Romanian letter on an English page can only come from the app.
const OUTCOMES = {
  answer: 'Where is the baggage office?',
  'answer-not-found': 'Is there wifi?',
  bounced: 'joke asdf',
  confirmation: 'The toilet near gate 3 is flooded.',
  emergency: 'fire near gate 2',
} as const

async function open(page: Page, lang: Lang) {
  await page.addInitScript((l) => localStorage.setItem('flare.language', l), lang)
  await page.goto('/q/arr-wc-02')
  await expect(page.locator('html')).toHaveAttribute('lang', lang)
}

async function send(page: Page, text: string) {
  await page.locator('#message').fill(text)
  await page.locator('button[type=submit]').click()
}

/** Visible text in the page's own language: drops elements marked as another language (the
 *  Passenger's quoted text, the other-language Emergency line, the language button) and hidden ones. */
function ownLanguageText(page: Page) {
  return page.evaluate(() => {
    const lang = document.documentElement.lang
    const root = document.body.cloneNode(true) as HTMLElement
    root.querySelectorAll('[lang], [aria-hidden="true"], [hidden], script, style').forEach((el) => {
      if (!el.hasAttribute('lang') || el.getAttribute('lang') !== lang) el.remove()
    })
    return root.textContent ?? ''
  })
}

/** Fails if any string of the other language appears on the page (strings shorter than 5 characters,
 *  like "112", are shared and skipped). On English pages, any Romanian letter also fails. */
async function expectSingleLanguage(page: Page, lang: Lang) {
  const text = await ownLanguageText(page)
  const other = strings[lang === 'ro' ? 'en' : 'ro'] as Record<string, string>
  const mine = strings[lang] as Record<string, string>
  for (const [key, value] of Object.entries(other)) {
    if (value.length < 5 || value === mine[key]) continue
    expect(text, `"${key}" from the other language is on the ${lang} page`).not.toContain(value)
  }
  if (lang === 'en') {
    const withoutAirport = text.replace(en['app.airport'], '')
    expect(withoutAirport, 'Romanian letters on the English page').not.toMatch(/[ăâîșțĂÂÎȘȚ]/)
  }
}

async function expectNoHorizontalScroll(page: Page) {
  const overflow = await page.evaluate(() => document.documentElement.scrollWidth - window.innerWidth)
  expect(overflow, 'page is wider than the screen').toBeLessThanOrEqual(0)
}

async function shot(page: Page, name: string) {
  await page.screenshot({ path: `test-results/screens/${test.info().project.name}-${name}.png`, fullPage: true })
}

for (const lang of ['ro', 'en'] as const) {
  test.describe(`${lang.toUpperCase()}`, () => {
    test('Compose is in one language, fits the screen, and shows 112', async ({ page }) => {
      await open(page, lang)
      await expect(page.getByRole('heading', { level: 1 })).toHaveText(strings[lang]['compose.title'])
      await expect(page.locator('.emergency-strip a[href="tel:112"]')).toBeVisible()
      await expect(page).toHaveTitle(`${strings[lang]['page.compose']} – Flare`)
      await expectSingleLanguage(page, lang)
      await expectNoHorizontalScroll(page)
      await shot(page, `${lang}-compose`)

      await page.locator('.callback__toggle input').check()
      await expect(page.locator('#contact')).toBeVisible()
      await shot(page, `${lang}-compose-callback`)
    })

    test('Sending is in one language', async ({ page }) => {
      await open(page, lang)
      await send(page, OUTCOMES.confirmation)
      await expect(page.getByRole('heading', { name: strings[lang]['sending.checking'] })).toBeVisible()
      await expectSingleLanguage(page, lang)
      await expectNoHorizontalScroll(page)
      await shot(page, `${lang}-sending`)
    })

    for (const [name, text] of Object.entries(OUTCOMES)) {
      test(`${name} is in one language, fits the screen, has focus on its heading, and shows 112`, async ({ page }) => {
        await open(page, lang)
        await send(page, text)
        await expect(page.getByRole('heading', { name: strings[lang]['sending.checking'] })).toBeHidden({ timeout: 10_000 })

        await expect(page.locator('main h1')).toBeFocused()
        await expect(page.locator('.emergency-strip a[href="tel:112"]')).toBeVisible()
        await expectSingleLanguage(page, lang)
        await expectNoHorizontalScroll(page)
        await shot(page, `${lang}-${name}`)
      })
    }
  })
}

test('the third Bounce offers "Send anyway", which ends on Confirmation', async ({ page }) => {
  await open(page, 'en')
  for (let i = 0; i < 3; i++) {
    await send(page, 'joke')
    await page.getByRole('button', { name: 'Edit message' }).waitFor()
    if (i < 2) {
      await expect(page.getByRole('button', { name: 'Send anyway' })).toHaveCount(0)
      await page.getByRole('button', { name: 'Edit message' }).click()
    }
  }
  await page.getByRole('button', { name: 'Send anyway' }).click()
  await expect(page.getByRole('heading', { name: en['confirm.title'] })).toBeVisible()
})

test('the language button shows a flag and the other language code, and keeps the text', async ({ page }) => {
  await open(page, 'en')
  await page.locator('#message').fill('My bag is missing')
  const button = page.locator('.language-button')
  await expect(button).toHaveText('RO')
  await expect(button.locator('svg.flag')).toBeVisible()
  await button.click()
  await expect(page.locator('html')).toHaveAttribute('lang', 'ro')
  await expect(button).toHaveText('EN')
  await expect(page.locator('#message')).toHaveValue('My bag is missing')
})

test('keyboard order on Compose: 112, language, message, call back, Send', async ({ page }, info) => {
  test.skip(info.project.name !== 'desktop', 'keyboard test runs once, on desktop')
  await open(page, 'en')
  await page.locator('#message').fill('Broken lift')
  await page.mouse.click(5, 5) // outside the app column: focus starts from the page
  const stops: string[] = []
  for (let i = 0; i < 5; i++) {
    await page.keyboard.press('Tab')
    stops.push(
      await page.evaluate(() => {
        const el = document.activeElement as HTMLElement
        if (el.matches('.emergency-strip__call')) return '112'
        if (el.matches('.language-button')) return 'language'
        if (el.id === 'message') return 'message'
        if (el.matches('.callback__toggle input')) return 'call back'
        if (el.matches('button[type=submit]')) return 'send'
        return el.outerHTML.slice(0, 80)
      }),
    )
  }
  expect(stops).toEqual(['112', 'language', 'message', 'call back', 'send'])
})

test('the favicon and touch icons are the Flare logo', async ({ page, request }) => {
  await open(page, 'ro')
  const icons = await page.locator('link[rel="icon"], link[rel="apple-touch-icon"]').evaluateAll((links) =>
    links.map((link) => link.getAttribute('href')),
  )
  expect(icons).toEqual(['/favicon.ico', '/icon-192.png', '/apple-touch-icon.png'])
  for (const href of icons) {
    const response = await request.get(href!)
    expect(response.ok(), href!).toBe(true)
    expect(response.headers()['content-type'], href!).toMatch(/^image\//)
  }
})

test('on wide screens the strip, header and content form one centred column', async ({ page }, info) => {
  test.skip(info.project.name !== 'desktop', 'desktop only')
  await open(page, 'en')
  const box = async (selector: string) => (await page.locator(selector).boundingBox())!
  const [app, strip, header, main] = await Promise.all(['.app', '.emergency-strip', '.app__header', '.app__main'].map(box))
  expect(app.width).toBeLessThanOrEqual(600)
  expect(Math.abs(app.x + app.width / 2 - 720)).toBeLessThanOrEqual(1)
  // Within 2 px: the parts sit inside the column's 1 px border.
  for (const part of [strip, header, main]) {
    expect(Math.abs(part.x - app.x)).toBeLessThanOrEqual(2)
    expect(Math.abs(part.width - app.width)).toBeLessThanOrEqual(2)
  }
})
