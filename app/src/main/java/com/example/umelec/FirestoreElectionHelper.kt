package com.example.umelec

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.Date

/**
 * Helper class for Firestore Election operations
 */
object FirestoreElectionHelper {
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private const val ELECTIONS_COLLECTION = "elections"
    private const val TAG = "FirestoreElectionHelper"

    /**
     * Determine the current election state based on dates for user's college
     */
    fun determineElectionState(
        onSuccess: (ElectionState) -> Unit,
        onFailure: (String) -> Unit
    ) {
        // Get current user's college information first
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            onFailure("User not authenticated")
            return
        }

        firestore.collection("users").document(currentUser.uid)
            .get()
            .addOnSuccessListener { userDoc ->
                if (!userDoc.exists()) {
                    onFailure("User profile not found")
                    return@addOnSuccessListener
                }

                val userCollege = userDoc.getString("college") ?: ""
                if (userCollege.isEmpty()) {
                    onFailure("User college information not found")
                    return@addOnSuccessListener
                }

                // Get elections for user's college only
                Log.d(TAG, "Querying elections for college: $userCollege")
                firestore.collection(ELECTIONS_COLLECTION)
                    .whereEqualTo("isActive", true)
                    .whereEqualTo("college", userCollege)
                    .get()
                    .addOnSuccessListener { documents ->
                Log.d(TAG, "Found ${documents.size()} active election(s) for college: $userCollege")
                if (documents.isEmpty) {
                    Log.d(TAG, "No active elections found for college: $userCollege")
                    onSuccess(ElectionState.NO_ELECTION)
                    return@addOnSuccessListener
                }

                // Get the most recent election by comparing startDate
                val mostRecentElection = documents.documents.maxByOrNull { doc ->
                    val timestamp = doc.getTimestamp("startDate")
                    timestamp?.toDate()?.time ?: 0L
                }

                if (mostRecentElection == null) {
                    Log.d(TAG, "No valid election found (all elections missing startDate)")
                    onSuccess(ElectionState.NO_ELECTION)
                    return@addOnSuccessListener
                }

                val now = Date()
                val startDateTimestamp = mostRecentElection.getTimestamp("startDate")
                val endDateTimestamp = mostRecentElection.getTimestamp("endDate")
                
                val startDate = startDateTimestamp?.toDate() ?: Date(Long.MAX_VALUE)
                val endDate = endDateTimestamp?.toDate() ?: Date(0)
                
                val electionStatus = mostRecentElection.getString("status") ?: "unknown"
                Log.d(TAG, "Most recent election: ${mostRecentElection.id}, status: $electionStatus, startDate: $startDate, endDate: $endDate, now: $now")

                val state = when {
                    now.before(startDate) -> {
                        Log.d(TAG, "Election is UPCOMING (now is before startDate)")
                        ElectionState.UPCOMING
                    }
                    now.after(endDate) -> {
                        Log.d(TAG, "Election is ENDED (now is after endDate)")
                        ElectionState.ENDED
                    }
                    else -> {
                        Log.d(TAG, "Election is ONGOING (now is between startDate and endDate)")
                        ElectionState.ONGOING
                    }
                }

                        Log.d(TAG, "Election state determined: $state (start: $startDate, end: $endDate, now: $now)")
                        onSuccess(state)
                    }
                    .addOnFailureListener { exception ->
                        Log.e(TAG, "Error determining election state: ${exception.message}", exception)
                        onFailure(exception.message ?: "Failed to determine election state")
                    }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error getting user profile: ${exception.message}", exception)
                onFailure(exception.message ?: "Failed to get user profile")
            }
    }

    /**
     * Get current active election details for user's college
     */
    fun getCurrentElection(
        onSuccess: (ElectionDetails?) -> Unit,
        onFailure: (String) -> Unit
    ) {
        // Get current user's college information first
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            onFailure("User not authenticated")
            return
        }

        firestore.collection("users").document(currentUser.uid)
            .get()
            .addOnSuccessListener { userDoc ->
                if (!userDoc.exists()) {
                    onFailure("User profile not found")
                    return@addOnSuccessListener
                }

                val userCollege = userDoc.getString("college") ?: ""
                if (userCollege.isEmpty()) {
                    onFailure("User college information not found")
                    return@addOnSuccessListener
                }

                // Get elections for user's college only
                firestore.collection(ELECTIONS_COLLECTION)
                    .whereEqualTo("isActive", true)
                    .whereEqualTo("college", userCollege)
                    .get()
                    .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Log.d(TAG, "No active elections found for getCurrentElection")
                    onSuccess(null)
                    return@addOnSuccessListener
                }

                // Get the most recent election by comparing startDate
                val mostRecentDoc = documents.documents.maxByOrNull { doc ->
                    val timestamp = doc.getTimestamp("startDate")
                    timestamp?.toDate()?.time ?: 0L
                }

                if (mostRecentDoc == null) {
                    Log.d(TAG, "No valid election document found")
                    onSuccess(null)
                    return@addOnSuccessListener
                }

                val data = mostRecentDoc.data ?: run {
                    Log.d(TAG, "Election document has no data")
                    onSuccess(null)
                    return@addOnSuccessListener
                }

                val title = data["title"] as? String ?: ""
                val startDateTimestamp = mostRecentDoc.getTimestamp("startDate")
                val endDateTimestamp = mostRecentDoc.getTimestamp("endDate")
                
                val startDate = startDateTimestamp?.toDate() ?: Date()
                val endDate = endDateTimestamp?.toDate() ?: Date()
                
                val period = formatDateRange(startDate, endDate)
                val status = when {
                    Date().before(startDate) -> "Upcoming"
                    Date().after(endDate) -> "Ended"
                    else -> "Ongoing"
                }

                val electionDetails = ElectionDetails(
                    title = title,
                    period = period,
                    status = status
                )

                        Log.d(TAG, "Current election loaded: $title ($status)")
                        onSuccess(electionDetails)
                    }
                    .addOnFailureListener { exception ->
                        Log.e(TAG, "Error getting current election: ${exception.message}", exception)
                        onFailure(exception.message ?: "Failed to get election details")
                    }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error getting user profile: ${exception.message}", exception)
                onFailure(exception.message ?: "Failed to get user profile")
            }
    }

    /**
     * Get election ID for current active election in user's college
     */
    fun getCurrentElectionId(
        onSuccess: (String?) -> Unit,
        onFailure: (String) -> Unit
    ) {
        // Get current user's college information first
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            onFailure("User not authenticated")
            return
        }

        firestore.collection("users").document(currentUser.uid)
            .get()
            .addOnSuccessListener { userDoc ->
                if (!userDoc.exists()) {
                    onFailure("User profile not found")
                    return@addOnSuccessListener
                }

                val userCollege = userDoc.getString("college") ?: ""
                if (userCollege.isEmpty()) {
                    onFailure("User college information not found")
                    return@addOnSuccessListener
                }

                // Get elections for user's college only
                firestore.collection(ELECTIONS_COLLECTION)
                    .whereEqualTo("isActive", true)
                    .whereEqualTo("college", userCollege)
                    .get()
                    .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Log.d(TAG, "No active elections found for getCurrentElectionId")
                    onSuccess(null)
                    return@addOnSuccessListener
                }
                
                // Get the most recent election by comparing startDate
                val mostRecentDoc = documents.documents.maxByOrNull { doc ->
                    val timestamp = doc.getTimestamp("startDate")
                    timestamp?.toDate()?.time ?: 0L
                }
                
                        if (mostRecentDoc != null) {
                            Log.d(TAG, "Current election ID: ${mostRecentDoc.id}")
                            onSuccess(mostRecentDoc.id)
                        } else {
                            Log.d(TAG, "No valid election document found")
                            onSuccess(null)
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.e(TAG, "Error getting election ID: ${exception.message}", exception)
                        onFailure(exception.message ?: "Failed to get election ID")
                    }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error getting user profile: ${exception.message}", exception)
                onFailure(exception.message ?: "Failed to get user profile")
            }
    }

    /**
     * Check if user has already voted in current election
     */
    fun hasUserVoted(
        userId: String,
        electionId: String,
        onSuccess: (Boolean) -> Unit,
        onFailure: (String) -> Unit
    ) {
        firestore.collection("votes")
            .whereEqualTo("userId", userId)
            .whereEqualTo("electionId", electionId)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                onSuccess(!documents.isEmpty)
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error checking if user voted: ${exception.message}", exception)
                onFailure(exception.message ?: "Failed to check vote status")
            }
    }

    /**
     * Format date range as string
     */
    private fun formatDateRange(startDate: Date, endDate: Date): String {
        val format = java.text.SimpleDateFormat("MMMM dd, yyyy", java.util.Locale.getDefault())
        val startStr = format.format(startDate)
        val endStr = format.format(endDate)
        return "$startStr - $endStr"
    }
}

