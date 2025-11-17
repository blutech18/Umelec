package com.example.umelec

import android.util.Log
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
     * Determine the current election state based on dates
     */
    fun determineElectionState(
        onSuccess: (ElectionState) -> Unit,
        onFailure: (String) -> Unit
    ) {
        firestore.collection(ELECTIONS_COLLECTION)
            .whereEqualTo("isActive", true)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Log.d(TAG, "No active elections found")
                    onSuccess(ElectionState.NO_ELECTION)
                    return@addOnSuccessListener
                }

                // Get the most recent election by comparing startDate
                val mostRecentElection = documents.documents.maxByOrNull { doc ->
                    val timestamp = doc.getTimestamp("startDate")
                    timestamp?.toDate()?.time ?: 0L
                }

                if (mostRecentElection == null) {
                    Log.d(TAG, "No valid election found")
                    onSuccess(ElectionState.NO_ELECTION)
                    return@addOnSuccessListener
                }

                val now = Date()
                val startDateTimestamp = mostRecentElection.getTimestamp("startDate")
                val endDateTimestamp = mostRecentElection.getTimestamp("endDate")
                
                val startDate = startDateTimestamp?.toDate() ?: Date(Long.MAX_VALUE)
                val endDate = endDateTimestamp?.toDate() ?: Date(0)

                val state = when {
                    now.before(startDate) -> ElectionState.UPCOMING
                    now.after(endDate) -> ElectionState.ENDED
                    else -> ElectionState.ONGOING
                }

                Log.d(TAG, "Election state determined: $state (start: $startDate, end: $endDate)")
                onSuccess(state)
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error determining election state: ${exception.message}", exception)
                onFailure(exception.message ?: "Failed to determine election state")
            }
    }

    /**
     * Get current active election details
     */
    fun getCurrentElection(
        onSuccess: (ElectionDetails?) -> Unit,
        onFailure: (String) -> Unit
    ) {
        firestore.collection(ELECTIONS_COLLECTION)
            .whereEqualTo("isActive", true)
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

    /**
     * Get election ID for current active election
     */
    fun getCurrentElectionId(
        onSuccess: (String?) -> Unit,
        onFailure: (String) -> Unit
    ) {
        firestore.collection(ELECTIONS_COLLECTION)
            .whereEqualTo("isActive", true)
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

