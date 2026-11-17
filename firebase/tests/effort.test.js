import { describe, it, beforeEach } from 'node:test';
import { assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { serverTime, testEnv, firebase } from './setup.js';

describe('Effort snapshots', () => {
  const item = (db) => db.doc('spaces/s1/items/i1');
  const record = (by, points) => ({
    itemId: 'i1', completedBy: by, completedAt: serverTime(),
    ...(points === undefined ? {} : { effortPoints: points }),
  });
  const complete = (db, points, extra = {}, credited = true) => {
    const batch = db.batch();
    batch.update(item(db), { status: 'done', completedBy: 'ben', completedAt: serverTime(), effortRecorded: true, ...extra });
    batch.set(db.collection('spaces/s1/completions').doc(), { ...record('ben', points), ...(credited ? {} : { effortCredited: false }) });
    return batch.commit();
  };
  const reopen = (db, points) => {
    const batch = db.batch();
    batch.update(item(db), { status: 'open', completedBy: firebase.firestore.FieldValue.delete(), completedAt: firebase.firestore.FieldValue.delete() });
    batch.set(db.collection('spaces/s1/completions').doc(), { ...record('ben', points), action: 'reopen' });
    return batch.commit();
  };
  beforeEach(async () => {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      const db = context.firestore();
      await db.doc('spaces/s1').set({ memberIds: ['ana', 'ben'], name: 'Home', accent: 0, createdBy: 'ana' });
      await db.doc('spaces/s1/members/ana').set({ role: 'owner' });
      await db.doc('spaces/s1/members/ben').set({ role: 'member' });
      await item(db).set({ spaceId: 's1', title: 'Bins', status: 'open', createdBy: 'ana', modules: { effort: { points: 3 } } });
    });
  });

  it('accepts only optional whole effort points from one to five', async () => {
    const db = testEnv.authenticatedContext('ben').firestore();
    for (const points of [1, 5]) await assertSucceeds(item(db).update({ 'modules.effort': { points } }));
    for (const points of [0, 6, 1.5, '3', null]) await assertFails(item(db).update({ 'modules.effort': { points } }));
    await assertFails(item(db).update({ 'modules.effort': {} }));
    await assertFails(item(db).update({ 'modules.effort': null }));
    await assertFails(item(db).update({ 'modules.effort': { points: 2, extra: true } }));
    await assertSucceeds(item(db).update({ 'modules.effort': firebase.firestore.FieldValue.delete() }));
  });

  it('requires the saved snapshot in the actual completer’s name', async () => {
    const db = testEnv.authenticatedContext('ben').firestore();
    for (const points of [undefined, 0, 6, 2, '3']) await assertFails(complete(db, points));
    await assertFails(complete(db, 3, {}, false));
    await assertSucceeds(complete(db, 3));
    await assertFails(db.collection('spaces/s1/completions').add(record('ana', 3)));
    await assertFails(complete(db, 3));
  });

  it('keeps the original points after changing or removing effort and recompleting', async () => {
    const db = testEnv.authenticatedContext('ben').firestore();
    await assertSucceeds(complete(db, 3));
    await assertFails(reopen(db, 3));
    await assertSucceeds(reopen(db));
    await assertSucceeds(item(db).update({ 'modules.effort': { points: 5 } }));
    await assertFails(complete(db, 5));
    await assertFails(item(db).update({ effortRecorded: false }));
    await assertFails(item(db).update({ effortRecorded: firebase.firestore.FieldValue.delete() }));
    await assertSucceeds(complete(db, 5, {}, false));
    await assertSucceeds(item(db).update({ 'modules.effort': firebase.firestore.FieldValue.delete() }));
    const snapshot = await db.collection('spaces/s1/completions').where('effortPoints', '==', 3).get();
    await assertFails(snapshot.docs[0].ref.update({ effortPoints: 5 }));
    await assertFails(snapshot.docs[0].ref.delete());
  });

  it('credits no points without the module, including effort added after reopening', async () => {
    const db = testEnv.authenticatedContext('ben').firestore();
    await assertSucceeds(item(db).update({ 'modules.effort': firebase.firestore.FieldValue.delete() }));
    await assertFails(complete(db, 1));
    await assertSucceeds(complete(db));
    await assertSucceeds(reopen(db));
    await assertSucceeds(item(db).update({ 'modules.effort': { points: 2 } }));
    await assertFails(complete(db, 2));
    await assertSucceeds(complete(db, 2, {}, false));
  });

  it('records separate snapshots when a recurring item advances', async () => {
    const db = testEnv.authenticatedContext('ben').firestore();
    await assertSucceeds(item(db).update({ 'modules.schedule': { date: '2026-11-16' }, 'modules.recurrence': { unit: 'day', every: 1 } }));
    const batch = db.batch();
    batch.update(item(db), { 'modules.schedule.date': '2026-11-17', effortRecorded: firebase.firestore.FieldValue.delete() });
    batch.set(db.collection('spaces/s1/completions').doc(), record('ben', 3));
    await assertSucceeds(batch.commit());
    await assertSucceeds(item(db).update({ 'modules.effort': { points: 4 } }));
    await assertSucceeds(complete(db, 4));
  });

  it('denies effort writes and history reads to outsiders', async () => {
    const db = testEnv.authenticatedContext('outsider').firestore();
    await assertFails(item(db).update({ 'modules.effort': { points: 1 } }));
    await assertFails(db.collection('spaces/s1/completions').where('completedAt', '>=', new Date(0)).get());
  });

  it('validates effort when creating an item and cannot start with credited effort', async () => {
    const db = testEnv.authenticatedContext('ben').firestore();
    const target = db.doc('spaces/s1/items/new');
    const data = { spaceId: 's1', title: 'New task', status: 'open', createdBy: 'ben', createdAt: serverTime() };
    for (const effort of [{}, null, { points: 0 }, { points: 6 }, { points: 2.5 }]) {
      await assertFails(target.set({ ...data, modules: { effort } }));
    }
    await assertFails(target.set({ ...data, modules: { effort: { points: 1 } }, effortRecorded: true }));
    await assertSucceeds(target.set({ ...data, modules: { effort: { points: 1 } } }));
  });
});
