package com.example.umelec

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

/**
 * Helper class for Firestore Vote operations
 */
object FirestoreVoteHelper {
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
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

                // Prepare vote data
                val voteData = hashMapOf<String, Any>(
                    "voteId" to voteId,
                    "userId" to userId,
                    "electionId" to electionId,
                    "selections" to selections,
                    "submittedAt" to com.google.firebase.Timestamp.now(),
                    "isVerified" to false
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
                val signature = data["signature"] as? String

                val receipt = VoteReceipt(
                    voteId = voteId,
                    submittedAt = submittedAt,
                    signaturePreview = signature?.take(8) ?: ""
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
        firestore.collection(VOTES_COLLECTION)
            .whereEqualTo("electionId", electionId)
            .get()
            .addOnSuccessListener { voteDocuments ->
                // Count votes by position and candidate
                val talliesMap = mutableMapOf<String, MutableMap<String, Int>>() // positionId -> (candidateId -> count)
                val positionNamesMap = mutableMapOf<String, String>()
                val candidateNamesMap = mutableMapOf<String, String>()

                voteDocuments.documents.forEach { voteDoc ->
                    val voteData = voteDoc.data ?: return@forEach
                    val selections = voteData["selections"] as? Map<String, Any> ?: return@forEach

                    selections.forEach { (positionId, candidateData) ->
                        val candidateMap = candidateData as? Map<String, Any> ?: return@forEach
                        val candidateId = candidateMap["candidateId"] as? String ?: return@forEach
                        val candidateName = candidateMap["candidateName"] as? String ?: return@forEach
                        val positionName = candidateMap["positionName"] as? String ?: return@forEach

                        positionNamesMap[positionId] = positionName
                        candidateNamesMap[candidateId] = candidateName

                        if (!talliesMap.containsKey(positionId)) {
                            talliesMap[positionId] = mutableMapOf()
                        }
                        talliesMap[positionId]!![candidateId] = 
                            (talliesMap[positionId]!![candidateId] ?: 0) + 1
                    }
                }

                // Convert to VoteTally list
                val tallies = mutableListOf<VoteTally>()
                talliesMap.forEach { (positionId, candidateCounts) ->
                    candidateCounts.forEach { (candidateId, count) ->
                        tallies.add(
                            VoteTally(
                                positionId = positionId,
                                positionName = positionNamesMap[positionId] ?: positionId,
                                candidateId = candidateId,
                                candidateName = candidateNamesMap[candidateId] ?: "Unknown",
                                voteCount = count
                            )
                        )
                    }
                }

                onSuccess(tallies.sortedBy { it.positionName })
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error getting vote tallies: ${exception.message}", exception)
                onFailure(exception.message ?: "Failed to get vote tallies")
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
            // Group by position and get top candidate per position
            val leadingByPosition = tallies
                .groupBy { it.positionName }
                .mapNotNull { (position, positionTallies) ->
                    val topCandidate = positionTallies.maxByOrNull { it.voteCount }
                    topCandidate?.let {
                        LeadingCandidate(
                            position = position,
                            name = it.candidateName,
                            votes = it.voteCount,
                            profileResId = R.drawable.ic_profile
                        )
                    }
                }
                .sortedByDescending { it.votes }
                .take(limit)

            onSuccess(leadingByPosition)
        }, onFailure)
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

