import { test, expect } from '@playwright/test'
import { COOKIE_POLICY_VERSION } from '../../app/config/cookiePolicy'

function makeStaleConsentCookie(version = '0.9.0') {
  return JSON.stringify({ accepted: true, timestamp: new Date().toISOString(), version })
}

test.describe('Cookie Banner', () => {
  test.beforeEach(async ({ context }) => {
    await context.clearCookies()
  })

  test('banner is visible on first visit', async ({ page }) => {
    await page.goto('/')
    await expect(page.getByRole('banner', { name: 'Cookie consent' })).toBeVisible()
  })

  test('clicking "Cookie Policy" navigates to /cookie-policy without dismissing banner', async ({ page }) => {
    await page.goto('/')
    await page.getByRole('link', { name: 'Cookie Policy' }).click()
    await expect(page).toHaveURL('/cookie-policy')
    await expect(page.getByRole('banner', { name: 'Cookie consent' })).toBeVisible()
  })

  test('returning to / after visiting cookie policy still shows banner', async ({ page }) => {
    await page.goto('/cookie-policy')
    await page.getByRole('link', { name: /back to home/i }).click()
    await expect(page).toHaveURL('/')
    await expect(page.getByRole('banner', { name: 'Cookie consent' })).toBeVisible()
  })

  test('clicking "Agree" hides banner and it does not reappear on next visit', async ({ page }) => {
    await page.goto('/')
    await page.getByRole('button', { name: 'Agree' }).click()
    await expect(page.getByRole('banner', { name: 'Cookie consent' })).not.toBeVisible()

    await page.reload()
    await expect(page.getByRole('banner', { name: 'Cookie consent' })).not.toBeVisible()
  })

  test('outdated stored consent causes banner to reappear', async ({ context, page }) => {
    await context.addCookies([{
      name: 'cookie_consent',
      value: makeStaleConsentCookie('0.9.0'),
      url: 'http://localhost:3000',
    }])
    await page.goto('/')
    await expect(page.getByRole('banner', { name: 'Cookie consent' })).toBeVisible()
  })
})
