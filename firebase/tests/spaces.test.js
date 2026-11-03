import { describe, it, beforeEach } from 'node:test';
import assert from 'node:assert';
import { assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { serverTime, testEnv } from './setup.js';

describe('Spaces collection', () => {
  beforeEach(async () => {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      const db = context.firestore();
      await db.collection('spaces').doc('s1').set({
        name: 'Home',
        accent: 0,
        memberIds: ['alice', 'bob', 'charlie'],
        createdBy: 'alice',
      });
      await db.collection('spaces').doc('s1').collection('members').doc('alice').set({
        role: 'owner',
        displayName: 'Alice',
      });
      await db.collection('spaces').doc('s1').collection('members').doc('bob').set({
        role: 'admin',
        displayName: 'Bob',
      });
      await db.collection('spaces').doc('s1').collection('members').doc('charlie').set({
        role: 'member',
        displayName: 'Charlie',
      });
    });
  });

  it('allows space members to read the space', async () => {
    const aliceDb = testEnv.authenticatedContext('alice').firestore();
    const bobDb = testEnv.authenticatedContext('bob').firestore();
    const charlieDb = testEnv.authenticatedContext('charlie').firestore();

    const aliceDoc = await assertSucceeds(aliceDb.collection('spaces').doc('s1').get());
    assert.strictEqual(aliceDoc.data().name, 'Home');

    const bobDoc = await assertSucceeds(bobDb.collection('spaces').doc('s1').get());
    assert.strictEqual(bobDoc.data().name, 'Home');

    const charlieDoc = await assertSucceeds(charlieDb.collection('spaces').doc('s1').get());
    assert.strictEqual(charlieDoc.data().name, 'Home');
  });

  it('denies non-members from reading the space', async () => {
    const davidDb = testEnv.authenticatedContext('david').firestore();
    const err = await assertFails(davidDb.collection('spaces').doc('s1').get());
    assert.ok(err);
  });

  it('allows an authenticated user to create a space when createdBy matches and user is in memberIds', async () => {
    const davidDb = testEnv.authenticatedContext('david').firestore();
    const success = await assertSucceeds(davidDb.collection('spaces').doc('s2').set({
      name: 'David Space',
      accent: 2,
      memberIds: ['david'],
      createdBy: 'david',
      ownerId: 'david',
      createdAt: serverTime(),
    }));
    assert.ok(!success);
  });

  it('denies creating a space if createdBy does not match authenticated user or user not in memberIds', async () => {
    const davidDb = testEnv.authenticatedContext('david').firestore();
    const failCreatedBy = await assertFails(davidDb.collection('spaces').doc('s3').set({
      name: 'Forged Space',
      accent: 1,
      memberIds: ['david'],
      createdBy: 'alice',
      createdAt: serverTime(),
    }));
    assert.ok(failCreatedBy);

    const failMemberIds = await assertFails(davidDb.collection('spaces').doc('s4').set({
      name: 'Forged Space 2',
      accent: 1,
      memberIds: ['alice'],
      createdBy: 'david',
      createdAt: serverTime(),
    }));
    assert.ok(failMemberIds);
  });

  it('denies a new space with other members, without a name or without a server creation time', async () => {
    const davidDb = testEnv.authenticatedContext('david').firestore();
    const space = {
      name: 'David Space', accent: 0, memberIds: ['david'], createdBy: 'david', ownerId: 'david', createdAt: serverTime(),
    };

    await assertFails(davidDb.collection('spaces').doc('s5').set({ ...space, memberIds: ['david', 'alice'] }));
    await assertFails(davidDb.collection('spaces').doc('s8').set({ ...space, ownerId: 'alice' }));
    const { ownerId, ...withoutOwner } = space;
    await assertFails(davidDb.collection('spaces').doc('s9').set(withoutOwner));
    await assertFails(davidDb.collection('spaces').doc('s6').set({ ...space, name: '  ' }));
    await assertFails(davidDb.collection('spaces').doc('s7').set({ ...space, createdAt: new Date(0) }));
  });

  it('denies changing who created the space, even to the owner', async () => {
    const aliceDb = testEnv.authenticatedContext('alice').firestore();
    await assertFails(aliceDb.collection('spaces').doc('s1').update({ createdBy: 'bob' }));
  });

  it('allows owner and admin to update space fields, denies regular members and non-members', async () => {
    const aliceDb = testEnv.authenticatedContext('alice').firestore();
    const bobDb = testEnv.authenticatedContext('bob').firestore();
    const charlieDb = testEnv.authenticatedContext('charlie').firestore();
    const davidDb = testEnv.authenticatedContext('david').firestore();

    const ownerUpdate = await assertSucceeds(aliceDb.collection('spaces').doc('s1').update({ name: 'Home by Alice' }));
    assert.ok(!ownerUpdate);

    const adminUpdate = await assertSucceeds(bobDb.collection('spaces').doc('s1').update({ name: 'Home by Bob' }));
    assert.ok(!adminUpdate);

    const memberUpdate = await assertFails(charlieDb.collection('spaces').doc('s1').update({ name: 'Home by Charlie' }));
    assert.ok(memberUpdate);

    const outsiderUpdate = await assertFails(davidDb.collection('spaces').doc('s1').update({ name: 'Home by David' }));
    assert.ok(outsiderUpdate);
  });

  it('lets an owner or admin change the name and the colour, together or one at a time', async () => {
    const space = (uid) => testEnv.authenticatedContext(uid).firestore().collection('spaces').doc('s1');

    await assertSucceeds(space('alice').update({ name: 'Flat 4C' }));
    await assertSucceeds(space('alice').update({ accent: 3 }));
    await assertSucceeds(space('bob').update({ name: 'Flat 4D', accent: 2 }));
    await assertSucceeds(space('bob').update({ accent: 0 }));
  });

  it('denies a plain member and someone outside the space changing the name or the colour', async () => {
    const space = (uid) => testEnv.authenticatedContext(uid).firestore().collection('spaces').doc('s1');

    await assertFails(space('charlie').update({ name: 'Mine now' }));
    await assertFails(space('charlie').update({ accent: 3 }));
    await assertFails(space('david').update({ name: 'Mine now' }));
    await assertFails(space('david').update({ accent: 3 }));
  });

  it('keeps the name a non-blank string and the colour a whole number', async () => {
    const space = testEnv.authenticatedContext('alice').firestore().collection('spaces').doc('s1');

    await assertFails(space.update({ name: '' }));
    await assertFails(space.update({ name: '   ' }));
    await assertFails(space.update({ name: 42 }));
    await assertFails(space.update({ accent: 'violet' }));
    await assertFails(space.update({ accent: 1.5 }));
  });

  it('changes nothing about the space when an invalid edit is refused', async () => {
    const space = testEnv.authenticatedContext('alice').firestore().collection('spaces').doc('s1');
    await assertFails(space.update({ name: '   ', accent: 3 }));

    const after = await assertSucceeds(space.get());
    assert.strictEqual(after.data().name, 'Home');
    assert.strictEqual(after.data().accent, 0);
  });

  it('allows only the owner to delete the space', async () => {
    const bobDb = testEnv.authenticatedContext('bob').firestore();
    const charlieDb = testEnv.authenticatedContext('charlie').firestore();
    const davidDb = testEnv.authenticatedContext('david').firestore();
    const aliceDb = testEnv.authenticatedContext('alice').firestore();

    const adminDel = await assertFails(bobDb.collection('spaces').doc('s1').delete());
    assert.ok(adminDel);

    const memberDel = await assertFails(charlieDb.collection('spaces').doc('s1').delete());
    assert.ok(memberDel);

    const outsiderDel = await assertFails(davidDb.collection('spaces').doc('s1').delete());
    assert.ok(outsiderDel);

    const ownerDel = await assertSucceeds(aliceDb.collection('spaces').doc('s1').delete());
    assert.ok(!ownerDel);
  });
});
