import { describe, it, beforeEach } from 'node:test';
import { assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { firebase, serverTime, testEnv } from './setup.js';

// An item's planned cost (ADR 0014): checked when it changes, and charged in the completion's batch.
describe('Item cost', () => {
  const cost = { paidBy: 'alice', split: { alice: 600, charlie: 400 } };
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

  it('takes a cost in the shape of an expense, on a new item or a written one', async () => {
    await assertSucceeds(space(db()).collection('items').doc('i2').set({
      spaceId: 's1', title: 'Rent', status: 'open', createdBy: 'charlie', createdAt: serverTime(), modules: { cost },
    }));
    const custom = { ...cost, customAllocation: { alice: false, charlie: true } };
    await assertSucceeds(item(db()).update({ 'modules.cost': custom }));
    await assertSucceeds(item(db()).update({ 'modules.cost': firebase.firestore.FieldValue.delete() }));
  });

  it('refuses a cost that charges someone outside the space or has no shares', async () => {
    for (const invalid of [
      { ...cost, paidBy: 'outsider' },
      { ...cost, split: {} },
      { ...cost, split: { outsider: 1000 } },
      { ...cost, cents: 1000 },
      { ...cost, customAllocation: true },
    ]) {
      await assertFails(item(db()).update({ 'modules.cost': invalid }));
    }
  });

  it('keeps unrelated edits working after someone in the cost left', async () => {
    await testEnv.withSecurityRulesDisabled(async context => space(context.firestore()).update({ memberIds: ['charlie'] }));
    await assertSucceeds(item(db()).update({ title: 'Detergent and soap' }));
    await assertFails(item(db()).update({ 'modules.cost': { ...cost, paidBy: 'charlie' } }));
    await assertSucceeds(item(db()).update({ 'modules.cost': { paidBy: 'charlie', split: { charlie: 1000 } } }));
  });

  it('adds the expense in the completion batch, and reopening keeps the charge', async () => {
    const database = db();
    const batch = database.batch();
    batch.update(item(database), { status: 'done', completedBy: 'charlie', completedAt: serverTime(), costExpenseId: 'e1' });
    batch.set(space(database).collection('completions').doc('c1'), { itemId: 'i1', completedBy: 'charlie', completedAt: serverTime() });
    batch.set(space(database).collection('expenses').doc('e1'), {
      spaceId: 's1', title: 'Detergent', paidBy: 'alice', split: cost.split, date: '2026-11-12', kind: 'expense',
      itemId: 'i1', createdBy: 'charlie', createdAt: serverTime(),
    });
    await assertSucceeds(batch.commit());

    const reopen = database.batch();
    reopen.update(item(database), {
      status: 'open', completedBy: firebase.firestore.FieldValue.delete(), completedAt: firebase.firestore.FieldValue.delete(),
    });
    reopen.set(space(database).collection('completions').doc('r1'), {
      itemId: 'i1', completedBy: 'charlie', completedAt: serverTime(), action: 'reopen',
    });
    await assertSucceeds(reopen.commit());
    if ((await item(database).get()).data().costExpenseId !== 'e1') throw Error('Reopening lost the charge');
  });
});
