package com.example.umelec

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import java.util.*

/**
 * Helper class for Firestore Vote operations
 */
object FirestoreVoteHelper {
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance("us-central1")
    private const val VOTES_COLLECTION = "votes"
    private const val TAG = "FirestoreVoteHelper"

    /**
     * Submit a vote to Firestore
     * @param userId User ID from Firebase Auth
     * @param electionId Current election ID
     * @param selections Map of positionId -> candidate data (candidateId, candidateName, positionName)
     * @param signatureBase64 Base64 encoded signature image
     * @param onSuccess Callback with vote ID
     * @param onFailure Callback with error message
     */
    fun submitVote(
        userId: String,
        electionId: String,
        selections: Map<String, Map<String, String>>, // positionId -> (candidateId, candidateName, positionName)
        signatureBase64: String?,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        // First check if user already voted
        firestore.collection(VOTES_COLLECTION)
            .whereEqualTo("userId", userId)
            .whereEqualTo("electionId", electionId)
            .limit(1)
            .get()
            .addOnSuccessListener { existingVotes ->
                if (!existingVotes.isEmpty) {
                    onFailure("You have already voted in this election")
                    return@addOnSuccessListener
                }

                // Generate vote ID
                val voteId = firestore.collection(VOTES_COLLECTION).document().id

                // Generate DSA key pair for vote submission
                val dsaKeyPair = VoteCryptographyHelper.generateDSAKeyPair()
                if (dsaKeyPair == null) {
                    Log.e(TAG, "Failed to generate DSA key pair")
                    onFailure("Failed to generate cryptographic keys for vote submission")
                    return@addOnSuccessListener
                }

                // Create vote data string for signing
                val voteDataString = VoteCryptographyHelper.buildVoteDataString(
                    voteId = voteId,
                    electionId = electionId,
                    selections = selections
                )

                // Sign vote data with DSA private key
                val digitalSignature = VoteCryptographyHelper.signVoteData(
                    voteDataString,
                    dsaKeyPair.privateKey
                )

                if (digitalSignature == null) {
                    Log.e(TAG, "Failed to sign vote data")
                    onFailure("Failed to sign vote data")
                    return@addOnSuccessListener
                }

                // Prepare vote data
                val voteData = hashMapOf<String, Any>(
                    "voteId" to voteId,
                    "userId" to userId,
                    "electionId" to electionId,
                    "selections" to selections,
                    "submittedAt" to com.google.firebase.Timestamp.now(),
                    "isVerified" to false,
                    // Store DSA public key for later verification
                    "dsaPublicKey" to dsaKeyPair.publicKey,
                    // Store digital signature
                    "digitalSignature" to digitalSignature,
                    // Store signature preview (first 8 characters) for receipt verification
                    "signaturePreview" to VoteCryptographyHelper.getSignaturePreview(digitalSignature)
                )

                signatureBase64?.let {
                    voteData["signature"] = it
                }

                // Submit vote
                firestore.collection(VOTES_COLLECTION)
                    .document(voteId)
                    .set(voteData)
                    .addOnSuccessListener {
                        Log.d(TAG, "Vote submitted successfully: $voteId")
                        onSuccess(voteId)
                    }
                    .addOnFailureListener { exception ->
                        Log.e(TAG, "Error submitting vote: ${exception.message}", exception)
                        onFailure(exception.message ?: "Failed to submit vote")
                    }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error checking existing vote: ${exception.message}", exception)
                onFailure(exception.message ?: "Failed to check vote status")
            }
    }

    /**
     * Get vote receipt/verification data
     */
    fun getVoteReceipt(
        userId: String,
        electionId: String,
        onSuccess: (VoteReceipt?) -> Unit,
        onFailure: (String) -> Unit
    ) {
        firestore.collection(VOTES_COLLECTION)
            .whereEqualTo("userId", userId)
            .whereEqualTo("electionId", electionId)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    onSuccess(null)
                    return@addOnSuccessListener
                }

                val doc = documents.documents[0]
                val data = doc.data ?: run {
                    onSuccess(null)
                    return@addOnSuccessListener
                }

                val voteId = data["voteId"] as? String ?: doc.id
                // Use DocumentSnapshot getTimestamp method instead of casting
                val submittedAtTimestamp = doc.getTimestamp("submittedAt")
                val submittedAt = submittedAtTimestamp?.toDate() ?: Date()
                // Use digital signature preview if available, otherwise fall back to signature image preview
                val signaturePreview = data["signaturePreview"] as? String
                    ?: (data["digitalSignature"] as? String)?.take(8)
                    ?: (data["signature"] as? String)?.take(8)
                    ?: ""

                val receipt = VoteReceipt(
                    voteId = voteId,
                    submittedAt = submittedAt,
                    signaturePreview = signaturePreview
                )

                onSuccess(receipt)
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error getting vote receipt: ${exception.message}", exception)
                onFailure(exception.message ?: "Failed to get vote receipt")
            }
    }

    /**
     * Get vote tallies/statistics for ongoing election
     */
    fun getVoteTallies(
        electionId: String,
        onSuccess: (List<VoteTally>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val data = hashMapOf("electionId" to electionId)

        functions
            .getHttpsCallable("getVoteTallies")
            .call(data)
            .addOnSuccessListener { result ->
                val resultData = result.getData() as? Map<*, *> ?: run {
                    onFailure("Invalid response format")
                    return@addOnSuccessListener
                }

                val talliesList = (resultData["tallies"] as? List<*>)?.mapNotNull { entry ->
                    val map = entry as? Map<*, *> ?: return@mapNotNull null
                    val positionId = map["positionId"] as? String ?: return@mapNotNull null
                    val positionName = map["positionName"] as? String ?: positionId
                    val candidateId = map["candidateId"] as? String ?: return@mapNotNull null
                    val candidateName = map["candidateName"] as? String ?: "Unknown"
                    val voteCount = (map["voteCount"] as? Number)?.toInt() ?: 0

                            VoteTally(
                                positionId = positionId,
                        positionName = positionName,
                                candidateId = candidateId,
                        candidateName = candidateName,
                        voteCount = voteCount
                        )
                } ?: emptyList()

                onSuccess(talliesList.sortedBy { it.positionName })
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error getting vote tallies via function: ${exception.message}", exception)
                val message = when (exception) {
                    is FirebaseFunctionsException -> {
                        when (exception.code) {
                            FirebaseFunctionsException.Code.PERMISSION_DENIED -> "You don't have permission to view tallies."
                            else -> exception.message ?: "Failed to get vote tallies"
                        }
                    }
                    else -> exception.message ?: "Failed to get vote tallies"
                }
                onFailure(message)
            }
    }

    /**
     * Get leading candidates for results preview
     */
    fun getLeadingCandidates(
        electionId: String,
        limit: Int = 3,
        onSuccess: (List<LeadingCandidate>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        getVoteTallies(electionId, { tallies ->
            if (tallies.isEmpty()) {
                onSuccess(emptyList())
                return@getVoteTallies
            }
            
            // Group by position and get top candidate per position
            val positionGroups = tallies.groupBy { it.positionName }
            val leadingCandidatesList = mutableListOf<LeadingCandidate>()
            var completedCount = 0
            val totalPositions = positionGroups.size
            
            positionGroups.forEach { (position, positionTallies) ->
                val topCandidate = positionTallies.maxByOrNull { it.voteCount }
                topCandidate?.let { tally ->
                    // Fetch candidate photoUrl from Firestore
                    firestore.collection("candidates")
                        .document(tally.candidateId)
                        .get()
                        .addOnSuccessListener { candidateDoc ->
                            // Get photoUrl from document, handle null or missing field
                            val photoUrl = if (candidateDoc.exists() && candidateDoc.contains("photoUrl")) {
                                candidateDoc.getString("photoUrl")
                            } else {
                                null
                            }
                            // Use default avatar URL if photoUrl is not available or is empty
                            val defaultAvatarUrl = "https://images.icon-icons.com/1378/PNG/512/avatardefault_92824.png"
                            val finalPhotoUrl = if (photoUrl.isNullOrBlank()) {
                                defaultAvatarUrl
                            } else {
                                photoUrl.trim()
                            }
                            Log.d(TAG, "Candidate ${tally.candidateName}: photoUrl=$photoUrl, finalPhotoUrl=$finalPhotoUrl")
                            val leadingCandidate = LeadingCandidate(
                                position = position,
                                name = tally.candidateName,
                                votes = tally.voteCount,
                                profileResId = R.drawable.ic_profile,
                                photoUrl = finalPhotoUrl
                            )
                            leadingCandidatesList.add(leadingCandidate)
                            completedCount++
                            
                            // When all candidates are processed, return the sorted list
                            if (completedCount == totalPositions) {
                                val sorted = leadingCandidatesList
                                    .sortedByDescending { it.votes }
                                    .take(limit)
                                onSuccess(sorted)
                            }
                        }
                        .addOnFailureListener { exception ->
                            // If fetching photoUrl fails, use default avatar URL
                            Log.e(TAG, "Error fetching candidate photoUrl: ${exception.message}")
                            val defaultAvatarUrl = "https://images.icon-icons.com/1378/PNG/512/avatardefault_92824.png"
                            val leadingCandidate = LeadingCandidate(
                                position = position,
                                name = tally.candidateName,
                                votes = tally.voteCount,
                                profileResId = R.drawable.ic_profile,
                                photoUrl = defaultAvatarUrl
                            )
                            leadingCandidatesList.add(leadingCandidate)
                            completedCount++
                            
                            if (completedCount == totalPositions) {
                                val sorted = leadingCandidatesList
                                    .sortedByDescending { it.votes }
                                    .take(limit)
                                onSuccess(sorted)
                            }
                        }
                } ?: run {
                    // No top candidate found for this position
                    completedCount++
                    if (completedCount == totalPositions) {
                        val sorted = leadingCandidatesList
                            .sortedByDescending { it.votes }
                            .take(limit)
                        onSuccess(sorted)
                    }
                }
            }
        }, onFailure)
    }

    /**
     * Verify vote by reference code (voteId) and digital signature snippet
     * @param voteId Reference code (vote ID)
     * @param signatureSnippet First 8 characters of the digital signature
     * @param electionId Optional election ID for additional validation
     * @param onSuccess Callback with verification result (true if verified)
     * @param onFailure Callback with error message
     */
    fun verifyVoteByReferenceCode(
        voteId: String,
        signatureSnippet: String,
        electionId: String? = null,
        onSuccess: (Boolean) -> Unit,
        onFailure: (String) -> Unit
    ) {
        // Access vote directly by document ID (voteId is the document ID)
        // This is more efficient and works better with security rules
        firestore.collection(VOTES_COLLECTION)
            .document(voteId)
            .get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    Log.d(TAG, "No vote found with voteId: $voteId")
                    onSuccess(false)
                    return@addOnSuccessListener
                }

                val data = document.data ?: run {
                    Log.e(TAG, "Vote document has no data for voteId: $voteId")
                    onSuccess(false)
                    return@addOnSuccessListener
                }

                // Verify that the voteId field in the document matches the document ID
                val docVoteId = data["voteId"] as? String
                if (docVoteId != null && docVoteId != voteId) {
                    Log.w(TAG, "VoteId mismatch: document ID is '$voteId' but voteId field is '$docVoteId'")
                    // Continue anyway - the document ID is the authoritative source
                }

                // Optional: Verify election ID matches
                if (electionId != null) {
                    val docElectionId = data["electionId"] as? String
                    if (docElectionId != electionId) {
                        Log.d(TAG, "Election ID mismatch: expected $electionId, got $docElectionId")
                        onSuccess(false)
                        return@addOnSuccessListener
                    }
                }

                // Get stored signature preview - use same fallback logic as PDF generation
                // 1. Try signaturePreview field first
                var storedSignaturePreview = (data["signaturePreview"] as? String)?.trim() ?: ""
                
                // 2. Fallback: if signaturePreview is empty, try getting first 8 chars from digitalSignature
                if (storedSignaturePreview.isEmpty()) {
                    val digitalSignature = (data["digitalSignature"] as? String)?.trim() ?: ""
                    if (digitalSignature.isNotEmpty()) {
                        storedSignaturePreview = digitalSignature.take(8)
                        Log.d(TAG, "Using digitalSignature field for preview (first 8 chars)")
                    }
                }
                
                // 3. Final fallback: generate hash from signature image (same as PDF)
                if (storedSignaturePreview.isEmpty()) {
                    val signatureBase64 = (data["signature"] as? String)?.trim() ?: ""
                    if (signatureBase64.isNotEmpty()) {
                        try {
                            val signatureBytes = android.util.Base64.decode(signatureBase64, android.util.Base64.DEFAULT)
                            val digest = java.security.MessageDigest.getInstance("SHA-256")
                            val hashBytes = digest.digest(signatureBytes)
                            val hashHex = "0x" + hashBytes.joinToString("") { "%02x".format(it) }
                            storedSignaturePreview = hashHex.take(8)
                            Log.d(TAG, "Using signature hash for preview (first 8 chars of SHA-256)")
                        } catch (e: Exception) {
                            Log.e(TAG, "Error generating hash from signature: ${e.message}")
                        }
                    }
                }
                
                if (storedSignaturePreview.isEmpty()) {
                    Log.e(TAG, "No signature preview found for voteId: $voteId. Available fields: ${data.keys}")
                    onSuccess(false)
                    return@addOnSuccessListener
                }
                
                // Trim and normalize both strings before comparison
                // Take first 8 characters and convert to uppercase for case-insensitive comparison
                val normalizedStored = storedSignaturePreview.take(8).trim().uppercase()
                val normalizedProvided = signatureSnippet.take(8).trim().uppercase()
                
                Log.d(TAG, "Comparing signatures - Stored: '$normalizedStored' (length: ${normalizedStored.length}), Provided: '$normalizedProvided' (length: ${normalizedProvided.length})")
                Log.d(TAG, "Full stored signature preview: '$storedSignaturePreview'")
                
                // Compare signature snippets (case-insensitive, trimmed)
                val isMatch = normalizedStored == normalizedProvided

                if (isMatch) {
                    Log.d(TAG, "Vote verification successful for voteId: $voteId")
                } else {
                    Log.d(TAG, "Signature snippet mismatch for voteId: $voteId (stored: ${storedSignaturePreview.take(8)}, provided: ${signatureSnippet.take(8)})")
                }

                onSuccess(isMatch)
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error verifying vote: ${exception.message}", exception)
                onFailure(exception.message ?: "Failed to verify vote")
            }
    }
}

/**
 * Data class for vote receipt
 */
data class VoteReceipt(
    val voteId: String,
    val submittedAt: Date,
    val signaturePreview: String
)

/**
 * Data class for vote tally
 */
data class VoteTally(
    val positionId: String,
    val positionName: String,
    val candidateId: String,
    val candidateName: String,
    val voteCount: Int
)

