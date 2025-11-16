package com.example.umelec

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

/**
 * Helper class for Firestore Candidate operations
 */
object FirestoreCandidateHelper {
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private const val CANDIDATES_COLLECTION = "candidates"
    private const val TAG = "FirestoreCandidateHelper"

    /**
     * Get all candidates for a specific election and position
     */
    fun getCandidatesForPosition(
        electionId: String,
        positionId: String,
        onSuccess: (List<CandidateChoices>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        firestore.collection(CANDIDATES_COLLECTION)
            .whereEqualTo("electionId", electionId)
            .whereEqualTo("positionId", positionId)
            .whereEqualTo("isActive", true)
            .orderBy("name", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val candidates = documents.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    val id = doc.id
                    val name = data["name"] as? String ?: return@mapNotNull null
                    CandidateChoices(id = id, name = name)
                }
                onSuccess(candidates)
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error getting candidates: ${exception.message}", exception)
                onFailure(exception.message ?: "Failed to get candidates")
            }
    }

    /**
     * Get candidate details including courseInfo for Position.kt
     */
    fun getCandidateDetails(
        candidateId: String,
        onSuccess: (Map<String, String>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        firestore.collection(CANDIDATES_COLLECTION)
            .document(candidateId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val data = document.data ?: run {
                        onFailure("Candidate data is null")
                        return@addOnSuccessListener
                    }
                    val details = mapOf(
                        "candidateId" to document.id,
                        "name" to (data["name"] as? String ?: ""),
                        "courseInfo" to (data["courseInfo"] as? String ?: ""),
                        "positionName" to (data["positionName"] as? String ?: "")
                    )
                    onSuccess(details)
                } else {
                    onFailure("Candidate not found")
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error getting candidate details: ${exception.message}", exception)
                onFailure(exception.message ?: "Failed to get candidate details")
            }
    }

    /**
     * Get all positions for a specific election
     */
    fun getPositionsForElection(
        electionId: String,
        onSuccess: (List<VotingPosition>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        // First get unique position IDs
        firestore.collection(CANDIDATES_COLLECTION)
            .whereEqualTo("electionId", electionId)
            .whereEqualTo("isActive", true)
            .get()
            .addOnSuccessListener { documents ->
                // Group candidates by position
                val positionsMap = mutableMapOf<String, MutableList<CandidateChoices>>()
                val positionNamesMap = mutableMapOf<String, String>()

                documents.documents.forEach { doc ->
                    val data = doc.data ?: return@forEach
                    val positionId = data["positionId"] as? String ?: return@forEach
                    val positionName = data["positionName"] as? String ?: return@forEach
                    val candidateId = doc.id
                    val candidateName = data["name"] as? String ?: return@forEach

                    positionNamesMap[positionId] = positionName

                    if (!positionsMap.containsKey(positionId)) {
                        positionsMap[positionId] = mutableListOf()
                    }
                    positionsMap[positionId]?.add(CandidateChoices(id = candidateId, name = candidateName))
                }

                // Convert to VotingPosition list
                val positions = positionsMap.map { (positionId, candidates) ->
                    VotingPosition(
                        id = positionId,
                        title = positionNamesMap[positionId] ?: positionId,
                        candidates = candidates.sortedBy { it.name }
                    )
                }.sortedBy { it.title }

                onSuccess(positions)
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error getting positions: ${exception.message}", exception)
                onFailure(exception.message ?: "Failed to get positions")
            }
    }

    /**
     * Get candidate details for preview (Homepage)
     */
    fun getCandidatesForPreview(
        electionId: String,
        limit: Int = 5,
        onSuccess: (List<Candidate>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        firestore.collection(CANDIDATES_COLLECTION)
            .whereEqualTo("electionId", electionId)
            .whereEqualTo("isActive", true)
            .limit(limit.toLong())
            .get()
            .addOnSuccessListener { documents ->
                val candidates = documents.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    val name = data["name"] as? String ?: return@mapNotNull null
                    val position = data["positionName"] as? String ?: "Unknown"
                    // Use default drawable - can be enhanced with image URL later
                    Candidate(
                        name = name,
                        position = position,
                        photoResource = R.drawable.ic_profile
                    )
                }
                onSuccess(candidates)
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error getting candidates for preview: ${exception.message}", exception)
                onFailure(exception.message ?: "Failed to get candidates")
            }
    }

    /**
     * Get winning candidates for ended election
     */
    fun getWinningCandidates(
        electionId: String,
        onSuccess: (List<WinningCandidate>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        // Get winners from results collection or calculate from votes
        firestore.collection("results")
            .whereEqualTo("electionId", electionId)
            .whereEqualTo("isWinner", true)
            .orderBy("positionName", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    // If no results collection, calculate from votes
                    calculateWinnersFromVotes(electionId, onSuccess, onFailure)
                    return@addOnSuccessListener
                }

                val winners = documents.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    val name = data["candidateName"] as? String ?: return@mapNotNull null
                    val position = data["positionName"] as? String ?: "Unknown"
                    WinningCandidate(
                        name = name,
                        position = position,
                        photoResource = R.drawable.ic_profile
                    )
                }
                onSuccess(winners)
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error getting winning candidates: ${exception.message}", exception)
                // Fallback to calculating from votes
                calculateWinnersFromVotes(electionId, onSuccess, onFailure)
            }
    }

    /**
     * Calculate winners from votes collection
     */
    private fun calculateWinnersFromVotes(
        electionId: String,
        onSuccess: (List<WinningCandidate>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        firestore.collection("votes")
            .whereEqualTo("electionId", electionId)
            .get()
            .addOnSuccessListener { voteDocuments ->
                // Count votes by position and candidate
                val voteCounts = mutableMapOf<String, MutableMap<String, Int>>() // positionId -> (candidateId -> count)
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

                        if (!voteCounts.containsKey(positionId)) {
                            voteCounts[positionId] = mutableMapOf()
                        }
                        voteCounts[positionId]!![candidateId] = 
                            (voteCounts[positionId]!![candidateId] ?: 0) + 1
                    }
                }

                // Find winners (highest vote count per position)
                val winners = voteCounts.mapNotNull { (positionId, candidateCounts) ->
                    val winner = candidateCounts.maxByOrNull { it.value }
                    winner?.let {
                        WinningCandidate(
                            name = candidateNamesMap[it.key] ?: "Unknown",
                            position = positionNamesMap[positionId] ?: positionId,
                            photoResource = R.drawable.ic_profile
                        )
                    }
                }

                onSuccess(winners.sortedBy { it.position })
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error calculating winners: ${exception.message}", exception)
                onFailure(exception.message ?: "Failed to calculate winners")
            }
    }

    /**
     * Get candidate platform details
     */
    fun getCandidatePlatformDetails(
        candidateId: String,
        onSuccess: (CandidatePlatformDetails?) -> Unit,
        onFailure: (String) -> Unit
    ) {
        firestore.collection(CANDIDATES_COLLECTION)
            .document(candidateId)
            .get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    onSuccess(null)
                    return@addOnSuccessListener
                }

                val data = document.data ?: run {
                    onSuccess(null)
                    return@addOnSuccessListener
                }

                val details = CandidatePlatformDetails(
                    candidateId = document.id,
                    name = data["name"] as? String ?: "Unknown",
                    position = data["positionName"] as? String ?: "Unknown",
                    courseInfo = data["courseInfo"] as? String ?: "",
                    profilePictureResource = R.drawable.ic_profile,
                    credentials = data["credentials"] as? String ?: "",
                    advocacy = data["advocacy"] as? String ?: ""
                )

                onSuccess(details)
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error getting candidate details: ${exception.message}", exception)
                onFailure(exception.message ?: "Failed to get candidate details")
            }
    }
}

