import { expect, test } from '@playwright/test';

test.use({ channel: 'chrome' });

const fixtureUrl = 'http://localhost:5173/playwright/fixtures/language-runtime.html';

async function openWithStoredLanguage(page, storedLanguage) {
  await page.addInitScript((language) => {
    if (sessionStorage.getItem('language_fixture_seeded')) return;
    localStorage.clear();
    if (language !== null) localStorage.setItem('app_lang', language);
    sessionStorage.setItem('language_fixture_seeded', '1');
  }, storedLanguage);
  await page.goto(fixtureUrl);
}

async function expectRuntimeLanguage(page, expected) {
  await expect(page.getByTestId('context-language')).toHaveText(expected);
  await expect(page.getByTestId('i18next-language')).toHaveText(expected);
  await expect(page.getByTestId('legacy-copy')).toHaveText(expected === 'vi' ? 'Trang chủ' : 'Home');
  await expect.poll(() => page.evaluate(() => localStorage.getItem('app_lang'))).toBe(expected);
  await expect.poll(() => page.evaluate(() => document.documentElement.lang)).toBe(expected);
}

test('normalizes an invalid stored locale to English', async ({ page }) => {
  await openWithStoredLanguage(page, 'garbage');
  await expectRuntimeLanguage(page, 'en');
});

test('defaults empty storage to English', async ({ page }) => {
  await openWithStoredLanguage(page, null);
  await expectRuntimeLanguage(page, 'en');
});

test('restores Vietnamese from storage', async ({ page }) => {
  await openWithStoredLanguage(page, 'vi');
  await expectRuntimeLanguage(page, 'vi');
});

test('follows i18next changes and keeps the useLanguage controls compatible', async ({ page }) => {
  await openWithStoredLanguage(page, null);

  await page.evaluate(() => window.changeI18nextLanguage('vi'));
  await expectRuntimeLanguage(page, 'vi');

  await page.getByRole('button', { name: 'Toggle language' }).click();
  await expectRuntimeLanguage(page, 'en');
});

test('reloads with the language selected through useLanguage', async ({ page }) => {
  await openWithStoredLanguage(page, null);

  await page.getByRole('button', { name: 'Set Vietnamese' }).click();
  await expectRuntimeLanguage(page, 'vi');
  await page.reload();
  await expectRuntimeLanguage(page, 'vi');
});
