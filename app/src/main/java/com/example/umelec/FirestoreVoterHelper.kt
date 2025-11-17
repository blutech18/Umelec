package com.example.umelec

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

/**
 * Helper class for Voter statistics and management operations
 */
object FirestoreVoterHelper {
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private const val USERS_COLLECTION = "users"
    private const val VOTES_COLLECTION = "votes"
    private const val TAG = "FirestoreVoterHelper"

    /**
     * Get total eligible voters count
     */
    fun getTotalEligibleVoters(
        onSuccess: (Int) -> Unit,
        onFailure: (String) -> Unit
    ) {
        firestore.collection(USERS_COLLECTION)
            .whereEqualTo("role", "VOTER")
            .get()
            .addOnSuccessListener { documents ->
                val count = documents.size()
                Log.d(TAG, "Total eligible voters: $count")
                onSuccess(count)
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error getting eligible voters: ${exception.message}", exception)
                onFailure(exception.message ?: "Failed to get eligible voters")
            }
    }

    /**
     * Get total voted count for an election
     */
    fun getTotalVoted(
        electionId: String,
        onSuccess: (Int) -> Unit,
        onFailure: (String) -> Unit
    ) {
        firestore.collection(VOTES_COLLECTION)
            .whereEqualTo("electionId", electionId)
            .get()
            .addOnSuccessListener { documents ->
                val count = documents.size()
                Log.d(TAG, "Total voted for election $electionId: $count")
                onSuccess(count)
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error getting voted count: ${exception.message}", exception)
                onFailure(exception.message ?: "Failed to get voted count")
            }
    }

    /**
     * Get voter statistics by year
     */
    fun getVoterStatisticsByYear(
        electionId: String,
        onSuccess: (Map<String, Int>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        // Get all votes for the election
        firestore.collection(VOTES_COLLECTION)
            .whereEqualTo("electionId", electionId)
            .get()
            .addOnSuccessListener { voteDocuments ->
                val userIds = voteDocuments.documents.mapNotNull { it.getString("userId") }.distinct()

                if (userIds.isEmpty()) {
                    onSuccess(emptyMap())
                    return@addOnSuccessListener
                }

                // Get user data for voters
                val yearCounts = mutableMapOf<String, Int>()
                var completed = 0
                val total = userIds.size

                userIds.forEach { userId ->
                    firestore.collection(USERS_COLLECTION)
                        .document(userId)
                        .get()
                        .addOnSuccessListener { userDoc ->
                            val yearRaw = userDoc.getString("year") ?: "Unknown"
                            // Normalize year format: "2nd Year" -> "2nd", "3rd Year" -> "3rd", etc.
                            val year = yearRaw.replace(" Year", "").trim()
                            yearCounts[year] = (yearCounts[year] ?: 0) + 1

                            completed++
                            if (completed == total) {
                                Log.d(TAG, "Year distribution: $yearCounts")
                                onSuccess(yearCounts)
                            }
                        }
                        .addOnFailureListener { exception ->
                            Log.e(TAG, "Error getting user data: ${exception.message}", exception)
                            completed++
                            if (completed == total) {
                                onSuccess(yearCounts)
                            }
                        }
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error getting votes: ${exception.message}", exception)
                onFailure(exception.message ?: "Failed to get voter statistics")
            }
    }

    /**
     * Get list of all voters
     */
    fun getAllVoters(
        onSuccess: (List<Map<String, Any>>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        Log.d(TAG, "Getting all voters...")
        firestore.collection(USERS_COLLECTION)
            .whereEqualTo("role", "VOTER")
            .get()
            .addOnSuccessListener { documents ->
                Log.d(TAG, "Found ${documents.size()} voters in Firestore")
                val voters = documents.documents.mapNotNull { doc ->
                    val data = doc.data?.toMutableMap() ?: run {
                        Log.w(TAG, "Voter document ${doc.id} has no data")
                        return@mapNotNull null
                    }
                    data["userId"] = doc.id
                    Log.d(TAG, "Voter: ${data["firstname"]} ${data["lastname"]} (ID: ${doc.id})")
                    data
                }
                // Sort client-side to avoid index requirement
                val sortedVoters = voters.sortedBy { it["firstname"] as? String ?: "" }
                Log.d(TAG, "Returning ${sortedVoters.size} sorted voters")
                onSuccess(sortedVoters)
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error getting voters: ${exception.message}", exception)
                onFailure(exception.message ?: "Failed to get voters")
            }
    }

    /**
     * Get voters who have voted in an election
     */
    fun getVotersWhoVoted(
        electionId: String,
        onSuccess: (List<Map<String, Any>>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        Log.d(TAG, "Getting voters who voted for election: $electionId")
        firestore.collection(VOTES_COLLECTION)
            .whereEqualTo("electionId", electionId)
            .get()
            .addOnSuccessListener { voteDocuments ->
                Log.d(TAG, "Found ${voteDocuments.size()} votes for election $electionId")
                val userIds = voteDocuments.documents.mapNotNull { doc ->
                    val userId = doc.getString("userId")
                    Log.d(TAG, "Vote ${doc.id} has userId: $userId")
                    userId
                }.distinct()
                
                Log.d(TAG, "Unique user IDs who voted: $userIds (${userIds.size} voters)")

                if (userIds.isEmpty()) {
                    Log.w(TAG, "No voters found for election $electionId")
                    onSuccess(emptyList())
                    return@addOnSuccessListener
                }

                val voters = mutableListOf<Map<String, Any>>()
                var completed = 0
                val total = userIds.size

                userIds.forEach { userId ->
                    firestore.collection(USERS_COLLECTION)
                        .document(userId)
                        .get()
                        .addOnSuccessListener { userDoc ->
                            if (userDoc.exists()) {
                                val data = userDoc.data?.toMutableMap() ?: mutableMapOf()
                                data["userId"] = userDoc.id
                                voters.add(data)
                                Log.d(TAG, "Added voter: ${data["firstname"]} ${data["lastname"]} (ID: $userId)")
                            } else {
                                Log.w(TAG, "User document not found for ID: $userId")
                            }

                            completed++
                            if (completed == total) {
                                Log.d(TAG, "Retrieved ${voters.size} voters who voted")
                                onSuccess(voters.sortedBy { it["firstname"] as? String ?: "" })
                            }
                        }
                        .addOnFailureListener { exception ->
                            Log.e(TAG, "Error getting user $userId: ${exception.message}", exception)
                            completed++
                            if (completed == total) {
                                Log.d(TAG, "Retrieved ${voters.size} voters who voted (with some errors)")
                                onSuccess(voters.sortedBy { it["firstname"] as? String ?: "" })
                            }
                        }
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error getting votes: ${exception.message}", exception)
                onFailure(exception.message ?: "Failed to get voters who voted")
            }
    }

    /**
     * Get voters who haven't voted in an election
     */
    fun getVotersWhoHaventVoted(
        electionId: String,
        onSuccess: (List<Map<String, Any>>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        // Get all voters
        getAllVoters({ allVoters ->
            // Get voters who voted
            getVotersWhoVoted(electionId, { votedVoters ->
                val votedIds = votedVoters.mapNotNull { it["userId"] as? String }.toSet()
                val notVoted = allVoters.filter { it["userId"] as? String !in votedIds }
                onSuccess(notVoted)
            }, onFailure)
        }, onFailure)
    }
}

