import { describe, it, beforeEach } from 'node:test';
import { assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { serverTime, testEnv } from './setup.js';

describe('Completions subcollection', () => {
  const completion = (by) => ({ itemId: 'item1', completedBy: by, completedAt: serverTime() });

  // Marks item1 done and records the completion in one batch, the way the app completes an item.
  const complete = (db, record) => {
    const space = db.collection('spaces').doc('s1');
    const batch = db.batch();
    batch.update(space.collection('items').doc('item1'), {
      status: 'done',
      completedBy: record.completedBy,
      completedAt: serverTime(),
    });
    batch.set(space.collection('completions').doc(), record);
    return batch.commit();
  };

  beforeEach(async () => {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      const space = context.firestore().collection('spaces').doc('s1');
      await space.set({ name: 'Home', accent: 0, memberIds: ['alice', 'charlie'], createdBy: 'alice' });
      await space.collection('members').doc('alice').set({ role: 'owner', displayName: 'Alice' });
      await space.collection('members').doc('charlie').set({ role: 'member', displayName: 'Charlie' });
      await space.collection('items').doc('item1').set({ spaceId: 's1', title: 'Bins', status: 'open', createdBy: 'alice' });
      await space.collection('completions').doc('c1').set({ itemId: 'item1', completedBy: 'alice', completedAt: new Date() });
    });
  });

  it('lets members read the history, not others', async () => {
    const completions = (uid) =>
      testEnv.authenticatedContext(uid).firestore().collection('spaces').doc('s1').collection('completions');

    await assertSucceeds(completions('charlie').where('itemId', '==', 'item1').get());
    await assertFails(completions('david').where('itemId', '==', 'item1').get());
  });

  it('lets a member record their own completion with the item it completes', async () => {
    const charlieDb = testEnv.authenticatedContext('charlie').firestore();
    await assertSucceeds(complete(charlieDb, completion('charlie')));
  });

  it('denies a completion in someone else\'s name, with other fields, or without completing the item', async () => {
    const charlieDb = testEnv.authenticatedContext('charlie').firestore();
    const completions = charlieDb.collection('spaces').doc('s1').collection('completions');

    await assertFails(complete(charlieDb, completion('alice')));
    await assertFails(complete(charlieDb, { ...completion('charlie'), note: 'extra' }));
    await assertFails(complete(charlieDb, { ...completion('charlie'), completedAt: new Date(0) }));
    await assertFails(completions.add(completion('charlie')));
    await assertFails(completions.add({ ...completion('charlie'), itemId: 'missing' }));
  });

  // Moves a recurring item1 to its next date and records the completion in one batch, the way the app rolls it forward.
  const rollForward = (db, record, date = '2026-11-13') => {
    const space = db.collection('spaces').doc('s1');
    const batch = db.batch();
    batch.update(space.collection('items').doc('item1'), { 'modules.schedule': { date } });
    batch.set(space.collection('completions').doc(), record);
    return batch.commit();
  };

  const makeRecurring = (modules) => testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection('spaces').doc('s1').collection('items').doc('item1').update({ modules });
  });

  it('lets a member complete a recurring item by moving it to a later date', async () => {
    await makeRecurring({ schedule: { date: '2026-11-06' }, recurrence: { unit: 'week', every: 1, weekdays: [5] } });
    const charlieDb = testEnv.authenticatedContext('charlie').firestore();
    await assertSucceeds(rollForward(charlieDb, completion('charlie')));
  });

  it('denies rolling forward an item that does not recur, or to an earlier or the same date', async () => {
    const charlieDb = testEnv.authenticatedContext('charlie').firestore();
    await makeRecurring({ schedule: { date: '2026-11-06' } });
    await assertFails(rollForward(charlieDb, completion('charlie')));

    await makeRecurring({ schedule: { date: '2026-11-06' }, recurrence: { unit: 'day', every: 1 } });
    await assertFails(rollForward(charlieDb, completion('charlie'), '2026-11-06'));
    await assertFails(rollForward(charlieDb, completion('charlie'), '2026-11-01'));
  });

  // Opens item1 again and records the reopening in one batch, the way the app reopens an item.
  const reopen = (db, record) => {
    const space = db.collection('spaces').doc('s1');
    const batch = db.batch();
    batch.update(space.collection('items').doc('item1'), { status: 'open' });
    batch.set(space.collection('completions').doc(), record);
    return batch.commit();
  };

  const reopening = (by) => ({ ...completion(by), action: 'reopen' });

  it('lets a member record their own reopening with the item it reopens', async () => {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      await context.firestore().collection('spaces').doc('s1').collection('items').doc('item1')
        .update({ status: 'done', completedBy: 'alice', completedAt: new Date() });
    });
    const charlieDb = testEnv.authenticatedContext('charlie').firestore();
    await assertSucceeds(reopen(charlieDb, reopening('charlie')));
  });

  it('denies a reopening of an open item or that leaves it done, another action, or a completion of an open item', async () => {
    const charlieDb = testEnv.authenticatedContext('charlie').firestore();
    const completions = charlieDb.collection('spaces').doc('s1').collection('completions');

    await assertFails(complete(charlieDb, reopening('charlie')));
    await assertFails(reopen(charlieDb, { ...completion('charlie'), action: 'archive' }));
    await assertFails(reopen(charlieDb, reopening('alice')));
    await assertFails(reopen(charlieDb, completion('charlie')));
    await assertFails(completions.add(reopening('charlie')));
  });

  it('denies a non-member a completion', async () => {
    const davidDb = testEnv.authenticatedContext('david').firestore();
    await assertFails(complete(davidDb, completion('david')));
  });

  it('never lets anyone edit or delete a completion, the owner included', async () => {
    const aliceDb = testEnv.authenticatedContext('alice').firestore();
    const recorded = aliceDb.collection('spaces').doc('s1').collection('completions').doc('c1');

    await assertFails(recorded.update({ completedBy: 'charlie' }));
    await assertFails(recorded.delete());
  });
});
