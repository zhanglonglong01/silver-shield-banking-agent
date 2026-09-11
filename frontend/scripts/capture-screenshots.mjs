import { mkdir } from 'node:fs/promises'
import { resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import { chromium } from '@playwright/test'

const configuredBaseUrl = process.env.DEMO_BASE_URL ?? 'http://127.0.0.1:3000'
const baseUrl = new URL(configuredBaseUrl)

if (baseUrl.protocol !== 'http:' || baseUrl.hostname !== '127.0.0.1' || baseUrl.username || baseUrl.password) {
  throw new Error('DEMO_BASE_URL 必须是 http://127.0.0.1:<port>，截图脚本不会访问外部站点。')
}

baseUrl.pathname = '/'
baseUrl.search = ''
baseUrl.hash = ''

const outputDirectory = fileURLToPath(new URL('../../docs/images/', import.meta.url))
const paths = {
  overview: resolve(outputDirectory, 'overview.png'),
  agent: resolve(outputDirectory, 'agent-demo.png'),
  security: resolve(outputDirectory, 'security-demo.png')
}

await mkdir(outputDirectory, { recursive: true })

const browser = await chromium.launch({ headless: true })

try {
  const context = await browser.newContext({
    viewport: { width: 1440, height: 900 },
    deviceScaleFactor: 1,
    colorScheme: 'light',
    locale: 'zh-CN',
    timezoneId: 'Asia/Shanghai'
  })

  await context.route('**/*', async route => {
    const requestUrl = new URL(route.request().url())
    if (requestUrl.hostname !== '127.0.0.1') {
      await route.abort('blockedbyclient')
      return
    }
    await route.continue()
  })

  const page = await context.newPage()
  await page.emulateMedia({ reducedMotion: 'reduce' })

  async function open(path) {
    await page.goto(new URL(path, baseUrl).toString(), { waitUntil: 'networkidle' })
  }

  async function stabilize({ hideDynamicIds = false } = {}) {
    if (hideDynamicIds) {
      await page.locator('.confirm-meta').evaluateAll(elements => {
        for (const element of elements) element.remove()
      })
      await page.locator('.summary-grid').evaluateAll(grids => {
        for (const grid of grids) {
          for (const label of grid.querySelectorAll('dt')) {
            if (label.textContent?.endsWith('Id') && label.nextElementSibling) {
              label.nextElementSibling.textContent = '内部标识（已脱敏）'
            }
          }
        }
      })
    }
    await page.evaluate(async () => {
      await document.fonts.ready
      window.scrollTo(0, 0)
    })
  }

  await open('/login')
  await page.getByLabel('用户名').fill('demo.alice')
  await page.getByLabel('密码').fill('Demo@2026!')
  await page.getByRole('button', { name: '安全登录' }).click()
  await page.waitForURL(url => url.pathname === '/agent')
  await page.getByRole('heading', { name: '智能体工作台' }).waitFor()

  await page.getByRole('link', { name: '账户与账单' }).click()
  await page.waitForURL(url => url.pathname === '/accounts')
  await page.getByRole('heading', { name: '账户与账单' }).waitFor()
  await page.locator('.account-card').first().waitFor()
  await page.locator('tbody tr').first().waitFor()
  await stabilize()
  await page.screenshot({ path: paths.overview, fullPage: true, animations: 'disabled' })

  await page.getByRole('link', { name: '智能体工作台' }).click()
  await page.waitForURL(url => url.pathname === '/agent')
  await page.getByLabel('向银行智能体发送消息').fill('从日常账户向张伟转账 88 元')
  await page.getByRole('button', { name: '发送' }).click()
  const confirmationCard = page.getByTestId('confirmation-card')
  await confirmationCard.waitFor({ timeout: 20_000 })
  await confirmationCard.getByText('需要独立确认').waitFor()
  await stabilize({ hideDynamicIds: true })
  await page.screenshot({ path: paths.agent, fullPage: true, animations: 'disabled' })

  await page.getByRole('link', { name: '安全与沙箱' }).click()
  await page.waitForURL(url => url.pathname === '/security')
  await page.getByRole('heading', { name: '安全与沙箱' }).waitFor()
  await page.getByRole('heading', { name: '明确的信任边界' }).waitFor()
  await stabilize()
  await page.screenshot({ path: paths.security, fullPage: true, animations: 'disabled' })

  console.log(`Screenshots written to ${outputDirectory}`)
  for (const path of Object.values(paths)) console.log(`- ${path}`)

  await context.close()
} finally {
  await browser.close()
}
