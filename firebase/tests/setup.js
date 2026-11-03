// What every rules test file shares: the emulator with firestore.rules loaded, cleared before each test. The files
// share one emulator project, so they run one at a time (`--test-concurrency=1` in package.json).
import { before, after, beforeEach } from 'node:test';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import firebase from 'firebase/compat/app';
import 'firebase/compat/firestore';
import { initializeTestEnvironment } from '@firebase/rules-unit-testing';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

export { firebase };
export const serverTime = () => firebase.firestore.FieldValue.serverTimestamp();

// Set once `before` runs; the test files read it inside their tests.
export let testEnv;

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: 'demo-orbit',
    firestore: {
      rules: fs.readFileSync(path.resolve(__dirname, '../firestore.rules'), 'utf8'),
      host: '127.0.0.1',
      port: 8080,
    },
  });
});

after(async () => {
  await testEnv?.cleanup();
});

beforeEach(async () => {
  await testEnv.clearFirestore();
});
