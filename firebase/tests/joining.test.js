import { describe, it, beforeEach } from 'node:test';
import assert from 'node:assert';
import { assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { firebase, serverTime, testEnv } from './setup.js';

describe('Joining a space', () => {
  const DAY = 24 * 60 * 60 * 1000;
  const inDays = (days) => new Date(Date.now() + days * DAY);
  const serverTime = () => firebase.firestore.FieldValue.serverTimestamp();
  const arrayUnion = (...uids) => firebase.firestore.FieldValue.arrayUnion(...uids);

  beforeEach(async () => {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      const db = context.firestore();
      await db.collection('spaces').doc('s1').set({
        name: 'Home', accent: 1, memberIds: ['alice', 'bob', 'charlie'], createdBy: 'alice',
      });
      const members = db.collection('spaces').doc('s1').collection('members');
      await members.doc('alice').set({ role: 'owner', displayName: 'Alice' });
      await members.doc('bob').set({ role: 'admin', displayName: 'Bob' });
      await members.doc('charlie').set({ role: 'member', displayName: 'Charlie' });
      await db.collection('spaces').doc('s2').set({
        name: 'Elsewhere', accent: 2, memberIds: ['zed'], createdBy: 'zed',
      });
      await db.collection('spaces').doc('s2').collection('members').doc('zed').set({
        role: 'owner', displayName: 'Zed',
      });
      const invitation = (spaceId, overrides = {}) => ({
        spaceId, createdBy: 'alice', createdAt: new Date(), expiresAt: inDays(7), revoked: false, ...overrides,
      });
      await db.collection('invitations').doc('GOOD2345').set(invitation('s1'));
      await db.collection('invitations').doc('REVK2345').set(invitation('s1', { revoked: true }));
      await db.collection('invitations').doc('OLD22345').set(invitation('s1', { expiresAt: inDays(-1) }));
      await db.collection('invitations').doc('ELSE2345').set(invitation('s2'));
    });
  });

  // The batch the app sends to join: the caller appended to memberIds, and their member document.
  const join = (uid, code, { member = {}, spaceId = 's1', adds = [uid], memberDoc = uid } = {}) => {
    const db = testEnv.authenticatedContext(uid).firestore();
    const space = db.collection('spaces').doc(spaceId);
    const batch = db.batch();
    batch.update(space, { memberIds: arrayUnion(...adds) });
    batch.set(space.collection('members').doc(memberDoc), {
      role: 'member', displayName: uid, joinedAt: serverTime(), invitationCode: code, ...member,
    });
    return batch.commit();
  };

  const memberIds = async () => {
    let ids;
    await testEnv.withSecurityRulesDisabled(async (context) => {
      ids = (await context.firestore().collection('spaces').doc('s1').get()).data().memberIds;
    });
    return ids;
  };

  it('lets someone with a valid invitation join, and then read the space', async () => {
    await assertSucceeds(join('dave', 'GOOD2345'));

    const db = testEnv.authenticatedContext('dave').firestore();
    const space = await assertSucceeds(db.collection('spaces').doc('s1').get());
    assert.deepStrictEqual(space.data().memberIds, ['alice', 'bob', 'charlie', 'dave']);
    const me = await assertSucceeds(db.collection('spaces').doc('s1').collection('members').doc('dave').get());
    assert.strictEqual(me.data().role, 'member');
  });

  it('refuses a revoked invitation, an expired one, an unknown code and one for another space', async () => {
    await assertFails(join('dave', 'REVK2345'));
    await assertFails(join('dave', 'OLD22345'));
    await assertFails(join('dave', 'NOPE2345'));
    await assertFails(join('dave', 'ELSE2345'));
  });

  it('changes nothing when a join is refused', async () => {
    await assertFails(join('dave', 'REVK2345'));

    assert.deepStrictEqual(await memberIds(), ['alice', 'bob', 'charlie']);
  });

  it('refuses a join that is more than the caller becoming a plain member', async () => {
    await assertFails(join('dave', 'GOOD2345', { member: { role: 'admin' } }));
    await assertFails(join('dave', 'GOOD2345', { member: { role: 'owner' } }));
    await assertFails(join('dave', 'GOOD2345', { member: { extra: true } }));
    await assertFails(join('dave', 'GOOD2345', { member: { joinedAt: new Date() } }));
    await assertFails(join('dave', 'GOOD2345', { member: { invitationCode: 'NOPE2345' } }));
    await assertFails(join('dave', 'GOOD2345', { member: { displayName: 42 } }));
  });

  it('refuses a join that adds someone else, or more than one person', async () => {
    await assertFails(join('dave', 'GOOD2345', { adds: ['eve'] }));
    await assertFails(join('dave', 'GOOD2345', { adds: ['dave', 'eve'] }));
    await assertFails(join('dave', 'GOOD2345', { memberDoc: 'eve' }));
  });

  it('refuses either half of the join on its own', async () => {
    const db = testEnv.authenticatedContext('dave').firestore();
    const space = db.collection('spaces').doc('s1');
    await assertFails(space.update({ memberIds: arrayUnion('dave') }));
    await assertFails(space.collection('members').doc('dave').set({
      role: 'member', displayName: 'dave', joinedAt: serverTime(), invitationCode: 'GOOD2345',
    }));
  });

  it('changes nothing when a member joins again', async () => {
    await assertFails(join('charlie', 'GOOD2345'));

    assert.deepStrictEqual(await memberIds(), ['alice', 'bob', 'charlie']);
    const db = testEnv.authenticatedContext('charlie').firestore();
    const me = await assertSucceeds(db.collection('spaces').doc('s1').collection('members').doc('charlie').get());
    assert.strictEqual(me.data().displayName, 'Charlie');
  });

  it('does not let an admin or the owner join again and lose their role', async () => {
    await assertFails(join('bob', 'GOOD2345'));
    await assertFails(join('alice', 'GOOD2345'));

    const db = testEnv.authenticatedContext('bob').firestore();
    const bob = await assertSucceeds(db.collection('spaces').doc('s1').collection('members').doc('bob').get());
    assert.strictEqual(bob.data().role, 'admin');
  });

  it('does not let an owner or admin add a member any other way', async () => {
    const addTo = (uid) => testEnv.authenticatedContext(uid).firestore()
      .collection('spaces').doc('s1').update({ memberIds: arrayUnion('eve') });
    await assertFails(addTo('alice'));
    await assertFails(addTo('bob'));
    const addDoc = (uid) => testEnv.authenticatedContext(uid).firestore()
      .collection('spaces').doc('s1').collection('members').doc('eve').set({ role: 'member', displayName: 'Eve' });
    await assertFails(addDoc('alice'));
    await assertFails(addDoc('bob'));
  });

  it('still lets an owner or admin edit the space without touching who is in it', async () => {
    const db = testEnv.authenticatedContext('bob').firestore();
    await assertSucceeds(db.collection('spaces').doc('s1').update({ name: 'Home sweet home' }));
  });

  it('keeps the invitation and the join time of a member fixed, but not the rest of their profile', async () => {
    await assertSucceeds(join('dave', 'GOOD2345'));
    const me = testEnv.authenticatedContext('dave').firestore()
      .collection('spaces').doc('s1').collection('members').doc('dave');

    await assertFails(me.update({ invitationCode: 'ELSE2345' }));
    await assertFails(me.update({ joinedAt: serverTime() }));
    await assertSucceeds(me.update({ displayName: 'Dave P.' }));
  });
});
