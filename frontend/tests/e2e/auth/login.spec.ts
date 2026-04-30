import { test, expect } from '@playwright/test'

test.describe('Auth — Login flow', () => {
  test('unauthenticated visitor is redirected from /profile to /login', async ({ page }) => {
    await page.route('**/api/v1/users/me', (route) => {
      route.fulfill({ status: 401, body: JSON.stringify({ status: 401 }) })
    })

    await page.goto('/profile')
    await expect(page).toHaveURL(/\/login/)
  })

  test('/login page renders login button', async ({ page }) => {
    await page.route('**/api/v1/users/me', (route) => {
      route.fulfill({ status: 401, body: JSON.stringify({ status: 401 }) })
    })

    await page.goto('/login')
    // Scope to <main> to avoid matching the UserMenu "Login with Google" button in the header
    await expect(
      page.getByRole('main').getByRole('button', { name: /login with google/i }),
    ).toBeVisible()
  })

  test('authenticated user visiting /login is redirected to /profile', async ({ page }) => {
    await page.route('**/api/v1/users/me', (route) => {
      route.fulfill({
        status: 200,
        body: JSON.stringify({
          id: 'uuid-123',
          email: 'user@test.com',
          displayName: 'Test User',
          profilePictureUrl: null,
        }),
      })
    })

    await page.goto('/login')
    // The auth middleware and login.vue onMounted both call fetchCurrentUser;
    // wait for navigation rather than asserting the URL immediately.
    await expect(page).toHaveURL(/\/profile/, { timeout: 10000 })
  })
})
