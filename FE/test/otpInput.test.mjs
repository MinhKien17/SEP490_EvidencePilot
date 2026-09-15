import assert from 'node:assert/strict';
import test from 'node:test';
import { fileURLToPath } from 'node:url';
import { chromium } from '@playwright/test';
import { createServer } from 'vite';

const projectRoot = fileURLToPath(new URL('..', import.meta.url));

test('OTP input keeps the six-digit Profile contract', async (t) => {
  const server = await createServer({
    root: projectRoot,
    configFile: false,
    appType: 'custom',
    logLevel: 'silent',
    server: { host: '127.0.0.1', port: 0 },
  });
  server.middlewares.use(async (request, response, next) => {
    if (request.url !== '/__otp_test__') return next();
    response.setHeader('Content-Type', 'text/html');
    response.end(await server.transformIndexHtml(request.url, `
      <div id="root"></div>
      <script type="module">
        import React, { createRef } from 'react';
        import { createRoot } from 'react-dom/client';
        import OtpInput from '/src/components/ui/OtpInput.jsx';

        const events = [];
        const ref = createRef();
        const root = createRoot(document.getElementById('root'));
        const render = (props = {}) => root.render(React.createElement(OtpInput, {
          ref,
          label: 'Email verification code',
          hint: 'Enter the six-digit code',
          onChange: (value) => events.push(['change', value]),
          onComplete: (value) => events.push(['complete', value]),
          ...props,
        }));
        window.otpHarness = { events, ref, render };
        render();
      </script>
    `));
  });
  await server.listen();
  t.after(() => server.close());

  const browser = await chromium.launch({ channel: 'chrome' });
  t.after(() => browser.close());

  const address = server.httpServer.address();
  const page = await browser.newPage();
  await page.goto(`http://127.0.0.1:${address.port}/__otp_test__`);

  const input = page.getByRole('textbox', { name: 'Email verification code' });
  await input.waitFor({ timeout: 5000 });
  assert.equal(await page.locator('input').count(), 1);
  assert.equal(await input.getAttribute('autocomplete'), 'one-time-code');
  assert.equal(await input.getAttribute('inputmode'), 'numeric');
  assert.equal(await input.getAttribute('maxlength'), '6');

  await input.fill('12a34');
  assert.equal(await input.inputValue(), '1234');
  await input.evaluate((element) => {
    const clipboardData = new DataTransfer();
    clipboardData.setData('text/plain', '98 76-54');
    element.dispatchEvent(new ClipboardEvent('paste', {
      bubbles: true,
      cancelable: true,
      clipboardData,
    }));
  });
  assert.equal(await input.inputValue(), '987654');
  assert.deepEqual(
    await page.evaluate(() => window.otpHarness.events.at(-1)),
    ['complete', '987654'],
  );

  await page.evaluate(() => window.otpHarness.ref.current.clear());
  assert.equal(await input.inputValue(), '');
  await input.fill('123');
  await input.press('End');
  await input.press('Backspace');
  assert.equal(await input.inputValue(), '12');
  await page.evaluate(() => window.otpHarness.ref.current.focus());
  assert.equal(await input.evaluate((element) => element === document.activeElement), true);

  await page.evaluate(() => window.otpHarness.render({
    disabled: true,
    status: 'error',
    errorMessage: 'Invalid code',
  }));
  await input.waitFor({ state: 'visible' });
  assert.equal(await input.isDisabled(), true);
  assert.equal(await input.getAttribute('aria-invalid'), 'true');
  assert.equal(await page.getByRole('status').textContent(), 'Invalid code');

  await page.evaluate(() => window.otpHarness.render({
    status: 'error',
    errorMessage: 'Invalid code',
  }));
  await page.waitForFunction(() => !document.querySelector('input').disabled);
  assert.equal(await input.evaluate((element) => element === document.activeElement), true);
});
