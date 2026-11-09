import { describe, it, beforeEach } from 'node:test';
import { assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { serverTime, testEnv } from './setup.js';

describe('Expenses subcollection', () => {
  const expenses = (db) => db.collection('spaces').doc('s1').collection('expenses');
  const expense = (fields = {}) => ({
    spaceId: 's1',
    title: 'Groceries',
    paidBy: 'charlie',
    split: { alice: 620, charlie: 620 },
    date: '2026-11-02',
    createdBy: 'charlie',
    createdAt: serverTime(),
    kind: 'expense',
    ...fields,
  });

  beforeEach(async () => {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      const db = context.firestore();
      await db.collection('spaces').doc('s1').set({
        name: 'Home',
        accent: 0,
        memberIds: ['alice', 'bob', 'charlie'],
        createdBy: 'alice',
      });
      await db.collection('spaces').doc('s1').collection('members').doc('alice').set({ role: 'owner' });
      await db.collection('spaces').doc('s1').collection('members').doc('bob').set({ role: 'admin' });
      await db.collection('spaces').doc('s1').collection('members').doc('charlie').set({ role: 'member' });
      await expenses(db).doc('e1').set(expense({ paidBy: 'alice', createdBy: 'alice' }));
    });
  });

  it('allows members to read the ledger, denies non-members', async () => {
    const charlieDb = testEnv.authenticatedContext('charlie').firestore();
    const davidDb = testEnv.authenticatedContext('david').firestore();

    await assertSucceeds(expenses(charlieDb).get());
    await assertFails(expenses(davidDb).get());
  });

  it('allows members to add an expense or a settlement as themselves', async () => {
    const charlieDb = testEnv.authenticatedContext('charlie').firestore();

    await assertSucceeds(expenses(charlieDb).doc('e2').set(expense()));
    await assertSucceeds(expenses(charlieDb).doc('e3').set(expense({ kind: 'settlement', split: { alice: 1240 } })));
    await assertSucceeds(expenses(charlieDb).doc('e4').set(expense({ itemId: 'item1' })));
  });

  it('denies recording a settlement in the name of someone else, who must say they paid it', async () => {
    const charlieDb = testEnv.authenticatedContext('charlie').firestore();

    await assertFails(expenses(charlieDb).doc('e2').set(expense({ kind: 'settlement', paidBy: 'alice', split: { charlie: 620 } })));
    await assertSucceeds(expenses(charlieDb).doc('e3').set(expense({ paidBy: 'alice', split: { charlie: 620 } })));
  });

  it('keeps who paid a settlement when it is edited', async () => {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      await expenses(context.firestore()).doc('s1').set(expense({ kind: 'settlement', split: { alice: 1240 } }));
    });
    const aliceDb = testEnv.authenticatedContext('alice').firestore();

    await assertFails(expenses(aliceDb).doc('s1').update({ paidBy: 'alice' }));
    await assertSucceeds(expenses(aliceDb).doc('s1').update({ date: '2026-11-03' }));
  });

  it('denies an expense from a non-member or that misstates how it was created', async () => {
    const charlieDb = testEnv.authenticatedContext('charlie').firestore();
    const davidDb = testEnv.authenticatedContext('david').firestore();

    await assertFails(expenses(davidDb).doc('e2').set(expense({ createdBy: 'david' })));
    await assertFails(expenses(charlieDb).doc('e2').set(expense({ createdBy: 'alice' })));
    await assertFails(expenses(charlieDb).doc('e2').set(expense({ spaceId: 's2' })));
    await assertFails(expenses(charlieDb).doc('e2').set(expense({ createdAt: new Date(0) })));
  });

  it('denies an expense whose fields have the wrong type', async () => {
    const charlieDb = testEnv.authenticatedContext('charlie').firestore();

    await assertFails(expenses(charlieDb).doc('e2').set(expense({ title: '   ' })));
    await assertFails(expenses(charlieDb).doc('e2').set(expense({ paidBy: 7 })));
    await assertFails(expenses(charlieDb).doc('e2').set(expense({ split: 'half' })));
    await assertFails(expenses(charlieDb).doc('e2').set(expense({ split: {} })));
    await assertFails(expenses(charlieDb).doc('e2').set(expense({ date: 20261102 })));
    await assertFails(expenses(charlieDb).doc('e2').set(expense({ kind: 'gift' })));
    await assertFails(expenses(charlieDb).doc('e2').set(expense({ itemId: 7 })));
  });

  it('lets every member edit an expense, keeping how it was created', async () => {
    const charlieDb = testEnv.authenticatedContext('charlie').firestore();
    const davidDb = testEnv.authenticatedContext('david').firestore();

    await assertSucceeds(expenses(charlieDb).doc('e1').update({ split: { charlie: 1240 }, title: 'Food' }));
    await assertFails(expenses(davidDb).doc('e1').update({ title: 'Mine' }));
    await assertFails(expenses(charlieDb).doc('e1').update({ title: '' }));
    await assertFails(expenses(charlieDb).doc('e1').update({ split: 'all of it' }));
    await assertFails(expenses(charlieDb).doc('e1').update({ createdBy: 'charlie' }));
    await assertFails(expenses(charlieDb).doc('e1').update({ spaceId: 's2' }));
    await assertFails(expenses(charlieDb).doc('e1').update({ kind: 'settlement' }));
    await assertFails(expenses(charlieDb).doc('e1').update({ itemId: 'item1' }));
  });

  it('lets the creator, an admin or the owner delete an expense, and no other member', async () => {
    const charlieDb = testEnv.authenticatedContext('charlie').firestore();
    const bobDb = testEnv.authenticatedContext('bob').firestore();
    const aliceDb = testEnv.authenticatedContext('alice').firestore();
    const davidDb = testEnv.authenticatedContext('david').firestore();

    await assertFails(expenses(davidDb).doc('e1').delete());
    await assertFails(expenses(charlieDb).doc('e1').delete());

    await assertSucceeds(expenses(charlieDb).doc('mine').set(expense()));
    await assertSucceeds(expenses(charlieDb).doc('mine').delete());

    await assertSucceeds(expenses(charlieDb).doc('e2').set(expense()));
    await assertSucceeds(expenses(bobDb).doc('e2').delete());
    await assertSucceeds(expenses(aliceDb).doc('e1').delete());
  });
});
