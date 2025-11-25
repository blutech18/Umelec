/**
 * Script to register a specific leader account
 * Usage: node scripts/register-leader.js
 */

const path = require("path");
const fs = require("fs");

// Try to load firebase-admin from functions/node_modules first
let admin;
try {
  const functionsNodeModules = path.join(__dirname, "..", "functions", "node_modules", "firebase-admin");
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

    if (!serviceAccount) {
      throw new Error("❌ Service account key not found. Please ensure serviceAccountKey.json exists.");
    }

    admin.initializeApp({
      credential: admin.credential.cert(serviceAccount),
    });
  }
} catch (error) {
  console.error("❌ Failed to initialize Firebase Admin:", error.message);
  process.exit(1);
}

const auth = admin.auth();
const firestore = admin.firestore();

// Leader accounts to register - Each leader manages elections for their specific college and course
const LEADER_ACCOUNTS = [
  {
    email: "adominguez.k12152912@umak.edu.ph",
    password: "Leader123@",
    firstName: "Angelo",
    lastName: "Dominguez", 
    college: "College of Engineering and Technology (CET)",
    acronym: "CET",
    course: "Bachelor of Science in Civil Engineering",
    courseCode: "BSCE",
    year: "4th Year",
    role: "LEADER",
    isVerified: true
  },
  {
    email: "jalbaladejo.a12345573@umak.edu.ph",
    password: "Leader123@",
    firstName: "Jericho",
    lastName: "Albaladejo", 
    college: "College of Computing and Information Sciences (CCIS)",
    acronym: "CCIS",
    course: "Bachelor of Science in Information Technology",
    courseCode: "BSIT",
    year: "3rd Year",
    role: "LEADER",
    isVerified: true
  },
  {
    email: "sluzada.a12345610@umak.edu.ph",
    password: "Leader123@",
    firstName: "Sofia",
    lastName: "Luzada", 
    college: "College of Business and Financial Science (CBFS)",
    acronym: "CBFS",
    course: "Bachelor of Science in Business Administration",
    courseCode: "BSBA",
    year: "2nd Year",
    role: "LEADER",
    isVerified: true
  }
];

/**
 * Delete existing user account (both Auth and Firestore)
 */
async function deleteUser(email) {
  try {
    let uid = null;
    
    // Try to get and delete auth user
    try {
      const userRecord = await auth.getUserByEmail(email);
      uid = userRecord.uid;
      await auth.deleteUser(uid);
      console.log(`   ✓ Deleted auth user: ${email}`);
    } catch (error) {
      if (error.code === 'auth/user-not-found') {
        console.log(`   • Auth user not found (skipped): ${email}`);
      } else {
        throw error;
      }
    }

    // Delete Firestore document by UID if we have it
    if (uid) {
      try {
        await firestore.collection("users").doc(uid).delete();
        console.log(`   ✓ Deleted Firestore document (by UID): ${email}`);
      } catch (error) {
        console.log(`   • Firestore document not found (skipped): ${email}`);
      }
    }

    // Also try to delete by email (in case UID doesn't match)
    try {
      const usersByEmail = await firestore.collection("users")
        .where("email", "==", email)
        .get();
      
      if (!usersByEmail.empty) {
        const batch = firestore.batch();
        usersByEmail.docs.forEach((doc) => batch.delete(doc.ref));
        await batch.commit();
        console.log(`   ✓ Deleted ${usersByEmail.size} Firestore document(s) by email: ${email}`);
      }
    } catch (error) {
      // Ignore errors for email-based deletion
    }
  } catch (error) {
    console.error(`   ⚠️  Error deleting user ${email}:`, error.message);
    throw error;
  }
}

/**
 * Delete all existing leader accounts
 */
async function deleteExistingLeaders() {
  console.log("🧹 Deleting existing leader accounts...\n");
  
  for (let i = 0; i < LEADER_ACCOUNTS.length; i++) {
    const account = LEADER_ACCOUNTS[i];
    console.log(`   [${i + 1}/${LEADER_ACCOUNTS.length}] Deleting: ${account.email}`);
    await deleteUser(account.email);
  }
  
  console.log("✓ Existing leader accounts deleted\n");
}

/**
 * Create a new user account
 */
async function createUser(userData) {
  try {
    // Create new auth user
    const userRecord = await auth.createUser({
      email: userData.email,
      password: userData.password,
      emailVerified: userData.isVerified || false,
      displayName: `${userData.firstName} ${userData.lastName}`
    });
    console.log(`✓ Created auth user: ${userData.email}`);

    // Create Firestore document
    const userDoc = {
      email: userData.email,
      firstname: userData.firstName,
      lastname: userData.lastName,
      name: `${userData.firstName} ${userData.lastName}`,
      college: userData.college,
      acronym: userData.acronym,
      role: userData.role,
      isVerified: userData.isVerified || false,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    };

    // Add course-specific fields for leaders
    if (userData.role === "LEADER") {
      userDoc.course = userData.course;
      userDoc.courseCode = userData.courseCode;
      userDoc.year = userData.year;
    }

    // Add student-specific fields for voters
    if (userData.role === "VOTER") {
      userDoc.studentId = userData.studentId;
      userDoc.gender = userData.gender;
      userDoc.year = userData.year;
    }

    await firestore.collection("users").doc(userRecord.uid).set(userDoc);
    console.log(`✓ Created Firestore document for: ${userData.email}`);

    return userRecord.uid;
  } catch (error) {
    console.error(`❌ Error creating user ${userData.email}:`, error.message);
    throw error;
  }
}

/**
 * Main function to register multiple leader accounts
 */
async function registerLeaders() {
  try {
    console.log("🚀 Starting leader accounts registration...\n");
    
    // Step 1: Delete all existing leader accounts
    await deleteExistingLeaders();
    
    // Step 2: Create new leader accounts
    console.log("📝 Creating new leader accounts...\n");
    const registeredAccounts = [];
    
    for (let i = 0; i < LEADER_ACCOUNTS.length; i++) {
      const account = LEADER_ACCOUNTS[i];
      console.log(`📝 Registering leader account ${i + 1}/${LEADER_ACCOUNTS.length}...`);
      console.log(`   Email: ${account.email}`);
      
      const uid = await createUser(account);
      registeredAccounts.push({ ...account, uid });
      
      console.log(`✓ Successfully registered: ${account.firstName} ${account.lastName}\n`);
    }
    
    console.log("✅ All leader accounts registered successfully!");
    console.log("📧 Leader Account Details:");
    console.log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    
    registeredAccounts.forEach((account, index) => {
      console.log(`\n👤 Leader Account ${index + 1}:`);
      console.log(`   Email: ${account.email}`);
      console.log(`   Password: ${account.password}`);
      console.log(`   Name: ${account.firstName} ${account.lastName}`);
      console.log(`   Role: ${account.role}`);
      console.log(`   College: ${account.college} (${account.acronym})`);
      console.log(`   Course: ${account.course} (${account.courseCode})`);
      console.log(`   Year Level: ${account.year}`);
      console.log(`   Verified: ${account.isVerified}`);
      console.log(`   UID: ${account.uid}`);
    });
    
    console.log("\n🎉 You can now log in with any of these leader accounts!");
    
  } catch (error) {
    console.error("❌ Leader registration failed:", error.message);
    process.exit(1);
  }
}

// Run the registration
registerLeaders()
  .then(() => {
    console.log("\n🏁 Script completed successfully!");
    process.exit(0);
  })
  .catch((error) => {
    console.error("❌ Script failed:", error);
    process.exit(1);
  });
