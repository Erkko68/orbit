import { describe, it, beforeEach } from 'node:test';
import assert from 'node:assert';
import { assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { firebase, serverTime, testEnv } from './setup.js';

describe('Members subcollection', () => {
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

  it('allows members to read member documents and denies non-members', async () => {
    const charlieDb = testEnv.authenticatedContext('charlie').firestore();
    const davidDb = testEnv.authenticatedContext('david').firestore();

    const readSuccess = await assertSucceeds(
      charlieDb.collection('spaces').doc('s1').collection('members').doc('alice').get()
    );
    assert.strictEqual(readSuccess.data().displayName, 'Alice');

    const readFail = await assertFails(
      davidDb.collection('spaces').doc('s1').collection('members').doc('alice').get()
    );
    assert.ok(readFail);
  });

  it('allows space creator to create their owner member document', async () => {
    const davidDb = testEnv.authenticatedContext('david').firestore();
    await assertSucceeds(davidDb.collection('spaces').doc('s2').set({
      name: 'David Space',
      accent: 0,
      memberIds: ['david'],
      createdBy: 'david',
      ownerId: 'david',
      createdAt: serverTime(),
    }));

    const memberSuccess = await assertSucceeds(
      davidDb.collection('spaces').doc('s2').collection('members').doc('david').set({
        role: 'owner',
        displayName: 'David',
      })
    );
    assert.ok(!memberSuccess);
  });

  it('denies an admin or the owner adding a member directly: members join through an invitation', async () => {
    const addMember = (uid, memberId, role) => testEnv.authenticatedContext(uid).firestore()
      .collection('spaces').doc('s1').collection('members').doc(memberId).set({ role, displayName: memberId });

    await assertFails(addMember('alice', 'david', 'member'));
    await assertFails(addMember('bob', 'david', 'admin'));
    await assertFails(addMember('charlie', 'david', 'member'));
    await assertFails(addMember('david', 'david', 'member'));
  });

  it('denies admin from adding an owner', async () => {
    const bobDb = testEnv.authenticatedContext('bob').firestore();
    const adminAddsOwner = await assertFails(
      bobDb.collection('spaces').doc('s1').collection('members').doc('eve').set({
        role: 'owner',
        displayName: 'Eve',
      })
    );
    assert.ok(adminAddsOwner);
  });

  it('allows member to edit their own profile copy without changing role', async () => {
    const charlieDb = testEnv.authenticatedContext('charlie').firestore();
    const editSuccess = await assertSucceeds(
      charlieDb.collection('spaces').doc('s1').collection('members').doc('charlie').update({
        displayName: 'Charlie P.',
      })
    );
    assert.ok(!editSuccess);
  });

  it('denies regular member from changing their own role', async () => {
    const charlieDb = testEnv.authenticatedContext('charlie').firestore();
    const promoteSelf = await assertFails(
      charlieDb.collection('spaces').doc('s1').collection('members').doc('charlie').update({
        role: 'admin',
      })
    );
    assert.ok(promoteSelf);
  });

  it('denies an admin changing roles, the owner\'s included', async () => {
    const bobDb = testEnv.authenticatedContext('bob').firestore();

    await assertFails(
      bobDb.collection('spaces').doc('s1').collection('members').doc('charlie').update({
        role: 'admin',
      })
    );

    const changeOwner = await assertFails(
      bobDb.collection('spaces').doc('s1').collection('members').doc('alice').update({
        role: 'member',
      })
    );
    assert.ok(changeOwner);
  });

  // Alice owns s1, bob is an admin and charlie a plain member. Only an update is under test here.
  const setRole = (uid, memberId, role) => testEnv.authenticatedContext(uid).firestore()
    .collection('spaces').doc('s1').collection('members').doc(memberId).update({ role });

  it('lets the owner set a member to admin and an admin back to member', async () => {
    await assertSucceeds(setRole('alice', 'charlie', 'admin'));
    await assertSucceeds(setRole('alice', 'bob', 'member'));
  });

  it('denies an admin making an admin or a member of anyone', async () => {
    await assertFails(setRole('bob', 'charlie', 'admin'));
    await assertSucceeds(setRole('alice', 'charlie', 'admin'));
    await assertFails(setRole('bob', 'charlie', 'member'));
  });

  it('denies anyone making a second owner, the owner included', async () => {
    await assertFails(setRole('alice', 'bob', 'owner'));
    await assertFails(setRole('alice', 'charlie', 'owner'));
    await assertFails(setRole('bob', 'charlie', 'owner'));
    await assertFails(setRole('bob', 'bob', 'owner'));
    await assertFails(setRole('charlie', 'charlie', 'owner'));
  });

  it('denies changing the role of the owner, even by the owner', async () => {
    await assertFails(setRole('alice', 'alice', 'admin'));
    await assertFails(setRole('alice', 'alice', 'member'));
    await assertFails(setRole('bob', 'alice', 'member'));
    await assertFails(setRole('bob', 'alice', 'admin'));
  });

  it('denies a plain member changing any role, their own included', async () => {
    await assertFails(setRole('charlie', 'bob', 'member'));
    await assertFails(setRole('charlie', 'charlie', 'admin'));
  });

  it('denies someone outside the space changing a role', async () => {
    await assertFails(setRole('david', 'charlie', 'admin'));
  });

  it('denies a role that is not one of admin and member', async () => {
    await assertFails(setRole('alice', 'charlie', 'superuser'));
    await assertFails(setRole('alice', 'charlie', 'Admin'));
  });

  it('lets the owner still edit their own profile copy, with the role unchanged', async () => {
    await assertSucceeds(testEnv.authenticatedContext('alice').firestore()
      .collection('spaces').doc('s1').collection('members').doc('alice').update({ displayName: 'Alice P.' }));
  });

  it('denies a member deleting their own membership without leaving memberIds, and the owner deleting theirs', async () => {
    const charlieDb = testEnv.authenticatedContext('charlie').firestore();
    const aliceDb = testEnv.authenticatedContext('alice').firestore();

    await assertFails(charlieDb.collection('spaces').doc('s1').collection('members').doc('charlie').delete());

    const ownerLeavesFail = await assertFails(
      aliceDb.collection('spaces').doc('s1').collection('members').doc('alice').delete()
    );
    assert.ok(ownerLeavesFail);
  });

  it('allows admin to remove regular members but denies removing admins or owner', async () => {
    const bobDb = testEnv.authenticatedContext('bob').firestore();

    const removeMember = await assertSucceeds(
      bobDb.collection('spaces').doc('s1').collection('members').doc('charlie').delete()
    );
    assert.ok(!removeMember);

    const removeOwner = await assertFails(
      bobDb.collection('spaces').doc('s1').collection('members').doc('alice').delete()
    );
    assert.ok(removeOwner);
  });

  it('allows owner to remove non-owner members', async () => {
    const aliceDb = testEnv.authenticatedContext('alice').firestore();
    const removeAdmin = await assertSucceeds(
      aliceDb.collection('spaces').doc('s1').collection('members').doc('bob').delete()
    );
    assert.ok(!removeAdmin);
  });

  describe('away period (#48)', () => {
    const away = (uid, who) =>
      testEnv.authenticatedContext(uid).firestore().collection('spaces').doc('s1').collection('members').doc(who);

    it('lets a member set and clear only their own away days', async () => {
      await assertSucceeds(away('charlie', 'charlie').update({ awayFrom: '2026-11-20', awayUntil: '2026-11-27' }));
      await assertSucceeds(away('charlie', 'charlie').update({ awayUntil: '2026-11-30' }));
      await assertSucceeds(away('charlie', 'charlie').update({
        awayFrom: firebase.firestore.FieldValue.delete(),
        awayUntil: firebase.firestore.FieldValue.delete(),
      }));
    });

    it('lets the owner and an admin set their own away days too', async () => {
      await assertSucceeds(away('alice', 'alice').update({ awayFrom: '2026-11-20', awayUntil: '2026-11-20' }));
      await assertSucceeds(away('bob', 'bob').update({ awayFrom: '2026-11-20', awayUntil: '2026-11-21' }));
    });

    it('denies setting the away days of someone else, owner and admin included', async () => {
      const period = { awayFrom: '2026-11-20', awayUntil: '2026-11-27' };
      await assertFails(away('alice', 'charlie').update(period));
      await assertFails(away('bob', 'charlie').update(period));
      await assertFails(away('charlie', 'alice').update(period));
    });

    it('denies the owner changing a role and the away days of a member in one write', async () => {
      await assertFails(away('alice', 'charlie').update({ role: 'admin', awayFrom: '2026-11-20', awayUntil: '2026-11-27' }));
      await assertSucceeds(away('alice', 'charlie').update({ role: 'admin' }));
    });

    it('denies a period that is half set, ends before it starts or is not a day', async () => {
      await assertFails(away('charlie', 'charlie').update({ awayFrom: '2026-11-20' }));
      await assertFails(away('charlie', 'charlie').update({ awayFrom: '2026-11-27', awayUntil: '2026-11-20' }));
      await assertFails(away('charlie', 'charlie').update({ awayFrom: 'tomorrow', awayUntil: 'next week' }));
      await assertFails(away('charlie', 'charlie').update({ awayFrom: 20261120, awayUntil: 20261127 }));
    });

    it('denies a member changing their role together with their away days', async () => {
      await assertFails(away('charlie', 'charlie').update({ role: 'admin', awayFrom: '2026-11-20', awayUntil: '2026-11-27' }));
    });
  });
});
