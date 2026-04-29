import { test, expect } from '@playwright/test'

async function isInViewport(page: import('@playwright/test').Page, locator: import('@playwright/test').Locator) {
  return page.evaluate((el) => {
    if (!el) return false
    const rect = el.getBoundingClientRect()
    return (
      rect.top >= 0 &&
      rect.left >= 0 &&
      rect.bottom <= window.innerHeight &&
      rect.right <= window.innerWidth
    )
  }, await locator.elementHandle())
}

test.describe('Landing Page', () => {
  test('hero headline is visible above the fold on mobile (375x667)', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 667 })
    await page.goto('/')
    const headline = page.getByRole('heading', { level: 1 })
    await expect(headline).toBeVisible()
    const inViewport = await isInViewport(page, headline)
    expect(inViewport).toBe(true)
  })

  test('hero headline is visible above the fold on desktop xl (1280x800)', async ({ page }) => {
    await page.setViewportSize({ width: 1280, height: 800 })
    await page.goto('/')
    const headline = page.getByRole('heading', { level: 1 })
    await expect(headline).toBeVisible()
    const inViewport = await isInViewport(page, headline)
    expect(inViewport).toBe(true)
  })

  test('landing page contains primary product description', async ({ page }) => {
    await page.goto('/')
    await expect(page.getByRole('heading', { level: 1 })).toBeVisible()
    await expect(page.getByRole('heading', { name: /commission model/i })).toBeVisible()
  })
})
