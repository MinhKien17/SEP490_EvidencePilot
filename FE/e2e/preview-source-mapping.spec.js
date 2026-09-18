import { test, expect } from '@playwright/test';

test.use({ channel: 'chrome' });

const MODULE_URL = 'http://localhost:5173/src/utils/previewSelection.js';

async function resolveOnContent(page, html, select) {
  await page.goto('http://localhost:5173/');
  await page.setContent(`<div id="preview">${html}</div>`);
  return page.evaluate(async ({ moduleUrl, select }) => {
    const mod = await import(moduleUrl);
    const host = document.querySelector('#preview');
    const texts = [...host.querySelectorAll('*')]
      .flatMap(el => [...el.childNodes].filter(n => n.nodeType === 3));
    const target = texts.find(n => n.textContent.includes(select.text));
    if (!target) return { harness: 'text-not-found' };
    const range = document.createRange();
    range.setStart(target, select.from);
    range.setEnd(target, select.to);
    const selection = window.getSelection();
    selection.removeAllRanges();
    selection.addRange(range);
    return mod.resolvePreviewRange(host);
    // eslint-disable-next-line no-unused-vars
  }, { moduleUrl: MODULE_URL, select }).catch(error => ({ harness: String(error).slice(0, 120) }));
}

test('exact plain-text selection maps to canonical offsets', async ({ page }) => {
  const result = await resolveOnContent(
    page,
    '<p><span data-ss="0" data-se="25">A fish is different here.</span></p>',
    { text: 'fish', from: 2, to: 6 },
  );
  expect(result).toEqual({ from: 2, to: 6 });
});

test('second duplicate occurrence maps by position, not search', async ({ page }) => {
  const result = await resolveOnContent(
    page,
    '<p><span data-ss="0" data-se="38">A fish is different from another fish.</span></p>',
    { text: 'another fish.', from: 33, to: 37 },
  );
  // An indexOf-based mapping would resolve to the first "fish" (2..6).
  expect(result).toEqual({ from: 33, to: 37 });
});

test('unmapped KaTeX-style output refuses honestly', async ({ page }) => {
  const result = await resolveOnContent(
    page,
    '<p><span data-ss="0" data-se="4">See </span><span class="katex">y</span></p>',
    { text: 'y', from: 0, to: 1 },
  );
  expect(result).toEqual({ unmappable: 'unmapped-content' });
});

test('empty selection refuses', async ({ page }) => {
  await page.goto('http://localhost:5173/');
  await page.setContent('<div id="preview"><p><span data-ss="0" data-se="5">Hello</span></p></div>');
  const result = await page.evaluate(async moduleUrl => {
    const mod = await import(moduleUrl);
    window.getSelection()?.removeAllRanges();
    return mod.resolvePreviewRange(document.querySelector('#preview'));
  }, MODULE_URL);
  expect(result).toEqual({ unmappable: 'empty-selection' });
});
