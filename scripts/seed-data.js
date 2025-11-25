/**
 * Comprehensive data seeding script for UMelec
 * Seeds: Users (voters & leaders), Elections, Positions, Candidates, FAQs
 * 
 * Usage (from functions directory):
 *   node ../scripts/seed-data.js
 * 
 * Or from project root:
 *   node scripts/seed-data.js
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
  console.error("❌ Error initializing Firebase Admin:");
  console.error("   Please ensure you have a service account key file.");
  console.error("   Error:", error.message);
  process.exit(1);
}

const db = admin.firestore();
const auth = admin.auth();

// ============================================================================
// SEED DATA CONFIGURATION
// ============================================================================

const SEED_DATA = {
  // Test Users
  users: {
    leaders: [
      {
        email: "leader@umak.edu.ph",
        password: "Leader123@",
        firstName: "Test",
        lastName: "Leader",
        college: "College of Engineering and Technology (CET)",
        acronym: "CET",
        role: "LEADER",
        isVerified: true
      }
    ],
    voters: [
      {
        email: "voter1@umak.edu.ph",
        password: "Voter123@",
        studentId: "K20240001",
        firstName: "John",
        lastName: "Doe",
        gender: "Male",
        college: "College of Engineering and Technology (CET)",
        acronym: "CET",
        year: "3rd Year",
        role: "VOTER",
        isVerified: true,
        shouldVote: true
      },
      {
        email: "voter2@umak.edu.ph",
        password: "Voter123@",
        studentId: "K20240002",
        firstName: "Jane",
        lastName: "Smith",
        gender: "Female",
        college: "College of Engineering and Technology (CET)",
        acronym: "CET",
        year: "4th Year",
        role: "VOTER",
        isVerified: true,
        shouldVote: true
      },
      {
        email: "voter3@umak.edu.ph",
        password: "Voter123@",
        studentId: "K20240003",
        firstName: "Bob",
        lastName: "Johnson",
        gender: "Male",
        college: "College of Engineering and Technology (CET)",
        acronym: "CET",
        year: "2nd Year",
        role: "VOTER",
        isVerified: true,
        shouldVote: false
      },
      {
        email: "voter4@umak.edu.ph",
        password: "Voter123@",
        studentId: "K20240004",
        firstName: "Alice",
        lastName: "Reyes",
        gender: "Female",
        college: "College of Engineering and Technology (CET)",
        acronym: "CET",
        year: "1st Year",
        role: "VOTER",
        isVerified: true,
        shouldVote: false
      },
      {
        email: "voter5@umak.edu.ph",
        password: "Voter123@",
        studentId: "K20240005",
        firstName: "Carlos",
        lastName: "Lim",
        gender: "Male",
        college: "College of Engineering and Technology (CET)",
        acronym: "CET",
        year: "5th Year",
        role: "VOTER",
        isVerified: true,
        shouldVote: false
      }
    ]
  },

  // Election (matches leader's college - CET)
  election: {
    title: "2025 College of Engineering and Technology Student Council Election",
    startDate: new Date(Date.now() - 1 * 24 * 60 * 60 * 1000), // 1 day ago (so it's ONGOING)
    endDate: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000), // 7 days from now
    isAbstainEnabled: true,
    status: "approved", // pending, approved, active, ended - set to approved so it's visible
    college: "College of Engineering and Technology (CET)", // Must match leader's college
    acronym: "CET" // Must match leader's acronym
  },

  // Positions and Candidates
  positions: [
    {
      positionName: "President",
      candidates: [
        { name: "Alice Martinez", courseInfo: "4th Year", credentials: "Computer Engineering, Former Vice President with 3 years of leadership experience in student government", advocacy: "Improving student services and campus facilities through enhanced communication and resource allocation" },
        { name: "Carlos Rodriguez", courseInfo: "4th Year", credentials: "Electrical Engineering, Former Secretary with 2 years of experience in organizational management", advocacy: "Academic excellence and student welfare by promoting tutoring programs and mental health support" },
        { name: "Maria Santos", courseInfo: "3rd Year", credentials: "Civil Engineering, Class Representative with 1 year of experience in student advocacy", advocacy: "Transparency and student engagement through regular town halls and feedback sessions" }
      ]
    },
    {
      positionName: "Vice President",
      candidates: [
        { name: "David Kim", courseInfo: "3rd Year", credentials: "Mechanical Engineering, Former Treasurer with 2 years of financial management experience", advocacy: "Financial transparency and budget management to ensure responsible use of student funds" },
        { name: "Sarah Lee", courseInfo: "4th Year", credentials: "Industrial Engineering, Event Coordinator with 1 year of experience organizing campus activities", advocacy: "Student activities and events to build a vibrant campus community and school spirit" }
      ]
    },
    {
      positionName: "Secretary",
      candidates: [
        { name: "Michael Chen", courseInfo: "2nd Year", credentials: "Computer Engineering, Class Secretary with 1 year of experience in documentation and record-keeping", advocacy: "Efficient communication and documentation to keep students informed and engaged" },
        { name: "Emily Garcia", courseInfo: "3rd Year", credentials: "Electrical Engineering, Student Volunteer with 1 year of community service experience", advocacy: "Student representation and voice to ensure every student's concerns are heard and addressed" }
      ]
    },
    {
      positionName: "Treasurer",
      candidates: [
        { name: "James Wilson", courseInfo: "4th Year", credentials: "Civil Engineering, Former Auditor with 2 years of experience in financial oversight and accountability", advocacy: "Financial accountability and transparency through detailed budget reports and open financial records" },
        { name: "Lisa Anderson", courseInfo: "3rd Year", credentials: "Industrial Engineering, Budget Committee Member with 1 year of experience in financial planning", advocacy: "Wise budget allocation and planning to maximize benefits for all students and campus programs" }
      ]
    }
  ],

  // Notifications
  notifications: [
    {
      title: "Election Reminder (Open)",
      previewText: "University Election is now open for voting.",
      fullText: "The University Election is now open for voting. Please cast your vote before the deadline. Click here to view candidates and make your selections.",
      type: "REMINDER",
      targetUserId: null // null means all users
    },
    {
      title: "Election Reminder (Closed)",
      previewText: "University Election voting period has ended.",
      fullText: "The University Election voting period has ended. Thank you for your participation. Results will be announced soon.",
      type: "REMINDER",
      targetUserId: null
    },
    {
      title: "Vote Submitted",
      previewText: "Your vote has been successfully submitted.",
      fullText: "Your vote has been successfully submitted. Thank you for participating in the election. Your Vote ID will be provided in your receipt.",
      type: "SUBMISSION",
      targetUserId: null // Will be set per user when vote is submitted
    }
  ],

  // FAQs
  faqs: [
    {
      category: "General",
      question: "What is UMelec?",
      answer: "UMelec is the University of Makati's electronic voting system designed to facilitate secure and transparent student council elections."
    },
    {
      category: "General",
      question: "How do I vote?",
      answer: "Log in with your UMAK email account, select your preferred candidates for each position, review your choices, and submit your vote with your digital signature."
    },
    {
      category: "General",
      question: "Can I change my vote after submitting?",
      answer: "No, votes are final once submitted. Please review your selections carefully before confirming your vote."
    },
    {
      category: "Voting",
      question: "What if I don't want to vote for any candidate in a position?",
      answer: "If the abstain option is enabled for the election, you can choose to abstain from voting in specific positions. This option will be available during the voting process."
    },
    {
      category: "Voting",
      question: "How do I know my vote was counted?",
      answer: "After submitting your vote, you will receive a confirmation receipt via email. You can also view your vote receipt in the app."
    },
    {
      category: "Voting",
      question: "What happens if I don't vote?",
      answer: "Voting is your right and responsibility as a student. While not voting won't result in penalties, your participation helps ensure fair representation."
    },
    {
      category: "Technical",
      question: "What if I forget my password?",
      answer: "Use the 'Forgot Password' feature on the login screen. You will receive a password reset link via email."
    },
    {
      category: "Technical",
      question: "Can I vote from my mobile device?",
      answer: "Yes, UMelec is fully accessible on mobile devices. You can vote using the Android app or through a mobile web browser."
    },
    {
      category: "Technical",
      question: "What if I encounter technical issues while voting?",
      answer: "Contact the election committee immediately. Do not attempt to vote multiple times. Technical support will assist you in resolving the issue."
    },
    {
      category: "Election",
      question: "When are the election dates?",
      answer: "Election dates are announced by the election committee. Check the app homepage for the current election period and important dates."
    },
    {
      category: "Election",
      question: "Who can vote?",
      answer: "All registered students with valid UMAK email accounts (@umak.edu.ph) are eligible to vote in their respective college elections."
    },
    {
      category: "Election",
      question: "How are election results determined?",
      answer: "Election results are determined by the candidate with the highest number of votes for each position. Results are announced after the voting period ends."
    }
  ]
};

const COLLECTIONS_TO_CLEAR = ["votes", "candidates", "positions", "elections", "faqs", "users", "notifications"];

// ============================================================================
// HELPER FUNCTIONS
// ============================================================================

async function deleteCollection(collectionName, batchSize = 500) {
  let totalDeleted = 0;
  try {
    while (true) {
      const snapshot = await db.collection(collectionName).limit(batchSize).get();
      if (snapshot.empty) {
        break;
      }

      const batch = db.batch();
      snapshot.docs.forEach((doc) => batch.delete(doc.ref));
      await batch.commit();
      totalDeleted += snapshot.size;
    }
  } catch (error) {
    console.error(`   ⚠️  Error deleting collection ${collectionName}:`, error.message);
    throw error;
  }
  return totalDeleted;
}

async function deleteAuthUserIfExists(email) {
  try {
    const userRecord = await auth.getUserByEmail(email);
    await auth.deleteUser(userRecord.uid);
    console.log(`   ✓ Deleted auth user: ${email}`);
  } catch (error) {
    if (error.code === "auth/user-not-found") {
      console.log(`   • Auth user not found (skipped): ${email}`);
    } else {
      console.error(`   ⚠️  Error deleting auth user ${email}:`, error.message);
      throw error;
    }
  }
}

async function deleteExistingData() {
  console.log("🧹 Deleting existing Firestore data...");
  for (const collectionName of COLLECTIONS_TO_CLEAR) {
    const deletedCount = await deleteCollection(collectionName);
    console.log(`   • ${collectionName}: ${deletedCount} document(s) deleted`);
  }

  console.log("🗑️  Removing seeded auth users...");
  const accounts = [...SEED_DATA.users.leaders, ...SEED_DATA.users.voters];
  for (const account of accounts) {
    await deleteAuthUserIfExists(account.email);
  }
  console.log("✓ Existing data cleared\n");
}

async function createUser(userData) {
  try {
    // Check if user already exists
    let userRecord;
    let isExisting = false;
    try {
      userRecord = await auth.getUserByEmail(userData.email);
      isExisting = true;
      console.log(`   ⚠️  User already exists: ${userData.email}`);
    } catch (error) {
      if (error.code !== "auth/user-not-found") {
        throw error;
      }
    }

    if (!isExisting) {
      // Create new user
      userRecord = await auth.createUser({
        email: userData.email,
        password: userData.password,
        emailVerified: userData.isVerified || false,
        displayName: `${userData.firstName} ${userData.lastName}`,
      });
    }

    // Prepare Firestore user data
    const firestoreUserData = {
      email: userData.email,
      firstname: userData.firstName,
      lastname: userData.lastName,
      college: userData.college,
      acronym: userData.acronym,
      role: userData.role,
      isVerified: userData.isVerified || false,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    };

    if (userData.studentId) {
      firestoreUserData.studentId = userData.studentId;
    }

    if (userData.gender) {
      firestoreUserData.gender = userData.gender;
    }

    // Add year level from registration step
    if (userData.year) {
      firestoreUserData.year = userData.year;
    }

    // Update or create Firestore document
    if (isExisting) {
      // Update existing user (preserve createdAt)
      await db.collection("users").doc(userRecord.uid).update(firestoreUserData);
      console.log(`   ✓ Updated: ${userData.email} (${userData.role})`);
    } else {
      // Create new user
      firestoreUserData.createdAt = admin.firestore.FieldValue.serverTimestamp();
      await db.collection("users").doc(userRecord.uid).set(firestoreUserData);
      console.log(`   ✓ Created: ${userData.email} (${userData.role})`);
    }
    return userRecord.uid;
  } catch (error) {
    console.error(`   ❌ Error creating user ${userData.email}:`, error.message);
    throw error;
  }
}


async function createElection(electionData, leaderUid = null) {
  try {
    const electionDoc = {
      title: electionData.title,
      startDate: admin.firestore.Timestamp.fromDate(electionData.startDate),
      endDate: admin.firestore.Timestamp.fromDate(electionData.endDate),
      isActive: true,
      isAbstainEnabled: electionData.isAbstainEnabled,
      status: electionData.status,
      college: electionData.college || "", // College name (required for filtering)
      acronym: electionData.acronym || "", // College acronym (required for filtering)
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    };

    // Add createdBy field if leader UID is provided
    if (leaderUid) {
      electionDoc.createdBy = leaderUid;
    }

    const docRef = await db.collection("elections").add(electionDoc);
    console.log(`   ✓ Created election: ${electionData.title} (${electionData.college || 'No college specified'})`);
    return docRef.id;
  } catch (error) {
    console.error("   ❌ Error creating election:", error.message);
    throw error;
  }
}

async function createPosition(electionId, positionData) {
  try {
    const positionDoc = {
      electionId: electionId,
      positionName: positionData.positionName,
      isActive: true,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    };

    const docRef = await db.collection("positions").add(positionDoc);
    console.log(`   ✓ Created position: ${positionData.positionName}`);
    return docRef.id;
  } catch (error) {
    console.error(`   ❌ Error creating position ${positionData.positionName}:`, error.message);
    throw error;
  }
}

async function createCandidate(electionId, positionId, positionName, candidateData, createdBy = null) {
  try {
    const candidateDoc = {
      electionId: electionId,
      positionId: positionId,
      positionName: positionName,
      name: candidateData.name,
      isActive: true,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    };

    // Add createdBy field if provided (leader UID)
    if (createdBy) {
      candidateDoc.createdBy = createdBy;
    }

    // Add optional fields if provided
    if (candidateData.courseInfo) {
      candidateDoc.courseInfo = candidateData.courseInfo;
    }
    if (candidateData.credentials) {
      candidateDoc.credentials = candidateData.credentials;
    }
    if (candidateData.advocacy) {
      candidateDoc.advocacy = candidateData.advocacy;
    }
    
    // Add photoUrl field - use provided URL or default avatar
    const DEFAULT_AVATAR_URL = "https://images.icon-icons.com/1378/PNG/512/avatardefault_92824.png";
    candidateDoc.photoUrl = candidateData.photoUrl || DEFAULT_AVATAR_URL;

    const docRef = await db.collection("candidates").add(candidateDoc);
    console.log(`     ✓ Created candidate: ${candidateData.name}`);
    return docRef.id;
  } catch (error) {
    console.error(`   ❌ Error creating candidate ${candidateData.name}:`, error.message);
    throw error;
  }
}

async function createNotification(notificationData, targetUserId = null) {
  try {
    const notificationDoc = {
      title: notificationData.title,
      previewText: notificationData.previewText,
      fullText: notificationData.fullText,
      type: notificationData.type,
      timestamp: admin.firestore.Timestamp.now(),
      readBy: []
    };

    // Add targetUserId if specified
    if (targetUserId) {
      notificationDoc.targetUserId = targetUserId;
    }

    const docRef = await db.collection("notifications").add(notificationDoc);
    console.log(`   ✓ Created notification: ${notificationData.title}`);
    return docRef.id;
  } catch (error) {
    console.error(`   ❌ Error creating notification:`, error.message);
    throw error;
  }
}

async function createFaq(faqData, order) {
  try {
    // Create new FAQ (duplicate checking is handled in the main seeding function)
    const faqDoc = {
      category: faqData.category,
      question: faqData.question,
      answer: faqData.answer,
      order: order,
      isActive: true,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    };

    const docRef = await db.collection("faqs").add(faqDoc);
    console.log(`   ✓ Created FAQ: ${faqData.question.substring(0, 50)}...`);
    return docRef.id;
  } catch (error) {
    console.error(`   ❌ Error creating FAQ:`, error.message);
    throw error;
  }
}

async function createVote(userId, electionId, positionId, candidateId, candidateName, positionName) {
  try {
    // Check if user already voted
    const existingVotes = await db.collection("votes")
      .where("userId", "==", userId)
      .where("electionId", "==", electionId)
      .limit(1)
      .get();

    if (!existingVotes.empty) {
      // User already voted, update the existing vote
      const existingVoteId = existingVotes.docs[0].id;
      const existingVote = existingVotes.docs[0].data();
      const selections = existingVote.selections || {};

      // Add or update selection for this position
      selections[positionId] = {
        candidateId: candidateId,
        candidateName: candidateName,
        positionName: positionName
      };

      await db.collection("votes").doc(existingVoteId).update({
        selections: selections
      });
      return existingVoteId;
    } else {
      // Create new vote
      const voteId = db.collection("votes").doc().id;
      const selections = {
        [positionId]: {
          candidateId: candidateId,
          candidateName: candidateName,
          positionName: positionName
        }
      };

      const voteDoc = {
        voteId: voteId,
        userId: userId,
        electionId: electionId,
        selections: selections,
        submittedAt: admin.firestore.FieldValue.serverTimestamp(),
        isVerified: false
      };

      await db.collection("votes").doc(voteId).set(voteDoc);
      return voteId;
    }
  } catch (error) {
    console.error(`   ❌ Error creating vote:`, error.message);
    throw error;
  }
}

async function createVotesForVoters(electionId, userIds, positionIds, candidatesMap) {
  try {
    console.log("🗳️  Creating votes for voters...");
    let voteCount = 0;
    let votersProcessed = 0;

    // Create votes for each voter
    for (let i = 0; i < userIds.voters.length; i++) {
      const voter = userIds.voters[i];

      if (!voter.shouldVote) {
        console.log(`   ⏭️  Skipped vote creation for ${voter.email} (not voting)`);
        continue;
      }

      const userId = voter.uid;
      votersProcessed++;
      
      // Each voter votes for different candidates to create variety
      const selections = {};
      
      // Voting patterns for variety:
      // Voter 1: Votes for first candidate in each position
      // Voter 2: Votes for second candidate (or first if only one)
      // Voter 3: Votes for last candidate (or first if only one)
      // Additional voters: Rotate through candidates
      
      for (const position of positionIds) {
        const candidates = candidatesMap[position.positionId] || [];
        if (candidates.length > 0) {
          let candidateIndex;
          if (i === 0) {
            candidateIndex = 0; // First voter votes for first candidate
          } else if (i === 1) {
            candidateIndex = Math.min(1, candidates.length - 1); // Second voter votes for second or first
          } else if (i === 2) {
            candidateIndex = candidates.length - 1; // Third voter votes for last candidate
          } else {
            // For additional voters, rotate through candidates
            candidateIndex = i % candidates.length;
          }

          const candidate = candidates[candidateIndex];
          selections[position.positionId] = {
            candidateId: candidate.candidateId,
            candidateName: candidate.name,
            positionName: position.positionName
          };
        }
      }

      // Only create vote if voter has selections for all positions
      if (Object.keys(selections).length === positionIds.length) {
        // Check if vote already exists
        const existingVotes = await db.collection("votes")
          .where("userId", "==", userId)
          .where("electionId", "==", electionId)
          .limit(1)
          .get();

        if (!existingVotes.empty) {
          // Update existing vote
          const existingVoteId = existingVotes.docs[0].id;
          await db.collection("votes").doc(existingVoteId).update({
            selections: selections,
            submittedAt: admin.firestore.FieldValue.serverTimestamp()
          });
          const voterEmail = voter.email || "unknown";
          console.log(`   ✓ Updated vote for voter ${i + 1} (${voterEmail})`);
        } else {
          // Create new vote
          const voteId = db.collection("votes").doc().id;
          const voteDoc = {
            voteId: voteId,
            userId: userId,
            electionId: electionId,
            selections: selections,
            submittedAt: admin.firestore.FieldValue.serverTimestamp(),
            isVerified: false
          };

          await db.collection("votes").doc(voteId).set(voteDoc);
          const voterEmail = voter.email || "unknown";
          console.log(`   ✓ Created vote for voter ${i + 1} (${voterEmail}) - ${Object.keys(selections).length} position(s)`);
        }
        voteCount++;
      } else {
        console.log(`   ⚠️  Skipped voter ${i + 1} - incomplete selections (${Object.keys(selections).length}/${positionIds.length} positions)`);
      }
    }

    console.log(`✓ Created/updated ${voteCount} vote(s) for ${votersProcessed} voter(s) (out of ${userIds.voters.length})\n`);
    return voteCount;
  } catch (error) {
    console.error("   ❌ Error creating votes:", error.message);
    throw error;
  }
}

// ============================================================================
// MAIN SEEDING FUNCTION
// ============================================================================

async function seedData() {
  console.log("\n🌱 Starting data seeding...\n");

  try {
    // 1. Delete existing data
    await deleteExistingData();

    // 2. Create Users
    console.log("📝 Creating users...");
    const userIds = { leaders: [], voters: [] };

    for (const leader of SEED_DATA.users.leaders) {
      const uid = await createUser(leader);
      userIds.leaders.push(uid);
    }

    for (const voter of SEED_DATA.users.voters) {
      const uid = await createUser(voter);
      userIds.voters.push({
        uid,
        email: voter.email,
        shouldVote: voter.shouldVote === true
      });
    }

    console.log(`✓ Created ${userIds.leaders.length} leader(s) and ${userIds.voters.length} voter(s)\n`);

    // 3. Create election (using leader's college)
    console.log("🗳️  Creating election...");
    const leaderUid = userIds.leaders[0]; // Get the first leader's UID
    const leaderCollege = SEED_DATA.users.leaders[0].college;
    const leaderAcronym = SEED_DATA.users.leaders[0].acronym;
    
    // Ensure election data matches leader's college
    const electionData = {
      ...SEED_DATA.election,
      college: leaderCollege,
      acronym: leaderAcronym
    };
    
    const electionId = await createElection(electionData, leaderUid);
    console.log(`✓ Election ID: ${electionId}`);
    console.log(`✓ Election college: ${leaderCollege} (${leaderAcronym})\n`);

    // 4. Create Positions and Candidates
    console.log("👥 Creating positions and candidates...");
    const positionIds = [];
    const candidatesMap = {}; // positionId -> array of {candidateId, name}

    // Get the leader UID to assign as createdBy
    const leaderUid = userIds.leaders[0]; // Use the first leader's UID

    for (const position of SEED_DATA.positions) {
      const positionId = await createPosition(electionId, position);
      positionIds.push({ positionId, positionName: position.positionName });
      candidatesMap[positionId] = [];

      // Create candidates for this position with createdBy field
      for (const candidate of position.candidates) {
        const candidateId = await createCandidate(electionId, positionId, position.positionName, candidate, leaderUid);
        candidatesMap[positionId].push({ candidateId, name: candidate.name });
      }
    }

    console.log(`✓ Created ${positionIds.length} position(s) with candidates\n`);

    // 5. Create FAQs
    console.log("❓ Creating/updating FAQs...");
    let faqOrder = 0;
    let faqCreated = 0;
    let faqUpdated = 0;
    const processedFaqIds = new Set(); // Track which FAQ IDs we've processed
    
    // Create/update FAQs from seed data (check by question, regardless of isActive status)
    for (const faq of SEED_DATA.faqs) {
      // Check for existing FAQ by question (check all FAQs, not just active ones)
      const existingFaqs = await db.collection("faqs")
        .where("question", "==", faq.question)
        .limit(1)
        .get();
      
      if (!existingFaqs.empty) {
        // Update existing FAQ (reactivate if it was inactive)
        const existingFaqId = existingFaqs.docs[0].id;
        await db.collection("faqs").doc(existingFaqId).update({
          category: faq.category,
          answer: faq.answer,
          order: faqOrder++,
          isActive: true,
          updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        });
        processedFaqIds.add(existingFaqId);
        faqUpdated++;
        console.log(`   ✓ Updated FAQ: ${faq.question.substring(0, 50)}...`);
      } else {
        // Create new FAQ
        const faqId = await createFaq(faq, faqOrder++);
        processedFaqIds.add(faqId);
        faqCreated++;
      }
    }
    
    // Mark any remaining active FAQs (not in seed data) as inactive to clean up duplicates
    const allActiveFaqs = await db.collection("faqs")
      .where("isActive", "==", true)
      .get();
    
    if (!allActiveFaqs.empty) {
      const batch = db.batch();
      let batchCount = 0;
      const maxBatchSize = 500;
      let deactivatedCount = 0;
      
      for (const faqDoc of allActiveFaqs.docs) {
        // Only deactivate if we didn't process this FAQ
        if (!processedFaqIds.has(faqDoc.id)) {
          if (batchCount >= maxBatchSize) {
            await batch.commit();
            batch = db.batch();
            batchCount = 0;
          }
          batch.update(faqDoc.ref, { isActive: false });
          batchCount++;
          deactivatedCount++;
        }
      }
      
      if (batchCount > 0) {
        await batch.commit();
      }
      
      if (deactivatedCount > 0) {
        console.log(`   🧹 Deactivated ${deactivatedCount} duplicate/old FAQ(s)`);
      }
    }
    
    console.log(`✓ Processed ${SEED_DATA.faqs.length} FAQ(s) (${faqCreated} created, ${faqUpdated} updated)\n`);

    // 6. Create Notifications
    console.log("🔔 Creating notifications...");
    let notificationCount = 0;
    for (const notification of SEED_DATA.notifications) {
      // For REMINDER notifications, create for all users (targetUserId = null)
      // For SUBMISSION notifications, we'll create them when votes are submitted
      if (notification.type === "REMINDER") {
        await createNotification(notification, null);
        notificationCount++;
      }
      // Note: SUBMISSION notifications are created automatically when votes are submitted
      // So we don't seed them here, but we include them in the seed data structure
      // for reference/documentation purposes
    }
    console.log(`✓ Created ${notificationCount} notification(s) (Election Reminder types)\n`);
    console.log(`   Note: Vote Submitted notifications are created automatically when votes are submitted\n`);

    // 7. Create Votes
    const voteCount = await createVotesForVoters(electionId, userIds, positionIds, candidatesMap);

    // Summary
    console.log("✅ Data seeding completed successfully!\n");
    console.log("📋 Summary:");
    console.log(`   - Users: ${userIds.leaders.length} leader(s), ${userIds.voters.length} voter(s)`);
    console.log(`   - Elections: 1`);
    console.log(`   - Positions: ${positionIds.length}`);
    console.log(`   - Candidates: ${SEED_DATA.positions.reduce((sum, p) => sum + p.candidates.length, 0)}`);
    console.log(`   - FAQs: ${SEED_DATA.faqs.length}`);
    console.log(`   - Notifications: ${notificationCount} (Election Reminder types)`);
    console.log(`   - Votes: ${voteCount}`);
    console.log("\n🎉 You can now test the application with seeded data!");
    console.log("\n📧 Test Accounts:");
    console.log("   Leader: leader@umak.edu.ph / Leader123@");
    console.log("   Voter 1: voter1@umak.edu.ph / Voter123@");
    console.log("   Voter 2: voter2@umak.edu.ph / Voter123@");
    console.log("   Voter 3: voter3@umak.edu.ph / Voter123@");
    console.log("   Voter 4: voter4@umak.edu.ph / Voter123@");
    console.log("   Voter 5: voter5@umak.edu.ph / Voter123@\n");

    process.exit(0);
  } catch (error) {
    console.error("\n❌ Error during seeding:", error);
    process.exit(1);
  }
}

// Run seeding
seedData();

