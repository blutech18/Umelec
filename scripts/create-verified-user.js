/**
 * Utility script to create/update a verified voter account in both
 * Firebase Authentication and Firestore.
 *
 * Usage (from project root):
 *   node scripts/create-verified-user.js
 */

const path = require("path");
const fs = require("fs");

// Try to load firebase-admin from functions/node_modules first
let admin;
try {
  const functionsNodeModules = path.join(
    __dirname,
    "..",
    "functions",
    "node_modules",
    "firebase-admin"
  );
  if (fs.existsSync(functionsNodeModules)) {
    admin = require(functionsNodeModules);
  } else {
    admin = require("firebase-admin");
  }
} catch (e) {
  admin = require("firebase-admin");
}

// Initialize Firebase Admin
try {
  if (admin.apps.length === 0) {
    let serviceAccount;

    if (process.env.GOOGLE_APPLICATION_CREDENTIALS) {
      serviceAccount = require(process.env.GOOGLE_APPLICATION_CREDENTIALS);
      console.log(`✓ Using service account from: ${process.env.GOOGLE_APPLICATION_CREDENTIALS}`);
    } else {
      const possiblePaths = [
        path.join(__dirname, "..", "serviceAccountKey.json"),
        path.join(process.cwd(), "serviceAccountKey.json"),
        path.join(process.cwd(), "..", "serviceAccountKey.json"),
      ];

      for (const serviceAccountPath of possiblePaths) {
        try {
          serviceAccount = require(serviceAccountPath);
          console.log(`✓ Found service account at: ${serviceAccountPath}`);
          break;
        } catch (e) {
          // Continue to next path
        }
      }
    }

    if (serviceAccount) {
      admin.initializeApp({
        credential: admin.credential.cert(serviceAccount),
      });
    } else {
      admin.initializeApp();
      console.log("✓ Using default Firebase Admin initialization");
    }
  }
} catch (error) {
  console.error("❌ Error initializing Firebase Admin:", error.message);
  process.exit(1);
}

const db = admin.firestore();
const auth = admin.auth();

const USER_DATA = {
  email: "blutech18@umak.edu.ph",
  password: "Password123!",
  firstName: "Blu",
  lastName: "Tech",
  college: "College of Computing and Information Sciences (CCIS)",
  acronym: "CCIS",
  year: "3rd",
  role: "VOTER",
  isVerified: true,
};

async function deleteFirestoreUserDocs(email, uid) {
  if (uid) {
    const userRef = db.collection("users").doc(uid);
    const snapshot = await userRef.get();
    if (snapshot.exists) {
      await userRef.delete();
      console.log(`✓ Deleted Firestore user (by UID): ${email}`);
    }
  }

  const duplicates = await db
    .collection("users")
    .where("email", "==", email)
    .get();

  if (!duplicates.empty) {
    const batch = db.batch();
    duplicates.forEach((doc) => batch.delete(doc.ref));
    await batch.commit();
    console.log(`✓ Deleted ${duplicates.size} Firestore user document(s) by email`);
  } else if (!uid) {
    console.log("• No Firestore documents found for email");
  }
}

async function deleteExistingAccount(email) {
  let uid;
  try {
    const userRecord = await auth.getUserByEmail(email);
    uid = userRecord.uid;
    await auth.deleteUser(uid);
    console.log(`✓ Deleted auth user: ${email}`);
  } catch (error) {
    if (error.code === "auth/user-not-found") {
      console.log(`• Auth user not found (skip delete): ${email}`);
    } else {
      throw error;
    }
  }

  await deleteFirestoreUserDocs(email, uid);
}

async function upsertAuthUser(userData) {
  let userRecord;
  let created = false;

  try {
    userRecord = await auth.getUserByEmail(userData.email);
    console.log(`⚠️  Auth user already exists: ${userData.email}`);
  } catch (error) {
    if (error.code !== "auth/user-not-found") {
      throw error;
    }

    userRecord = await auth.createUser({
      email: userData.email,
      password: userData.password,
      emailVerified: true,
      displayName: `${userData.firstName} ${userData.lastName}`,
    });
    created = true;
    console.log(`✓ Created auth user: ${userData.email}`);
  }

  // Ensure the password, verification, and display name are up to date
  await auth.updateUser(userRecord.uid, {
    password: userData.password,
    emailVerified: true,
    displayName: `${userData.firstName} ${userData.lastName}`,
  });
  return { uid: userRecord.uid, created };
}

async function upsertFirestoreUser(uid, userData) {
  const userRef = db.collection("users").doc(uid);
  const payload = {
    email: userData.email,
    firstname: userData.firstName,
    lastname: userData.lastName,
    college: userData.college,
    acronym: userData.acronym,
    role: userData.role,
    isVerified: userData.isVerified,
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
  };

  // Add year for voters (normalize format: "2nd Year" -> "2nd")
  if (userData.year) {
    payload.year = userData.year.replace(" Year", "").trim();
  }

  await userRef.set(
    {
      ...payload,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    },
    { merge: true }
  );
  console.log(`✓ Synced Firestore user: ${userData.email}`);
}

async function main() {
  console.log("\n👤 Creating verified voter account...\n");
  try {
    console.log("🔄 Clearing existing account (if any)...");
    await deleteExistingAccount(USER_DATA.email);

    const { uid, created } = await upsertAuthUser(USER_DATA);
    await upsertFirestoreUser(uid, USER_DATA);

    console.log("\n✅ Account ready!");
    console.log("   Email: blutech18@umak.edu.ph");
    console.log("   Password: Password123!");
    console.log(`   Status: ${created ? "Created" : "Updated"} in Auth + Firestore`);
    process.exit(0);
  } catch (error) {
    console.error("\n❌ Failed to create/update account:", error.message);
    process.exit(1);
  }
}

main();


