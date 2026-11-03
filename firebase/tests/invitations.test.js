import { describe, it, beforeEach } from 'node:test';
import assert from 'node:assert';
import { assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { serverTime, testEnv } from './setup.js';

describe('Invitations', () => {
  const DAY = 24 * 60 * 60 * 1000;
  const inDays = (days) => new Date(Date.now() + days * DAY);

  // A valid invitation to s1 made by alice, with anything in overrides changed.
  const invitation = (overrides = {}) => ({
    spaceId: 's1',
    createdBy: 'alice',
    createdAt: serverTime(),
    expiresAt: inDays(7),
    revoked: false,
    ...overrides,
  });

  beforeEach(async () => {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      const db = context.firestore();
      await db.collection('spaces').doc('s1').set({
        name: 'Home',
        accent: 1,
        memberIds: ['alice', 'bob', 'charlie'],
        createdBy: 'alice',
      });
      await db.collection('spaces').doc('s2').set({
        name: 'Elsewhere',
        accent: 2,
        memberIds: ['david'],
        createdBy: 'david',
      });
      const members = db.collection('spaces').doc('s1').collection('members');
      await members.doc('alice').set({ role: 'owner', displayName: 'Alice' });
      await members.doc('bob').set({ role: 'admin', displayName: 'Bob' });
      await members.doc('charlie').set({ role: 'member', displayName: 'Charlie' });
      await db.collection('spaces').doc('s2').collection('members').doc('david').set({
        role: 'owner',
        displayName: 'David',
      });
      await db.collection('invitations').doc('ABCD2345').set({
        spaceId: 's1',
        createdBy: 'alice',
        createdAt: new Date(),
        expiresAt: inDays(7),
        revoked: false,
      });
    });
  });

  const create = (uid, code, data) =>
    testEnv.authenticatedContext(uid).firestore().collection('invitations').doc(code).set(data);

  it('lets the owner and an admin create an invitation', async () => {
    await assertSucceeds(create('alice', 'WXYZ6789', invitation()));
    await assertSucceeds(create('bob', 'KMNP2345', invitation({ createdBy: 'bob' })));
  });

  it('denies a plain member and a non-member creating one', async () => {
    await assertFails(create('charlie', 'WXYZ6789', invitation({ createdBy: 'charlie' })));
    await assertFails(create('david', 'KMNP2345', invitation({ createdBy: 'david' })));
    await assertFails(testEnv.unauthenticatedContext().firestore()
      .collection('invitations').doc('QRST2345').set(invitation()));
  });

  it('denies an invitation to a space the user does not own or administer', async () => {
    await assertFails(create('alice', 'WXYZ6789', invitation({ spaceId: 's2' })));
  });

  it('denies an invitation made in the name of another user', async () => {
    await assertFails(create('alice', 'WXYZ6789', invitation({ createdBy: 'bob' })));
  });

  it('denies an invitation that is already revoked, has no expiry in range or has other fields', async () => {
    await assertFails(create('alice', 'WXYZ6789', invitation({ revoked: true })));
    await assertFails(create('alice', 'WXYZ6789', invitation({ expiresAt: inDays(-1) })));
    await assertFails(create('alice', 'WXYZ6789', invitation({ expiresAt: inDays(31) })));
    await assertFails(create('alice', 'WXYZ6789', invitation({ expiresAt: 'tomorrow' })));
    await assertFails(create('alice', 'WXYZ6789', invitation({ role: 'admin' })));
    await assertFails(create('alice', 'WXYZ6789', { ...invitation(), createdAt: new Date() }));
    const { expiresAt, ...withoutExpiry } = invitation();
    await assertFails(create('alice', 'WXYZ6789', withoutExpiry));
  });

  it('accepts an expiry up to 30 days away', async () => {
    await assertSucceeds(create('alice', 'WXYZ6789', invitation({ expiresAt: inDays(29) })));
  });

  it('denies codes that are not eight characters of the code alphabet', async () => {
    await assertFails(create('alice', 'ABCD234', invitation()));
    await assertFails(create('alice', 'ABCD23456', invitation()));
    await assertFails(create('alice', 'abcd2345', invitation()));
    await assertFails(create('alice', 'ABCDO345', invitation()));
    await assertFails(create('alice', 'ABCDI345', invitation()));
    await assertFails(create('alice', 'ABCD1345', invitation()));
  });

  it('lets any signed-in user read an invitation by its code, and nobody else', async () => {
    const read = (ctx) => ctx.firestore().collection('invitations').doc('ABCD2345').get();
    const asDavid = await assertSucceeds(read(testEnv.authenticatedContext('david')));
    assert.strictEqual(asDavid.data().spaceId, 's1');
    await assertSucceeds(read(testEnv.authenticatedContext('charlie')));
    await assertFails(read(testEnv.unauthenticatedContext()));
  });

  it('lets the owner and an admin list the invitations of their space only', async () => {
    const list = (uid, spaceId) => testEnv.authenticatedContext(uid).firestore()
      .collection('invitations').where('spaceId', '==', spaceId).get();
    const asAlice = await assertSucceeds(list('alice', 's1'));
    assert.strictEqual(asAlice.size, 1);
    await assertSucceeds(list('bob', 's1'));
    await assertFails(list('charlie', 's1'));
    await assertFails(list('david', 's1'));
    await assertFails(list('alice', 's2'));
  });

  it('denies listing every invitation', async () => {
    await assertFails(testEnv.authenticatedContext('alice').firestore().collection('invitations').get());
  });

  it('lets the owner and an admin revoke an invitation', async () => {
    const revoke = (uid) => testEnv.authenticatedContext(uid).firestore()
      .collection('invitations').doc('ABCD2345').update({ revoked: true });
    await assertSucceeds(revoke('alice'));
    await assertSucceeds(revoke('bob'));
  });

  it('denies a plain member and a non-member revoking one', async () => {
    const revoke = (uid) => testEnv.authenticatedContext(uid).firestore()
      .collection('invitations').doc('ABCD2345').update({ revoked: true });
    await assertFails(revoke('charlie'));
    await assertFails(revoke('david'));
  });

  it('denies any other change to an invitation, and un-revoking it', async () => {
    const ref = testEnv.authenticatedContext('alice').firestore().collection('invitations').doc('ABCD2345');
    await assertFails(ref.update({ expiresAt: inDays(20) }));
    await assertFails(ref.update({ spaceId: 's2' }));
    await assertFails(ref.update({ revoked: true, expiresAt: inDays(20) }));
    await assertFails(ref.update({ revoked: false }));
    await assertSucceeds(ref.update({ revoked: true }));
    await assertFails(ref.update({ revoked: false }));
  });

  it('denies deleting an invitation, even to the owner', async () => {
    await assertFails(testEnv.authenticatedContext('alice').firestore()
      .collection('invitations').doc('ABCD2345').delete());
  });
});
