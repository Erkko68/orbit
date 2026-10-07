import { describe, it, before, after, beforeEach } from 'node:test';
import assert from 'node:assert';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} from '@firebase/rules-unit-testing';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const PROJECT_ID = 'demo-orbit';

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
      }));
      assert.ok(failCreatedBy);

      const failMemberIds = await assertFails(davidDb.collection('spaces').doc('s4').set({
        name: 'Forged Space 2',
        accent: 1,
        memberIds: ['alice'],
        createdBy: 'david',
      }));
      assert.ok(failMemberIds);
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
      }));

      const memberSuccess = await assertSucceeds(
        davidDb.collection('spaces').doc('s2').collection('members').doc('david').set({
          role: 'owner',
          displayName: 'David',
        })
      );
      assert.ok(!memberSuccess);
    });

    it('allows admin and owner to add members or admins, denies normal members and outsiders', async () => {
      const aliceDb = testEnv.authenticatedContext('alice').firestore();
      const bobDb = testEnv.authenticatedContext('bob').firestore();
      const charlieDb = testEnv.authenticatedContext('charlie').firestore();
      const davidDb = testEnv.authenticatedContext('david').firestore();

      const ownerAdds = await assertSucceeds(
        aliceDb.collection('spaces').doc('s1').collection('members').doc('eve').set({
          role: 'member',
          displayName: 'Eve',
        })
      );
      assert.ok(!ownerAdds);

      const adminAdds = await assertSucceeds(
        bobDb.collection('spaces').doc('s1').collection('members').doc('frank').set({
          role: 'admin',
          displayName: 'Frank',
        })
      );
      assert.ok(!adminAdds);

      const memberAdds = await assertFails(
        charlieDb.collection('spaces').doc('s1').collection('members').doc('george').set({
          role: 'member',
          displayName: 'George',
        })
      );
      assert.ok(memberAdds);

      const outsiderAdds = await assertFails(
        davidDb.collection('spaces').doc('s1').collection('members').doc('harry').set({
          role: 'member',
          displayName: 'Harry',
        })
      );
      assert.ok(outsiderAdds);
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

    it('allows owner to change any member role including transferring ownership', async () => {
      const aliceDb = testEnv.authenticatedContext('alice').firestore();

      const transfer = await assertSucceeds(
        aliceDb.collection('spaces').doc('s1').collection('members').doc('bob').update({
          role: 'owner',
        })
      );
      assert.ok(!transfer);
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
        })
      );
      assert.ok(nonMemberCreate);

      const fakeCreator = await assertFails(
        charlieDb.collection('spaces').doc('s1').collection('items').doc('item4').set({
          spaceId: 's1',
          title: 'Forged item',
          status: 'open',
          createdBy: 'alice',
        })
      );
      assert.ok(fakeCreator);

      const wrongSpace = await assertFails(
        charlieDb.collection('spaces').doc('s1').collection('items').doc('item5').set({
          spaceId: 'other-space',
          title: 'Wrong space item',
          status: 'open',
          createdBy: 'charlie',
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

    it('allows members to delete items, denies non-members', async () => {
      const charlieDb = testEnv.authenticatedContext('charlie').firestore();
      const davidDb = testEnv.authenticatedContext('david').firestore();

      const nonMemberDel = await assertFails(
        davidDb.collection('spaces').doc('s1').collection('items').doc('item1').delete()
      );
      assert.ok(nonMemberDel);

      const memberDel = await assertSucceeds(
        charlieDb.collection('spaces').doc('s1').collection('items').doc('item1').delete()
      );
      assert.ok(!memberDel);
    });
  });
});
