import { describe, it } from 'node:test';
import assert from 'node:assert';
import { readFileSync } from 'node:fs';

const config = JSON.parse(readFileSync(new URL('../firebase.json', import.meta.url), 'utf8'));
const page = readFileSync(new URL('../hosting/join/index.html', import.meta.url), 'utf8');

// The page of an invitation link (#295). What it must agree on with the app and the rules.
describe('Invitation link page', () => {
  it('is served for every /join/<code> address', () => {
    assert.strictEqual(config.hosting.public, 'hosting');
    assert.deepStrictEqual(config.hosting.rewrites, [{ source: '/join/**', destination: '/join/index.html' }]);
  });

  it('opens the invitation in the app through the orbit:// link the app registers', () => {
    assert.ok(page.includes('"orbit://join/" + code'));
  });

  it('accepts the code alphabet and length of the app and the rules', () => {
    assert.ok(page.includes('/^[ABCDEFGHJKLMNPQRSTUVWXYZ23456789]{8}$/'));
  });

  it('says the same in the four languages of the app', () => {
    for (const language of ['en', 'es', 'ca', 'it']) {
      assert.ok(new RegExp(`\\n        ${language}: \\{`).test(page), language);
    }
  });

  it('is not indexed and sends no referrer', () => {
    assert.ok(page.includes('<meta name="robots" content="noindex">'));
    const rule = config.hosting.headers.find((header) => header.source === '/join/**');
    assert.ok(rule.headers.some((h) => h.key === 'Referrer-Policy' && h.value === 'no-referrer'));
  });
});
