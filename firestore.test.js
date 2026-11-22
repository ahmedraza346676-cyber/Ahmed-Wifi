const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_isp_owner";
const BOB_UID = "bob_isp_owner";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validSubscriberPayload(uid, overrides = {}) {
  const now = new Date(Date.now() - 2000);
  return {
    userId: uid,
    subscriberCode: "NET-1001",
    fullName: "Bilal Ahmed",
    phoneNumber: "0300-1234567",
    cnic: "35202-1234567-1",
    location: "House 14, Street 6, Model Town",
    areaZone: "Model Town",
    packageName: "30 Mbps Fiber",
    status: "ACTIVE",
    monthlyBill: 2500,
    pendingBill: 2500,
    totalPaid: 5000,
    lastBilledMonth: "2026-11",
    notes: "ONU installed near lounge",
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

function validPaymentPayload(uid, subscriberId, overrides = {}) {
  const now = new Date(Date.now() - 1000);
  return {
    userId: uid,
    subscriberId,
    subscriberName: "Bilal Ahmed",
    subscriberCode: "NET-1001",
    amount: 2500,
    billingMonth: "2026-11",
    paymentMethod: "JAZZCASH",
    note: "TID 883920",
    paidAt: now,
    createdAt: now,
    ...overrides,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot list, read, or write subscribers or payments", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("subscribers").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("payments").get());
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("subscribers")
      .doc("sub_1")
      .set(validSubscriberPayload(ALICE_UID))
  );
});

test("Owner: valid Kotlin-aligned subscriber create, profile update, and billing update succeed", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const subRef = aliceDb.collection("users").doc(ALICE_UID).collection("subscribers").doc("sub_1");

  await assertSucceeds(subRef.set(validSubscriberPayload(ALICE_UID)));
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("subscribers").get()
  );

  // Billing action update
  await assertSucceeds(
    subRef.update({
      pendingBill: 0,
      totalPaid: 7500,
      status: "ACTIVE",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Profile edit update
  await assertSucceeds(
    subRef.update({
      phoneNumber: "0321-7654321",
      packageName: "50 Mbps Fiber",
      monthlyBill: 3500,
      updatedAt: new Date(Date.now() - 300),
    })
  );
});

test("Owner: batch payment receipt creation succeeds when subscriber exists; direct payment update fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const subRef = aliceDb.collection("users").doc(ALICE_UID).collection("subscribers").doc("sub_1");
  const payRef = aliceDb.collection("users").doc(ALICE_UID).collection("payments").doc("pay_1");

  await assertSucceeds(subRef.set(validSubscriberPayload(ALICE_UID)));

  const batch = aliceDb.batch();
  batch.set(payRef, validPaymentPayload(ALICE_UID, "sub_1"));
  batch.update(subRef, {
    pendingBill: 0,
    totalPaid: 7500,
    status: "ACTIVE",
    updatedAt: new Date(Date.now() - 500),
  });
  await assertSucceeds(batch.commit());

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("payments").get()
  );

  // Payment receipts are immutable
  await assertFails(payRef.update({ amount: 9999 }));
});

test("Security bounds: reject cross-user access, ghost fields, @DocumentId 'id' field, future timestamps, and invalid status", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  const aliceSubRef = aliceDb.collection("users").doc(ALICE_UID).collection("subscribers").doc("sub_1");
  await assertSucceeds(aliceSubRef.set(validSubscriberPayload(ALICE_UID)));

  // Cross-user isolation
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("subscribers").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("subscribers").doc("sub_1").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("subscribers").doc("sub_1").delete());

  // Ghost field & @DocumentId 'id' field rejection
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("subscribers")
      .doc("sub_bad_id")
      .set(validSubscriberPayload(ALICE_UID, { id: "sub_bad_id" }))
  );
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("subscribers")
      .doc("sub_ghost")
      .set(validSubscriberPayload(ALICE_UID, { isAdmin: true }))
  );

  // Invalid status enum & negative bill
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("subscribers")
      .doc("sub_bad_status")
      .set(validSubscriberPayload(ALICE_UID, { status: "UNKNOWN" }))
  );
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("subscribers")
      .doc("sub_neg_bill")
      .set(validSubscriberPayload(ALICE_UID, { pendingBill: -100 }))
  );

  // Future timestamp tampered
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("subscribers")
      .doc("sub_future")
      .set(validSubscriberPayload(ALICE_UID, { createdAt: new Date(Date.now() + 60000) }))
  );

  // Immutable createdAt mutation rejected
  await assertFails(
    aliceSubRef.update({
      createdAt: new Date(Date.now() - 100),
      updatedAt: new Date(Date.now() - 100),
    })
  );

  // Orphaned payment (missing subscriber) rejected
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("payments")
      .doc("pay_orphan")
      .set(validPaymentPayload(ALICE_UID, "non_existent_sub"))
  );
});
