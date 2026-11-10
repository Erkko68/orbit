import { describe, it, beforeEach } from 'node:test';
import assert from 'node:assert';
import { assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { firebase, testEnv } from './setup.js';

describe('Removing a member from a space', () => {
  const arrayRemove = (...uids) => firebase.firestore.FieldValue.arrayRemove(...uids);

  beforeEach(async () => {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      const db = context.firestore();
      await db.collection('spaces').doc('s1').set({
        name: 'Home', accent: 1, memberIds: ['alice', 'bob', 'charlie', 'dana'], createdBy: 'alice', ownerId: 'alice',
      });
      const members = db.collection('spaces').doc('s1').collection('members');
      await members.doc('alice').set({ role: 'owner', displayName: 'Alice' });
      await members.doc('bob').set({ role: 'admin', displayName: 'Bob' });
      await members.doc('charlie').set({ role: 'member', displayName: 'Charlie' });
      await members.doc('dana').set({ role: 'member', displayName: 'Dana' });
    });
  });

  const spaceOf = (uid) => testEnv.authenticatedContext(uid).firestore().collection('spaces').doc('s1');

  // The batch the app sends to remove someone: them out of memberIds, their name kept, their document deleted.
  const remove = (uid, target, { removes = [target], memberDoc = target, names } = {}) => {
    const db = testEnv.authenticatedContext(uid).firestore();
    const space = db.collection('spaces').doc('s1');
    const batch = db.batch();
    batch.update(space, { memberIds: arrayRemove(...removes), ...(names ?? { [`formerMembers.${target}`]: target }) });
    batch.delete(space.collection('members').doc(memberDoc));
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

  it('lets the owner remove an admin and a plain member', async () => {
    await assertSucceeds(remove('alice', 'bob'));
    await assertSucceeds(remove('alice', 'charlie'));

    assert.deepStrictEqual((await read('spaces/s1')).memberIds, ['alice', 'dana']);
    assert.strictEqual(await read('spaces/s1/members/bob'), null);
  });

  it('lets an admin remove a plain member but not another admin or the owner', async () => {
    await assertSucceeds(remove('bob', 'charlie'));

    await assertFails(remove('bob', 'alice'));
    assert.strictEqual((await read('spaces/s1/members/alice')).role, 'owner');
  });

  it('refuses an admin removing another admin', async () => {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      await context.firestore().doc('spaces/s1/members/dana').update({ role: 'admin' });
    });

    await assertFails(remove('bob', 'dana'));
    assert.strictEqual((await read('spaces/s1/members/dana')).role, 'admin');
  });

  it('refuses a plain member removing anyone, and someone outside the space', async () => {
    await assertFails(remove('charlie', 'dana'));
    await assertFails(remove('charlie', 'bob'));
    await assertFails(remove('zed', 'charlie'));

    assert.deepStrictEqual((await read('spaces/s1')).memberIds, ['alice', 'bob', 'charlie', 'dana']);
  });

  it('refuses either half of a removal on its own', async () => {
    await assertFails(spaceOf('alice').update({ memberIds: arrayRemove('charlie'), 'formerMembers.charlie': 'Charlie' }));
    assert.deepStrictEqual((await read('spaces/s1')).memberIds, ['alice', 'bob', 'charlie', 'dana']);
  });

  it('refuses the space settings touching who is in the space', async () => {
    await assertFails(spaceOf('alice').update({ name: 'Ours', memberIds: ['alice'] }));
    await assertFails(spaceOf('bob').update({ memberIds: arrayRemove('charlie') }));
    assert.deepStrictEqual((await read('spaces/s1')).memberIds, ['alice', 'bob', 'charlie', 'dana']);
  });

  it('keeps the removed name, and only theirs', async () => {
    await assertFails(remove('alice', 'charlie', { names: {} }));
    await assertFails(remove('alice', 'charlie', { names: { 'formerMembers.charlie': 7 } }));
    await assertFails(remove('alice', 'charlie', { names: { 'formerMembers.dana': 'Dana' } }));
    await assertFails(remove('alice', 'charlie', {
      names: { 'formerMembers.charlie': 'Charlie', 'formerMembers.dana': 'Dana' },
    }));

    await assertSucceeds(remove('alice', 'charlie', { names: { 'formerMembers.charlie': 'Charlie' } }));
    assert.deepStrictEqual((await read('spaces/s1')).formerMembers, { charlie: 'Charlie' });
  });

  it('refuses taking more than one member out at a time', async () => {
    await assertFails(remove('alice', 'charlie', { removes: ['charlie', 'dana'] }));
    assert.deepStrictEqual((await read('spaces/s1')).memberIds, ['alice', 'bob', 'charlie', 'dana']);
  });

  it('refuses a removal whose member document is someone else', async () => {
    await assertFails(remove('alice', 'charlie', { memberDoc: 'dana' }));
  });

  it('stops a removed member reading the space', async () => {
    await assertSucceeds(remove('alice', 'charlie'));

    await assertFails(spaceOf('charlie').get());
  });
});
