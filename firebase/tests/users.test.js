import { describe, it } from 'node:test';
import assert from 'node:assert';
import { assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { testEnv } from './setup.js';

describe('Unauthenticated access', () => {
  it('denies unauthenticated read and write on users', async () => {
    const unauthDb = testEnv.unauthenticatedContext().firestore();
    const readErr = await assertFails(unauthDb.collection('users').doc('u1').get());
    assert.ok(readErr);
    const writeErr = await assertFails(unauthDb.collection('users').doc('u1').set({ displayName: 'Anon' }));
    assert.ok(writeErr);
  });

  it('denies unauthenticated read and write on spaces and subcollections', async () => {
    const unauthDb = testEnv.unauthenticatedContext().firestore();
    const spaceReadErr = await assertFails(unauthDb.collection('spaces').doc('s1').get());
    assert.ok(spaceReadErr);
    const spaceWriteErr = await assertFails(unauthDb.collection('spaces').doc('s1').set({ name: 'Anon' }));
    assert.ok(spaceWriteErr);
    const memberReadErr = await assertFails(unauthDb.collection('spaces').doc('s1').collection('members').get());
    assert.ok(memberReadErr);
    const itemReadErr = await assertFails(unauthDb.collection('spaces').doc('s1').collection('items').get());
    assert.ok(itemReadErr);
  });
});

describe('User private profiles', () => {
  it('allows a user to read and write their own profile', async () => {
    const aliceDb = testEnv.authenticatedContext('alice').firestore();
    const writeSuccess = await assertSucceeds(aliceDb.collection('users').doc('alice').set({
      displayName: 'Alice',
      language: 'en',
      timeZone: 'Europe/Madrid',
    }));
    assert.ok(!writeSuccess);
    const doc = await assertSucceeds(aliceDb.collection('users').doc('alice').get());
    assert.strictEqual(doc.data().displayName, 'Alice');
  });

  it('denies a user from reading or writing another user profile', async () => {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      await context.firestore().collection('users').doc('alice').set({ displayName: 'Alice' });
    });
    const bobDb = testEnv.authenticatedContext('bob').firestore();
    const readErr = await assertFails(bobDb.collection('users').doc('alice').get());
    assert.ok(readErr);
    const writeErr = await assertFails(bobDb.collection('users').doc('alice').update({ displayName: 'Hacked' }));
    assert.ok(writeErr);
  });
});
