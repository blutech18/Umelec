/**
 * Script to register CBFS voters for Sofia Luzada's college
 * Creates 3 voters: 2 who have voted and 1 who hasn't
 * Usage: node scripts/register-cba-voters.js
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
  console.error("❌ Firebase Admin SDK not found. Please install it first:");
  console.error("   cd functions && npm install firebase-admin");
  process.exit(1);
}

// Initialize Firebase Admin SDK
try {
  if (admin.apps.length === 0) {
    // Try to find service account key
    const serviceAccountPaths = [
      path.join(__dirname, "..", "serviceAccountKey.json"),
      path.join(__dirname, "..", "functions", "serviceAccountKey.json"),
      path.join(process.cwd(), "serviceAccountKey.json")
    ];

    let serviceAccount = null;
    for (const keyPath of serviceAccountPaths) {
      if (fs.existsSync(keyPath)) {
        console.log(`✓ Found service account at: ${keyPath}`);
        serviceAccount = require(keyPath);
        break;
      }
    }

    if (!serviceAccount) {
      console.error("❌ Service account key not found. Please ensure serviceAccountKey.json exists.");
      process.exit(1);
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

// Sofia Luzada's account details for reference
// Email: sluzada.a12345610@umak.edu.ph
// College: College of Business and Financial Science (CBFS)
// Acronym: CBFS
// Course: Bachelor of Science in Business Administration (BSBA)

// CBFS voter accounts to register (matching Sofia Luzada's college)
const CBA_VOTERS = [
  {
    email: "mgarcia.s12345001@umak.edu.ph",
    password: "Voter123@",
    firstName: "Maria",
    lastName: "Garcia",
    studentId: "2023-CBFS-001",
    college: "College of Business and Financial Science (CBFS)",
    acronym: "CBFS",
    course: "Bachelor of Science in Business Administration",
    courseCode: "BSBA",
    year: "2nd Year",
    gender: "Female",
    role: "VOTER",
    isVerified: true,
    hasVoted: true  // This voter has voted
  },
  {
    email: "jreyes.s12345002@umak.edu.ph",
    password: "Voter123@",
    firstName: "Juan",
    lastName: "Reyes",
    studentId: "2023-CBFS-002",
    college: "College of Business and Financial Science (CBFS)",
    acronym: "CBFS",
    course: "Bachelor of Science in Business Administration",
    courseCode: "BSBA",
    year: "1st Year",
    gender: "Male",
    role: "VOTER",
    isVerified: true,
    hasVoted: true  // This voter has voted
  },
  {
    email: "asantos.s12345003@umak.edu.ph",
    password: "Voter123@",
    firstName: "Ana",
    lastName: "Santos",
    studentId: "2023-CBFS-003",
    college: "College of Business and Financial Science (CBFS)",
    acronym: "CBFS",
    course: "Bachelor of Science in Business Administration",
    courseCode: "BSBA",
    year: "3rd Year",
    gender: "Female",
    role: "VOTER",
    isVerified: true,
    hasVoted: false  // This voter has NOT voted
  }
];

/**
 * Create or update a user account
 */
async function createUser(userData) {
  try {
    // Check if user already exists
    let userRecord;
    let isExisting = false;
    
    try {
      userRecord = await auth.getUserByEmail(userData.email);
      console.log(`ℹ️  User ${userData.email} already exists, updating...`);
      isExisting = true;
      
      // Update existing user
      await auth.updateUser(userRecord.uid, {
        password: userData.password,
        displayName: `${userData.firstName} ${userData.lastName}`,
        emailVerified: true
      });
      console.log(`✓ Updated auth user: ${userData.email}`);
      
    } catch (error) {
      if (error.code === 'auth/user-not-found') {
        // Create new user
        userRecord = await auth.createUser({
          email: userData.email,
          password: userData.password,
          displayName: `${userData.firstName} ${userData.lastName}`,
          emailVerified: true
        });
        console.log(`✓ Created auth user: ${userData.email}`);
      } else {
        throw error;
      }
    }

    // Create/update Firestore document
    const userDoc = {
      email: userData.email,
      firstname: userData.firstName,
      lastname: userData.lastName,
      name: `${userData.firstName} ${userData.lastName}`,
      studentId: userData.studentId,
      college: userData.college,
      acronym: userData.acronym,
      course: userData.course,
      courseCode: userData.courseCode,
      year: userData.year,
      gender: userData.gender,
      role: userData.role,
      isVerified: userData.isVerified || false,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    };

    await firestore.collection("users").doc(userRecord.uid).set(userDoc, { merge: true });
    console.log(`✓ Created/updated Firestore document for: ${userData.email}`);

    return { uid: userRecord.uid, userData };
  } catch (error) {
    console.error(`❌ Error creating user ${userData.email}:`, error.message);
    throw error;
  }
}

/**
 * Create a sample vote record for voters who have voted
 * Matches Sofia Luzada's college: College of Business and Financial Science (CBFS)
 */
async function createVoteRecord(voterUid, voterData) {
  try {
    // Sofia Luzada's college details for reference
    const SOFIA_COLLEGE = "College of Business and Financial Science (CBFS)";
    const SOFIA_ACRONYM = "CBFS";
    
    // First, try to find an active CBFS election by college name
    let electionsSnapshot = await firestore.collection("elections")
      .where("isActive", "==", true)
      .where("college", "==", SOFIA_COLLEGE)
      .limit(1)
      .get();

    // If not found, try by acronym
    if (electionsSnapshot.empty) {
      electionsSnapshot = await firestore.collection("elections")
        .where("isActive", "==", true)
        .where("acronym", "==", SOFIA_ACRONYM)
        .limit(1)
        .get();
    }

    // If still not found, try to find any active election (fallback)
    if (electionsSnapshot.empty) {
      electionsSnapshot = await firestore.collection("elections")
        .where("isActive", "==", true)
        .limit(1)
        .get();
    }

    if (electionsSnapshot.empty) {
      console.log(`ℹ️  No active election found for ${voterData.email} (${SOFIA_COLLEGE}), skipping vote creation`);
      console.log(`   Note: These voters are registered for Sofia Luzada's college: ${SOFIA_COLLEGE}`);
      return;
    }

    const electionDoc = electionsSnapshot.docs[0];
    const electionId = electionDoc.id;
    const electionData = electionDoc.data();

    // Create a vote record matching Sofia's college
    const voteRecord = {
      userId: voterUid,
      electionId: electionId,
      votes: {}, // Empty votes object - would contain actual vote data
      timestamp: admin.firestore.FieldValue.serverTimestamp(),
      isEncrypted: true,
      college: SOFIA_COLLEGE, // Ensure it matches Sofia's college
      acronym: SOFIA_ACRONYM  // Ensure it matches Sofia's acronym
    };

    await firestore.collection("votes").add(voteRecord);
    console.log(`✓ Created vote record for: ${voterData.email} (Election: ${electionData.title || electionId})`);

  } catch (error) {
    console.error(`❌ Error creating vote record for ${voterData.email}:`, error.message);
  }
}

/**
 * Main function to register CBFS voters
 * These voters are registered for Sofia Luzada's college elections
 */
async function registerCBAVoters() {
  try {
    console.log("🚀 Starting CBFS voter registration...\n");
    console.log("📋 Registering voters for Sofia Luzada's college:");
    console.log("   Leader: sluzada.a12345610@umak.edu.ph");
    console.log("   College: College of Business and Financial Science (CBFS)\n");
    
    const registeredVoters = [];
    
    for (let i = 0; i < CBA_VOTERS.length; i++) {
      const voter = CBA_VOTERS[i];
      console.log(`📝 Registering voter ${i + 1}/${CBA_VOTERS.length}...`);
      console.log(`   Email: ${voter.email}`);
      console.log(`   Student ID: ${voter.studentId}`);
      
      const result = await createUser(voter);
      registeredVoters.push({ ...result.userData, uid: result.uid });
      
      // Create vote record if voter has voted
      if (voter.hasVoted) {
        await createVoteRecord(result.uid, voter);
        console.log(`✓ Voter has voted: ${voter.firstName} ${voter.lastName}`);
      } else {
        console.log(`ℹ️  Voter has NOT voted: ${voter.firstName} ${voter.lastName}`);
      }
      
      console.log(`✓ Successfully registered: ${voter.firstName} ${voter.lastName}\n`);
    }
    
    console.log("✅ All CBFS voters registered successfully!");
    console.log("📧 CBFS Voter Account Details:");
    console.log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    
    registeredVoters.forEach((voter, index) => {
      console.log(`\n👤 Voter Account ${index + 1}:`);
      console.log(`   Email: ${voter.email}`);
      console.log(`   Password: ${voter.password}`);
      console.log(`   Name: ${voter.firstName} ${voter.lastName}`);
      console.log(`   Student ID: ${voter.studentId}`);
      console.log(`   College: ${voter.college} (${voter.acronym})`);
      console.log(`   Course: ${voter.course} (${voter.courseCode})`);
      console.log(`   Year Level: ${voter.year}`);
      console.log(`   Gender: ${voter.gender}`);
      console.log(`   Role: ${voter.role}`);
      console.log(`   Verified: ${voter.isVerified}`);
      console.log(`   Has Voted: ${voter.hasVoted ? '✅ Yes' : '❌ No'}`);
      console.log(`   UID: ${voter.uid}`);
    });
    
    console.log("\n🎉 CBFS voters are ready for Sofia Luzada's elections!");
    console.log("📊 Voting Status Summary:");
    console.log(`   • Voters who have voted: ${registeredVoters.filter(v => v.hasVoted).length}`);
    console.log(`   • Voters who haven't voted: ${registeredVoters.filter(v => !v.hasVoted).length}`);
    console.log(`   • Total CBFS voters: ${registeredVoters.length}`);
    
  } catch (error) {
    console.error("❌ CBFS voter registration failed:", error.message);
    process.exit(1);
  }
}

// Run the registration
registerCBAVoters()
  .then(() => {
    console.log("\n🏁 Script completed successfully!");
    process.exit(0);
  })
  .catch((error) => {
    console.error("❌ Script failed:", error);
    process.exit(1);
  });
