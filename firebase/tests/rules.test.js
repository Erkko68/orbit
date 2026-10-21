import { describe, it, before, after, beforeEach } from 'node:test';
import assert from 'node:assert';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import firebase from 'firebase/compat/app';
import 'firebase/compat/firestore';
import {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} from '@firebase/rules-unit-testing';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const PROJECT_ID = 'demo-orbit';
const serverTime = () => firebase.firestore.FieldValue.serverTimestamp();

describe('Firestore Security Rules', () => {
  let testEnv;

  before(async () => {
    testEnv = await initializeTestEnvironment({
      projectId: PROJECT_ID,
      firestore: {
        rules: fs.readFileSync(path.resolve(__dirname, '../firestore.rules'), 'utf8'),
        host: '127.0.0.1',
        port: 8080,
      },
    });
  });

  after(async () => {
    if (testEnv) {
      await testEnv.cleanup();
    }
  });

  beforeEach(async () => {
    await testEnv.clearFirestore();
  });

  describe('Unauthenticated access', () => {
    it('denies unauthenticated read and write on users', async () => {
      const unauthDb = testEnv.unauthenticatedContext().firestore();
      const readErr = await assertFails(unauthDb.collection('users').doc('u1').get());
      assert.ok(readErr);
      const writeErr = await assertFails(unauthDb.collection('users').doc('u1').set({ displayName: 'Anon' }));
      assert.ok(writeErr);
    });

    it('denies unauthenticated read and write on spaces and subcollections', async () => {
      const unauthDb = testEnv.unauthenticatedContext().firestore();
      const spaceReadErr = await assertFails(unauthDb.collection('spaces').doc('s1').get());
      assert.ok(spaceReadErr);
      const spaceWriteErr = await assertFails(unauthDb.collection('spaces').doc('s1').set({ name: 'Anon' }));
      assert.ok(spaceWriteErr);
      const memberReadErr = await assertFails(unauthDb.collection('spaces').doc('s1').collection('members').get());
      assert.ok(memberReadErr);
      const itemReadErr = await assertFails(unauthDb.collection('spaces').doc('s1').collection('items').get());
      assert.ok(itemReadErr);
    });
  });

  describe('User private profiles', () => {
    it('allows a user to read and write their own profile', async () => {
      const aliceDb = testEnv.authenticatedContext('alice').firestore();
      const writeSuccess = await assertSucceeds(aliceDb.collection('users').doc('alice').set({
        displayName: 'Alice',
        language: 'en',
        timeZone: 'Europe/Madrid',
      }));
      assert.ok(!writeSuccess);
      const doc = await assertSucceeds(aliceDb.collection('users').doc('alice').get());
      assert.strictEqual(doc.data().displayName, 'Alice');
    });

    it('denies a user from reading or writing another user profile', async () => {
      await testEnv.withSecurityRulesDisabled(async (context) => {
        await context.firestore().collection('users').doc('alice').set({ displayName: 'Alice' });
      });
      const bobDb = testEnv.authenticatedContext('bob').firestore();
      const readErr = await assertFails(bobDb.collection('users').doc('alice').get());
      assert.ok(readErr);
      const writeErr = await assertFails(bobDb.collection('users').doc('alice').update({ displayName: 'Hacked' }));
      assert.ok(writeErr);
    });
  });

  describe('Spaces collection', () => {
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

    it('allows space members to read the space', async () => {
      const aliceDb = testEnv.authenticatedContext('alice').firestore();
      const bobDb = testEnv.authenticatedContext('bob').firestore();
      const charlieDb = testEnv.authenticatedContext('charlie').firestore();

      const aliceDoc = await assertSucceeds(aliceDb.collection('spaces').doc('s1').get());
      assert.strictEqual(aliceDoc.data().name, 'Home');

      const bobDoc = await assertSucceeds(bobDb.collection('spaces').doc('s1').get());
      assert.strictEqual(bobDoc.data().name, 'Home');

      const charlieDoc = await assertSucceeds(charlieDb.collection('spaces').doc('s1').get());
      assert.strictEqual(charlieDoc.data().name, 'Home');
    });

    it('denies non-members from reading the space', async () => {
      const davidDb = testEnv.authenticatedContext('david').firestore();
      const err = await assertFails(davidDb.collection('spaces').doc('s1').get());
      assert.ok(err);
    });

    it('allows an authenticated user to create a space when createdBy matches and user is in memberIds', async () => {
      const davidDb = testEnv.authenticatedContext('david').firestore();
      const success = await assertSucceeds(davidDb.collection('spaces').doc('s2').set({
        name: 'David Space',
        accent: 2,
        memberIds: ['david'],
        createdBy: 'david',
        createdAt: serverTime(),
      }));
      assert.ok(!success);
    });

    it('denies creating a space if createdBy does not match authenticated user or user not in memberIds', async () => {
      const davidDb = testEnv.authenticatedContext('david').firestore();
      const failCreatedBy = await assertFails(davidDb.collection('spaces').doc('s3').set({
        name: 'Forged Space',
        accent: 1,
        memberIds: ['david'],
        createdBy: 'alice',
        createdAt: serverTime(),
      }));
      assert.ok(failCreatedBy);

      const failMemberIds = await assertFails(davidDb.collection('spaces').doc('s4').set({
        name: 'Forged Space 2',
        accent: 1,
        memberIds: ['alice'],
        createdBy: 'david',
        createdAt: serverTime(),
      }));
      assert.ok(failMemberIds);
    });

    it('denies a new space with other members, without a name or without a server creation time', async () => {
      const davidDb = testEnv.authenticatedContext('david').firestore();
      const space = { name: 'David Space', accent: 0, memberIds: ['david'], createdBy: 'david', createdAt: serverTime() };

      await assertFails(davidDb.collection('spaces').doc('s5').set({ ...space, memberIds: ['david', 'alice'] }));
      await assertFails(davidDb.collection('spaces').doc('s6').set({ ...space, name: '  ' }));
      await assertFails(davidDb.collection('spaces').doc('s7').set({ ...space, createdAt: new Date(0) }));
    });

    it('denies changing who created the space, even to the owner', async () => {
      const aliceDb = testEnv.authenticatedContext('alice').firestore();
      await assertFails(aliceDb.collection('spaces').doc('s1').update({ createdBy: 'bob' }));
    });

    it('allows owner and admin to update space fields, denies regular members and non-members', async () => {
      const aliceDb = testEnv.authenticatedContext('alice').firestore();
      const bobDb = testEnv.authenticatedContext('bob').firestore();
      const charlieDb = testEnv.authenticatedContext('charlie').firestore();
      const davidDb = testEnv.authenticatedContext('david').firestore();

      const ownerUpdate = await assertSucceeds(aliceDb.collection('spaces').doc('s1').update({ name: 'Home by Alice' }));
      assert.ok(!ownerUpdate);

      const adminUpdate = await assertSucceeds(bobDb.collection('spaces').doc('s1').update({ name: 'Home by Bob' }));
      assert.ok(!adminUpdate);

      const memberUpdate = await assertFails(charlieDb.collection('spaces').doc('s1').update({ name: 'Home by Charlie' }));
      assert.ok(memberUpdate);

      const outsiderUpdate = await assertFails(davidDb.collection('spaces').doc('s1').update({ name: 'Home by David' }));
      assert.ok(outsiderUpdate);
    });

    it('allows only the owner to delete the space', async () => {
      const bobDb = testEnv.authenticatedContext('bob').firestore();
      const charlieDb = testEnv.authenticatedContext('charlie').firestore();
      const davidDb = testEnv.authenticatedContext('david').firestore();
      const aliceDb = testEnv.authenticatedContext('alice').firestore();

      const adminDel = await assertFails(bobDb.collection('spaces').doc('s1').delete());
      assert.ok(adminDel);

      const memberDel = await assertFails(charlieDb.collection('spaces').doc('s1').delete());
      assert.ok(memberDel);

      const outsiderDel = await assertFails(davidDb.collection('spaces').doc('s1').delete());
      assert.ok(outsiderDel);

      const ownerDel = await assertSucceeds(aliceDb.collection('spaces').doc('s1').delete());
      assert.ok(!ownerDel);
    });
  });

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

    it('allows admin to promote member to admin, but denies changing owner', async () => {
      const bobDb = testEnv.authenticatedContext('bob').firestore();

      const promoteMember = await assertSucceeds(
        bobDb.collection('spaces').doc('s1').collection('members').doc('charlie').update({
          role: 'admin',
        })
      );
      assert.ok(!promoteMember);

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

    it('lets an admin change the role of a member and of another admin', async () => {
      await assertSucceeds(setRole('bob', 'charlie', 'admin'));
      await assertSucceeds(setRole('bob', 'charlie', 'member'));
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

    it('allows regular member to delete their own membership to leave, denies deleting owner', async () => {
      const charlieDb = testEnv.authenticatedContext('charlie').firestore();
      const aliceDb = testEnv.authenticatedContext('alice').firestore();

      const leaveSuccess = await assertSucceeds(
        charlieDb.collection('spaces').doc('s1').collection('members').doc('charlie').delete()
      );
      assert.ok(!leaveSuccess);

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
  });

  describe('Items subcollection', () => {
    beforeEach(async () => {
      await testEnv.withSecurityRulesDisabled(async (context) => {
        const db = context.firestore();
        await db.collection('spaces').doc('s1').set({
          name: 'Home',
          accent: 0,
          memberIds: ['alice', 'charlie'],
          createdBy: 'alice',
        });
        await db.collection('spaces').doc('s1').collection('members').doc('alice').set({
          role: 'owner',
          displayName: 'Alice',
        });
        await db.collection('spaces').doc('s1').collection('members').doc('charlie').set({
          role: 'member',
          displayName: 'Charlie',
        });
        await db.collection('spaces').doc('s1').collection('items').doc('item1').set({
          spaceId: 's1',
          title: 'Buy milk',
          status: 'open',
          createdBy: 'alice',
        });
      });
    });

    it('allows space members to read items, denies non-members', async () => {
      const charlieDb = testEnv.authenticatedContext('charlie').firestore();
      const davidDb = testEnv.authenticatedContext('david').firestore();

      const memberRead = await assertSucceeds(
        charlieDb.collection('spaces').doc('s1').collection('items').doc('item1').get()
      );
      assert.strictEqual(memberRead.data().title, 'Buy milk');

      const nonMemberRead = await assertFails(
        davidDb.collection('spaces').doc('s1').collection('items').doc('item1').get()
      );
      assert.ok(nonMemberRead);
    });

    it('allows members to create items with matching spaceId and createdBy', async () => {
      const charlieDb = testEnv.authenticatedContext('charlie').firestore();
      const createSuccess = await assertSucceeds(
        charlieDb.collection('spaces').doc('s1').collection('items').doc('item2').set({
          spaceId: 's1',
          title: 'Clean kitchen',
          status: 'open',
          createdBy: 'charlie',
          createdAt: serverTime(),
        })
      );
      assert.ok(!createSuccess);
    });

    it('denies creating items if user is non-member or createdBy / spaceId do not match', async () => {
      const charlieDb = testEnv.authenticatedContext('charlie').firestore();
      const davidDb = testEnv.authenticatedContext('david').firestore();

      const nonMemberCreate = await assertFails(
        davidDb.collection('spaces').doc('s1').collection('items').doc('item3').set({
          spaceId: 's1',
          title: 'Hacked item',
          status: 'open',
          createdBy: 'david',
          createdAt: serverTime(),
        })
      );
      assert.ok(nonMemberCreate);

      const fakeCreator = await assertFails(
        charlieDb.collection('spaces').doc('s1').collection('items').doc('item4').set({
          spaceId: 's1',
          title: 'Forged item',
          status: 'open',
          createdBy: 'alice',
          createdAt: serverTime(),
        })
      );
      assert.ok(fakeCreator);

      const wrongSpace = await assertFails(
        charlieDb.collection('spaces').doc('s1').collection('items').doc('item5').set({
          spaceId: 'other-space',
          title: 'Wrong space item',
          status: 'open',
          createdBy: 'charlie',
          createdAt: serverTime(),
        })
      );
      assert.ok(wrongSpace);
    });

    it('allows members to update and complete items, denies non-members', async () => {
      const charlieDb = testEnv.authenticatedContext('charlie').firestore();
      const davidDb = testEnv.authenticatedContext('david').firestore();

      const updateSuccess = await assertSucceeds(
        charlieDb.collection('spaces').doc('s1').collection('items').doc('item1').update({
          status: 'done',
          completedBy: 'charlie',
        })
      );
      assert.ok(!updateSuccess);

      const nonMemberUpdate = await assertFails(
        davidDb.collection('spaces').doc('s1').collection('items').doc('item1').update({
          status: 'open',
        })
      );
      assert.ok(nonMemberUpdate);
    });

    it('denies an item without a valid title or that starts done', async () => {
      const charlieDb = testEnv.authenticatedContext('charlie').firestore();
      const items = charlieDb.collection('spaces').doc('s1').collection('items');
      const item = { spaceId: 's1', title: 'Clean kitchen', status: 'open', createdBy: 'charlie', createdAt: serverTime() };

      await assertFails(items.doc('blank').set({ ...item, title: '   ' }));
      await assertFails(items.doc('long').set({ ...item, title: 'x'.repeat(121) }));
      await assertFails(items.doc('done').set({ ...item, status: 'done' }));
      await assertSucceeds(items.doc('longest').set({ ...item, title: 'x'.repeat(120) }));
    });

    it('denies an update that rewrites how the item was created or breaks its fields', async () => {
      const charlieDb = testEnv.authenticatedContext('charlie').firestore();
      const item = charlieDb.collection('spaces').doc('s1').collection('items').doc('item1');

      await assertFails(item.update({ createdBy: 'charlie' }));
      await assertFails(item.update({ spaceId: 'other-space' }));
      await assertFails(item.update({ createdAt: serverTime() }));
      await assertFails(item.update({ title: '' }));
      await assertFails(item.update({ status: 'archived' }));
      await assertFails(item.update({ status: 'done', completedBy: 'alice' }));
    });

    it('lets any member reopen an item', async () => {
      const charlieDb = testEnv.authenticatedContext('charlie').firestore();
      const item = charlieDb.collection('spaces').doc('s1').collection('items').doc('item1');
      await assertSucceeds(item.update({ status: 'done', completedBy: 'charlie' }));

      const aliceDb = testEnv.authenticatedContext('alice').firestore();
      await assertSucceeds(
        aliceDb.collection('spaces').doc('s1').collection('items').doc('item1').update({
          status: 'open',
          completedBy: firebase.firestore.FieldValue.delete(),
        })
      );
    });

    it('lets the creator, an admin or the owner delete an item, and no other member', async () => {
      const charlieDb = testEnv.authenticatedContext('charlie').firestore();
      const aliceDb = testEnv.authenticatedContext('alice').firestore();
      const davidDb = testEnv.authenticatedContext('david').firestore();
      const items = (db) => db.collection('spaces').doc('s1').collection('items');

      await assertFails(items(davidDb).doc('item1').delete());
      await assertFails(items(charlieDb).doc('item1').delete());

      await assertSucceeds(items(charlieDb).doc('mine').set({
        spaceId: 's1',
        title: 'Water plants',
        status: 'open',
        createdBy: 'charlie',
        createdAt: serverTime(),
      }));
      await assertSucceeds(items(charlieDb).doc('mine').delete());

      await assertSucceeds(items(aliceDb).doc('item1').delete());
    });
  });

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

  describe('Invitations', () => {
    const DAY = 24 * 60 * 60 * 1000;
    const inDays = (days) => new Date(Date.now() + days * DAY);

    // A valid invitation to s1 made by alice, with anything in overrides changed.
    const invitation = (overrides = {}) => ({
      spaceId: 's1',
      createdBy: 'alice',
      createdAt: serverTime(),
      expiresAt: inDays(7),
      revoked: false,
      ...overrides,
    });

    beforeEach(async () => {
      await testEnv.withSecurityRulesDisabled(async (context) => {
        const db = context.firestore();
        await db.collection('spaces').doc('s1').set({
          name: 'Home',
          accent: 1,
          memberIds: ['alice', 'bob', 'charlie'],
          createdBy: 'alice',
        });
        await db.collection('spaces').doc('s2').set({
          name: 'Elsewhere',
          accent: 2,
          memberIds: ['david'],
          createdBy: 'david',
        });
        const members = db.collection('spaces').doc('s1').collection('members');
        await members.doc('alice').set({ role: 'owner', displayName: 'Alice' });
        await members.doc('bob').set({ role: 'admin', displayName: 'Bob' });
        await members.doc('charlie').set({ role: 'member', displayName: 'Charlie' });
        await db.collection('spaces').doc('s2').collection('members').doc('david').set({
          role: 'owner',
          displayName: 'David',
        });
        await db.collection('invitations').doc('ABCD2345').set({
          spaceId: 's1',
          createdBy: 'alice',
          createdAt: new Date(),
          expiresAt: inDays(7),
          revoked: false,
        });
      });
    });

    const create = (uid, code, data) =>
      testEnv.authenticatedContext(uid).firestore().collection('invitations').doc(code).set(data);

    it('lets the owner and an admin create an invitation', async () => {
      await assertSucceeds(create('alice', 'WXYZ6789', invitation()));
      await assertSucceeds(create('bob', 'KMNP2345', invitation({ createdBy: 'bob' })));
    });

    it('denies a plain member and a non-member creating one', async () => {
      await assertFails(create('charlie', 'WXYZ6789', invitation({ createdBy: 'charlie' })));
      await assertFails(create('david', 'KMNP2345', invitation({ createdBy: 'david' })));
      await assertFails(testEnv.unauthenticatedContext().firestore()
        .collection('invitations').doc('QRST2345').set(invitation()));
    });

    it('denies an invitation to a space the user does not own or administer', async () => {
      await assertFails(create('alice', 'WXYZ6789', invitation({ spaceId: 's2' })));
    });

    it('denies an invitation made in the name of another user', async () => {
      await assertFails(create('alice', 'WXYZ6789', invitation({ createdBy: 'bob' })));
    });

    it('denies an invitation that is already revoked, has no expiry in range or has other fields', async () => {
      await assertFails(create('alice', 'WXYZ6789', invitation({ revoked: true })));
      await assertFails(create('alice', 'WXYZ6789', invitation({ expiresAt: inDays(-1) })));
      await assertFails(create('alice', 'WXYZ6789', invitation({ expiresAt: inDays(31) })));
      await assertFails(create('alice', 'WXYZ6789', invitation({ expiresAt: 'tomorrow' })));
      await assertFails(create('alice', 'WXYZ6789', invitation({ role: 'admin' })));
      await assertFails(create('alice', 'WXYZ6789', { ...invitation(), createdAt: new Date() }));
      const { expiresAt, ...withoutExpiry } = invitation();
      await assertFails(create('alice', 'WXYZ6789', withoutExpiry));
    });

    it('accepts an expiry up to 30 days away', async () => {
      await assertSucceeds(create('alice', 'WXYZ6789', invitation({ expiresAt: inDays(29) })));
    });

    it('denies codes that are not eight characters of the code alphabet', async () => {
      await assertFails(create('alice', 'ABCD234', invitation()));
      await assertFails(create('alice', 'ABCD23456', invitation()));
      await assertFails(create('alice', 'abcd2345', invitation()));
      await assertFails(create('alice', 'ABCDO345', invitation()));
      await assertFails(create('alice', 'ABCDI345', invitation()));
      await assertFails(create('alice', 'ABCD1345', invitation()));
    });

    it('lets any signed-in user read an invitation by its code, and nobody else', async () => {
      const read = (ctx) => ctx.firestore().collection('invitations').doc('ABCD2345').get();
      const asDavid = await assertSucceeds(read(testEnv.authenticatedContext('david')));
      assert.strictEqual(asDavid.data().spaceId, 's1');
      await assertSucceeds(read(testEnv.authenticatedContext('charlie')));
      await assertFails(read(testEnv.unauthenticatedContext()));
    });

    it('lets the owner and an admin list the invitations of their space only', async () => {
      const list = (uid, spaceId) => testEnv.authenticatedContext(uid).firestore()
        .collection('invitations').where('spaceId', '==', spaceId).get();
      const asAlice = await assertSucceeds(list('alice', 's1'));
      assert.strictEqual(asAlice.size, 1);
      await assertSucceeds(list('bob', 's1'));
      await assertFails(list('charlie', 's1'));
      await assertFails(list('david', 's1'));
      await assertFails(list('alice', 's2'));
    });

    it('denies listing every invitation', async () => {
      await assertFails(testEnv.authenticatedContext('alice').firestore().collection('invitations').get());
    });

    it('lets the owner and an admin revoke an invitation', async () => {
      const revoke = (uid) => testEnv.authenticatedContext(uid).firestore()
        .collection('invitations').doc('ABCD2345').update({ revoked: true });
      await assertSucceeds(revoke('alice'));
      await assertSucceeds(revoke('bob'));
    });

    it('denies a plain member and a non-member revoking one', async () => {
      const revoke = (uid) => testEnv.authenticatedContext(uid).firestore()
        .collection('invitations').doc('ABCD2345').update({ revoked: true });
      await assertFails(revoke('charlie'));
      await assertFails(revoke('david'));
    });

    it('denies any other change to an invitation, and un-revoking it', async () => {
      const ref = testEnv.authenticatedContext('alice').firestore().collection('invitations').doc('ABCD2345');
      await assertFails(ref.update({ expiresAt: inDays(20) }));
      await assertFails(ref.update({ spaceId: 's2' }));
      await assertFails(ref.update({ revoked: true, expiresAt: inDays(20) }));
      await assertFails(ref.update({ revoked: false }));
      await assertSucceeds(ref.update({ revoked: true }));
      await assertFails(ref.update({ revoked: false }));
    });

    it('denies deleting an invitation, even to the owner', async () => {
      await assertFails(testEnv.authenticatedContext('alice').firestore()
        .collection('invitations').doc('ABCD2345').delete());
    });
  });
});
