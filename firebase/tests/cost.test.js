import { describe, it, beforeEach } from 'node:test';
import { assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { firebase, serverTime, testEnv } from './setup.js';

describe('Item cost completion batch', () => {
  const cost = { cents: 1000, paidBy: 'alice', memberIds: ['alice', 'charlie'] };
  const space = db => db.collection('spaces').doc('s1');
  const item = db => space(db).collection('items').doc('i1');
  const db = () => testEnv.authenticatedContext('charlie').firestore();

  beforeEach(async () => {
    await testEnv.withSecurityRulesDisabled(async context => {
      const database = context.firestore();
      await space(database).set({ memberIds: ['alice', 'charlie'], createdBy: 'alice' });
      await item(database).set({ spaceId: 's1', title: 'Detergent', status: 'open', createdBy: 'alice', modules: { cost } });
    });
  });

  function complete(database, { id = 'c1', version = 1, expense = true, next = null, charged = true, paidBy = 'alice' } = {}) {
    const batch = database.batch();
    const fields = { completionVersion: version, lastCompletionId: id };
    if (next) {
      fields['modules.schedule'] = { date: next };
      fields.costExpenseId = firebase.firestore.FieldValue.delete();
    } else {
      Object.assign(fields, { status: 'done', completedBy: 'charlie', completedAt: serverTime() });
      if (charged) fields.costExpenseId = id;
    }
    batch.update(item(database), fields);
    const entry = { itemId: 'i1', completedBy: 'charlie', completedAt: serverTime() };
    if (charged) entry.expenseId = id;
    batch.set(space(database).collection('completions').doc(id), entry);
    if (expense) batch.set(space(database).collection('expenses').doc(id), {
      spaceId: 's1', title: 'Detergent', paidBy, split: { alice: 500, charlie: 500 },
      date: '2026-11-12', kind: 'expense', itemId: 'i1', createdBy: 'charlie', createdAt: serverTime(),
    });
    return batch.commit();
  }

  it('writes the item, history and expense together', async () => {
    const database = db();
    await assertSucceeds(complete(database));
    await assertFails(complete(database, { id: 'duplicate', version: 1 }));
  });

  it('rejects a missing or invalid expense and leaves the item open', async () => {
    const database = db();
    await assertFails(complete(database, { expense: false }));
    await assertFails(complete(database, { paidBy: 'charlie' }));
    if ((await item(database).get()).data().status !== 'open') throw Error('Partial completion');
    await assertSucceeds(complete(database));
  });

  it('requires the cost batch even for a direct status change', async () => {
    await assertFails(item(db()).update({ status: 'done', completedBy: 'charlie', completedAt: serverTime() }));
    await assertFails(item(db()).update({ costExpenseId: 'invented' }));
  });

  it('keeps the charged expense when reopened and completed again', async () => {
    const database = db();
    await complete(database);
    const batch = database.batch();
    batch.update(item(database), { status: 'open', completedBy: firebase.firestore.FieldValue.delete(), completedAt: firebase.firestore.FieldValue.delete() });
    batch.set(space(database).collection('completions').doc('reopen'), {
      itemId: 'i1', completedBy: 'charlie', completedAt: serverTime(), action: 'reopen',
    });
    await assertSucceeds(batch.commit());
    await assertSucceeds(item(database).update({ 'modules.cost.cents': 1200 }));
    await assertSucceeds(complete(database, { id: 'c2', version: 2, expense: false, charged: false }));
    const ledger = await space(database).collection('expenses').get();
    if (ledger.size !== 1 || ledger.docs[0].data().split.alice !== 500) throw Error('Changed a past expense');
  });

  it('charges each recurring completion and rejects a stale concurrent version', async () => {
    await testEnv.withSecurityRulesDisabled(async context => item(context.firestore()).update({
      'modules.schedule': { date: '2026-11-12' }, 'modules.recurrence': { unit: 'day', every: 1 },
    }));
    const database = db();
    await assertSucceeds(complete(database, { next: '2026-11-13' }));
    await assertFails(complete(database, { id: 'stale', version: 1, next: '2026-11-14' }));
    await assertSucceeds(complete(database, { id: 'c2', version: 2, next: '2026-11-14' }));
  });

  it('accepts only one of two devices completing the same occurrence', async () => {
    const first = db();
    const second = testEnv.authenticatedContext('charlie').firestore();
    const results = await Promise.allSettled([
      complete(first, { id: 'first' }), complete(second, { id: 'second' }),
    ]);
    if (results.filter(result => result.status === 'fulfilled').length !== 1) throw Error('Duplicate charge');
    if ((await space(first).collection('expenses').get()).size !== 1) throw Error('Duplicate ledger entry');
  });

  it('rejects a non-integer completion version', async () => {
    await assertFails(complete(db(), { version: 1.5 }));
  });

  it('validates cost edits and participant membership', async () => {
    const reference = item(db());
    for (const invalid of [ { ...cost, cents: 0 }, { ...cost, cents: 1.5 }, { ...cost, paidBy: 'outsider' }, { ...cost, memberIds: [] }, { ...cost, memberIds: ['outsider'] } ]) {
      await assertFails(reference.update({ 'modules.cost': invalid }));
    }
    await assertSucceeds(reference.update({ 'modules.cost': { ...cost, cents: 1 } }));
    await assertSucceeds(reference.update({ 'modules.cost': firebase.firestore.FieldValue.delete() }));
  });

  it('allows ordinary date edits without creating a purchase', async () => {
    const reference = item(db());
    await assertSucceeds(reference.update({ 'modules.schedule': { date: '2026-11-20' } }));
  });

  it('rejects a new charge after the configured payer leaves but permits correction', async () => {
    await testEnv.withSecurityRulesDisabled(async context => space(context.firestore()).update({ memberIds: ['charlie'] }));
    await assertFails(complete(db()));
    await assertSucceeds(item(db()).update({ title: 'Correct the cost' }));
    await assertSucceeds(item(db()).update({ 'modules.cost': { cents: 1000, paidBy: 'charlie', memberIds: ['charlie'] } }));
  });

  it('never clears the charged marker while reopening or recreates a deleted expense', async () => {
    const database = db();
    await complete(database);
    await assertSucceeds(space(database).collection('expenses').doc('c1').delete());
    const batch = database.batch();
    batch.update(item(database), { status: 'open', costExpenseId: firebase.firestore.FieldValue.delete(), completionVersion: 2, lastCompletionId: 'r1' });
    batch.set(space(database).collection('completions').doc('r1'), { itemId: 'i1', completedBy: 'charlie', completedAt: serverTime(), action: 'reopen' });
    await assertFails(batch.commit());
    await assertSucceeds(item(database).update({ status: 'open' }));
    await assertSucceeds(complete(database, { id: 'c2', version: 2, expense: false, charged: false }));
    if (!(await space(database).collection('expenses').get()).empty) throw Error('Recreated a deleted expense');
  });
});
