import { describe, it, beforeEach } from 'node:test';
import assert from 'node:assert';
import { assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { firebase, testEnv } from './setup.js';

describe('Leaving and handing over a space', () => {
  const arrayRemove = (...uids) => firebase.firestore.FieldValue.arrayRemove(...uids);

  beforeEach(async () => {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      const db = context.firestore();
      await db.collection('spaces').doc('s1').set({
        name: 'Home', accent: 1, memberIds: ['alice', 'bob', 'charlie'], createdBy: 'alice', ownerId: 'alice',
      });
      const members = db.collection('spaces').doc('s1').collection('members');
      await members.doc('alice').set({ role: 'owner', displayName: 'Alice' });
      await members.doc('bob').set({ role: 'admin', displayName: 'Bob' });
      await members.doc('charlie').set({ role: 'member', displayName: 'Charlie' });
      await db.collection('spaces').doc('solo').set({
        name: 'Alone', accent: 0, memberIds: ['zed'], createdBy: 'zed', ownerId: 'zed',
      });
      await db.collection('spaces').doc('solo').collection('members').doc('zed').set({ role: 'owner', displayName: 'Zed' });
    });
  });

  const spaceOf = (uid, spaceId = 's1') => testEnv.authenticatedContext(uid).firestore().collection('spaces').doc(spaceId);

  // The batch the app sends to leave: the caller out of memberIds, their name kept, their member document deleted.
  const leave = (uid, { removes = [uid], memberDoc = uid, names = { [`formerMembers.${uid}`]: uid } } = {}) => {
    const db = testEnv.authenticatedContext(uid).firestore();
    const space = db.collection('spaces').doc('s1');
    const batch = db.batch();
    batch.update(space, { memberIds: arrayRemove(...removes), ...names });
    batch.delete(space.collection('members').doc(memberDoc));
    return batch.commit();
  };

  // The batch the app sends to hand the space over: ownerId, the new owner's role and the caller's.
  const transfer = (uid, to, { ownerId = to, newRole = 'owner', ownRole = 'admin' } = {}) => {
    const db = testEnv.authenticatedContext(uid).firestore();
    const space = db.collection('spaces').doc('s1');
    const batch = db.batch();
    batch.update(space, { ownerId });
    batch.update(space.collection('members').doc(to), { role: newRole });
    batch.update(space.collection('members').doc(uid), { role: ownRole });
    return batch.commit();
  };

  const read = async (path) => {
    let data;
    await testEnv.withSecurityRulesDisabled(async (context) => {
      const doc = await context.firestore().doc(path).get();
      data = doc.exists ? doc.data() : null;
    });
    return data;
  };

  it('lets a plain member and an admin leave, and then they cannot read the space', async () => {
    await assertSucceeds(leave('charlie'));
    await assertSucceeds(leave('bob'));

    assert.deepStrictEqual((await read('spaces/s1')).memberIds, ['alice']);
    assert.strictEqual(await read('spaces/s1/members/charlie'), null);
    await assertFails(spaceOf('charlie').get());
  });

  it('keeps the name of whoever leaves, and only theirs', async () => {
    await assertFails(leave('charlie', { names: {} }));
    await assertFails(leave('charlie', { names: { 'formerMembers.charlie': 7 } }));
    await assertFails(leave('charlie', { names: { 'formerMembers.bob': 'Bob' } }));
    await assertFails(leave('charlie', { names: { 'formerMembers.charlie': 'Charlie', 'formerMembers.bob': 'Bob' } }));

    await assertSucceeds(leave('charlie', { names: { 'formerMembers.charlie': 'Charlie' } }));
    await assertSucceeds(leave('bob', { names: { 'formerMembers.bob': 'Bob' } }));

    assert.deepStrictEqual((await read('spaces/s1')).formerMembers, { charlie: 'Charlie', bob: 'Bob' });
  });

  it('refuses either half of leaving on its own', async () => {
    await assertFails(spaceOf('charlie').update({ memberIds: arrayRemove('charlie') }));
    await assertFails(spaceOf('charlie').collection('members').doc('charlie').delete());
    assert.deepStrictEqual((await read('spaces/s1')).memberIds, ['alice', 'bob', 'charlie']);
  });

  it('refuses a member taking someone else out on their way', async () => {
    await assertFails(leave('charlie', { removes: ['charlie', 'bob'] }));
    await assertFails(leave('charlie', { removes: ['bob'], memberDoc: 'bob' }));
  });

  it('refuses the owner leaving a space that has other members', async () => {
    await assertFails(leave('alice'));
    assert.strictEqual((await read('spaces/s1/members/alice')).role, 'owner');
  });

  it('refuses the owner leaving even as the only member: they delete the space instead', async () => {
    const db = testEnv.authenticatedContext('zed').firestore();
    const space = db.collection('spaces').doc('solo');
    const batch = db.batch();
    batch.update(space, { memberIds: arrayRemove('zed') });
    batch.delete(space.collection('members').doc('zed'));
    await assertFails(batch.commit());
  });

  it('lets nobody read the members or items of a space its owner deleted', async () => {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      await context.firestore().collection('spaces').doc('s1').collection('items').doc('i1').set({ title: 'Milk' });
    });
    await assertSucceeds(spaceOf('alice').delete());

    await assertFails(spaceOf('charlie').collection('members').doc('charlie').get());
    await assertFails(spaceOf('charlie').collection('items').doc('i1').get());
    await assertFails(spaceOf('alice').collection('members').doc('alice').get());
  });

  it('lets the owner hand the space to another member and become an admin', async () => {
    await assertSucceeds(transfer('alice', 'charlie'));

    assert.strictEqual((await read('spaces/s1')).ownerId, 'charlie');
    assert.strictEqual((await read('spaces/s1/members/charlie')).role, 'owner');
    assert.strictEqual((await read('spaces/s1/members/alice')).role, 'admin');
  });

  it('lets the new owner hand it on, and the old one no longer can', async () => {
    await assertSucceeds(transfer('alice', 'bob'));

    await assertFails(transfer('alice', 'charlie'));
    await assertSucceeds(transfer('bob', 'alice'));
  });

  it('also hands over a space made before ownerId existed', async () => {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      await context.firestore().collection('spaces').doc('s1').update({
        ownerId: firebase.firestore.FieldValue.delete(),
      });
    });

    await assertSucceeds(transfer('alice', 'bob'));
  });

  it('refuses an admin or a member handing the space over', async () => {
    await assertFails(transfer('bob', 'charlie'));
    await assertFails(transfer('charlie', 'bob'));
  });

  it('refuses a transfer to someone outside the space', async () => {
    await assertFails(transfer('alice', 'dave'));
  });

  it('refuses a transfer that leaves the old owner owner, or names someone else in ownerId', async () => {
    await assertFails(transfer('alice', 'bob', { ownRole: 'owner' }));
    await assertFails(transfer('alice', 'bob', { ownRole: 'member' }));
    await assertFails(transfer('alice', 'bob', { ownerId: 'charlie' }));
  });

  it('refuses each part of a transfer on its own', async () => {
    const members = spaceOf('alice').collection('members');
    await assertFails(spaceOf('alice').update({ ownerId: 'bob' }));
    await assertFails(members.doc('bob').update({ role: 'owner' }));
    await assertFails(members.doc('alice').update({ role: 'admin' }));
    assert.strictEqual((await read('spaces/s1')).ownerId, 'alice');
    assert.strictEqual((await read('spaces/s1/members/alice')).role, 'owner');
  });

  it('refuses an owner or admin changing ownerId with the space settings', async () => {
    await assertFails(spaceOf('alice').update({ name: 'Mine', ownerId: 'bob' }));
    await assertFails(spaceOf('bob').update({ ownerId: 'bob' }));
  });
});
