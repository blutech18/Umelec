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
            .orderBy("name", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val candidates = documents.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    val isActive = data["isActive"] as? Boolean ?: true
                    if (!isActive) return@mapNotNull null
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
            .get()
            .addOnSuccessListener { documents ->
                // Group candidates by position
                val positionsMap = mutableMapOf<String, MutableList<CandidateChoices>>()
                val positionNamesMap = mutableMapOf<String, String>()

                documents.documents.forEach { doc ->
                    val data = doc.data ?: return@forEach
                    val isActive = data["isActive"] as? Boolean ?: true
                    if (!isActive) return@forEach
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
            .limit(limit.toLong())
            .get()
            .addOnSuccessListener { documents ->
                val candidates = documents.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    val isActive = data["isActive"] as? Boolean ?: true
                    if (!isActive) return@mapNotNull null
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
        FirestoreVoteHelper.getVoteTallies(
            electionId = electionId,
            onSuccess = { tallies ->
                val winners = tallies
                    .groupBy { it.positionId }
                    .mapNotNull { (positionId, candidateTallies) ->
                        val topCandidate = candidateTallies.maxByOrNull { it.voteCount }
                        topCandidate?.let {
                            WinningCandidate(
                                name = it.candidateName,
                                position = it.positionName,
                                photoResource = R.drawable.ic_profile
                            )
                        }
                    }
                    .sortedBy { it.position }

                onSuccess(winners)
            },
            onFailure = { error ->
                Log.e(TAG, "Error calculating winners from tallies: $error")
                onFailure(error)
            }
        )
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

    /**
     * Update candidate profile information
     */
    fun updateCandidateProfile(
        candidateId: String,
        courseInfo: String,
        credentials: String,
        advocacy: String,
        photoUrl: String? = null,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        Log.d(TAG, "Updating candidate profile: $candidateId")
        
        val updateData = hashMapOf<String, Any>(
            "courseInfo" to courseInfo,
            "credentials" to credentials,
            "advocacy" to advocacy
        )

        // Add photo URL if provided
        photoUrl?.let {
            updateData["photoUrl"] = it
        }

        firestore.collection(CANDIDATES_COLLECTION)
            .document(candidateId)
            .update(updateData)
            .addOnSuccessListener {
                Log.d(TAG, "Successfully updated candidate profile: $candidateId")
                onSuccess()
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error updating candidate profile: ${exception.message}", exception)
                onFailure(exception.message ?: "Failed to update candidate profile")
            }
    }
}

