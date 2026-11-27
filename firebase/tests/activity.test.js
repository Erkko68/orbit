import { describe, it, beforeEach } from 'node:test';
import { assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { serverTime, testEnv } from './setup.js';

describe('Activity subcollection', () => {
  const entry = (by, fields = {}) => ({
    actorId: by,
    at: serverTime(),
    action: 'edit',
    target: 'item',
    targetId: 'item1',
    title: 'Bins',
    changes: { title: '"Trash"' },
    ...fields,
  });

  const space = (db) => db.collection('spaces').doc('s1');

  // Renames item1 and records it in one batch, the way the app edits an item.
  const rename = (db, record) => {
    const batch = db.batch();
    batch.update(space(db).collection('items').doc('item1'), { title: 'Bins' });
    batch.set(space(db).collection('activity').doc(), record);
    return batch.commit();
  };

  beforeEach(async () => {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      const s1 = space(context.firestore());
      await s1.set({ name: 'Home', accent: 0, memberIds: ['alice', 'charlie'], createdBy: 'alice' });
      await s1.collection('members').doc('alice').set({ role: 'owner', displayName: 'Alice' });
      await s1.collection('members').doc('charlie').set({ role: 'member', displayName: 'Charlie' });
      await s1.collection('items').doc('item1').set({ spaceId: 's1', title: 'Trash', status: 'open', createdBy: 'charlie' });
      await s1.collection('activity').doc('a1').set({ ...entry('alice'), at: new Date() });
    });
  });

  it('lets members read the history, not others', async () => {
    const activity = (uid) => space(testEnv.authenticatedContext(uid).firestore()).collection('activity');

    await assertSucceeds(activity('charlie').where('at', '>=', new Date(0)).orderBy('at', 'desc').get());
    await assertFails(activity('david').where('at', '>=', new Date(0)).get());
  });

  it('lets a member record their own change with the write it records', async () => {
    const charlieDb = testEnv.authenticatedContext('charlie').firestore();
    await assertSucceeds(rename(charlieDb, entry('charlie')));
  });

  it('lets a member record a delete once the target is gone, and an assistant plan mark its entries', async () => {
    const charlieDb = testEnv.authenticatedContext('charlie').firestore();
    const batch = charlieDb.batch();
    batch.delete(space(charlieDb).collection('items').doc('item1'));
    batch.set(space(charlieDb).collection('activity').doc(), entry('charlie', { action: 'delete' }));
    await assertSucceeds(batch.commit());

    const created = charlieDb.batch();
    const item = space(charlieDb).collection('items').doc();
    created.set(item, { spaceId: 's1', title: 'Plants', status: 'open', createdBy: 'charlie', createdAt: serverTime() });
    created.set(space(charlieDb).collection('activity').doc(), entry('charlie', {
      action: 'create', targetId: item.id, title: 'Plants', changes: {}, source: 'assistant', planId: 'p1',
    }));
    await assertSucceeds(created.commit());
  });

  it('denies an entry in someone else\'s name, with other fields, or with a wrong time, action or source', async () => {
    const charlieDb = testEnv.authenticatedContext('charlie').firestore();

    await assertFails(rename(charlieDb, entry('alice')));
    await assertFails(rename(charlieDb, { ...entry('charlie'), note: 'extra' }));
    await assertFails(rename(charlieDb, { ...entry('charlie'), at: new Date(0) }));
    await assertFails(rename(charlieDb, entry('charlie', { action: 'archive' })));
    await assertFails(rename(charlieDb, entry('charlie', { target: 'space' })));
    await assertFails(rename(charlieDb, entry('charlie', { source: 'person' })));
    await assertFails(rename(charlieDb, entry('charlie', { changes: 'title' })));
  });

  it('denies an entry whose target does not match the write: missing, or still there after a delete', async () => {
    const charlieDb = testEnv.authenticatedContext('charlie').firestore();
    const activity = space(charlieDb).collection('activity');

    await assertFails(activity.add(entry('charlie', { targetId: 'missing' })));
    await assertFails(activity.add(entry('charlie', { action: 'delete' })));
  });

  it('denies outsiders, and nobody edits or deletes an entry', async () => {
    const davidDb = testEnv.authenticatedContext('david').firestore();
    await assertFails(space(davidDb).collection('activity').add(entry('david')));

    const aliceDb = testEnv.authenticatedContext('alice').firestore();
    await assertFails(space(aliceDb).collection('activity').doc('a1').update({ title: 'Changed' }));
    await assertFails(space(aliceDb).collection('activity').doc('a1').delete());
  });
});
