import { describe, it, beforeEach } from 'node:test';
import assert from 'node:assert';
import { assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { firebase, serverTime, testEnv } from './setup.js';

describe('Items subcollection', () => {
  beforeEach(async () => {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      const db = context.firestore();
      await db.collection('spaces').doc('s1').set({
        name: 'Home',
        accent: 0,
        memberIds: ['alice', 'charlie'],
        createdBy: 'alice',
      });
      await db.collection('spaces').doc('s1').collection('members').doc('alice').set({
        role: 'owner',
        displayName: 'Alice',
      });
      await db.collection('spaces').doc('s1').collection('members').doc('charlie').set({
        role: 'member',
        displayName: 'Charlie',
      });
      await db.collection('spaces').doc('s1').collection('items').doc('item1').set({
        spaceId: 's1',
        title: 'Buy milk',
        status: 'open',
        createdBy: 'alice',
      });
    });
  });

  it('allows space members to read items, denies non-members', async () => {
    const charlieDb = testEnv.authenticatedContext('charlie').firestore();
    const davidDb = testEnv.authenticatedContext('david').firestore();

    const memberRead = await assertSucceeds(
      charlieDb.collection('spaces').doc('s1').collection('items').doc('item1').get()
    );
    assert.strictEqual(memberRead.data().title, 'Buy milk');

    const nonMemberRead = await assertFails(
      davidDb.collection('spaces').doc('s1').collection('items').doc('item1').get()
    );
    assert.ok(nonMemberRead);
  });

  it('allows members to create items with matching spaceId and createdBy', async () => {
    const charlieDb = testEnv.authenticatedContext('charlie').firestore();
    const createSuccess = await assertSucceeds(
      charlieDb.collection('spaces').doc('s1').collection('items').doc('item2').set({
        spaceId: 's1',
        title: 'Clean kitchen',
        status: 'open',
        createdBy: 'charlie',
        createdAt: serverTime(),
      })
    );
    assert.ok(!createSuccess);
  });

  it('denies creating items if user is non-member or createdBy / spaceId do not match', async () => {
    const charlieDb = testEnv.authenticatedContext('charlie').firestore();
    const davidDb = testEnv.authenticatedContext('david').firestore();

    const nonMemberCreate = await assertFails(
      davidDb.collection('spaces').doc('s1').collection('items').doc('item3').set({
        spaceId: 's1',
        title: 'Hacked item',
        status: 'open',
        createdBy: 'david',
        createdAt: serverTime(),
      })
    );
    assert.ok(nonMemberCreate);

    const fakeCreator = await assertFails(
      charlieDb.collection('spaces').doc('s1').collection('items').doc('item4').set({
        spaceId: 's1',
        title: 'Forged item',
        status: 'open',
        createdBy: 'alice',
        createdAt: serverTime(),
      })
    );
    assert.ok(fakeCreator);

    const wrongSpace = await assertFails(
      charlieDb.collection('spaces').doc('s1').collection('items').doc('item5').set({
        spaceId: 'other-space',
        title: 'Wrong space item',
        status: 'open',
        createdBy: 'charlie',
        createdAt: serverTime(),
      })
    );
    assert.ok(wrongSpace);
  });

  it('allows members to update and complete items, denies non-members', async () => {
    const charlieDb = testEnv.authenticatedContext('charlie').firestore();
    const davidDb = testEnv.authenticatedContext('david').firestore();

    const updateSuccess = await assertSucceeds(
      charlieDb.collection('spaces').doc('s1').collection('items').doc('item1').update({
        status: 'done',
        completedBy: 'charlie',
      })
    );
    assert.ok(!updateSuccess);

    const nonMemberUpdate = await assertFails(
      davidDb.collection('spaces').doc('s1').collection('items').doc('item1').update({
        status: 'open',
      })
    );
    assert.ok(nonMemberUpdate);
  });

  it('denies an item without a valid title or that starts done', async () => {
    const charlieDb = testEnv.authenticatedContext('charlie').firestore();
    const items = charlieDb.collection('spaces').doc('s1').collection('items');
    const item = { spaceId: 's1', title: 'Clean kitchen', status: 'open', createdBy: 'charlie', createdAt: serverTime() };

    await assertFails(items.doc('blank').set({ ...item, title: '   ' }));
    await assertFails(items.doc('long').set({ ...item, title: 'x'.repeat(121) }));
    await assertFails(items.doc('done').set({ ...item, status: 'done' }));
    await assertSucceeds(items.doc('longest').set({ ...item, title: 'x'.repeat(120) }));
  });

  it('denies an update that rewrites how the item was created or breaks its fields', async () => {
    const charlieDb = testEnv.authenticatedContext('charlie').firestore();
    const item = charlieDb.collection('spaces').doc('s1').collection('items').doc('item1');

    await assertFails(item.update({ createdBy: 'charlie' }));
    await assertFails(item.update({ spaceId: 'other-space' }));
    await assertFails(item.update({ createdAt: serverTime() }));
    await assertFails(item.update({ title: '' }));
    await assertFails(item.update({ status: 'archived' }));
    await assertFails(item.update({ status: 'done', completedBy: 'alice' }));
  });

  it('lets any member reopen an item', async () => {
    const charlieDb = testEnv.authenticatedContext('charlie').firestore();
    const item = charlieDb.collection('spaces').doc('s1').collection('items').doc('item1');
    await assertSucceeds(item.update({ status: 'done', completedBy: 'charlie' }));

    const aliceDb = testEnv.authenticatedContext('alice').firestore();
    await assertSucceeds(
      aliceDb.collection('spaces').doc('s1').collection('items').doc('item1').update({
        status: 'open',
        completedBy: firebase.firestore.FieldValue.delete(),
      })
    );
  });

  it('lets the creator, an admin or the owner delete an item, and no other member', async () => {
    const charlieDb = testEnv.authenticatedContext('charlie').firestore();
    const aliceDb = testEnv.authenticatedContext('alice').firestore();
    const davidDb = testEnv.authenticatedContext('david').firestore();
    const items = (db) => db.collection('spaces').doc('s1').collection('items');

    await assertFails(items(davidDb).doc('item1').delete());
    await assertFails(items(charlieDb).doc('item1').delete());

    await assertSucceeds(items(charlieDb).doc('mine').set({
      spaceId: 's1',
      title: 'Water plants',
      status: 'open',
      createdBy: 'charlie',
      createdAt: serverTime(),
    }));
    await assertSucceeds(items(charlieDb).doc('mine').delete());

    await assertSucceeds(items(aliceDb).doc('item1').delete());
  });
});
