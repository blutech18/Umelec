# UMelec - Electronic Voting System

UMelec is a comprehensive, secure, and transparent electronic voting and election management system designed specifically for educational institutions (specifically tailored for the University of Makati with `@umak.edu.ph` voter verification). The platform enables a cryptographically secure voting lifecycle with digital signatures, role-based administration, real-time analytics, and automated audit logs.

---

## 📋 Table of Contents

- [Overview](#-overview)
- [Key Features](#-key-features)
- [System Architecture](#-system-architecture)
- [Application Flow & Workflows](#-application-flow--workflows)
  - [1. User Registration Flow](#1-user-registration-flow)
  - [2. Secure Voting Flow](#2-secure-voting-flow)
  - [3. Vote Receipt & Cryptographic Verification Flow](#3-vote-receipt--cryptographic-verification-flow)
- [Cryptographic Architecture (DSA Security)](#-cryptographic-architecture-dsa-security)
  - [Canonical Serialization](#canonical-serialization)
  - [Key Pair & Signing Flow](#key-pair--signing-flow)
  - [Public Auditability](#public-auditability)
- [Firestore Database Schema](#-firestore-database-schema)
- [Firebase Security Rules](#-firebase-security-rules)
- [Cloud Functions (Backend Services)](#-cloud-functions-backend-services)
- [Project Directory Structure](#-project-directory-structure)
- [Setup & Deployment Instructions](#-setup--deployment-instructions)
  - [Prerequisites](#prerequisites)
  - [Firebase Configuration](#firebase-configuration)
  - [Android Project Setup](#android-project-setup)
  - [Cloud Functions Setup](#cloud-functions-setup)

---

## 🎯 Overview

UMelec provides a native Android experience for students to vote and student election administrators (leaders/COSEL) to manage the entire election lifecycle. By leveraging modern mobile UX paradigms and back-end cloud platforms, it replaces paper ballots with:
1. **Immutable vote registration** on Cloud Firestore.
2. **Cryptographic signatures** ensuring voter intent cannot be spoofed, tampered with, or updated.
3. **Voter privacy** by decoupling email notifications and PDF receipts from candidates voted for.
4. **Real-time monitoring** for administrators without compromising ballot secrecy.

The platform distinguishes two main roles:
* **VOTER**: Registers with school email, validates identity, casts a single vote per election, reviews candidates, receives verified receipts, and views official results.
* **LEADER (COSEL)**: Manages candidate directories, configures position categories, verifies and controls voter directories, tracks voter turnout, generates PDF audit logs, and triggers reminders.

---

## ✨ Key Features

### For Voters
* **Three-Step Secure Registration**:
  1. Auth Account registration with `@umak.edu.ph` email verification.
  2. Student Info setup with regular expression pattern checks (Student ID: `^[A-Z][0-9]{8}$`, Name, Year, College, Gender).
  3. Digital Signature drawing & Terms/Privacy agreement checkbox controls.
* **Ballot Submission**: Multi-stage voting (Ballot selection $\rightarrow$ Selection review $\rightarrow$ Signature drawing $\rightarrow$ Final prompt).
* **Vote Receipt**: Viewable reference code, timestamp, and signature snippet; download PDF receipt to local storage; or trigger an automated email receipt with the PDF attached.
* **In-App Alerts & Turnout**: Push alerts for submissions and admin notifications; real-time results dashboards.
* **Vote Verification**: Self-service check module to verify vote integrity using a reference code and signature snippet.

### For Leaders (COSEL Administrators)
* **Election Setup**: Set election titles, start/end dates and times, active statuses.
* **Candidate Control**: Add, update, and manage candidates; upload profile photos to Firebase Storage.
* **Voter Control**: Verify register lists, manage student records, view registration metrics.
* **Turnout Analytics**: Monitor ongoing vote tallies in real time using visual charts (`DoughnutChartView`).
* **Automated Audit Logs**: Export master election summaries and audit reports containing cryptographic signature lists.

---

## 🏗️ System Architecture

UMelec is built on a client-server architecture using native Android technologies integrated with Google Firebase:

```
┌────────────────────────────────────────────────────────┐
│                      Android Client                    │
│    ────────────────────────────────────────────────    │
│  - Native Kotlin App (Android SDK 24+)                 │
│  - Material Design UI Components                       │
│  - MVVM-like architecture with custom helper bindings  │
│  - GCACace Signature Pad                               │
└───────────┬───────────────────┬────────────────────────┘
            │                   │
            │ (Auth & Data)     │ (Backend API Calls)
            ▼                   ▼
┌────────────────────────┐┌──────────────────────────────┐
│     Firebase Cloud     ││    Firebase Cloud Functions  │
│  ────────────────────  ││  ──────────────────────────  │
│  - Firebase Auth       ││  - sendEmail (Nodemailer)    │
│  - Cloud Firestore     ││  - generatePasswordResetCode │
│  - Firebase Storage    ││  - getVoteTallies (Secure)   │
└────────────────────────┘└──────────────────────────────┘
```

---

## 📱 Application Flow & Workflows

### 1. User Registration Flow

The user registration is split into a robust 3-stage wizard to prevent database pollution and ensure user verification:

```mermaid
sequenceDiagram
    participant User
    participant RegActivity as Step 1: Credentials (RegisterActivity)
    participant RegActivity2 as Step 2: Student Profile (RegisterActivity2)
    participant RegActivity3 as Step 3: Signature & Legal (RegisterActivity3)
    participant Auth as Firebase Auth
    participant Db as Cloud Firestore

    User->>RegActivity: Enter Email & Password
    Note over RegActivity: Validate email domain (@umak.edu.ph)<br/>Validate password complexity
    RegActivity->>Auth: createUserWithEmailAndPassword()
    Auth-->>RegActivity: Success (UID)
    RegActivity->>RegActivity2: Launch Step 2 (Pass Temporary Credentials)
    
    User->>RegActivity2: Enter Student ID, Names, College, Year, Gender
    Note over RegActivity2: Validate patterns (ID: K12345678)<br/>Auto-capitalize inputs
    RegActivity2->>Db: saveUserDataToFirestore(Merge)
    Db-->>RegActivity2: Success
    RegActivity2->>RegActivity3: Launch Step 3
    
    User->>RegActivity3: Review Terms/Privacy, Draw Handwritten Signature
    RegActivity3->>Db: updateUserDataInFirestore(signature, registrationCompleted: true)
    Db-->>RegActivity3: Success
    RegActivity3->>User: Show Custom Success Dialog -> Redirect to Login
```

* **Step 1 (`RegisterActivity.kt`)**: Validates that the input is a valid school email address (`@umak.edu.ph`). Enforces complex password guidelines (8+ characters, uppercase, lowercase, special character, and digit). It registers the user to Firebase Auth and saves temporary credentials locally. If the user navigates back before finishing Step 3, the temporary account is cleaned up.
* **Step 2 (`RegisterActivity2.kt`)**: Registers student information. Enforces student ID validation rules matching UMak's enrollment card standard (`^[A-Z][0-9]{8}$`).
* **Step 3 (`RegisterActivity3.kt`)**: Users must check boxes representing agreement to the Terms and Conditions and Privacy Policy. They draw a handwritten signature on a signature pad. The signature is captured as a bitmap, compressed to JPEG, encoded to Base64, and merged into the Firestore `/users/{userId}` record alongside a `registrationCompleted: true` flag.

---

### 2. Secure Voting Flow

Voters cast ballots through a controlled flow to prevent multiple votes and ensure intent verification:

```mermaid
sequenceDiagram
    participant Voter
    participant Ballot as Castvote.kt (Ballot Screen)
    participant Review as Castvote2.kt (Review Screen)
    participant Sign as Castvote3.kt (Signature Screen)
    participant Crypto as VoteCryptographyHelper
    participant Db as Cloud Firestore
    participant Function as Firebase Cloud Function (sendEmail)
    participant SMTP as SMTP Email Server

    Voter->>Ballot: Choose Candidate (or Abstain) for each Position
    Ballot->>Review: Launch Castvote2 with selections Bundle
    Voter->>Review: Review selections (Abstains highlighted in Red)
    Voter->>Review: Confirm Selections
    Review->>Sign: Launch Castvote3
    Voter->>Sign: Draw signature & Click Submit
    
    activate Sign
    Sign->>Crypto: generateDSAKeyPair()
    Crypto-->>Sign: Public Key & Private Key (Base64)
    
    Sign->>Crypto: buildVoteDataString(voteId, electionId, selections)
    Crypto-->>Sign: Canonical Vote Data (String)
    
    Sign->>Crypto: signVoteData(canonicalString, privateKey)
    Crypto-->>Sign: Digital Signature
    
    Sign->>Db: Save Vote doc (voteId, selections, digitalSignature, dsaPublicKey, signaturePreview)
    Db-->>Sign: Immutable Vote Saved
    
    Sign->>Function: Call sendEmail(to, emailType=VOTE_CONFIRMATION, pdfBase64, ...)
    deactivate Sign
    
    activate Function
    Note over Function: Render HTML template (protect selections privacy)
    Function->>SMTP: Send Email with PDF Receipt Attachment
    SMTP-->>Function: Success
    Function-->>Sign: Success Response
    deactivate Function
```

* **Ballot Selection (`Castvote.kt`)**: Dynamically queries positions and candidates for the active election from Firestore. Renders a card layout for each position containing radio groups. An "Abstain" choice is automatically appended to each category. Voters must select a candidate or select "Abstain" for every position category. Unselected cards trigger a red warning highlight and block submission.
* **Review Screen (`Castvote2.kt`)**: Summarizes the selections. Highlights "Abstain" selections in red to call out skipped positions, allowing voters to double-check their choices or return to the dynamic ballot.
* **Signature & Submit (`Castvote3.kt`)**: Displays a signature canvas. Once the voter signs, the submission logic executes:
  1. Verifies if the voter has already voted (Firestore read check).
  2. Instantiates `VoteCryptographyHelper` to generate a 2048-bit DSA Key Pair.
  3. Builds a canonical JSON string of the vote selections.
  4. Signs the canonical string with the private key using `SHA256withDSA` to produce a unique base64 digital signature.
  5. Submits the vote payload, including the digital signature, signature preview snippet, and public key, to the `/votes` collection (enforced as immutable via security rules).
  6. Launches the success notification and receipt download sequence.

---

### 3. Vote Receipt & Cryptographic Verification Flow

After voting, the system generates receipts and allows self-service verification:

```mermaid
sequenceDiagram
    participant Verifier
    participant App as Verification.kt / Helper
    participant Db as Cloud Firestore
    participant Crypto as VoteCryptographyHelper

    Verifier->>App: Input Reference Code (voteId) & Signature Snippet
    App->>Db: Fetch Vote Document by voteId
    Db-->>App: Vote Data (selections, digitalSignature, dsaPublicKey)
    
    App->>Crypto: buildVoteDataString(voteId, electionId, selections)
    Crypto-->>App: Canonical Vote Data String
    
    App->>Crypto: verifyVoteSignature(canonicalString, digitalSignature, dsaPublicKey)
    Crypto-->>App: Verification Result (true/false)
    
    App->>Verifier: Display Verification Status (Signature Verified / Failed)
```

1. **Receipt Generation (`ReceiptPdfHelper.kt`)**: Renders a PDF receipt containing:
   * Header with the official commission logo and subtitle ("Commission on Student Election (COSEL)").
   * Election name and PST timestamp.
   * **Vote ID** (Reference Code) and **Digital Signature Snippet** (first 8 characters of the cryptographic signature).
   * **Privacy Protection**: To preserve voter secrecy, the candidate selections are omitted from the PDF receipt, and the in-app confirmation emails contain no selections.
2. **Email Receipt (`EmailService.kt` / Cloud Functions)**: The PDF is converted to a base64 string, sent via HttpsCallable to the `sendEmail` Cloud Function, and dispatched to the voter's verified inbox using SMTP.
3. **Verification Process (`FirestoreVoteHelper.verifyVoteByReferenceCode`)**: Voters can query the status of their vote in the app. The system fetches the corresponding vote document, reconstructs the canonical string of the recorded selections, and verifies the signature using the recorded public key. If the signature matches, it confirms that the ballot has not been tampered with or modified.

---

## 🔒 Cryptographic Architecture (DSA Security)

UMelec uses digital signatures to guarantee election integrity, non-repudiation, and auditability:

```
[Vote Data JSON]
   ├── "voteId": "UUID"
   ├── "electionId": "UUID"
   └── "selections": {"pos_1": {"candidateId": "C1", ...}}
         │
         ▼
  (Sorted Tree Map)
         │
         ▼
[Canonical String] ─────────► [SHA-256 Digest] ──► [DSA Sign (Private Key)] ──► [Digital Signature]
```

### Canonical Serialization
To ensure that signature generation and verification result in identical byte representations, selections are sorted before hashing:
* Selections are loaded into a `TreeMap<String, Map<String, String>>` to enforce sorted position IDs.
* Sub-entries (candidates) are loaded into a `TreeMap<String, String>` to sort key-value pairs alphabetically.
* The output is concatenated as: `voteId|electionId|{SortedSelectionsJson}`.

### Key Pair & Signing Flow
* **Algorithm**: Digital Signature Algorithm (DSA)
* **Key Size**: 2048 bits
* **Signature Algorithm**: `SHA256withDSA`
* **Flow**:
  1. A one-time key pair is generated when the user submits their vote.
  2. The private key signs the canonical representation.
  3. The public key is saved in the vote document.
  4. The private key is discarded, preventing subsequent modifications to the ballot.

### Public Auditability
The public key saved in the database allows anyone to verify that the vote selections match the digital signature. However, because the `voteId` (document path) is a random UUID, arbitrary read sweeps are blocked, maintaining ballot privacy.

---

## 🗄️ Firestore Database Schema

### `users` (User Profiles)
```json
{
  "studentId": "K12345678",
  "firstname": "John",
  "lastname": "Doe",
  "gender": "Male",
  "year": "3rd Year",
  "college": "College of Computing and Information Sciences (CCIS)",
  "email": "johndoe@umak.edu.ph",
  "role": "VOTER | LEADER",
  "signature": "Base64ImageString...",
  "termsAgreed": true,
  "registrationCompleted": true,
  "createdAt": "Timestamp"
}
```

### `elections` (Election Configurations)
```json
{
  "title": "USG Presidential Election 2026",
  "startDate": "Timestamp",
  "endDate": "Timestamp",
  "status": "ACTIVE | INACTIVE",
  "createdAt": "Timestamp"
}
```

### `positions` (Positions under Elections)
```json
{
  "electionId": "election_doc_id",
  "title": "President",
  "rankOrder": 1
}
```

### `candidates` (Candidate Directory)
```json
{
  "electionId": "election_doc_id",
  "positionId": "position_doc_id",
  "name": "Jane Smith",
  "party": "Student Alliance Party",
  "photoUrl": "https://storage.googleapis.com/...",
  "platform": "Detailed platforms...",
  "status": "ACTIVE | INACTIVE"
}
```

### `votes` (Cryptographic Ballot Records)
```json
{
  "voteId": "vote_doc_id",
  "userId": "user_uid",
  "electionId": "election_doc_id",
  "selections": {
    "position_id_1": {
      "candidateId": "candidate_id_1",
      "candidateName": "Jane Smith",
      "positionName": "President"
    }
  },
  "submittedAt": "Timestamp",
  "isVerified": false,
  "dsaPublicKey": "Base64DSAPublicKey...",
  "digitalSignature": "Base64DSADigitalSignature...",
  "signaturePreview": "8CharSigSnippet",
  "signature": "Base64HanddrawnSignature..."
}
```

---

## 🛡️ Firebase Security Rules

Firestore security rules enforce role-based access control and ballot immutability:

* **Users Collection**: Read/write access is restricted to the authenticated owner (`request.auth.uid == userId`). Leaders can read user profiles to compile voter lists and statistics.
* **Elections & Candidates**: Any authenticated user can read configurations. Create, update, and delete actions are restricted to administrators (`isLeader()`).
* **Votes Collection**:
  * Voters can create a vote document if `request.resource.data.userId` matches their UID.
  * Update and delete operations are denied, ensuring votes are immutable.
  * Authenticated users can read their own vote document.
  * Direct access to verifying votes by ID is allowed, but database queries require the exact `voteId` to preserve voter privacy.
  * Leaders can read all votes to compute tallies.

---

## ⚙️ Cloud Functions (Backend Services)

UMelec uses Firebase Cloud Functions (v2 API, Node.js 22 runtime) to secure administrative actions and offload heavy background operations:

1. **`sendEmail` (Callable)**:
   * Connects to a configured SMTP server via Nodemailer.
   * Contains responsive HTML templates for account verification, welcome greetings, and passwords resets.
   * Handles `VOTE_CONFIRMATION` requests: compiles the transaction metadata, attaches the dynamically generated PDF receipt, and emails it to the voter.
2. **`testEmail` (Callable)**:
   * Tests the backend SMTP configuration.
3. **`generatePasswordResetCode` (Callable)**:
   * Generates secure password reset links using the Firebase Admin SDK.
4. **`getVoteTallies` (Callable)**:
   * Aggregates vote counts per candidate.
   * Allows voters to fetch tally statistics without granting direct read access to individual votes, preserving ballot secrecy.

---

## 📁 Project Directory Structure

```
UMelec/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/umelec/
│   │   │   │   ├── Activities/       # Core screen activities (Login, Castvote, Results)
│   │   │   │   ├── Helpers/          # Auth, Cryptography, PDF, and database helpers
│   │   │   │   └── ...
│   │   │   ├── res/
│   │   │   │   ├── layout/           # Material XML views & interfaces
│   │   │   │   ├── drawable/         # Project vector drawables & background layers
│   │   │   │   ├── values/           # Theme resources, color values, strings
│   │   │   │   └── ...
│   │   │   └── AndroidManifest.xml   # Permissions and activity registry
│   │   └── test/                     # Local Junit unit tests
│   ├── build.gradle.kts              # Application build config
│   └── google-services.json          # Client Firebase configuration
├── functions/
│   ├── index.js                      # V2 Cloud Functions (Node.js 22)
│   ├── package.json                  # Firebase Admin, Nodemailer config
│   └── node_modules/
├── firestore.rules                   # Database access configurations
├── storage.rules                     # Cloud Storage access configurations
├── firebase.json                     # Firebase deployment configuration
├── build.gradle.kts                  # Root project configuration
├── settings.gradle.kts               # Dependency management settings
└── gradle/
    └── libs.versions.toml            # Centralized version catalog
```

---

## 🚀 Setup & Deployment Instructions

### Prerequisites
* **Android Development**: Android Studio Hedgehog (or newer) with JDK 17.
* **Node.js Environment**: Node.js 22.x with npm installed.
* **Firebase Tools**: Run `npm install -g firebase-tools` to install the CLI.
* **SMTP Server**: Credentials for a mail server (such as Gmail app passwords or SendGrid).

### Firebase Configuration
1. Open the [Firebase Console](https://console.firebase.google.com/) and create a project.
2. Enable **Authentication** (Email/Password), **Cloud Firestore**, **Firebase Storage**, and **Cloud Functions**.
3. Register your Android app in Firebase using package name `com.example.umelec`.
4. Download `google-services.json` and place it in the `app/` directory.

### Android Project Setup
1. Clone the repository and open the project in Android Studio.
2. Sync the Gradle files.
3. Configure `local.properties` with your Android SDK path:
   ```properties
   sdk.dir=C\:\\Users\\YourUser\\AppData\\Local\\Android\\Sdk
   ```
4. Build the project using the wrapper:
   ```bash
   ./gradlew build
   ```

### Cloud Functions Setup
1. Navigate to the functions folder:
   ```bash
   cd functions
   ```
2. Install the backend dependencies:
   ```bash
   npm install
   ```
3. Set your SMTP environment variables in the Firebase Console or via a `.env` file in the functions directory:
   ```env
   SMTP_HOST=smtp.gmail.com
   SMTP_PORT=587
   SMTP_USER=your-email@umak.edu.ph
   SMTP_PASSWORD=your-app-password
   ```
   Or use a unified connection URL:
   ```env
   SMTP_URL=smtps://username:password@smtp.gmail.com:465
   ```
4. Deploy the functions, firestore rules, and storage rules:
   ```bash
   firebase use --add <your-firebase-project-id>
   firebase deploy
   ```

---
*Developed by the UMelec Development Team. For inquiries regarding electronic voting compliance or cryptographic audit reviews, please contact the administrators.*
