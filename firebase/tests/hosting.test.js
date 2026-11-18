import { describe, it } from 'node:test';
import assert from 'node:assert';
import { readFileSync } from 'node:fs';

const config = JSON.parse(readFileSync(new URL('../firebase.json', import.meta.url), 'utf8'));
const page = readFileSync(new URL('../hosting/join/index.html', import.meta.url), 'utf8');
const script = readFileSync(new URL('../hosting/join/join.js', import.meta.url), 'utf8');
const headers = Object.fromEntries(
  config.hosting.headers.find((header) => header.source === '/join/**').headers.map((h) => [h.key, h.value]),
);

// The page of an invitation link (#295). What it must agree on with the app and the rules.
describe('Invitation link page', () => {
  it('is served for every /join/<code> address', () => {
    assert.strictEqual(config.hosting.public, 'hosting');
    assert.deepStrictEqual(config.hosting.rewrites, [{ source: '/join/**', destination: '/join/index.html' }]);
  });

  it('opens the invitation in the app through the orbit:// link the app registers', () => {
    assert.ok(script.includes('"orbit://join/" + code'));
  });

  it('accepts the code alphabet and length of the app and the rules', () => {
    assert.ok(script.includes('/^[ABCDEFGHJKLMNPQRSTUVWXYZ23456789]{8}$/'));
  });

  it('says the same in the four languages of the app', () => {
    for (const language of ['en', 'es', 'ca', 'it']) {
      assert.ok(new RegExp(`\\n    ${language}: \\{`).test(script), language);
    }
  });

  it('is not indexed and sends no referrer', () => {
    assert.ok(page.includes('<meta name="robots" content="noindex">'));
    assert.strictEqual(headers['Referrer-Policy'], 'no-referrer');
  });

  // #324: what OWASP ZAP found missing. The CSP allows only the page's own files, so nothing may be inline.
  it('runs only its own files, under a strict Content Security Policy', () => {
    assert.ok(page.includes('<script src="/join/join.js"></script>'));
    assert.ok(page.includes('<link rel="stylesheet" href="/join/join.css">'));
    assert.ok(!/<script>|<style>|\sstyle=|\son[a-z]+=/.test(page));
    assert.match(headers['Content-Security-Policy'], /^default-src 'none'; script-src 'self'; style-src 'self';/);
    assert.match(headers['Content-Security-Policy'], /frame-ancestors 'none'/);
    assert.strictEqual(headers['X-Frame-Options'], 'DENY');
    assert.strictEqual(headers['X-Content-Type-Options'], 'nosniff');
  });
});
